package com.dehold.contentmanager.config;

import com.dehold.contentmanager.analytics.service.ApiAccessCounter;
import com.dehold.contentmanager.ratelimiter.config.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("!test")
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final ApiAccessCounter apiAccessCounter;

    @Autowired
    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor, ApiAccessCounter apiAccessCounter) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.apiAccessCounter = apiAccessCounter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");

        registry.addInterceptor(apiAccessCounter)
                .addPathPatterns("/api/**");
    }
}
