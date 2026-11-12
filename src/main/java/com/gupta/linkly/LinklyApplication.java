package com.gupta.linkly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LinklyApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinklyApplication.class, args);
    }
}
