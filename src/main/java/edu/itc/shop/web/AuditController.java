
package edu.itc.shop.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.shop.domain.SecurityAudit;
import edu.itc.shop.service.AuditService;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<SecurityAudit> getAuditLogs(
            @RequestParam(required = false) String username) {

        if (username == null || username.isBlank()) {
            return auditService.findAll();
        }

        return auditService.findByUsername(username);
    }
}
