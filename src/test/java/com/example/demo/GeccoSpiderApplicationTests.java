package com.example.demo;

import com.example.demo.lagou.LagouSpider;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URLEncoder;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GeccoSpiderApplicationTests {

    @Autowired
    private LagouSpider lagouSpider;

    @Test
    public void doCrawl() {
        try {
            Map<String,String> searchParams = ImmutableMap.of(
                    "px", "new",   //按新到旧排序
                    "gx", "全职",  //只抓全职
                    "yx", "15k-25k" //薪资范围
            );
            lagouSpider.crawl(searchParams,1);  //抓前30页
            lagouSpider.exportExcel(); //导出excel
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
