package com.pipelinecrm.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Layer 4 composition root. This class exists to start the framework and to point it at
 * the adapter packages; it holds no behaviour of its own.
 */
@SpringBootApplication(scanBasePackages = "com.pipelinecrm")
public class PipelineCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipelineCrmApplication.class, args);
    }
}
