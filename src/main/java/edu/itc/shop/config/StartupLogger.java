package edu.itc.shop.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger {

    @EventListener(ApplicationReadyEvent.class)
    public void logUrlOnStartup() {
        System.out.println("\n========================================================");
        System.out.println("Server is running! Click below to open in browser:");
        System.out.println("http://localhost:8080");
        System.out.println("http://localhost:8080/products");
        System.out.println("Products API: http://localhost:8080/api/products");
        System.out.println("========================================================\n");
    }
}