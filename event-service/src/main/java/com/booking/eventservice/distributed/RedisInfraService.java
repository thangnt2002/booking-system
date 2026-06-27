package com.booking.eventservice.distributed;

import org.springframework.data.redis.core.RedisTemplate;

public interface RedisInfraService {

    RedisTemplate<Object, Object> getRedisTemplate();

    void setObject(String key, Object value);

    <T> T getObject(String key, Class<T> targetClass);

    void delete(String key);

    void setInt(String key, int value);

    int getInt(String key);
}
