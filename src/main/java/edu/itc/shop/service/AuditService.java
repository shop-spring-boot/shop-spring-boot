
package edu.itc.shop.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.SecurityAudit;
import edu.itc.shop.repository.SecurityAuditRepository;

@Service
public class AuditService {

    private final SecurityAuditRepository repository;

    public AuditService(SecurityAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String type,
            String username,
            String ip,
            String requestId,
            String detail) {

        SecurityAudit audit = new SecurityAudit(
                Instant.now(),
                type,
                username,
                ip,
                requestId,
                detail
        );

        repository.save(audit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRoleChanged(
            String username,
            String ip,
            String requestId,
            String oldRole,
            String newRole) {

        record(
                "ROLE_CHANGED",
                username,
                ip,
                requestId,
                "Role changed from " + oldRole + " to " + newRole
        );
    }

    @Transactional(readOnly = true)
    public List<SecurityAudit> findAll() {
        return repository.findAllByOrderByAtDesc();
    }

    @Transactional(readOnly = true)
    public List<SecurityAudit> findByUsername(String username) {
        return repository.findByUsernameOrderByAtDesc(username);
    }
}
