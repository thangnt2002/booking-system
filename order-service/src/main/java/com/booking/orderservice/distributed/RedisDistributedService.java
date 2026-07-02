package com.booking.orderservice.distributed;

public interface RedisDistributedService {

    RedisDistributedLocker getDistributedLock(String lockKey);

}
