package com.example.demo.spider;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.easypoi.OutputJob;
import com.example.demo.easypoi.OutputJobDetail;
import com.example.demo.easypoi.OutputJobHandler;
import com.example.demo.easypoi.OutputJobRoute;
import com.example.demo.model.PositionDetail;
import com.example.demo.model.PositionInfo;
import com.example.demo.model.RouteInfo;
import com.example.demo.properties.GaodeProperties;
import com.example.demo.spider.lagou.LagouSpider;
import com.example.demo.utils.DateUtils;
import com.example.demo.utils.HttpUtils;
import com.example.demo.utils.PoiUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LagouSpiderSingleThread implements InitializingBean {

    @Autowired
    private GaodeProperties gaodeProperties;

    @Autowired
    private MongoTemplate mongoTemplate;


    private LagouSpider spider;

    @Override
    public void afterPropertiesSet() throws Exception {
        spider = new LagouSpider();
        spider.initSpider(gaodeProperties, mongoTemplate);
    }

    public void crawl(Map<String,String> searchParams,Integer pageStart, Integer pageEnd) throws InterruptedException {
        for (int page = pageStart; page <= pageEnd; page++) {
            spider.crawl(searchParams, page);
        }
    }

}
