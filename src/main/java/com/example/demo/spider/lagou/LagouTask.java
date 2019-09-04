package com.example.demo.spider.lagou;

import com.example.demo.config.CtxConifg;
import com.example.demo.properties.GaodeProperties;
import com.example.demo.utils.SleepTools;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Map;
import java.util.concurrent.RecursiveAction;

public class LagouTask extends RecursiveAction {

    private int fromPage; //开始页
    private int toPage; //结束页

    private Map<String,String> searchParams; //抓取筛选条件

    private LagouSpider spider;

    private LagouTask(Map<String,String> searchParams, int fromPage, int toPage) {
        this.searchParams = searchParams;
        this.fromPage = fromPage;
        this.toPage = toPage;
    }

    public static LagouTask newBean(Map<String,String> searchParams, int fromPage, int toPage){
        ConfigurableApplicationContext context = (ConfigurableApplicationContext) CtxConifg.getContext();
        LagouTask lagouTask = new LagouTask(searchParams, fromPage, toPage);
        LagouSpider spider = new LagouSpider();
        spider.setMongoTemplate(context.getBean(MongoTemplate.class));
        spider.setGaodeProperties(context.getBean(GaodeProperties.class));
        lagouTask.setSpider(spider);
        lagouTask.initSpider(); //初始化
        return lagouTask;
    }

    @Override
    protected void compute() {
        System.out.println(Thread.currentThread().getName());
        if(toPage - fromPage == 0){ //抓取单页
            try {
                spider.crawl(searchParams, toPage);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            int midPage = (toPage + fromPage)/2;
            LagouTask left = LagouTask.newBean(searchParams, fromPage, midPage);
            LagouTask right = LagouTask.newBean(searchParams,midPage+1, toPage);

            invokeAll(left, right);
            left.join();
            right.join();
        }
    }

    public void setSpider(LagouSpider spider) {
        this.spider = spider;
    }

    public void initSpider(){
        if(spider!=null)
            spider.initSpider();
    }
}
