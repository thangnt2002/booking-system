package com.booking.common.distributed;

public interface RedisDistributedService {

    RedisDistributedLocker getDistributedLock(String lockKey);

}
