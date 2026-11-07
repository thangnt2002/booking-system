package com.booking.common.utils;

import com.booking.common.dtos.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public class FeignClientUtils {

    public static <T> T getApiResDataOrDefaultValue(ResponseEntity<ApiResponse<T>> response, T defaultValue) {
        return Optional.ofNullable(response)
                .map(ResponseEntity::getBody)
                .map(ApiResponse::getData)
                .orElse(defaultValue);
    }
}
