package edu.itc.shop.web;

public record TokenResponse(
        String token,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn
) {}
