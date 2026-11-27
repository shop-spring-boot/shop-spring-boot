package edu.itc.shop.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Validated
@ConfigurationProperties("shop.security")
public record SecurityProps(@NotBlank @Size(min = 32) String jwtSecret,
                            Duration accessTokenTtl) {}