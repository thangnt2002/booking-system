package com.booking.orderservice.service.impl;


import com.booking.common.services.StockViewService;
import com.booking.orderservice.distributed.RedisDistributedLocker;
import com.booking.orderservice.distributed.RedisDistributedService;
import com.booking.orderservice.dtos.CursorDTO;
import com.booking.orderservice.dtos.Page;
import com.booking.orderservice.dtos.response.OrderResponseDTO;
import com.booking.orderservice.dtos.response.TicketResponseDTO;
import com.booking.orderservice.entities.Order;
import com.booking.orderservice.enums.OrderStatus;
import com.booking.orderservice.enums.OutboxStatus;
import com.booking.orderservice.event.TicketStockEvent;
import com.booking.common.exceptions.BusinessException;
import com.booking.common.exceptions.ErrorCode;
import com.booking.common.exceptions.NotFoundException;
import com.booking.orderservice.mapper.OrderMapper;
import com.booking.orderservice.entities.OutboxMessage;
import com.booking.orderservice.repository.OrderRepository;
import com.booking.orderservice.clients.EventClient;
import com.booking.orderservice.service.CursorService;
import com.booking.orderservice.service.OrderService;
import com.booking.orderservice.service.OutboxService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.booking.common.utils.UserUtils.getCurrentUserId;
import static com.booking.orderservice.common.Constant.*;
import static com.booking.orderservice.common.Utils.genRequestLockKey;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderServiceImpl implements OrderService {
    OrderRepository orderRepository;
    RedisDistributedService redisDistributedService;
    EventClient eventClient;
    OutboxService outboxService;
    CursorService cursorService;
    ObjectMapper objectMapper;
    OrderMapper orderMapper;
    StockViewService stockViewService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean order(UUID ticketId, int quantity) {
        // check redis
        Long remaining = stockViewService.getRemaining(ticketId);
        if (remaining != null && remaining < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }

        try {
         BigDecimal effectivePrice = getEffectivePrice(ticketId);
            // create order
            LocalDateTime now = LocalDateTime.now();
            UUID userId = getCurrentUserId();
            String orderNumber = String.format("BNB-HN-%s-%s-%s", userId, ticketId, System.currentTimeMillis());
            Order order = Order.builder()
                    .customerId(userId)
                    .orderNumber(orderNumber)
                    .ticketId(ticketId)
                    .status(OrderStatus.PENDING)
                    .quantity(quantity)
                    .price(effectivePrice)
                    .orderDate(now)
                    .updatedAt(now)
                    .createdAt(now)
                    .build();

            TicketStockEvent ticketDecreaseStockEvent = new TicketStockEvent(ticketId, quantity);

            String jsonMsg = objectMapper.writeValueAsString(ticketDecreaseStockEvent);
            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .topic(TICKET_DECREASE_STOCK_TOPIC)
                    .aggregateType(TicketStockEvent.class.getSimpleName())
                    .payload(jsonMsg)
                    .eventType("decreaseStock")
                    .createdAt(Instant.now())
                    .status(OutboxStatus.PENDING)
                    .build();

            String tableName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            orderRepository.insertOrder(tableName, order);

            outboxService.save(outboxMessage);
            return true;
        } catch (Exception e) {
            log.error("Decrease failed for ticket id {}, err: {}", ticketId, e.getMessage());
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

    private BigDecimal getEffectivePrice(UUID ticketId) {
        TicketResponseDTO ticket = eventClient.findTicketById(ticketId).getData();
        if (ticket == null) return BigDecimal.valueOf(-1);
        if (ticket.getFlashPrice() != null && ticket.getFlashPrice().compareTo(BigDecimal.valueOf(0)) > 0) {
            return ticket.getOriginalPrice();
        }
        return ticket.getOriginalPrice() != null ? ticket.getOriginalPrice() : BigDecimal.valueOf(-1);
    }

//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void buildErrorEvent(String ticketId, int quantity) {
//        TicketStockEvent event = TicketStockEvent.builder()
//                .ticketId(ticketId)
//                .quantity(quantity)
//                .build();
//        try {
//            String jsonMsg = objectMapper.writeValueAsString(event);
//            OutboxMessage outboxMessage = OutboxMessage.builder()
//                    .topic(TICKET_RESTOCK_TOPIC)
//                    .aggregateType(TicketStockEvent.class.getSimpleName())
//                    .payload(jsonMsg)
//                    .eventType("ticketRestock")
//                    .createdAt(Instant.now())
//                    .status(OutboxStatus.PENDING)
//                    .build();
//            outboxService.save(outboxMessage);
//        } catch (Exception e) {
//            // noti admin
//            log.error("Create restock event failed, event = {}", event);
//            throw new BusinessException(ErrorCode.SERVER_ERROR);
//        }
//    }

    @Override
    @Transactional
    public boolean cancelOrder(String orderNumber) {
        // update order status
        log.info("cancel order: {}", orderNumber);
        String lockKey = genRequestLockKey(Order.class, orderNumber);
        RedisDistributedLocker locker = redisDistributedService.getDistributedLock(lockKey);
        try {
            boolean isLock = locker.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLock) {
                log.info("Order {} lock is occupied, please wait to release lock... ", orderNumber);
                return false;
            }
            log.info("lock success");
            String yearMonth = extractYearMonthFromOrder(orderNumber);
            Order order = orderRepository.findByOrderNumber(yearMonth, orderNumber);
            if (order == null) {
                log.error("order number {} not found", orderNumber);
                throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
            }

            if (order.getStatus().equals(OrderStatus.CANCELLED)
                    || order.getStatus().equals(OrderStatus.EXPIRED)
                    || order.getStatus().equals(OrderStatus.REFUNDED)
            ) {
                log.info("Cannot cancel, current status is {}", order.getStatus());
                return false;
            }
            // change db status
            boolean changeOrderStatus = orderRepository.changeStatus(yearMonth, orderNumber, OrderStatus.CANCELLED);
            if (!changeOrderStatus) {
                log.error("Change order status failed for order {}", orderNumber);
                return false;
            }

            boolean isRestockSuccess = eventClient.releaseStock(order.getTicketId(), order.getQuantity()).getData();
            if (!isRestockSuccess) {
                log.error("Restock failed, cancel failed for order {}", orderNumber);
                throw new BusinessException(ErrorCode.SERVER_ERROR);
            }
            log.info("Cancel order successfully!");
            return true;
        } catch (InterruptedException e) {
            log.error("cancel order failed {} err {}", orderNumber, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            locker.unlock();
        }
    }

    @Override
    public Page<OrderResponseDTO> getPage(String userId, String table, String cursor, int limit, String search) {
        int safeLimit = Math.min(limit, 50) + 1;
        CursorDTO cursorDTO = cursorService.parseCursor(cursor);
        List<Order> orders = orderRepository.findCursorPage(userId, cursorDTO.getId(), table, cursorDTO.getDate(), safeLimit, search);

        if (orders.isEmpty()) {
            return Page.<OrderResponseDTO>builder()
                    .data(List.of())
                    .build();
        }

        boolean hasNext = false;
        String nextCursor = StringUtils.EMPTY;
        if (orders.size() == safeLimit) {
            orders.removeLast();
            hasNext = true;
        }
        if(hasNext) {
            Order lastOrder = orders.getLast();
            nextCursor = cursorService.generateCursor(lastOrder.getId().toString(), lastOrder.getCreatedAt());
        }
        List<OrderResponseDTO> orderResponses = new ArrayList<>(orders
                .stream()
                .map(orderMapper::toResponse)
                .toList());
        return Page.<OrderResponseDTO>builder()
                .data(orderResponses)
                .cursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    private String extractYearMonthFromOrder(String orderNumber) {
        try {
            String[] parts = orderNumber.split("-");
            if (parts.length != 5) {
                throw new BusinessException(ErrorCode.INVALID_TIME_FORMAT);
            }
            long timeStamp = Long.parseLong(parts[parts.length - 1]);
            LocalDateTime date = Instant.ofEpochMilli(timeStamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            return date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        } catch (Exception e) {
            log.error("failed to extract year month: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
