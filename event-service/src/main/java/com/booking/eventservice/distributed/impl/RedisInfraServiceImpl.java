package com.booking.eventservice.distributed.impl;

import com.booking.eventservice.distributed.RedisInfraService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisInfraServiceImpl implements RedisInfraService {

    RedisTemplate<String, Object> redisTemplate;

    @Override
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    @Override
    public void setObject(String key, Object value) {
        if(!StringUtils.hasLength(key)){
            log.info("Set key failed!");
            return;
        }
        try{
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e){
            log.error("set object err: key = {}, err = {}", key, e.getMessage());
        }
    }

    @Override
    public <T> T getObject(String key, Class<T> targetClass) {
        Object result = redisTemplate.opsForValue().get(key);
        switch (result) {
            case null -> {
                return null;
            }
            case Map map -> {
                try {

                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.convertValue(result, targetClass);
                } catch (IllegalArgumentException e) {
                    log.error("Error converting LinkedHashMap to object: {} for key: {}", e.getMessage(), key);
                    return null;
                }
            }
            case String s -> {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(s, targetClass);
                } catch (JsonProcessingException e) {
                    log.error("Error converting String to object: {} for key: {}", e.getMessage(), key);
                    return null;
                }
            }
            default -> {
            }
        }

        return null;
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
