
package edu.itc.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.itc.shop.domain.SecurityAudit;

public interface SecurityAuditRepository
        extends JpaRepository<SecurityAudit, Long> {

    List<SecurityAudit> findAllByOrderByAtDesc();

    List<SecurityAudit> findByUsernameOrderByAtDesc(String username);
}
