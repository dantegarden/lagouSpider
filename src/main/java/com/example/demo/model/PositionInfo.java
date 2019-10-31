package com.example.demo.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection="position_info")
@Data
public class PositionInfo {
    /**公司id*/
    private Long companyId;
    /**职位名*/
    private String positionName;
    /**工作年限要求*/
    private String workYear;
    /**学历要求*/
    private String education;
    /**是否全职*/
    private String jobNature;
    /**融资情况**/
    private String financeStage;
    /**公司规模**/
    private String companySize;
    /**公司logo**/
    private String companyLogo;
    private String industryField;
    private String companyFullName;
    private String companyShortName;
    private String city;
    private String district;
    private String salary;

    @Id
    private Long positionId;
    private String positionAdvantage;
    @JSONField(format="yyyy-MM-dd")
    private Date createTime;
    private Integer score;
    private Integer approve;
    private List<String> positionLables;
    private List<String> industryLables;
    private List<String> companyLabelList;
    private Long publisherId;
    private List<String> businessZones;
    private Double longitude;
    private Double latitude;
    private String formatCreateTime;
    private Integer adWord;
    private Integer deliver;
    private String gradeDescription;
    private String promotionScoreExplain;

    private String firstType;
    private String secondType;
    private String thirdType;

    private List<String> skillLables;
    private Integer isSchoolJob;
    private Integer isHotHire;
    private List<String> hitags;
    private String subwayline;
    private String stationname;
    private String linestaion;
    private Integer resumeProcessRate;
    private Integer resumeProcessDay;
    private String imState;
    private Date lastLogin;
    private String explain;
    private String plus;
    private Integer pcShow;
    private Integer appShow;

    private PositionDetail positionDetail;
    private RouteInfo routeInfo;

    public void setCompanyLogo(String companyLogo) {
        if(!companyLogo.startsWith("http://")){
            companyLogo = "http://www.lgstatic.com/thumbnail_160x160/" + companyLogo;
        }
        this.companyLogo = companyLogo;
    }

    @Override
    public String toString() {
        return "PositionInfo{" +
                "companyId=" + companyId +
                ", positionName='" + positionName + '\'' +
                ", workYear='" + workYear + '\'' +
                ", education='" + education + '\'' +
                ", jobNature='" + jobNature + '\'' +
                ", financeStage='" + financeStage + '\'' +
                ", companySize='" + companySize + '\'' +
                ", companyLogo='" + companyLogo + '\'' +
                ", industryField='" + industryField + '\'' +
                ", companyFullName='" + companyFullName + '\'' +
                ", companyShortName='" + companyShortName + '\'' +
                ", city='" + city + '\'' +
                ", district='" + district + '\'' +
                ", salary='" + salary + '\'' +
                ", positionId=" + positionId +
                ", positionAdvantage='" + positionAdvantage + '\'' +
                ", createTime=" + createTime +
                ", score=" + score +
                ", approve=" + approve +
                ", positionLables=" + positionLables +
                ", industryLables=" + industryLables +
                ", companyLabelList=" + companyLabelList +
                ", publisherId=" + publisherId +
                ", businessZones=" + businessZones +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", formatCreateTime='" + formatCreateTime + '\'' +
                ", adWord=" + adWord +
                ", deliver=" + deliver +
                ", gradeDescription='" + gradeDescription + '\'' +
                ", promotionScoreExplain='" + promotionScoreExplain + '\'' +
                ", firstType='" + firstType + '\'' +
                ", secondType='" + secondType + '\'' +
                ", thirdType='" + thirdType + '\'' +
                ", skillLables=" + skillLables +
                ", isSchoolJob=" + isSchoolJob +
                ", isHotHire=" + isHotHire +
                ", hitags=" + hitags +
                ", subwayline='" + subwayline + '\'' +
                ", stationname='" + stationname + '\'' +
                ", linestaion='" + linestaion + '\'' +
                ", resumeProcessRate=" + resumeProcessRate +
                ", resumeProcessDay=" + resumeProcessDay +
                ", imState='" + imState + '\'' +
                ", lastLogin=" + lastLogin +
                ", explain='" + explain + '\'' +
                ", plus='" + plus + '\'' +
                ", pcShow=" + pcShow +
                ", appShow=" + appShow +
                '}';
    }
}
