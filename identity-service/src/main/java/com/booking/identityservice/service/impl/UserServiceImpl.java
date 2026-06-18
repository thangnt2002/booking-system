package com.booking.identityservice.service.impl;

import com.booking.event.EventMsg;
import com.booking.identityservice.common.PredefinedRole;
import com.booking.identityservice.dto.ApiResponse;
import com.booking.identityservice.dto.request.UserCreationRequest;
import com.booking.identityservice.dto.request.UserProfileCreationRequest;
import com.booking.identityservice.dto.request.UserUpdateRequest;
import com.booking.identityservice.dto.response.UserCreationResponse;
import com.booking.identityservice.entity.Role;
import com.booking.identityservice.entity.User;
import com.booking.identityservice.exception.BusinessException;
import com.booking.identityservice.exception.ErrorCode;
import com.booking.identityservice.exception.NotFoundException;
import com.booking.identityservice.mapper.UserMapper;
import com.booking.identityservice.mapper.UserProfileMapper;
import com.booking.identityservice.repository.RoleRepository;
import com.booking.identityservice.repository.UserRepository;
import com.booking.identityservice.repository.httpclient.ProfileClient;
import com.booking.identityservice.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {

     UserRepository userRepository;
     UserMapper userMapper;
     PasswordEncoder passwordEncoder;
     RoleRepository roleRepository;
     ProfileClient profileClient;
     UserProfileMapper userProfileMapper;
     KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public User create(UserCreationRequest userCreationRequest) {
        User user = userMapper.toUser(userCreationRequest);
        Set<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.USER_ROLE).ifPresent(roles::add);
        user.setPassword(passwordEncoder.encode(userCreationRequest.getPassword()));
        user.setRoles(roles);
        try{
            userRepository.save(user);
        }catch (DataIntegrityViolationException e){
            throw new BusinessException(ErrorCode.USER_EXISTED.getCode());
        }
        UserProfileCreationRequest userProfileRequest = userProfileMapper.toUserProfileRequest(userCreationRequest);
        userProfileRequest.setUserId(user.getId());
        profileClient.createUserProfile(userProfileRequest);
        EventMsg notificationEvent = EventMsg
                .builder()
                .subject("Subject")
                .channel("EMAIL")
                .recipient(userCreationRequest.getEmail())
                .body("Body for test")
                .build();
        log.info("Event = {}", notificationEvent);
        return user;
    }

    @Override
    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND.getCode()));
    }

    @Override
    public User update(UserUpdateRequest userUpdateRequest) {
        boolean isUserExisted = userRepository.existsById(userUpdateRequest.getId());
        if(!isUserExisted){
            throw new NotFoundException(404);
        }
        User user = userMapper.toUser(userUpdateRequest);
        List<Role> roles = roleRepository.findAllById(userUpdateRequest.getRoles());

        user.setRoles(new HashSet<>(roles));
        return userRepository.save(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
//    @PreAuthorize("hasAllAuthorities('READ_ALL_USER')")
    @Override
    public ApiResponse<UserCreationResponse> getAll() {
        return null;
    }
}
