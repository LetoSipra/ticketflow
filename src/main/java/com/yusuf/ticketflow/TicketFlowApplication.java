package com.yusuf.ticketflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
//import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// @EnableScheduling // Scheduler for later use
@ConfigurationPropertiesScan
public class TicketFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketFlowApplication.class, args);
    }
}