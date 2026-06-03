package com.bank.demo.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Jenkins AI Monitor application.
 *
 * @SpringBootApplication   = @Configuration + @ComponentScan + @EnableAutoConfiguration
 *   (Python analogy: this is like your main() + setting up dependency injection container)
 *
 * @EnableScheduling        = turns on Spring's cron/fixed-rate task scheduler
 *   (Python analogy: like APScheduler's BackgroundScheduler.start())
 */
@SpringBootApplication
@EnableScheduling
public class JenkinsAiMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(JenkinsAiMonitorApplication.class, args);
    }
}
