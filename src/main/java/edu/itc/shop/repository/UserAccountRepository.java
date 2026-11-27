package edu.itc.shop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.itc.shop.domain.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
}