package com.example.demo.spider.lagou;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.example.demo.anti.UserAgent;
import com.example.demo.anti.proxy.CircleBlockingQueue;
import com.example.demo.anti.proxy.ProxyIp;
import com.example.demo.anti.proxy.ProxySpider;
import com.example.demo.model.PositionDetail;
import com.example.demo.model.PositionInfo;
import com.example.demo.model.RouteInfo;
import com.example.demo.properties.GaodeProperties;
import com.example.demo.utils.HttpUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class LagouSpider {

    private final Logger log = LoggerFactory.getLogger(LagouSpider.class);

    public String START_URL = "https://www.lagou.com/jobs/list_%s?city=%s&needAddtionalResult=false&fromSearch=true&labelWords=&suginput=";
    public String PARSE_URL = "https://www.lagou.com/jobs/positionAjax.json?city=%s&needAddtionalResult=false";
    public String DETAIL_URL = "https://www.lagou.com/jobs/{positionId}.html";
    public Map<String,String> HEADERS = null;

    public static final String GAODE_URL = "https://restapi.amap.com/v3/direction/transit/integrated";

    private GaodeProperties gaodeProperties;
    private MongoTemplate mongoTemplate;

    private List<PositionInfo> retiesPositions = Lists.newArrayList();

    //重试
    private static final Integer MAX_RETRIES = 3;
    private static final Integer DEFAULT_WAIT_DURATION = 5000;
    private static final Integer DEFAULT_WAIT_TIMES = 20;

    //代理ip池
    private final ProxySpider proxySpider = new ProxySpider(1);

    /**给单线程调试时复用**/
    public void initSpider(GaodeProperties g, MongoTemplate m){
        this.gaodeProperties = g;
        this.mongoTemplate = m;
        this.initSpider();
    }

    public void initSpider(){
        this.initSpider(true);
    }

    /**初始化操作*/
    public void initSpider(boolean useProxy) {
        try {
            String encodeKd = URLEncoder.encode(gaodeProperties.getKd(), "UTF-8");
            String encodeCity = URLEncoder.encode(gaodeProperties.getCity(), "UTF-8");
            START_URL = String.format(START_URL, encodeKd, encodeCity);
            PARSE_URL = String.format(PARSE_URL, gaodeProperties.getCity());
            HEADERS = ImmutableMap.of(
                    "Accept", "application/json, text/javascript, */*; q=0.01",
                    "Referer", String.format("https://www.lagou.com/jobs/list_%s?city=%s&cl=false&fromSearch=true&labelWords=&suginput=", encodeKd, encodeCity),
                    "user-agent", UserAgent.randomOne()
            );

            //是否启用代理池
            if(useProxy){
//                while(true){
//                    if(proxySpider.isProxyReady())
//                        break;
//                }
                log.info("代理池就绪");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void crawl(Map<String,String> searchParams, Integer page) throws InterruptedException {
        this.crawl(searchParams, page, 0);
    }

    public void crawl(Map<String,String> searchParams, Integer page, int retries) throws InterruptedException {
        log.info("-------获取初始cookies--------");
        List<String> cookies = HttpUtils.sendForGettingCookie(START_URL, HEADERS, null);
        cookies.forEach(log::info);

        String parse_url = PARSE_URL;
        if(!searchParams.isEmpty())
            for (Map.Entry<String,String> searchEntry: searchParams.entrySet()) {
                parse_url += "&" + searchEntry.getKey() + "=" + searchEntry.getValue();
            }

        log.info("-------抓取第{}页--------", page);
        Map<String,String> data = ImmutableMap.of(
                "first", "true",
                "pn", String.valueOf(page),
                "kd", gaodeProperties.getKd());
        String result = HttpUtils.sendPost(PARSE_URL, cookies, HEADERS,  data);
        log.info(result);
        List<PositionInfo> positionInfoList = null;
        try{
            JSONObject resultObject = JSON.parseObject(result);
            if(resultObject.get("state")!=null && resultObject.getInteger("state").equals(2402)){
                resultObject = waitAndRetry(cookies, data);
            }
            JSONArray infoArr = resultObject.getJSONObject("content").getJSONObject("positionResult").getJSONArray("result");
            positionInfoList = JSONObject.parseArray(infoArr.toJSONString(),  PositionInfo.class);
        }catch (NullPointerException | JSONException e){
            log.error("抓取第{}页失败", page);
            if(retries < MAX_RETRIES ){
                retries = retries + 1;
                //重试
                log.error("抓取第{}页失败，重试第{}次", page, retries);
                crawl(searchParams, page, retries);
                return;
            }else{
                log.error("抓取第{}页失败，已达到最大重试次数", page);
                return;
            }
        }

        crawlDetail(cookies, positionInfoList);

//        log.info("------更换cookies");
//        cookies = HttpUtils.sendForGettingCookie(START_URL, HEADERS, null);
//        cookies.forEach(log::info);
//        Thread.sleep(10000);

        log.info("--------常规抓取完毕，对失败的进行重试--------");
        if(!CollectionUtils.isEmpty(retiesPositions))
            while(retiesPositions.size() > 0){
                PositionInfo retryPosition = retiesPositions.remove(0);
                crawlDetail(cookies, Collections.singletonList(retryPosition));
            }

        System.out.println("完毕");
    }



    public void crawlDetail(List<String> cookies, List<PositionInfo> positionInfoList){
        for (PositionInfo position: positionInfoList) {
            log.info("------抓取职位{}明细", position.getPositionId());
            WriteFlagContext.setWriteFlag(Boolean.TRUE);
            getPositionDetail(cookies, position);
            RouteInfo routeInfo = exist(position.getPositionId());
            if(routeInfo==null){
                log.info("------抓取通勤路线规划{}", position.getPositionId());
                getRouteInfo(position);
            }else{
                position.setRouteInfo(routeInfo);
            }
            //String detailResult = HttpUtils.sendGet(detail_url, HEADERS, null);
            if(WriteFlagContext.getWriteFlag()){
                mongoTemplate.save(position);
            }else{
                retiesPositions.add(position);
            }
        }
    }

    public void getPositionDetail(List<String> cookies, PositionInfo position) {
        this.getPositionDetail(cookies, position, 0);
    }

    public void getPositionDetail(List<String> cookies, PositionInfo position, int retries) {
        try {
            String detail_url = DETAIL_URL.replace("{positionId}", position.getPositionId()+"");
            String cookie = StringUtils.join(cookies, " ");

            Document doc = Jsoup.connect(detail_url)
                    .headers(HEADERS)
                    .header("Cookie", cookie)
                    .timeout(3000)
                    .get();

            PositionDetail positionDetail = new PositionDetail();
            positionDetail.setPositionForm(doc.select(".position-head h4.company").text());
            positionDetail.setPositionName(doc.select(".position-head h2.name").text());
            positionDetail.setPublishTime(doc.select(".position-head p.publish_time").text());
            positionDetail.setJobAdventages(doc.select(".content_l dd.job-advantage p").text());
            //职位描述
            String jobDetailHTML = doc.select("dl.job_detail .job-detail").html();
            jobDetailHTML = jobDetailHTML.replace("<br>", "#####");
            Document doc2 = Jsoup.parse(jobDetailHTML);
            List<String> descriptions = doc2.select("p").eachText();
            descriptions = descriptions.stream().map(desc -> { return desc.replace("#####", "\n");}).collect(Collectors.toList());
            positionDetail.setDescription(StringUtils.join(descriptions, "\n\t"));
            //位置
            String address = doc.select(".job-address .work_addr").text().replace("查看地图", "");
            positionDetail.setCompanyAddress(address);
            //发布人
            positionDetail.setPublisher(doc.select(".publisher_name .name").text());
            positionDetail.setPublisherJob(doc.select(".publisher_name .pos").text());
            //公司信息
            positionDetail.setCompanyName(doc.select("#job_company .fl-cn").text());
            positionDetail.setIndustryField(doc.select("#job_company li:contains(领域) .c_feature_name").text());
            positionDetail.setFinanceStage(doc.select("#job_company li:contains(发展阶段) .c_feature_name").text());
            positionDetail.setFinanceOrg(doc.select("#job_company li:contains(投资机构) .c_feature_name").text());
            positionDetail.setCompanySize(doc.select("#job_company li:contains(规模) .c_feature_name").text());
            positionDetail.setOfficialWebsite(doc.select("#job_company li:contains(公司主页) a").text());
            //连接
            positionDetail.setClickUrl(detail_url);

            position.setPositionDetail(positionDetail);
        } catch (IOException e) {
            log.info("------抓取职位{}明细失败", position.getPositionId());
            if(retries < MAX_RETRIES ){
                retries = retries + 1;
                //重试
                log.error("抓取职位{}明细失败，重试第{}次", position.getPositionId(), retries);
                getPositionDetail(cookies, position, retries);
                return;
            }else{
                log.error("抓取职位{}明细失败，已达到最大重试次数", position.getPositionId());
                WriteFlagContext.setWriteFlag(Boolean.FALSE);
                return;
            }
        }

    }

    public void getRouteInfo(PositionInfo positionInfo){
        this.getRouteInfo(positionInfo, 0);
    }

    public void getRouteInfo(PositionInfo positionInfo, int retries){
        try {
            Double longtitude = positionInfo.getLongitude(); //经度
            Double latitude = positionInfo.getLatitude(); //纬度
            Map<String, String> data = (Map)ImmutableMap.builder()
                    .put("key", gaodeProperties.getAppKey())
                    .put("origin", gaodeProperties.getCoordirate())
                    .put("destination", longtitude + "," + latitude)
                    .put("city", gaodeProperties.getCity())
                    .put("cityd", gaodeProperties.getCity())
                    .put("strategy", "0") //时间短优先
                    .put("nightflag", "0")
                    .build();
            String distanceResult = HttpUtils.sendGet(GAODE_URL, ImmutableMap.of(
                    "accept", "application/json, text/javascript, */*; q=0.01",
                    "content-type", "application/x-www-form-urlencoded; charset=UTF-8",
                    "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36"), data);
            System.out.println(distanceResult);
            JSONObject resultObject = JSON.parseObject(distanceResult);
            Integer status = resultObject.getInteger("status");
            if(status==1){
                RouteInfo routeInfo = JSONObject.parseObject(resultObject.getJSONObject("route").toJSONString(), RouteInfo.class);
                if(!CollectionUtils.isEmpty(routeInfo.getTransits())){
                    routeInfo.setBestTransit(routeInfo.getTransits().get(0));
                }
                positionInfo.setRouteInfo(routeInfo);
            }
        } catch (Exception e) {
            log.info("------抓取通勤路线规划{}", positionInfo.getPositionId());
            if(retries < MAX_RETRIES ){
                retries = retries + 1;
                //重试
                log.error("------抓取通勤路线规划{}，重试第{}次", positionInfo.getPositionId(), retries);
                getRouteInfo(positionInfo, retries);
                return;
            }else{
                log.error("------抓取通勤路线规划{}，已达到最大重试次数", positionInfo.getPositionId());
                WriteFlagContext.setWriteFlag(Boolean.FALSE);
                return;
            }
        }
    }

    /**是否已持久化**/
    public RouteInfo exist(Long positionId){
        Query query = Query.query(Criteria.where("positionId").is(positionId));
        PositionInfo one = mongoTemplate.findOne(query, PositionInfo.class);
        return one==null? null: one.getRouteInfo();
    }


    private JSONObject waitAndRetry(List<String> cookies, Map<String, String> data) {
        JSONObject resultObject = null;
        try {
            log.info("操作过于频繁，开始等待({})", DEFAULT_WAIT_DURATION);
            Thread.currentThread().sleep(DEFAULT_WAIT_DURATION);
            for(int i=0; i<20; i++){
                String result = HttpUtils.sendPost(PARSE_URL, cookies, HEADERS,  data);
                log.info(result);
                resultObject = JSON.parseObject(result);
                if(resultObject.get("state")!=null && resultObject.getInteger("state").equals(2402)){
                    Thread.currentThread().sleep(DEFAULT_WAIT_DURATION);
                }else{
                    return resultObject;
                }
            }
            log.error("线程【{}】, 抓取第{}页等待超时，仍然未能恢复", Thread.currentThread().getName(), data.get("pn"));
            throw new RuntimeException("等待超时，仍然未能恢复");
        } catch (InterruptedException e) {
            e.printStackTrace();
            return resultObject;
        }
    }
}
