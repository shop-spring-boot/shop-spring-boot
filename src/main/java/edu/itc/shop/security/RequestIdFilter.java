package edu.itc.shop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String rid = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("rid", rid);
        res.setHeader("X-Request-Id", rid);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("rid");
        }
    }
}