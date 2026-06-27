package com.booking.eventservice.dto.cache;

import com.booking.eventservice.entity.Ticket;
import lombok.Data;

@Data
public class TicketCache extends BaseCache{
    private Long version;
    private Ticket ticket;

    public TicketCache withClone(Ticket ticket){
        this.ticket = ticket;
        return this;
    }

    public TicketCache withVersion(Long version){
        this.version = version;
        return this;
    }

}