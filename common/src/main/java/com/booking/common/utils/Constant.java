package com.booking.common.utils;

public class Constant {
    public static final String USER_NOT_FOUND = "User not found xx";
    public static final String USER_EXISTED = "User already existed";
    public static final String FORBIDDEN = "You don't have permission ";
    public static final String SERVER_ERROR = "Server error";
    public static final String UN_AUTHORIZE = "Unauthorize";
    public static final String INVALID_DOB = "Must be > {min}";

    public static final String DATA_NOTFOUND = "Data not found";

    public static final String SEND_MAIL_FAILED = "Error when send email";
    public static final String INVALID_START_TIME = "Start time must be before end time";
    public static final String START_TIME_CANNOT_BE_IN_THE_PAST = "Start time cannot be in the past";
    public static final String INVALID_TIME_FORMAT = "Start time cannot be in the past";
    public static final String INVALID_TIME_LIMIT = "Invalid limit";
    public static final String OUT_OF_STOCK = "Out of stock";
    public static final String BAD_REQUEST = "Bad request";
    public static final String INVALID_IDEMPOTENCY_KEY = "Idempotency-Key header is required";
    public static final String TICKET_NOT_ON_SALE = "Ticket is not on sale";
    public static final String ORDER_NOT_CANCELLABLE = "Order can no longer be cancelled";
    public static final String INVALID_ORDER_STATE = "Order is not in a valid state for this action";
    public static final String REQUEST_IN_PROGRESS = "A request with the same Idempotency-Key is being processed";
    public static final String IDEMPOTENCY_KEY_REUSED = "Idempotency-Key was already used for a different request";
    public static final String SERVICE_UNAVAILABLE = "Service temporarily unavailable, please retry";

    // topic
    public static final String TOPIC_USER_OB = "user-ob-topic";
    public static final String TOPIC_CREATE_PROFILE = "user-profile-create-topic";
    public static final String TOPIC_ACCOUNT_REVERT= "account-revert-topic";
    public static final String TICKET_DECREASE_STOCK_TOPIC = "ticket-decrease-stock-topic";
    public static final String PLACE_ORDER_TOPIC = "place-order-topic";

    // token
    public static final String TOKEN_TYPE = "type";
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";

    public static final String VALID_INTERNAL_KEY_ATTRIBUTE = "valid-internal-key";
    public static final String APP_INTERNAL_HEADER = "booking-internal-key";
}
