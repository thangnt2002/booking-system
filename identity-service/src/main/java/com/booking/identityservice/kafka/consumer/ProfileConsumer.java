package com.booking.identityservice.kafka.consumer;

import com.booking.identityservice.event.ProfileRevertEvent;
import com.booking.identityservice.repository.UserRepository;
import com.booking.identityservice.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.booking.identityservice.common.Constant.TOPIC_ACCOUNT_REVERT;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileConsumer {

    UserService userService;

    @KafkaListener(topics = TOPIC_ACCOUNT_REVERT, groupId = "identity-group")
    public void revertAccount(ProfileRevertEvent msg) {
        String id = msg.getUserId();
        log.info("Delete account : {}", id);
        userService.delete(id);
    }
}
