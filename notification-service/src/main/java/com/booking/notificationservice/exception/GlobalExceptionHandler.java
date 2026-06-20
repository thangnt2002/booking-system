package com.booking.notificationservice.exception;

import com.booking.notificationservice.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> serverErrorExceptionHandler(Exception exception){
        ErrorCode error = ErrorCode.SERVER_ERROR;

        ApiResponse apiResponse = ApiResponse.builder()
                .code(error.getCode())
                .message(error.getMessage())
                .success(false)
                .build();
        return ResponseEntity.status(error.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = UnauthorizedException.class)
    ResponseEntity<ApiResponse> unauthorizeExceptionHandler(UnauthorizedException exception){

            ApiResponse apiResponse = ApiResponse.builder()
                    .code(exception.getCode())
                    .message(exception.getMessage())
                    .success(false)
                    .build();
            return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse> AccessDeniedExceptionHandler(AccessDeniedException exception){
        ErrorCode error = ErrorCode.FORBIDDEN;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(error.getCode())
                .message(error.getMessage())
                .success(false)
                .build();
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = EmailException.class)
    ResponseEntity<ApiResponse> EmailExceptionHandler(EmailException exception){
        ApiResponse apiResponse = ApiResponse.builder()
                .code(exception.getCode())
                .message(exception.getMessage())
                .success(false)
                .build();
        return ResponseEntity.status(ErrorCode.SEND_MAIL_FAILED.getStatusCode()).body(apiResponse);
    }
}
