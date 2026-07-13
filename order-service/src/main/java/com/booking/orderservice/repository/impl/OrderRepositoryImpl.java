package com.booking.orderservice.repository.impl;


import com.booking.orderservice.entity.Order;
import com.booking.orderservice.enums.OrderStatus;
import com.booking.orderservice.exception.BusinessException;
import com.booking.orderservice.exception.ErrorCode;
import com.booking.orderservice.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        String sql = "INSERT INTO " + tableName + " (id, order_number, customer_id, order_date," +
                "status, ticket_id, quantity, price, updated_at, created_at)" +
                "VALUES (:id, :orderNumber, :customerId, :orderDate, :status, :ticketId, :quantity, :price, " +
                ":updatedAt, :createdAt)";

        entityManager.createNativeQuery(sql)
                .setParameter("id", order.getId() != null ? order.getId() : UUID.randomUUID().toString())
                .setParameter("orderNumber", order.getOrderNumber())
                .setParameter("customerId", order.getCustomerId())
                .setParameter("orderDate", order.getOrderDate())
                .setParameter("status", order.getStatus().name())
                .setParameter("ticketId", order.getTicketId())
                .setParameter("quantity", order.getQuantity())
                .setParameter("price", order.getPrice())
                .setParameter("updatedAt", order.getUpdatedAt())
                .setParameter("createdAt", order.getCreatedAt())
                .executeUpdate();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
                throw new BusinessException(ErrorCode.SERVER_ERROR);
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

    private static final String CREATE_TABLE_TEMPLATE = """
        IF OBJECT_ID(N'%1$s', N'U') IS NULL
        BEGIN
            CREATE TABLE %1$s (
                id              NVARCHAR(36)     NOT NULL PRIMARY KEY,
                order_number    NVARCHAR(100)     NOT NULL,
                customer_id     NVARCHAR(36)     NOT NULL,
                order_date      DATETIME2        NOT NULL,
                status          NVARCHAR(30)     NOT NULL,
                ticket_id       NVARCHAR(36)     NOT NULL,
                quantity        INT              NOT NULL,
                price           DECIMAL(18,2)    NOT NULL,
                updated_at      DATETIME2        NULL,
                created_at      DATETIME2        NOT NULL DEFAULT SYSUTCDATETIME(),
                CONSTRAINT UQ_%1$s_order_number UNIQUE (order_number)
            )
        END
        """;
}
