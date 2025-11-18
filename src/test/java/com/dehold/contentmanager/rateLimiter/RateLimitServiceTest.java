package com.dehold.contentmanager.rateLimiter;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.ratelimiter.RateLimitService;
import com.dehold.contentmanager.ratelimiter.TokenBucket;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class RateLimitServiceTest  extends ContentManagerApplicationTests {

    //✔ returns same bucket for same key
    //✔ returns different buckets for different keys
    //✔ concurrent calls still return one instance per key
    //✔ constructor properties stored correctly

    @Test
    void sameKeyShouldReturnSameBucket() {
        RateLimitService service = new RateLimitService(10, 2, 1000);

        TokenBucket b1 = service.getBucketForKey("user1");
        TokenBucket b2 = service.getBucketForKey("user1");

        assertSame(b1, b2);
    }

    @Test
    void differentKeysShouldReturnDifferentBuckets() {
        RateLimitService service = new RateLimitService(10, 2, 1000);

        TokenBucket b1 = service.getBucketForKey("u1");
        TokenBucket b2 = service.getBucketForKey("u2");

        assertNotSame(b1, b2);
    }

    @Test
    void concurrentAccessShouldStillCreateSingleBucketPerKey() throws Exception {
        RateLimitService service = new RateLimitService(10, 2, 1000);

        ExecutorService exec = Executors.newFixedThreadPool(20);
        List<Future<TokenBucket>> futures = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            futures.add(exec.submit(() -> service.getBucketForKey("concurrentKey")));
        }

        Set<TokenBucket> resultSet = new HashSet<>();
        for (Future<TokenBucket> f : futures) {
            resultSet.add(f.get());
        }

        exec.shutdown();

        assertEquals(1, resultSet.size());
    }

    @Test
    void constructorShouldStoreConfigValues() throws Exception {
        RateLimitService service = new RateLimitService(50, 20, 5000);

        Field cap = service.getClass().getDeclaredField("capacity");
        Field refillTokens = service.getClass().getDeclaredField("refillTokens");
        Field interval = service.getClass().getDeclaredField("refillIntervalMillis");

        cap.setAccessible(true);
        refillTokens.setAccessible(true);
        interval.setAccessible(true);

        assertEquals(50L, cap.getLong(service));
        assertEquals(20L, refillTokens.getLong(service));
        assertEquals(5000L, interval.getLong(service));
    }
}
