package com.booking.orderservice.service;

import com.booking.common.events.CancelReason;
import com.booking.common.events.OrderCancelledEvent;
import com.booking.common.events.OrderPaidEvent;
import com.booking.common.events.OrderPlacedEvent;
import com.booking.common.events.ReservationFailedEvent;
import com.booking.common.events.Topics;
import com.booking.common.exceptions.BusinessException;
import com.booking.common.exceptions.ErrorCode;
import com.booking.common.exceptions.NotFoundException;
import com.booking.common.outbox.OutboxWriter;
import com.booking.orderservice.entities.Order;
import com.booking.orderservice.enums.OrderStatus;
import com.booking.orderservice.repository.OrderRepository;
import com.booking.orderservice.stock.RedisStockGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.booking.orderservice.support.TxHooks.afterCommit;

/**
 * Transaction boundaries of the order aggregate. Every method changes the order and
 * appends its event to the outbox in ONE database transaction; Redis side effects run
 * only after commit. Kept as a separate bean so {@code @Transactional} goes through the
 * Spring proxy and remote calls stay outside the transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTxService {

    private final OrderRepository orderRepository;
    private final OutboxWriter outboxWriter;
    private final RedisStockGate stockGate;

    @Transactional
    public Order createPending(Order order) {
        // flush now so a duplicate id surfaces here as DataIntegrityViolationException
        Order saved = orderRepository.saveAndFlush(order);
        outboxWriter.append(Topics.ORDER_PLACED, saved.getTicketId(), saved.getId(),
                new OrderPlacedEvent(saved.getId(), saved.getCustomerId(), saved.getTicketId(),
                        saved.getQuantity(), saved.getExpireTime()));
        return saved;
    }

    @Transactional
    public void cancel(Order order, CancelReason reason) {
        int updated = orderRepository.transition(order.getId(), OrderStatus.PENDING, OrderStatus.CANCELLED, Instant.now());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        publishCancelled(order, reason);
    }

    @Transactional
    public void markPaid(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.DATA_NOT_FOUND));
        if (order.getStatus() == OrderStatus.SUCCESS) {
            return; // payment callbacks are retried by providers: idempotent no-op
        }
        if (orderRepository.markPaid(orderId, Instant.now()) == 0) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATE);
        }
        outboxWriter.append(Topics.ORDER_PAID, order.getTicketId(), order.getId(),
                new OrderPaidEvent(order.getId(), order.getTicketId(), order.getQuantity()));
        afterCommit(() -> stockGate.consume(order.getTicketId(), order.getId()));
    }

    /** @return number of orders expired in this batch */
    @Transactional
    public int expireBatch(Instant now, int limit) {
        List<Order> overdue = orderRepository.lockExpired(now, limit);
        for (Order order : overdue) {
            order.setStatus(OrderStatus.EXPIRED); // rows are UPDLOCKed: plain dirty checking is safe
            publishCancelled(order, CancelReason.EXPIRED);
        }
        return overdue.size();
    }

    /** The database could not back the Redis reservation: fail the order and return the hold. */
    @Transactional
    public void failReservation(ReservationFailedEvent event) {
        int updated = orderRepository.transition(event.orderId(), OrderStatus.PENDING, OrderStatus.FAILED, Instant.now());
        if (updated == 1) {
            afterCommit(() -> stockGate.release(event.ticketId(), event.orderId()));
            return;
        }
        orderRepository.findById(event.orderId()).ifPresentOrElse(
                order -> {
                    if (order.getStatus() == OrderStatus.SUCCESS) {
                        // paid before the reservation result arrived: money must be returned
                        log.error("ALERT order {} is paid but has no DB reservation ({}) - refund required",
                                order.getId(), event.reason());
                    } else {
                        log.info("Reservation failed for order {} already in status {}", order.getId(), order.getStatus());
                    }
                },
                () -> log.warn("Reservation failed for unknown order {}", event.orderId()));
    }

    private void publishCancelled(Order order, CancelReason reason) {
        outboxWriter.append(Topics.ORDER_CANCELLED, order.getTicketId(), order.getId(),
                new OrderCancelledEvent(order.getId(), order.getCustomerId(), order.getTicketId(),
                        order.getQuantity(), reason));
        afterCommit(() -> stockGate.release(order.getTicketId(), order.getId()));
    }
}
