package edu.itc.shop;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import edu.itc.shop.domain.Role;
import edu.itc.shop.repository.UserAccountRepository;
import edu.itc.shop.service.AccountService;

@SpringBootApplication
public class ShopSecureApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopSecureApplication.class, args);
    }

    @Bean
    CommandLineRunner seedUsers(
            UserAccountRepository users,
            AccountService accounts,
            @Value("${shop.seed.admin-password}") String adminPass,
            @Value("${shop.seed.customer-password}") String customerPass) {
        return args -> {
            if (!users.existsByUsername("admin")) {
                accounts.register("admin", adminPass, Set.of(Role.ADMIN, Role.CUSTOMER));
            }
            if (!users.existsByUsername("alice")) {
                accounts.register("alice", customerPass, Set.of(Role.CUSTOMER));
            }
        };
    }
}