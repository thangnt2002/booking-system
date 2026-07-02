package com.booking.eventservice.service.impl;

import com.booking.eventservice.cache.TicketCacheService;
import com.booking.eventservice.distributed.RedisDistributedLocker;
import com.booking.eventservice.distributed.RedisDistributedService;
import com.booking.eventservice.entity.Order;
import com.booking.eventservice.entity.Ticket;
import com.booking.eventservice.enums.OrderStatus;
import com.booking.eventservice.exception.BusinessException;
import com.booking.eventservice.exception.ErrorCode;
import com.booking.eventservice.exception.NotFoundException;
import com.booking.eventservice.repository.OrderRepository;
import com.booking.eventservice.repository.TicketRepository;
import com.booking.eventservice.service.OrderService;
import com.booking.eventservice.service.TicketService;
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

import static com.booking.eventservice.common.Utils.genRequestLockKey;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderServiceImpl implements OrderService {
//TODO SPLIT TO ORDER SERVICE
    TicketRepository ticketRepository;
    OrderRepository ticketOrderRepository;
    TicketCacheService ticketCacheService;
    TicketService ticketService;
    private final RedisDistributedService redisDistributedService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean order(String ticketId, int quantity) {
        boolean isRedisDecremented = false;
        try {
            int deductionResult = ticketCacheService.decreaseStock(ticketId, quantity);
            if (deductionResult == -1) {
                log.info("decrease stock: cache miss for ticketId={}", ticketId);
                // TODO add stock available to cache
                // decrease after add stock to cache
                deductionResult = ticketCacheService.decreaseStock(ticketId, quantity);
            }
            if (deductionResult == 0) {
                log.info("Redis stock insufficient for ticketId={}", ticketId);
                return false;
            }
            isRedisDecremented = true;
            // decrease in cache ok -> sync to db
            boolean isDbDecremented = ticketRepository.decreaseStock(ticketId, quantity);

            if (!isDbDecremented) {
                ticketCacheService.increaseStock(ticketId, quantity);
                log.info("DecreaseStock failed for ticketId={}, quantity = {}", ticketId, quantity);
                return false;
            }

            // create order
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
            ticketOrderRepository.insertOrder(tableName, order);
            log.info("DecreaseStock success for ticketId={}, quantity = {}", ticketId, quantity);
            return true;
        } catch (PessimisticLockException e) {
            if (isRedisDecremented) {
                ticketCacheService.increaseStock(ticketId, quantity);
            }
            log.error("PessimisticLockException ticketId={}, quantity = {}", ticketId, quantity);
            return false;
        } catch (LockTimeoutException e) {
            if (isRedisDecremented) {
                ticketCacheService.increaseStock(ticketId, quantity);
            }
            log.error("LockTimeoutException ticketId={}, quantity = {}", ticketId, quantity);
            return false;
        } catch (Exception e) {
            if (isRedisDecremented) {
                ticketCacheService.increaseStock(ticketId, quantity);
            }
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
            Order order = ticketOrderRepository.findByOrderNumber(yearMonth, orderNumber);
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
            boolean changeOrderStatus = ticketOrderRepository.changeStatus(yearMonth, orderNumber, OrderStatus.CANCELLED);
            if (!changeOrderStatus) {
                log.error("Change order status failed for order {}", orderNumber);
                return false;
            }

            boolean restock = ticketRepository.restock(order.getTicketId(), order.getQuantity());
            if (!restock) {
                log.error("Restock in db failed for order {}, ticket = {}, quantity = {}", orderNumber, order.getTicketId(), order.getQuantity());
                return false;
            }
            boolean cacheRestock = ticketCacheService.increaseStock(order.getTicketId(), order.getQuantity());
            if (!cacheRestock) {
                log.error("Restock in cache failed for order {}, ticket = {}, quantity = {}", orderNumber, order.getTicketId(), order.getQuantity());
                return false;
            }
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

    private BigDecimal getEffectivePrice(String ticketId) {
        Ticket ticket = ticketService.findById(ticketId, null);
        if (ticket == null) return BigDecimal.valueOf(-1);
        if (ticket.getFlashPrice() != null && ticket.getFlashPrice().compareTo(BigDecimal.valueOf(0)) > 0) {
            return ticket.getOriginalPrice();
        }
        return ticket.getOriginalPrice() != null ? ticket.getOriginalPrice() : BigDecimal.valueOf(-1);
    }

}
