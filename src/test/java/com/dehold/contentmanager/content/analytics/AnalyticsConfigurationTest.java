package com.dehold.contentmanager.content.analytics;

import com.dehold.contentmanager.analytics.service.ApiAccessCounter;
import com.dehold.contentmanager.config.WebMvcConfig;
import com.dehold.contentmanager.ratelimiter.config.RateLimitInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AnalyticsConfigurationTest {
    @Test
    void shouldRegisterApiAccessCounterInterceptor() {
        RateLimitInterceptor rateLimitInterceptor = mock(RateLimitInterceptor.class);
        ApiAccessCounter apiAccessCounter = mock(ApiAccessCounter.class);
        WebMvcConfig config = new WebMvcConfig(rateLimitInterceptor, apiAccessCounter);

        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);

        when(registry.addInterceptor(any())).thenReturn(registration);
        when(registration.addPathPatterns(anyString())).thenReturn(registration);

        config.addInterceptors(registry);

        verify(registry).addInterceptor(apiAccessCounter);
        verify(registration, atLeastOnce()).addPathPatterns("/api/**"); // This is a bit fuzzy because other
        // interceptors might register the same pattern. But still good to make sure it's called at least once.
    }
}
