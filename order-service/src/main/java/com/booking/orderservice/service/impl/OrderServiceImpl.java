package com.booking.orderservice.service.impl;


import com.booking.orderservice.distributed.RedisDistributedLocker;
import com.booking.orderservice.distributed.RedisDistributedService;
import com.booking.orderservice.entity.Order;
import com.booking.orderservice.enums.OrderStatus;
import com.booking.orderservice.enums.OutboxStatus;
import com.booking.orderservice.event.TicketStockEvent;
import com.booking.orderservice.exception.BusinessException;
import com.booking.orderservice.exception.ErrorCode;
import com.booking.orderservice.exception.NotFoundException;
import com.booking.orderservice.outbox.entity.OutboxMessage;
import com.booking.orderservice.repository.OrderRepository;
import com.booking.orderservice.repository.http.EventClient;
import com.booking.orderservice.service.OrderService;
import com.booking.orderservice.service.OutboxService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean order(String ticketId, int quantity) {
        boolean isDecrement = eventClient.reserveStock(ticketId, quantity).getData();
        if (!isDecrement) {
            log.error("Decrease failed for ticket id {}", ticketId);
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
        try {
            // create order
            LocalDateTime now = LocalDateTime.now();
            //TODO get id from securityContextHolders
            String tempUserId = String.valueOf(UUID.randomUUID());
            String orderNumber = String.format("BNB-HN-%s-%s-%s", tempUserId, ticketId, System.currentTimeMillis());
            Order order = Order.builder()
                    .customerId(tempUserId)
                    .orderNumber(orderNumber)
                    .ticketId(ticketId)
                    .status(OrderStatus.PENDING)
                    .quantity(quantity)
                    .orderDate(now)
                    .updatedAt(now)
                    .createdAt(now)
                    .build();

            String jsonMsg = objectMapper.writeValueAsString(order);
            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .topic(PLACE_ORDER_TOPIC)
                    .aggregateType(Order.class.getSimpleName())
                    .payload(jsonMsg)
                    .eventType("placeOrderTopic")
                    .createdAt(LocalDateTime.now())
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxService.save(outboxMessage);
            return true;
        } catch (Exception e) {
            log.error("Decrease failed for ticket id {}, err: {}", ticketId, e.getMessage());
            buildErrorEvent(ticketId, quantity);
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void buildErrorEvent(String ticketId, int quantity) {
        TicketStockEvent event = TicketStockEvent.builder()
                .ticketId(ticketId)
                .quantity(quantity)
                .build();
        try {
            String jsonMsg = objectMapper.writeValueAsString(event);
            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .topic(TICKET_RESTOCK_TOPIC)
                    .aggregateType(TicketStockEvent.class.getSimpleName())
                    .payload(jsonMsg)
                    .eventType("ticketRestock")
                    .createdAt(LocalDateTime.now())
                    .status(OutboxStatus.PENDING)
                    .build();
            outboxService.save(outboxMessage);
        } catch (Exception e) {
            // noti admin
            log.error("Create restock event failed, event = {}", event);
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

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
