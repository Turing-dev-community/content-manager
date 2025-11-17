package com.dehold.contentmanager;

import org.springframework.boot.test.context.TestConfiguration;

import jakarta.annotation.PostConstruct;

@TestConfiguration
public class TestProfileConfig {

    @PostConstruct
    public void setTestProfile() {
        System.setProperty("spring.profiles.active", "h2");
    }
}
