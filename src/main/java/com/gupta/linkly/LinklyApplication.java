package com.gupta.linkly;

import com.gupta.linkly.config.TlsWarmup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LinklyApplication {

    public static void main(String[] args) {
        TlsWarmup.run();
        SpringApplication.run(LinklyApplication.class, args);
    }
}
