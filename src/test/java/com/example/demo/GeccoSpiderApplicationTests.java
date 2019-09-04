package com.example.demo;

import com.example.demo.spider.BossZhiPinSpider;
import com.example.demo.spider.LagouSpiderSingleThread;
import com.example.demo.spider.lagou.LagouExporter;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GeccoSpiderApplicationTests {

    @Autowired
    private LagouSpiderSingleThread lagouSpiderSingleThread;
    @Autowired
    private LagouExporter lagouExporter;

    @Autowired
    private BossZhiPinSpider bossZhiPinSpider;

    @Test
    public void doCrawlLagou() {
        try {
            Map<String,String> searchParams = ImmutableMap.of(
                    "px", "new",   //按新到旧排序
                    "gx", "全职",  //只抓全职
                    "yx", "15k-25k" //薪资范围
            );
            lagouSpiderSingleThread.crawl(searchParams,1,30);  //抓前30页
            lagouExporter.exportExcel(); //导出excel
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void doCrawlZhipin(){
        try {
            bossZhiPinSpider.crawl(1,30);
            bossZhiPinSpider.exportExcel();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
