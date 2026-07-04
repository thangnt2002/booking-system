package com.booking.orderservice.exception;

import com.booking.orderservice.dto.ApiResponse;
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
                    .code(403)
                    .message(exception.getMessage())
                    .success(false)
                    .build();
            return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = NotFoundException.class)
    ResponseEntity<ApiResponse> notFoundExceptionHandler(NotFoundException exception){
        ErrorCode err = exception.getCode();
        ApiResponse apiResponse = ApiResponse.builder()
                .success(false)
                .code(err.getCode())
                .message(err.getMessage())
                .build();
        return ResponseEntity.status(ErrorCode.DATA_NOT_FOUND.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = BusinessException.class)
    ResponseEntity<ApiResponse> businessExceptionHandler(BusinessException exception){
        ErrorCode err = exception.getCode();
        ApiResponse apiResponse = ApiResponse.builder()
                .success(false)
                .code(err.getCode())
                .message(err.getMessage())
                .build();
        return ResponseEntity.status(err.getStatusCode()).body(apiResponse);
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
}
