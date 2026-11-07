package com.booking.common.exceptions;

import com.booking.common.dtos.ApiResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> businessExceptionHandler(BusinessException exception) {
        ErrorCode error = exception.getCode() != null ? exception.getCode() : ErrorCode.SERVER_ERROR;
        if (error == ErrorCode.SERVER_ERROR) {
            log.error("Business exception mapped to server error", exception);
        }
        return build(error, error.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiResponse<Void>> notFoundExceptionHandler(NotFoundException exception) {
        ErrorCode error = exception.getCode() != null ? exception.getCode() : ErrorCode.DATA_NOT_FOUND;
        return build(error, error.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiResponse<Void>> unauthorizedExceptionHandler(UnauthorizedException exception) {
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .code(exception.getCode())
                .message(exception.getMessage())
                .success(false)
                .build();
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatusCode()).body(body);
    }

    /** @PreAuthorize denials are thrown inside the controller call, so they reach this advice, not the security entry point. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> accessDeniedHandler(AccessDeniedException exception) {
        return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiResponse<Void>> missingHeaderHandler(MissingRequestHeaderException exception) {
        ErrorCode error = IDEMPOTENCY_HEADER.equalsIgnoreCase(exception.getHeaderName())
                ? ErrorCode.INVALID_IDEMPOTENCY_KEY
                : ErrorCode.INVALID_REQUEST;
        return build(error, error.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiResponse<Void>> badRequestHandler(Exception exception) {
        log.debug("Malformed request: {}", exception.getMessage());
        return build(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> validationExceptionHandler(MethodArgumentNotValidException exception) {
        ErrorCode error = ErrorCode.INVALID_REQUEST;
        Map<String, Object> attributes = null;
        String enumKey = exception.getFieldError() != null ? exception.getFieldError().getDefaultMessage() : null;
        try {
            if (enumKey != null) {
                error = ErrorCode.valueOf(enumKey);
                var constraintViolation = exception.getBindingResult()
                        .getAllErrors().getFirst().unwrap(ConstraintViolation.class);
                attributes = constraintViolation.getConstraintDescriptor().getAttributes();
            }
        } catch (IllegalArgumentException ignored) {
            // message is not an ErrorCode key -> fall back to INVALID_REQUEST with the raw message
            return build(ErrorCode.INVALID_REQUEST, enumKey);
        }
        String message = attributes != null ? mapAttribute(error.getMessage(), attributes) : error.getMessage();
        return build(error, message);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> serverErrorExceptionHandler(Exception exception) {
        log.error("Unhandled exception", exception);
        return build(ErrorCode.SERVER_ERROR, ErrorCode.SERVER_ERROR.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode error, String message) {
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .code(error.getCode())
                .message(message)
                .success(false)
                .build();
        return ResponseEntity.status(error.getStatusCode()).body(body);
    }

    private String mapAttribute(String message, Map<String, Object> attribute) {
        String minValue = String.valueOf(attribute.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
