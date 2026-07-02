package com.booking.eventservice.repository.impl;

import com.booking.eventservice.entity.Order;
import com.booking.eventservice.enums.OrderStatus;
import com.booking.eventservice.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level =  AccessLevel.PRIVATE, makeFinal = true)
public class OrderRepositoryImpl implements OrderRepository {

    EntityManager entityManager;

    final static Map<String, Boolean> tableCreatedCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void insertOrder(String yearMonthFormat, Order order) {
        String tableName = createTableName(yearMonthFormat);
        ensureTableExist(tableName);
        String sql = "INSERT INTO " + tableName + " (order_number, customer_id, order_date," +
                "status, ticket_id, quantity, price, updated_at, created_at)" +
                "VALUES (:orderNumber, :customerId, :orderDate, :status, :ticketId, :quantity, :price, " +
                ":updatedAt, :createdAt)";

        entityManager.createNativeQuery(sql)
                .setParameter("order_number", order.getOrderNumber())
                .setParameter("customerId", order.getCustomerId())
                .setParameter("orderDate", order.getOrderDate())
                .setParameter("status", order.getStatus())
                .setParameter("ticketId", order.getTicketId())
                .setParameter("quantity", order.getQuantity())
                .setParameter("price", order.getPrice())
                .setParameter("updatedAt", order.getUpdatedAt())
                .setParameter("createdAt", order.getCreatedAt())
                .executeUpdate();
    }

    @Transactional
    void ensureTableExist(String tableName){
        if(tableCreatedCache.containsKey(tableName)){
            return;
        }
        synchronized (tableCreatedCache){
            if(tableCreatedCache.containsKey(tableName)){
                return;
            }
            try {
                String createTableQuery = String.format(CREATE_TABLE_TEMPLATE, tableName);
                entityManager.createNativeQuery(createTableQuery).executeUpdate();
                tableCreatedCache.put(tableName, true);
            }catch (Exception e){
                log.error("create table err for name: {}", tableName);
            }
        }
    }

    @Override
    public List<Object> findAll() {
        return List.of();
    }

    @Override
    public Order findByOrderNumber(String yearMonth, String orderNumber) {
        String sql = "SELECT * FROM " + createTableName(yearMonth) + " WHERE orderNumber = :orderNumber";
        List<Object[]> result = entityManager.createNativeQuery(sql)
                .setParameter("orderNumber", orderNumber)
                .getResultList();
        if(result.isEmpty()) return null;
        Object[] row = result.getFirst();
        return new Order(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                (LocalDateTime) row[3],
                (OrderStatus) row[4],
                (String) row[5],
                (Integer) row [6],
                (BigDecimal) row [7],
                (LocalDateTime) row[8],
                (LocalDateTime) row[9]
        );
    }

    @Override
    public boolean changeStatus(String yearMonth, String orderNumber, OrderStatus status) {
        String query = "UPDATE " + createTableName(yearMonth) + " o SET o.updated_at = CURRENT_TIMESTAMP " +
                "o.status = :status WHERE o.order_number = :orderNumber";
        int result = entityManager.createNativeQuery(query)
                .setParameter("status", status)
                .setParameter("orderNumber", orderNumber)
                .executeUpdate();
        return result > 0;
    }

    private String createTableName(String yearMonth){
        return String.format("ticket_order_%s", yearMonth);
    }

    private static final String CREATE_TABLE_TEMPLATE = "%s";
}
