package com.example.demo;

import com.example.demo.spider.lagou.LagouExporter;
import com.example.demo.spider.lagou.LagouRunner;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**采用多线程的爬虫**/
@RunWith(SpringRunner.class)
@SpringBootTest
public class LagouAppTest {

    @Autowired
    private LagouRunner lagouRunner;

    @Autowired
    private LagouExporter exporter;

    @Test
    public void doCrawlLagou() {
        Map<String,String> searchParams = ImmutableMap.of(
                "px", "new",   //按新到旧排序
                "gx", "全职",  //只抓全职
                "yx", "15k-25k" //薪资范围
        );
        lagouRunner.startCrawl(searchParams, 1, 10);
        exporter.exportExcel();
    }
}
