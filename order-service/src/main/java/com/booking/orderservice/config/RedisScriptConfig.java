package com.booking.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

/** Scripts are loaded once; RedisTemplate executes them by SHA (EVALSHA) after the first call. */
@Configuration
public class RedisScriptConfig {

    @Bean
    public RedisScript<Long> reserveStockScript() {
        return RedisScript.of(new ClassPathResource("lua/reserve_stock.lua"), Long.class);
    }

    @Bean
    public RedisScript<Long> releaseStockScript() {
        return RedisScript.of(new ClassPathResource("lua/release_stock.lua"), Long.class);
    }
}
