package com.example.demo.anti.proxy;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class ProxyIp {
    private String ip;
    private Integer port;
    private String serverAddress;
    private boolean isHidden;
    private boolean isHttps;
    private double speed;
    private double linkSpeed;
    private Date surviveTime; //到期时间
    private Date checkedTime;
}
