package edu.itc.shop.notification;

import java.time.Clock;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service 
public class WelcomeService {
    private final Notifier defaulNotifier;
    private final Notifier urgenNotifier;
    private final Clock clock;

    public WelcomeService(@Qualifier("emailNotifier") Notifier defaulNotifier, 
                        @Qualifier("smsNotifier") Notifier urgenNotifier, 
                        Clock clock) {
        this.defaulNotifier = defaulNotifier;
        this.urgenNotifier = urgenNotifier;
        this.clock = clock;
    }
    public String Welcome(String user, boolean urgent){
        String text = "Welcome " + user + "! It is " + LocalTime.now(clock).withNano(0);
        Notifier chosen = urgent ? urgenNotifier : defaulNotifier;
        chosen.send(user, text);
        return text;
    }
}
