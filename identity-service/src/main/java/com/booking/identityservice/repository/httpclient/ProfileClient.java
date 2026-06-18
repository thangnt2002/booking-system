package com.booking.identityservice.repository.httpclient;

import com.booking.identityservice.configuration.ClientAuthenRequestInterceptor;
import com.booking.identityservice.dto.request.UserProfileCreationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "profile-service",
        url = "${app.service.profile}",
        configuration = {ClientAuthenRequestInterceptor.class}
)
public interface ProfileClient {

    @PostMapping(value = "/internal/profiles/registration", produces = MediaType.APPLICATION_JSON_VALUE)
    Object createUserProfile(@RequestBody UserProfileCreationRequest request);

}