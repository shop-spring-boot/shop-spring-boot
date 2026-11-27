package edu.itc.shop.web;

import java.security.Principal;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {

    /** Public: reachable without login (see SecurityConfig). */
    @GetMapping("/public/hello")
    public Map<String, String> publicHello() {
        return Map.of("message", "Hello, anyone!");
    }

    /** Protected: needs a valid login. */
    @GetMapping("/private/hello")
    public Map<String, String> privateHello(Principal principal) {
        return Map.of("message", "Hello, " + principal.getName() + "!");
    }
}
