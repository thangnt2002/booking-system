package com.booking.common.exceptions;

import com.booking.common.utils.Constant;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    USER_EXISTED(400, Constant.USER_EXISTED, HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(404, Constant.USER_NOT_FOUND, HttpStatus.NOT_FOUND),
    FORBIDDEN(403, Constant.FORBIDDEN, HttpStatus.FORBIDDEN),
    SERVER_ERROR(500, Constant.SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
    UN_AUTHORIZE(401, Constant.UN_AUTHORIZE, HttpStatus.UNAUTHORIZED),
    INVALID_DOB(400, Constant.INVALID_DOB, HttpStatus.BAD_REQUEST),
    DATA_NOT_FOUND(404, Constant.DATA_NOTFOUND, HttpStatus.NOT_FOUND),
    INVALID_START_TIME(400, Constant.INVALID_START_TIME, HttpStatus.BAD_REQUEST),
    START_TIME_CANNOT_BE_IN_THE_PAST(400, Constant.START_TIME_CANNOT_BE_IN_THE_PAST, HttpStatus.BAD_REQUEST),
    INVALID_TIME_FORMAT(400, Constant.INVALID_TIME_FORMAT, HttpStatus.BAD_REQUEST),
    INVALID_TIME_LIMIT(400, Constant.INVALID_TIME_LIMIT, HttpStatus.BAD_REQUEST);


    ErrorCode(int code, String message, HttpStatusCode statusCode){
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}
