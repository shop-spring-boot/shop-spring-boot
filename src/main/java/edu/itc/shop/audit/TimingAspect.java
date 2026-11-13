package edu.itc.shop.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TimingAspect {
    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);

    // Added the order package to the execution list
    @Around("execution(public * edu.itc.shop.product.*Service.*(..)) || " +
            "execution(public * edu.itc.shop.notification.*Service.*(..)) || " +
            "execution(public * edu.itc.shop.order.*Service.*(..))")
    public Object time(ProceedingJoinPoint pjp) throws Throwable {
        long t0 = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            // Changed to log.info so it prints in the console
            log.info("{} took {} us", pjp.getSignature().toShortString(),
                    (System.nanoTime() - t0) / 1_000);
        }
    }
}