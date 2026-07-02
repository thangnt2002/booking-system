package com.booking.orderservice.service.impl;


import com.booking.orderservice.distributed.RedisDistributedLocker;
import com.booking.orderservice.distributed.RedisDistributedService;
import com.booking.orderservice.distributed.RedisInfraService;
import com.booking.orderservice.entity.Order;
import com.booking.orderservice.enums.OrderStatus;
import com.booking.orderservice.exception.BusinessException;
import com.booking.orderservice.exception.ErrorCode;
import com.booking.orderservice.exception.NotFoundException;
import com.booking.orderservice.repository.OrderRepository;
import com.booking.orderservice.service.OrderService;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.booking.orderservice.common.Utils.genRequestLockKey;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderServiceImpl implements OrderService {
//TODO SPLIT TO ORDER SERVICE

    OrderRepository orderRepository;
    RedisDistributedService redisDistributedService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean order(String ticketId, int quantity) {
        try{

        //TODO call to ticket service to decrease stock internal/tickets/decrement/{ticketId}/{quantity}

        // create order
            // TODO Check ticket here
        BigDecimal price = getEffectivePrice(ticketId);
        LocalDateTime now = LocalDateTime.now();
        //TODO get id from securityContextHolders
        String tempUserId = String.valueOf(UUID.randomUUID());
        String orderNumber = String.format("BNB-HN-%s-%s-%s", tempUserId, ticketId, System.currentTimeMillis());
        Order order = Order.builder()
                .customerId(tempUserId)
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING)
                .price(price)
                .quantity(quantity)
                .orderDate(now)
                .updatedAt(now)
                .createdAt(now)
                .build();
        String tableName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        orderRepository.insertOrder(tableName, order);
        log.info("DecreaseStock success for ticketId={}, quantity = {}", ticketId, quantity);
            return true;
        } catch (Exception e) {
            // TODO call to ticket service to restock
            log.error("Exception ticketId={}, quantity = {}", ticketId, quantity);
            return false;
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

            // TODO call ticket service to increase stock in cache and db

//            boolean restock = ticketRepository.restock(order.getTicketId(), order.getQuantity());
//            if (!restock) {
//                log.error("Restock in db failed for order {}, ticket = {}, quantity = {}", orderNumber, order.getTicketId(), order.getQuantity());
//                return false;
//            }
//            boolean cacheRestock = ticketCacheService.increaseStock(order.getTicketId(), order.getQuantity());
//            if (!cacheRestock) {
//                log.error("Restock in cache failed for order {}, ticket = {}, quantity = {}", orderNumber, order.getTicketId(), order.getQuantity());
//                return false;
//            }
            log.info("Cancel order successfully!");
            return true;
        } catch (InterruptedException e) {
            log.error("cancel order {} err {}", orderNumber, e.getMessage());
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
