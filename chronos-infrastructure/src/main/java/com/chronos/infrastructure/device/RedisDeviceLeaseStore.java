package com.chronos.infrastructure.device;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisDeviceLeaseStore {
    private final StringRedisTemplate redis;
    private final Duration ttl;

    public RedisDeviceLeaseStore(StringRedisTemplate redis,
            @Value("${chronos.device.lease-seconds:45}") long leaseSeconds) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(leaseSeconds);
    }

    private String key(String deviceId) { return "chronos:device-lease:" + deviceId; }
    private String value(String gatewayId, String connectionId) { return gatewayId + "|" + connectionId; }

    public boolean acquire(String deviceId, String gatewayId, String connectionId) {
        String key = key(deviceId);
        String value = value(gatewayId, connectionId);
        Boolean created = redis.opsForValue().setIfAbsent(key, value, ttl);
        if (Boolean.TRUE.equals(created)) return true;
        String current = redis.opsForValue().get(key);
        if (value.equals(current)) {
            redis.expire(key, ttl);
            return true;
        }
        return false;
    }

    public boolean renew(String deviceId, String gatewayId, String connectionId) {
        String key = key(deviceId);
        String expected = value(gatewayId, connectionId);
        String current = redis.opsForValue().get(key);
        if (!expected.equals(current)) return false;
        return Boolean.TRUE.equals(redis.expire(key, ttl));
    }

    public void release(String deviceId, String gatewayId, String connectionId) {
        String key = key(deviceId);
        String expected = value(gatewayId, connectionId);
        String current = redis.opsForValue().get(key);
        if (expected.equals(current)) redis.delete(key);
    }
}
