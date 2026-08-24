package com.pipelinecrm.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.pipelinecrm")
public class PipelineCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipelineCrmApplication.class, args);
    }
}
