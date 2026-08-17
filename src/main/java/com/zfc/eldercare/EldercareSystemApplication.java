package com.zfc.eldercare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EldercareSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EldercareSystemApplication.class, args);
    }

}