package com.booking.identityservice.scheduled;

import com.booking.identityservice.service.TokenService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CleanupTokenScheduled {

    TokenService tokenService;
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupInvalidTokens() {
        log.debug("Invalidating expiry token...");
        int invalidatedToken = tokenService.deleteByExpiryTimeBefore(new Date());
        log.debug("Invalidated {} token", invalidatedToken);
    }
}
