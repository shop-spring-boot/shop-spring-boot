package edu.itc.shop.security;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventLogger {
    private static final Logger log = LoggerFactory.getLogger("SECURITY");

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent e) {
        log.warn("login.failed user={} reason={}",
                e.getAuthentication().getName(), e.getException().getClass().getSimpleName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent e) {
        log.info("login.ok user={} type={}",
                e.getAuthentication().getName(), e.getAuthentication().getClass().getSimpleName());
    }

    @EventListener
    public void onDenied(AuthorizationDeniedEvent<?> e) {
        Authentication a = e.getAuthentication().get();
        log.warn("access.denied user={} object={}", a == null ? "anonymous" : a.getName(), describe(e.getObject()));
    }

    private String describe(Object o) {
        if (o instanceof MethodInvocation mi) {
            String args = Arrays.stream(mi.getArguments()).map(String::valueOf).collect(Collectors.joining(","));
            return mi.getMethod().getDeclaringClass().getSimpleName() + "." + mi.getMethod().getName() + "(" + args + ")";
        }
        if (o instanceof RequestAuthorizationContext c) {
            return c.getRequest().getMethod() + " " + c.getRequest().getRequestURI();
        }
        return String.valueOf(o);
    }
}