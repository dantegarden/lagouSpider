package com.example.demo.lagou.easypoi;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;

import java.io.Serializable;

@Data
public class OutputJobDetail implements Serializable {

    private static final long serialVersionUID = -1443811068930105508L;

    @Excel(name = "发布时间", orderNum = "10", width = 20)
    private String publishTime;
    @Excel(name = "岗位要求", orderNum = "12", width = 100)
    private String description;
    @Excel(name = "地址", orderNum = "13", width = 20)
    private String companyAddress;
    @Excel(name = "岗位发布人", orderNum = "98", width = 20)
    private String publisher;
    @Excel(name = "发布人职位", orderNum = "98", width = 20)
    private String publisherJob;
    @Excel(name = "公司官网", orderNum = "99", width = 20, isHyperlink = true)
    private String officialWebsite;
    @Excel(name = "原网址", orderNum = "100", width = 20, isHyperlink = true)
    private String clickUrl;
}
