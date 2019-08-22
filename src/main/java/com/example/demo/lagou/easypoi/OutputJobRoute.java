package com.example.demo.lagou.easypoi;

import cn.afterturn.easypoi.excel.annotation.Excel;
import com.example.demo.lagou.model.RouteTransit;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OutputJobRoute implements Serializable {
    private static final long serialVersionUID = -1443811068930105508L;

    @Excel(name = "距离(米)", orderNum = "20", width = 20)
    private String distance;
    @Excel(name = "上班耗时", orderNum = "20", width = 20)
    private String durationStr;
    @Excel(name = "需要步行(米)", orderNum = "20", width = 20)
    private Long walking_distance;
    @Excel(name = "路费(元)", orderNum = "20", width = 20)
    private Float cost;
    @Excel(name = "打车花费(元)", orderNum = "20", width = 20)
    private String taxi_cost;
}
