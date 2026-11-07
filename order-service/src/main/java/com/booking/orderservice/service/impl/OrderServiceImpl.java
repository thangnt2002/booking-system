package com.booking.orderservice.service.impl;

import com.booking.common.events.CancelReason;
import com.booking.common.exceptions.BusinessException;
import com.booking.common.exceptions.ErrorCode;
import com.booking.common.exceptions.NotFoundException;
import com.booking.orderservice.clients.TicketCatalog;
import com.booking.orderservice.config.OrderProperties;
import com.booking.orderservice.dtos.CursorDTO;
import com.booking.orderservice.dtos.Page;
import com.booking.orderservice.dtos.request.PlaceOrderRequest;
import com.booking.orderservice.dtos.response.OrderResponseDTO;
import com.booking.orderservice.dtos.response.TicketResponseDTO;
import com.booking.orderservice.entities.Order;
import com.booking.orderservice.enums.OrderStatus;
import com.booking.orderservice.enums.TicketStatus;
import com.booking.orderservice.mapper.OrderMapper;
import com.booking.orderservice.repository.OrderRepository;
import com.booking.orderservice.service.CursorService;
import com.booking.orderservice.service.OrderService;
import com.booking.orderservice.service.OrderTxService;
import com.booking.orderservice.stock.RedisStockGate;
import com.booking.orderservice.stock.ReserveResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.booking.common.utils.UserUtils.getCurrentUserId;

/**
 * Checkout flow, optimised for flash-sale bursts:
 * <ol>
 *   <li>Idempotency: order id = UUIDv3(customer, Idempotency-Key) - retries return the same order.</li>
 *   <li>Validation against a locally cached ticket (no DB/remote hit per request).</li>
 *   <li>Atomic Redis reservation (Lua) - losers are rejected without touching SQL Server.</li>
 *   <li>One short DB transaction: INSERT order + INSERT outbox row. No remote call inside it.</li>
 *   <li>event-service consumes OrderPlaced and re-checks stock in the database (source of truth).</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_ID_NAMESPACE = "checkout:v1:";
    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final Set<TicketStatus> NOT_SELLABLE =
            EnumSet.of(TicketStatus.DRAFT, TicketStatus.CANCELLED, TicketStatus.EXPIRED);
    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final OrderTxService orderTxService;
    private final RedisStockGate stockGate;
    private final TicketCatalog ticketCatalog;
    private final CursorService cursorService;
    private final OrderMapper orderMapper;
    private final OrderProperties properties;

    @Override
    public OrderResponseDTO placeOrder(PlaceOrderRequest request, UUID idempotencyKey) {
        if (idempotencyKey == null) {
            throw new BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
        int quantity = request.quantity();
        if (quantity < 1 || quantity > properties.maxQuantityPerOrder()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        UUID customerId = requireCurrentUser();
        UUID orderId = orderIdFor(customerId, idempotencyKey);

        // 1. replay of an already completed request
        var existing = orderRepository.findById(orderId);
        if (existing.isPresent()) {
            return replay(existing.get(), request);
        }

        // 2. validate + price (cached, outside any transaction)
        Instant now = Instant.now();
        TicketResponseDTO ticket = ticketCatalog.getTicket(request.ticketId());
        BigDecimal unitPrice = sellablePrice(ticket, now);

        // 3. admission gate
        boolean ownsHold = reserve(request.ticketId(), orderId, quantity);

        // 4. persist order + outbox atomically
        Order order = Order.builder()
                .id(orderId)
                .orderNumber(orderNumber(orderId, now))
                .customerId(customerId)
                .ticketId(request.ticketId())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .status(OrderStatus.PENDING)
                .orderDate(now)
                .expireTime(now.plus(properties.paymentTimeout()))
                .createdBy(customerId)
                .build();
        try {
            Order saved = orderTxService.createPending(order);
            log.info("Order placed id={} ticket={} qty={}", orderId, request.ticketId(), quantity);
            return orderMapper.toResponse(saved);
        } catch (DataIntegrityViolationException duplicate) {
            // a concurrent request with the same Idempotency-Key committed first; it owns the hold
            return orderRepository.findById(orderId)
                    .map(orderMapper::toResponse)
                    .orElseThrow(() -> new BusinessException(ErrorCode.REQUEST_IN_PROGRESS));
        } catch (RuntimeException e) {
            if (ownsHold) {
                stockGate.release(request.ticketId(), orderId);
            }
            throw e;
        }
    }

    @Override
    public OrderResponseDTO getMyOrder(UUID orderId) {
        return orderMapper.toResponse(findOwnedOrder(orderId));
    }

    @Override
    public void cancelMyOrder(UUID orderId) {
        Order order = findOwnedOrder(orderId);
        orderTxService.cancel(order, CancelReason.USER_CANCELLED);
        log.info("Order {} cancelled by customer", orderId);
    }

    @Override
    public void confirmPayment(UUID orderId) {
        orderTxService.markPaid(orderId);
        log.info("Order {} paid", orderId);
    }

    @Override
    public Page<OrderResponseDTO> getMyOrders(String cursor, int limit, String search) {
        UUID customerId = requireCurrentUser();
        int pageSize = Math.clamp(limit, 1, MAX_PAGE_SIZE);
        PageRequest fetch = PageRequest.of(0, pageSize + 1); // one extra row tells us if there is a next page
        String pattern = "%" + escapeLike(search == null ? "" : search.trim()) + "%";

        CursorDTO position = cursorService.parseCursor(cursor);
        List<Order> rows = position.getId() == null
                ? orderRepository.findFirstPage(customerId, pattern, fetch)
                : orderRepository.findPageAfter(customerId, pattern, position.getDate(),
                UUID.fromString(position.getId()), fetch);

        boolean hasNext = rows.size() > pageSize;
        List<Order> page = hasNext ? rows.subList(0, pageSize) : rows;
        String nextCursor = null;
        if (hasNext) {
            Order last = page.getLast();
            nextCursor = cursorService.generateCursor(last.getId().toString(), last.getCreatedDate());
        }
        return Page.<OrderResponseDTO>builder()
                .data(page.stream().map(orderMapper::toResponse).toList())
                .cursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    /** @return true if this call took the hold (and must give it back on failure) */
    private boolean reserve(UUID ticketId, UUID orderId, int quantity) {
        ReserveResult result = callGate(ticketId, orderId, quantity);
        if (result == ReserveResult.NOT_WARMED) {
            ticketCatalog.warmStock(ticketId);
            result = callGate(ticketId, orderId, quantity);
        }
        return switch (result) {
            case RESERVED -> true;
            // same order id already holds stock: an earlier attempt crashed before commit, or a
            // concurrent duplicate is in flight. Continue - the PK on orders decides the winner.
            case ALREADY_HELD -> false;
            case OUT_OF_STOCK -> throw new BusinessException(ErrorCode.OUT_OF_STOCK);
            case NOT_WARMED -> throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        };
    }

    private ReserveResult callGate(UUID ticketId, UUID orderId, int quantity) {
        try {
            return stockGate.reserve(ticketId, orderId, quantity);
        } catch (DataAccessException e) {
            // fail closed: never sell without the gate
            log.error("Redis unavailable while reserving ticket {}", ticketId, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private OrderResponseDTO replay(Order existing, PlaceOrderRequest request) {
        boolean samePayload = existing.getTicketId().equals(request.ticketId())
                && existing.getQuantity().equals(request.quantity());
        if (!samePayload) {
            // the key was reused for a different purchase
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        return orderMapper.toResponse(existing);
    }

    private BigDecimal sellablePrice(TicketResponseDTO ticket, Instant now) {
        boolean statusOk = ticket.getStatus() == null || !NOT_SELLABLE.contains(ticket.getStatus());
        boolean started = ticket.getSaleStartTime() == null || !now.isBefore(ticket.getSaleStartTime());
        boolean notEnded = ticket.getSaleEndTime() == null || now.isBefore(ticket.getSaleEndTime());
        if (!statusOk || !started || !notEnded) {
            throw new BusinessException(ErrorCode.TICKET_NOT_ON_SALE);
        }
        BigDecimal flash = ticket.getFlashPrice();
        BigDecimal price = flash != null && flash.signum() > 0 ? flash : ticket.getOriginalPrice();
        if (price == null || price.signum() < 0) {
            throw new BusinessException(ErrorCode.TICKET_NOT_ON_SALE);
        }
        return price;
    }

    private Order findOwnedOrder(UUID orderId) {
        UUID customerId = requireCurrentUser();
        // same 404 for "missing" and "not yours" so order ids cannot be probed
        return orderRepository.findById(orderId)
                .filter(order -> order.getCustomerId().equals(customerId))
                .orElseThrow(() -> new NotFoundException(ErrorCode.DATA_NOT_FOUND));
    }

    private static UUID requireCurrentUser() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UN_AUTHORIZE);
        }
        return userId;
    }

    private static UUID orderIdFor(UUID customerId, UUID idempotencyKey) {
        String seed = ORDER_ID_NAMESPACE + customerId + ":" + idempotencyKey;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    /** Human-friendly, deterministic per order id (so retries produce the same number). */
    private static String orderNumber(UUID orderId, Instant now) {
        String suffix = Long.toString(orderId.getMostSignificantBits() & Long.MAX_VALUE, 36)
                .toUpperCase(Locale.ROOT);
        return "ORD-" + ORDER_DATE.format(now) + "-" + suffix;
    }

    private static String escapeLike(String value) {
        return value.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }
}
