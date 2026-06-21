package com.booking.eventservice.exception;

import com.booking.eventservice.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final String MIN_ATTRIBUTE = "min";

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

    @ExceptionHandler(value = NotFoundException.class)
    ResponseEntity<ApiResponse> notFoundExceptionHandler(NotFoundException exception){
        ApiResponse apiResponse = ApiResponse.builder()
                .code(exception.getCode())
                .message(exception.getMessage())
                .success(false)
                .build();
        return ResponseEntity.status(ErrorCode.USER_NOT_FOUND.getStatusCode()).body(apiResponse);
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

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> validationExceptionHandler(MethodArgumentNotValidException exception){
        String enumKey = Objects.requireNonNull(exception.getFieldError()).getDefaultMessage();

        ErrorCode error = ErrorCode.INVALID_DOB;
        Map<String, Object> attributes = null;
        try{
            error = error.valueOf(enumKey);
            var constraintViolation = exception.getBindingResult()
                    .getAllErrors().getFirst().unwrap(ConstraintViolation.class);
            attributes = constraintViolation.getConstraintDescriptor().getAttributes();
            log.info(attributes.toString());

        }catch (IllegalArgumentException e){

        }
        ApiResponse apiResponse = ApiResponse.builder()
                .code(error.getCode())
                .message(Objects.nonNull(attributes) ? mapAttribute(error.getMessage(), attributes)
                        : error.getMessage())
                .success(false)
                .build();
        return ResponseEntity.status(ErrorCode.INVALID_DOB.getStatusCode()).body(apiResponse);
    }

    private String mapAttribute(String message, Map<String, Object> attribute){
        String minValue = String.valueOf(attribute.get(MIN_ATTRIBUTE));
        return message.replace("{"+MIN_ATTRIBUTE+"}", minValue);
    }
}
