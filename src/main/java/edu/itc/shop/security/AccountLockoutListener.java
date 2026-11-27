
package edu.itc.shop.security;

import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import edu.itc.shop.domain.UserAccount;
import edu.itc.shop.repository.UserAccountRepository;
import edu.itc.shop.service.AuditService;

@Component
public class AccountLockoutListener {

    private final UserAccountRepository users;
    private final AuditService auditService;

    public AccountLockoutListener(
            UserAccountRepository users,
            AuditService auditService) {
        this.users = users;
        this.auditService = auditService;
    }

    @EventListener
    @Transactional
    public void onLoginFailure(
            AuthenticationFailureBadCredentialsEvent event) {

        String username = event.getAuthentication().getName();

        users.findWithLockingByUsername(username).ifPresent(account -> {

            account.recordFailedLogin();
            users.save(account);

            if (!account.isAccountNonLocked()) {
                auditService.record(
                        "LOCKED",
                        account.getUsername(),
                        getClientIp(),
                        MDC.get("rid"),
                        "Account locked after five failed login attempts"
                );
            }
        });
    }

    @EventListener
    @Transactional
    public void onLoginSuccess(AuthenticationSuccessEvent event) {

        String username = event.getAuthentication().getName();

        users.findWithLockingByUsername(username).ifPresent(account -> {
            account.resetFailedLogins();
            users.save(account);
        });
    }

    private String getClientIp() {
        var attributes = RequestContextHolder.getRequestAttributes();

        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getRemoteAddr();
        }

        return null;
    }
}
