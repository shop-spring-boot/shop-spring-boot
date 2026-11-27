
package edu.itc.shop.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "security_audit", indexes = {
        @Index(name = "idx_audit_username", columnList = "username"),
        @Index(name = "idx_audit_at", columnList = "at"),
        @Index(name = "idx_audit_type", columnList = "type")
})
public class SecurityAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "at", nullable = false)
    private Instant at;

    @Column(name = "type", nullable = false, length = 40)
    private String type;

    @Column(name = "username", length = 40)
    private String username;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "detail", length = 500)
    private String detail;

    protected SecurityAudit() {
    }

    public SecurityAudit(
            Instant at,
            String type,
            String username,
            String ip,
            String requestId,
            String detail) {

        this.at = at;
        this.type = type;
        this.username = username;
        this.ip = ip;
        this.requestId = requestId;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public Instant getAt() {
        return at;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getDetail() {
        return detail;
    }
}
