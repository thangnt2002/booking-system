package com.booking.eventservice.dto.cache;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import static com.booking.eventservice.common.Constant.CACHE_NULL;

@Getter
@Setter
public class BaseCache {
    private String nullValue = StringUtils.EMPTY;

    public boolean isNullObject() {
        return CACHE_NULL.equals(this.nullValue);
    }

    public void markAsNull() {
        this.nullValue = CACHE_NULL;
    }
}
