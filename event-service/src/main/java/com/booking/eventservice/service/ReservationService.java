package com.booking.eventservice.service;

import com.booking.common.events.OrderCancelledEvent;
import com.booking.common.events.OrderPaidEvent;
import com.booking.common.events.OrderPlacedEvent;
import com.booking.common.events.ReservationFailedEvent;
import com.booking.common.events.Topics;
import com.booking.common.outbox.OutboxWriter;
import com.booking.eventservice.entity.Reservation;
import com.booking.eventservice.enums.ReservationStatus;
import com.booking.eventservice.repository.ReservationRepository;
import com.booking.eventservice.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Database side of the booking saga - the source of truth for stock.
 * Every handler is idempotent and tolerant to out-of-order delivery, and runs inside the
 * consumer's transaction (MANDATORY) together with the inbox row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final OutboxWriter outboxWriter;

    public void reserve(OrderPlacedEvent event) {
        if (reservationRepository.findByOrderId(event.orderId()).isPresent()) {
            // already reserved, or a cancellation tombstone arrived first
            log.info("Reservation for order {} already exists, skipping", event.orderId());
            return;
        }
        int updated = ticketRepository.reservedStock(event.ticketId(), event.quantity());
        if (updated == 0) {
            // Redis admitted the order but the DB has no stock left (e.g. counter re-warmed
            // while orders were in flight): compensate instead of overselling
            log.warn("DB stock insufficient for order {} ticket {} qty {}",
                    event.orderId(), event.ticketId(), event.quantity());
            outboxWriter.append(Topics.RESERVATION_FAILED, event.ticketId(), event.orderId(),
                    new ReservationFailedEvent(event.orderId(), event.ticketId(), "OUT_OF_STOCK"));
            return;
        }
        reservationRepository.save(Reservation.builder()
                .orderId(event.orderId())
                .customerId(event.customerId())
                .ticketId(event.ticketId())
                .quantity(event.quantity())
                .status(ReservationStatus.RESERVED)
                .expiresAt(event.expiresAt())
                .createdBy(event.customerId())
                .build());
        log.info("Reserved {} of ticket {} for order {}", event.quantity(), event.ticketId(), event.orderId());
    }

    public void cancel(OrderCancelledEvent event) {
        Instant now = Instant.now();
        Optional<Reservation> reservation = reservationRepository.findByOrderId(event.orderId());
        if (reservation.isEmpty()) {
            // cancel overtook the OrderPlaced event (or the reservation failed): leave a
            // tombstone so a late OrderPlaced does not reserve stock for a dead order
            reservationRepository.save(Reservation.builder()
                    .orderId(event.orderId())
                    .customerId(event.customerId())
                    .ticketId(event.ticketId())
                    .quantity(event.quantity())
                    .status(ReservationStatus.CANCELLED)
                    .cancelReason(event.reason())
                    .cancelledAt(now)
                    .expiresAt(now)
                    .build());
            log.info("Tombstone written for cancelled order {}", event.orderId());
            return;
        }
        if (reservationRepository.cancel(event.orderId(), event.reason(), now) == 1) {
            Reservation r = reservation.get();
            if (ticketRepository.restock(r.getTicketId(), r.getQuantity()) == 0) {
                throw new IllegalStateException("Restock invariant violated for order " + event.orderId());
            }
            log.info("Released {} of ticket {} for order {} ({})",
                    r.getQuantity(), r.getTicketId(), event.orderId(), event.reason());
        }
    }

    public void confirm(OrderPaidEvent event) {
        Reservation reservation = reservationRepository.findByOrderId(event.orderId())
                // retried with back-off, then DLT: a paid order without reservation needs a human
                .orElseThrow(() -> new IllegalStateException("No reservation for paid order " + event.orderId()));
        if (reservationRepository.confirm(event.orderId(), Instant.now()) == 1) {
            if (ticketRepository.decreaseStock(reservation.getTicketId(), reservation.getQuantity()) == 0) {
                throw new IllegalStateException("Sold invariant violated for order " + event.orderId());
            }
            log.info("Confirmed order {}", event.orderId());
        } else if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            log.error("ALERT order {} paid after its reservation was cancelled ({}) - refund required",
                    event.orderId(), reservation.getCancelReason());
        }
    }
}
