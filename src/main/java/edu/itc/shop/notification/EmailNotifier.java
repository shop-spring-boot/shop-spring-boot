package edu.itc.shop.notification;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary 
public class EmailNotifier implements Notifier{
    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    @Override 
    public void send(String to, String text) {
        log.info("EMAIL to={} text={}", to ,text);
    }

    @PostConstruct 
    void start() {log.info("EmailNotofier ready");}

    @PreDestroy 
    void stop() {log.info("EmailNotifier closing");}
    
}
