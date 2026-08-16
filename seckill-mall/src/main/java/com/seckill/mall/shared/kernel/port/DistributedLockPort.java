package com.seckill.mall.shared.kernel.port;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁端口（抽象 Redisson 锁操作）
 */
public interface DistributedLockPort {

    boolean tryLock(String key, long timeout, TimeUnit unit);

    void unlock(String key);
}