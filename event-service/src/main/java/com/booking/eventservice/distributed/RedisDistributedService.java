package com.booking.eventservice.distributed;

public interface RedisDistributedService {

    RedisDistributedLocker getDistributedLock(String lockKey);

}
