package com.householdops.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HS256 needs >= 256 bits; the dev default in application.yml is long enough, but must be overridden via JWT_SECRET in any real deployment. */
    private String secret;

    private long accessTokenTtlMinutes = 30;

    private long refreshTokenTtlDays = 7;
}
