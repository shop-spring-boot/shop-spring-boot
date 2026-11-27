package edu.itc.shop.web;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.shop.domain.Role;
import edu.itc.shop.domain.UserAccount;
import edu.itc.shop.service.AccountService;
import edu.itc.shop.web.dto.RegisterRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class RegisterController {

    private final AccountService accounts;

    public RegisterController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@RequestBody @Valid RegisterRequest req) {
        UserAccount saved = accounts.register(req.username(), req.password(), Set.of(Role.CUSTOMER));
        return Map.of("username", saved.getUsername());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Map.of("name", auth.getName(), "roles", roles);
    }
}
