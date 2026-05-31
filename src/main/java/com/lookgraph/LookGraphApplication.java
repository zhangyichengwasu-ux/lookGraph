package com.lookgraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LookGraphApplication {

    public static void main(String[] args) {
        SpringApplication.run(LookGraphApplication.class, args);
    }
}
