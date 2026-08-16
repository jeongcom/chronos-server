package com.chronos.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages="com.chronos")
public class ChronosApplication {
    public static void main(String[] args){SpringApplication.run(ChronosApplication.class,args);}
}
