package com.loopers.config.lock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LockProperties.class)
public class LockConfig {
}
