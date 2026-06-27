package com.booking.eventservice.exception;

import com.booking.eventservice.common.Constant;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    DATA_NOT_FOUND(404, Constant.DATA_NOTFOUND, HttpStatus.NOT_FOUND),
    FORBIDDEN(403, Constant.FORBIDDEN, HttpStatus.FORBIDDEN),
    SERVER_ERROR(500, Constant.SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
    UN_AUTHORIZE(401, Constant.UN_AUTHORIZE, HttpStatus.UNAUTHORIZED),
    INVALID_START_TIME(400, Constant.INVALID_START_TIME, HttpStatus.BAD_REQUEST),
    START_TIME_CANNOT_BE_IN_THE_PAST(400, Constant.START_TIME_CANNOT_BE_IN_THE_PAST, HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatusCode statusCode){
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}
