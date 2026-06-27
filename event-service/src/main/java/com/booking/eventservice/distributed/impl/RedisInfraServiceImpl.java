package com.booking.eventservice.distributed.impl;

import com.booking.eventservice.distributed.RedisInfraService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisInfraServiceImpl implements RedisInfraService {

    RedisTemplate<Object, Object> redisTemplate;
    ObjectMapper objectMapper;

    @Override
    public RedisTemplate<Object, Object> getRedisTemplate() {
        return redisTemplate;
    }

    @Override
    public void setObject(String key, Object value) {
        if(!StringUtils.hasLength(key)){
            log.info("Set key failed!");
            return;
        }
        try{
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue);
        } catch (Exception e){
            log.error("set object err: key = {}, err = {}", key, e.getMessage());
        }
    }

    @Override
    public <T> T getObject(String key, Class<T> targetClass) {
        Object jsonValue = redisTemplate.opsForValue().get(key);
        if(jsonValue == null){
            return null;
        }
        log.info("Type={}", jsonValue.getClass());
        log.info("Value={}", jsonValue);
        try {
            return objectMapper.readValue(jsonValue.toString(), targetClass);
        } catch (IllegalArgumentException e) {
            log.error("Error converting to object: {} for key: {}", e.getMessage(), key);
            return null;
        } catch (Exception e){
            log.error("Error server for object: {} for key: {}", e, key);
            return null;
        }
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void setInt(String key, int value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public int getInt(String key) {
        Object result = redisTemplate.opsForValue().get(key);
        return result != null ? (int) result : 0;
    }
}
