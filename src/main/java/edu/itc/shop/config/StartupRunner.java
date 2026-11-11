package edu.itc.shop.config;

import edu.itc.shop.product.ReportBuilder;
import edu.itc.shop.notification.WelcomeService;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component 
public class StartupRunner implements CommandLineRunner{
    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

    private final ApplicationContext context;
    private final WelcomeService welcomeService;
    private final ObjectProvider<ReportBuilder> reportBuilders;

    public StartupRunner(ApplicationContext context, WelcomeService welcomeService, ObjectProvider<ReportBuilder> reportBuilders){
        this.context = context;
        this.welcomeService = welcomeService;
        this.reportBuilders = reportBuilders;
    }

    @Override 
    public void run(String... args){
        var notifiers = Arrays.stream(context.getBeanDefinitionNames()).filter(n -> n.contains("Notifier")).sorted().toList();
        log.info("{} beans; notifiers = {}", context.getBeanDefinitionNames(), notifiers);

        welcomeService.Welcome("ann", false);
        welcomeService.Welcome("bob", true);

        ReportBuilder a = reportBuilders.getObject();
        ReportBuilder b = reportBuilders.getObject();
        log.info("prototype instances differ: {}", System.identityHashCode(a) != System.identityHashCode(b));
    }
    
}
