package com.chronos.gateway;

import com.chronos.gateway.config.GatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class ChronosDeviceGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChronosDeviceGatewayApplication.class, args);
    }
}
