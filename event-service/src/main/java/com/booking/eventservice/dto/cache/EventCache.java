package com.booking.eventservice.dto.cache;

import com.booking.eventservice.entity.Event;
import lombok.Data;

@Data
public class EventCache {

    private Long version;
    private Event event;

    public EventCache withClone(Event event){
        this.event = event;
        return this;
    }

    public EventCache withVersion(Long version){
        this.version = version;
        return this;
    }

}
