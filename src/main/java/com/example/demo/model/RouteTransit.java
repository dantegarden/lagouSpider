package com.example.demo.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class RouteTransit {

    @JSONField
    private Float cost;
    private Integer duration;
    private String durationStr;
    private Boolean nightflag;
    private Long walking_distance;//米
    private JSONArray segments;
    
    public void setCost(Object cost){
        if(cost instanceof String){
            String s = String.valueOf(cost);
            this.cost = new Float(s);
        }
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
        String durationStr = "";
        if(this.duration!=null){
            if(this.duration/3600 > 0){
                durationStr += this.duration/3600 + "小时";
            }
            durationStr += (this.duration%3600)/60 + "分" + (this.duration%3600)%60 + "秒";
        }
        this.durationStr = durationStr;
    }

}
