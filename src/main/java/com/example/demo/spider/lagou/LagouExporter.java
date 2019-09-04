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

@Component
public class LagouExporter {
    @Autowired
    private GaodeProperties gaodeProperties;
    @Autowired
    private MongoTemplate mongoTemplate;

    public void exportExcel(){
        Query query = new Query();
        query.with(Sort.by(
                Sort.Order.asc("routeInfo.bestTransit.duration"),
                Sort.Order.desc("createTime")
        ));
        List<PositionInfo> positions = this.mongoTemplate.find(query, PositionInfo.class);

        List<OutputJob> outputJobs = Lists.newArrayList();
        positions.forEach(position -> {
            OutputJob outputJob = new OutputJob();
            BeanUtils.copyProperties(position, outputJob);
            OutputJobDetail outputJobDetail = new OutputJobDetail();
            if(position.getPositionDetail()!=null)
                BeanUtils.copyProperties(position.getPositionDetail(), outputJobDetail);
            OutputJobRoute routeInfo = new OutputJobRoute();
            if(position.getRouteInfo()!=null){
                BeanUtils.copyProperties(position.getRouteInfo(), routeInfo);
                if(position.getRouteInfo().getBestTransit()!=null){
                    BeanUtils.copyProperties(position.getRouteInfo().getBestTransit(), routeInfo);
                }
            }
            outputJob.setPositionDetail(outputJobDetail);
            outputJob.setRouteInfo(routeInfo);
            outputJobs.add(outputJob);
        });
        ExportParams params = new ExportParams(gaodeProperties.getKd().toUpperCase() + "职位信息（拉勾）", DateUtils.todayDateStr(), ExcelType.XSSF);
        params.setDataHandler(new OutputJobHandler());
        Workbook workbook = ExcelExportUtil.exportExcel(params, OutputJob.class, outputJobs);

        //列的高度自适应
        XSSFSheet sheet = (XSSFSheet)workbook.getSheetAt(0);
        for(int i = 1; i <= sheet.getLastRowNum(); i ++) {
            XSSFRow row = sheet.getRow(i);
            PoiUtils.calcAndSetRowHeigt(row);
        }
        short lastCellNum = sheet.getRow(2).getLastCellNum();
        String alphabeta = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lastColCode = "";
        if(lastCellNum>26){
            lastColCode += alphabeta.charAt(lastCellNum/26-1);
        }
        lastColCode += alphabeta.charAt(lastCellNum%26-1)+"2";

        sheet.setAutoFilter(CellRangeAddress.valueOf("C2:" + lastColCode));

        File saveFile = new File("job-excel");
        if (!saveFile.exists()) {
            saveFile.mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(String.format("job-excel/%s职位信息（拉勾-%s）.xlsx", gaodeProperties.getKd().toUpperCase(), DateUtils.todayDateStr()));
            workbook.write(fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
