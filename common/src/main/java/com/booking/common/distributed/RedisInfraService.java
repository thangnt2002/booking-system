package com.booking.common.distributed;

import org.springframework.data.redis.core.RedisTemplate;

public interface RedisInfraService {

    RedisTemplate<Object, Object> getRedisTemplate();

    void setObject(String key, Object value);

    <T> T getObject(String key, Class<T> targetClass);

    /** SET key value NX: never overwrites a live value. Returns true if the key was written. */
    boolean setIfAbsent(String key, String value);

    void delete(String key);

    void setInt(String key, int value);

    int getInt(String key);
}
