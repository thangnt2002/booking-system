package com.booking.eventservice.service.impl;

import com.booking.eventservice.entity.Order;
import com.booking.eventservice.repository.TicketOrderRepository;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level =  AccessLevel.PRIVATE, makeFinal = true)
public class TicketOrderRepositoryImpl implements TicketOrderRepository {

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

    private String createTableName(String yearMonth){
        return String.format("ticket_order_%s", yearMonth);
    }

    private static final String CREATE_TABLE_TEMPLATE = "%s";
}
