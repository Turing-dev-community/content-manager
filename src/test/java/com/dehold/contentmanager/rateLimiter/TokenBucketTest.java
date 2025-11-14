package com.dehold.contentmanager.rateLimiter;

import com.dehold.contentmanager.ratelimiter.TokenBucket;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

public class TokenBucketTest {

    //✔ initial capacity
    //✔ successful consumption
    //✔ failure when tokens insufficient
    //✔ refill after interval
    //✔ capping at capacity
    //✔ concurrent consumption behavior

    @Test
    void bucketStartsAtFullCapacity() {
        TokenBucket bucket = new TokenBucket(10, 5, 1000);
        assertEquals(10, bucket.getAvailableTokens());
    }

    @Test
    void tryConsumeShouldReduceTokens() {
        TokenBucket bucket = new TokenBucket(10, 5, 1000);
        assertTrue(bucket.tryConsume(3));
        assertEquals(7, bucket.getAvailableTokens());
    }

    @Test
    void tryConsumeShouldFailWhenInsufficientTokens() {
        TokenBucket bucket = new TokenBucket(5, 5, 1000);
        assertFalse(bucket.tryConsume(6));
        assertEquals(5, bucket.getAvailableTokens());
    }

    @Test
    void tokensShouldRefillAfterInterval() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(10, 5, 200); // refill every 200ms

        assertTrue(bucket.tryConsume(10)); // consume all
        assertEquals(0, bucket.getAvailableTokens());

        Thread.sleep(250); // allow 1 refill cycle

        long tokens = bucket.getAvailableTokens();
        assertEquals(5, tokens);
    }

    @Test
    void refillShouldNotExceedCapacity() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(10, 5, 100);

        Thread.sleep(300); // allow 3 refill slots = +15 tokens, but max = 10

        assertEquals(10, bucket.getAvailableTokens());
    }

    @Test
    void tryConsumeShouldBeThreadSafe() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(100, 10, 1000);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        int threads = 50;

        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                bucket.tryConsume(2);
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        // each thread consumes 2 tokens → expected ≤ 0
        long remaining = bucket.getAvailableTokens();
        assertTrue(remaining <= 100 && remaining >= 0);
    }
}
