
package edu.itc.shop.security;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import edu.itc.shop.service.AuditService;

@Component
public class SecurityEventLogger {

    private static final Logger log =
            LoggerFactory.getLogger("SECURITY");

    private final AuditService auditService;

    public SecurityEventLogger(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent e) {

        String username = safeUsername(e.getAuthentication());

        log.warn("login.failed user={} reason={}",
                username,
                e.getException().getClass().getSimpleName());

        auditService.record(
                "LOGIN_FAILED",
                username,
                getClientIp(),
                getRequestId(),
                e.getException().getClass().getSimpleName()
        );
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent e) {

        String username = safeUsername(e.getAuthentication());

        log.info("login.ok user={} type={}",
                username,
                e.getAuthentication().getClass().getSimpleName());

        if (!"unknown".equals(username)) {
            auditService.record(
                    "LOGIN_OK",
                    username,
                    getClientIp(),
                    getRequestId(),
                    "Authentication successful"
            );
        }
    }

    @EventListener
    public void onDenied(AuthorizationDeniedEvent<?> e) {

        Authentication authentication = e.getAuthentication().get();

        String username = authentication == null
                ? "anonymous"
                : safeUsername(authentication);

        String object = describe(e.getObject());

        log.warn("access.denied user={} object={}",
                username, object);

        auditService.record(
                "ACCESS_DENIED",
                username,
                getClientIp(),
                getRequestId(),
                object
        );
    }

    private String safeUsername(Authentication authentication) {

        if (authentication == null) {
            return "anonymous";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        // Login events may contain the username as a String.
        // Never use Authentication.getName(), which could expose a JWT.
        if (principal instanceof String username
                && !username.isBlank()
                && username.length() <= 40) {
            return username;
        }

        return "unknown";
    }

    private String getClientIp() {

        var attributes =
                RequestContextHolder.getRequestAttributes();

        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getRemoteAddr();
        }

        return null;
    }

    private String getRequestId() {
        return MDC.get("rid");
    }

    private String describe(Object o) {

        if (o instanceof MethodInvocation mi) {
            return mi.getMethod().getDeclaringClass().getSimpleName()
                    + "." + mi.getMethod().getName() + "(...)";
        }

        if (o instanceof RequestAuthorizationContext c) {
            return c.getRequest().getMethod() + " "
                    + c.getRequest().getRequestURI();
        }

        return o == null ? "unknown"
                : o.getClass().getSimpleName();
    }
}
