package com.booking.profileservice.services.impl;

import com.booking.profileservice.dto.ApiResponse;
import com.booking.profileservice.dto.requests.UserProfileRequest;
import com.booking.profileservice.dto.responses.UserProfileResponse;
import com.booking.profileservice.entities.UserProfile;
import com.booking.profileservice.event.NotificationEvent;
import com.booking.profileservice.event.ProfileRevertEvent;
import com.booking.profileservice.exception.ErrorCode;
import com.booking.profileservice.exception.NotFoundException;
import com.booking.profileservice.kafka.producer.IdentityProducer;
import com.booking.profileservice.kafka.producer.NotificationProducer;
import com.booking.profileservice.mapper.UserProfileMapper;
import com.booking.profileservice.repositories.UserProfileRepository;
import com.booking.profileservice.services.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.booking.profileservice.common.Constant.TOPIC_ACCOUNT_REVERT;
import static com.booking.profileservice.common.Constant.TOPIC_USER_OB;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    UserProfileRepository userProfileRepository;
    UserProfileMapper userProfileMapper;
    NotificationProducer notificationProducer;
    IdentityProducer identityProducer;

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ApiResponse<UserProfileResponse> create(UserProfileRequest request) {
        try {
            UserProfile userProfile = userProfileMapper.toUserProfile(request);
            userProfile = userProfileRepository.save(userProfile);

            NotificationEvent notificationEvent = NotificationEvent
                    .builder()
                    .subject("Subject")
                    .channel("EMAIL")
                    .recipient(request.getEmail())
                    .body("Body for test")
                    .build();

            notificationProducer.sendNotificationEvent(TOPIC_USER_OB, notificationEvent);

            return ApiResponse.<UserProfileResponse>builder()
                    .success(true)
                    .code(201)
                    .data(userProfileMapper.toUserProfileResponse(userProfile))
                    .build();
        } catch (Exception e){

            ProfileRevertEvent event = userProfileMapper.toProfileRevertEvent(request);
            log.error(e.getMessage());
            log.error("revert profile {}", event);
            identityProducer.sendRevertAccountEvent(TOPIC_ACCOUNT_REVERT, event);
        }
      return null;
    }

    @Override
    public ApiResponse<UserProfileResponse> update(UserProfileRequest request) {
        return null;
    }

    @Override
    public UserProfile findById(String id) {
        return userProfileRepository.findById(id).orElseThrow(()-> new NotFoundException(ErrorCode.USER_NOT_FOUND.getCode()));
    }

    @Override
    public List<UserProfile> getAll() {
        return userProfileRepository.findAll();
    }
}
