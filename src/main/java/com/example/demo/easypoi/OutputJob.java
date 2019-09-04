package com.example.demo.easypoi;

import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.excel.annotation.ExcelEntity;
import cn.afterturn.easypoi.excel.annotation.ExcelIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class OutputJob implements Serializable {
    private static final long serialVersionUID = -1443811068930105508L;

    /**公司id*/
    @Excel(name = "公司ID", width = 20, isColumnHidden=true)
    private Long companyId;
    /**职位名*/
    @Excel(name = "职位名", orderNum = "3", width = 20)
    private String positionName;
    /**工作年限要求*/
    @Excel(name = "工作年限要求", orderNum = "4", width = 20)
    private String workYear;
    /**学历要求*/
    @Excel(name = "学历要求", orderNum = "5", width = 20)
    private String education;
    /**是否全职*/
    @Excel(name = "工作性质", orderNum = "6", width = 20)
    private String jobNature;
    /**融资情况**/
    @Excel(name = "融资情况", orderNum = "9", width = 20)
    private String financeStage;
    /**公司规模**/
    @Excel(name = "公司规模", orderNum = "7", width = 20)
    private String companySize;
    /**公司logo**/
    @Excel(name = "公司logo", orderNum = "99", width = 20)
    private String companyLogo;

    @Excel(name = "所属领域", orderNum = "9", width = 20)
    private String industryField;

    @Excel(name = "公司全称", orderNum = "2", width = 20)
    private String companyFullName;

    @Excel(name = "公司简称", orderNum = "1", width = 20)
    private String companyShortName;

    @Excel(name = "薪资范围", orderNum = "8", width = 20)
    private String salary;

    @Excel(name = "岗位ID", isColumnHidden = true, width = 20)
    private Long positionId;

    @Excel(name = "优势", orderNum = "11", width = 20)
    private String positionAdvantage;

    @Excel(name = "创建日期", orderNum = "10", exportFormat="yyyy年MM月dd日 HH:mm", width = 20)
    private Date createTime;

    @ExcelIgnore
    private Integer score;

    @ExcelIgnore
    private Integer approve;

//    private List<String> positionLables;
//    private List<String> industryLables;
//    private List<String> companyLabelList;
//    private Long publisherId;
//    private List<String> businessZones;
//    private Double longitude;
//    private Double latitude;
//    private String formatCreateTime;
//    private Integer adWord;
//    private Integer deliver;
//    private String gradeDescription;
//    private String promotionScoreExplain;
//
//    private String firstType;
//    private String secondType;
//    private String thirdType;
//
//    private List<String> skillLables;
//    private Integer isSchoolJob;
//    private Integer isHotHire;
//    private List<String> hitags;
    @Excel(name = "所在城市", orderNum = "14", width = 20, isColumnHidden = true)
    private String city;

    @Excel(name = "所在区", orderNum = "14", width = 20)
    private String district;

    @Excel(name = "附近地铁", orderNum = "14", width = 20)
    private String subwayline;
    @Excel(name = "最近地铁站", orderNum = "15", width = 20)
    private String stationname;
    @Excel(name = "地铁线路", orderNum = "16", width = 20)
    private String linestaion;
//    private Integer resumeProcessRate;
//    private Integer resumeProcessDay;
//    private String imState;
//    private Date lastLogin;
//    private String explain;
//    private String plus;
//    private Integer pcShow;
//    private Integer appShow;

    @ExcelEntity
    private OutputJobDetail positionDetail;
    @ExcelEntity
    private OutputJobRoute routeInfo;
}
