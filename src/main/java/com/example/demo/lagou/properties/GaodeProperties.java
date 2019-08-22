package com.example.demo.lagou.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Data
@Component
@ConfigurationProperties(prefix = "gaode")
public class GaodeProperties {
    private String appKey;
    private String coordirate;
    private String city;
    private String kd;
}
