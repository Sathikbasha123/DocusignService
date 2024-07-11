package com.saaspe.docusign.esignature;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
@ComponentScan(basePackages = "com.saaspe.docusign.*")
@SpringBootApplication
@EnableAsync
public class Esignature {

    public static void main(String[] args) {

        SpringApplication.run(Esignature.class, args);
    }

}
