package com.example.demo.spider.lagou;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import com.example.demo.easypoi.OutputJob;
import com.example.demo.easypoi.OutputJobDetail;
import com.example.demo.easypoi.OutputJobHandler;
import com.example.demo.easypoi.OutputJobRoute;
import com.example.demo.model.PositionInfo;
import com.example.demo.properties.GaodeProperties;
import com.example.demo.utils.DateUtils;
import com.example.demo.utils.PoiUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

@Component
@Slf4j
public class LagouRunner {

    public void startCrawl(Map<String,String> searchParams, int fromPage, int toPage){
        ForkJoinPool pool = new ForkJoinPool();
        LagouTask rootTask = LagouTask.newBean(searchParams, fromPage, toPage);
        long startTime = System.currentTimeMillis();
        pool.execute(rootTask);
        rootTask.join();

        pool.shutdown();
        System.out.println("耗时 "+ (System.currentTimeMillis() - startTime) + " ms ");
    }


}
