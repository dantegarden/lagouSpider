package com.example.demo.lagou.model;

import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Data
public class RouteInfo {
    private String origin;
    private String destination;
    private String distance;
    private String taxi_cost;
    private List<RouteTransit> transits;
    private RouteTransit bestTransit;
}
