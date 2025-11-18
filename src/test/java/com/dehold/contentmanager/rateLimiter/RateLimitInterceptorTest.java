package com.dehold.contentmanager.rateLimiter;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.ratelimiter.RateLimitInterceptor;
import com.dehold.contentmanager.ratelimiter.RateLimitService;
import com.dehold.contentmanager.ratelimiter.TokenBucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class RateLimitInterceptorTest  extends ContentManagerApplicationTests {


    //✔ request from unauthenticated user
    //✔ request from authenticated user (Principal)
    //✔ allowed request path
    //✔ blocking with 429
    //✔ skipping rate-limit for non-API endpoints
    //✔ key extraction (X-User-Id, IP fallback)

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        // Create RateLimitService with small bucket (for easy testing)
        RateLimitService service = new RateLimitService(100, 100, 60000);

        // Register interceptor manually
        RateLimitInterceptor interceptor = new RateLimitInterceptor(service);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyController())  // <— required endpoint
                .addInterceptors(interceptor)
                .build();
    }

    @Test
    void whenMoreThan100Requests_thenReturn429TooManyRequests() throws Exception {
        // Perform 100 valid requests
        for (int i = 1; i <= 100; i++) {
            mockMvc.perform(get("/api/test")
                            .header("X-User-Id", "user-123"))
                    .andExpect(status().isOk());
        }

        // 101st request should fail
        mockMvc.perform(get("/api/test")
                        .header("X-User-Id", "user-123"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("{\"error\":\"Too Many Requests\"}"))
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void shouldAllowRequestWhenBucketHasTokens() throws Exception {
        RateLimitService service = mock(RateLimitService.class);
        TokenBucket bucket = mock(TokenBucket.class);

        when(service.getBucketForKey(anyString())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        RateLimitInterceptor interceptor = new RateLimitInterceptor(service);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getRequestURI()).thenReturn("/api/test");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        boolean allowed = interceptor.preHandle(req, res, new Object());

        assertTrue(allowed);
    }

    @Test
    void shouldBlockRequestWhenBucketEmpty() throws Exception {
        RateLimitService service = mock(RateLimitService.class);
        TokenBucket bucket = mock(TokenBucket.class);

        when(service.getBucketForKey(anyString())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        RateLimitInterceptor interceptor = new RateLimitInterceptor(service);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();

        when(res.getWriter()).thenReturn(new PrintWriter(writer));
        when(req.getRequestURI()).thenReturn("/api/test");
        when(req.getRemoteAddr()).thenReturn("1.1.1.1");

        boolean allowed = interceptor.preHandle(req, res, new Object());

        assertFalse(allowed);
        verify(res).setStatus(429);
        assertTrue(writer.toString().contains("Too Many Requests"));
    }

    @Test
    void shouldSkipNonApiEndpoints() throws Exception {
        RateLimitService service = mock(RateLimitService.class);
        RateLimitInterceptor interceptor = new RateLimitInterceptor(service);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getRequestURI()).thenReturn("/health");

        boolean allowed = interceptor.preHandle(req, res, new Object());

        assertTrue(allowed);
        verify(service, never()).getBucketForKey(any());
    }

    @Test
    void shouldUsePrincipalAsKeyWhenAvailable() throws Exception {
        RateLimitService service = mock(RateLimitService.class);
        TokenBucket bucket = mock(TokenBucket.class);

        when(bucket.tryConsume(1)).thenReturn(true);
        when(service.getBucketForKey("user:john")).thenReturn(bucket);

        RateLimitInterceptor interceptor = new RateLimitInterceptor(service);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        Principal p = () -> "john";

        when(req.getRequestURI()).thenReturn("/api/test");
        when(req.getUserPrincipal()).thenReturn(p);

        boolean allowed = interceptor.preHandle(req, res, new Object());

        assertTrue(allowed);
        verify(service).getBucketForKey("user:john");
    }

    @Test
    void shouldFallbackToIpIfNoPrincipalAndNoHeader() throws Exception {
        RateLimitService service = mock(RateLimitService.class);
        TokenBucket bucket = mock(TokenBucket.class);

        when(bucket.tryConsume(1)).thenReturn(true);
        when(service.getBucketForKey("ip:10.0.0.5")).thenReturn(bucket);

        RateLimitInterceptor interceptor = new RateLimitInterceptor(service);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getRequestURI()).thenReturn("/api/test");
        when(req.getRemoteAddr()).thenReturn("10.0.0.5");

        boolean allowed = interceptor.preHandle(req, res, new Object());

        assertTrue(allowed);
        verify(service).getBucketForKey("ip:10.0.0.5");
    }
}
