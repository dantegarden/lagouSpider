package com.example.demo.spider;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.anti.UserAgent;
import com.example.demo.easypoi.OutputJob;
import com.example.demo.easypoi.OutputJobDetail;
import com.example.demo.easypoi.OutputJobHandler;
import com.example.demo.easypoi.OutputJobRoute;
import com.example.demo.model.PositionDetail;
import com.example.demo.model.PositionInfo;
import com.example.demo.model.PositionInfoZhipin;
import com.example.demo.model.RouteInfo;
import com.example.demo.properties.GaodeProperties;
import com.example.demo.spider.lagou.LagouSpider;
import com.example.demo.utils.DateUtils;
import com.example.demo.utils.HttpUtils;
import com.example.demo.utils.PoiUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BossZhiPinSpider implements InitializingBean {

    public static String PARSE_URL = "https://www.zhipin.com/%s/%s-%s/";

    public static final ImmutableMap<String,String> QUERY_CITIES = ImmutableMap.of("北京", "c101010100");
    public static final ImmutableMap<String,String> QUERY_XUELI = ImmutableMap.of("本科", "d_203");
    public static final ImmutableMap<String,String> QUERY_SALARY = ImmutableMap.of(
            "15-20K", "y_5",
            "20-30K", "y_6");

    public static Map<String,String> HEADERS = null;
    public static Map<String,String> COOKIES = null;

    @Autowired
    private GaodeProperties gaodeProperties;

    @Autowired
    private MongoTemplate mongoTemplate;

    private Map<String, Map<PositionInfo, PositionDetail>> retiesPositions = Maps.newHashMap();

    @Override
    public void afterPropertiesSet() throws Exception {
        PARSE_URL = String.format(PARSE_URL,
                QUERY_CITIES.get(gaodeProperties.getCity()),
                QUERY_SALARY.get("20-30K"),
                QUERY_XUELI.get("本科"));
        COOKIES = Maps.newHashMap();
        COOKIES.put("_uab_collina", "155003154213473690336617");
        COOKIES.put("lastCity", "101010100");
        COOKIES.put("Hm_lvt_194df3105ad7148dcf2b98a91b5e727a", "1566703245,1566792636,1566792668,1566913329");
        COOKIES.put("__c", "1566793706");
        COOKIES.put("__g", "-");
        COOKIES.put("t", "kPbapBb84hfAmhls");
        COOKIES.put("wt", "kPbapBb84hfAmhls");
        COOKIES.put("_bl_uid", "XOjh9zgzr3vxsza1prsp38b0qsb5");
        COOKIES.put("__zp_stoken__", "__zp_stoken__=91d9y7M2Voijmwimun7qhb9SCfKd4faBNnHQsGADdhTCuSQxtBUIYAeBnzKN8uv1rLzmedOdFKjFVn48%2FSjX4ID1bg%3D%3D");
        COOKIES.put("__l=l", "%2Fwww.zhipin.com%2F&r=https%3A%2F%2Fwww.zhipin.com%2F&friend_source=0&friend_source=0");
        COOKIES.put("__a", "9315112.1566793706..1566793706.######");
        StringBuffer cookies = new StringBuffer("");
        COOKIES.entrySet().forEach(a->{
            cookies.append(a.getKey() + "=" + a.getValue() + "; ") ;
        });
        HEADERS = ImmutableMap.of(
                ":authority", "www.zhipin.com",
                "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3",
                "Referer", PARSE_URL,
                "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36",
                "cookie", cookies.toString()
        );
    }

    int refreshTimes = 2;
    private void refreshCookies(){
        String newSuffix = String.format("%d.1.%d.%d", refreshTimes,refreshTimes,refreshTimes);
        String a1 = COOKIES.get("__a").replace("######", newSuffix);
        COOKIES.put("__a", a1);
        StringBuffer cookies = new StringBuffer("");
        COOKIES.entrySet().forEach(a->{
            cookies.append(a.getKey() + "=" + a.getValue() + "; ") ;
        });
        HEADERS = ImmutableMap.of(
                "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3",
                "Referer", PARSE_URL,
                "user-agent", UserAgent.randomOne(),
                "cookie", cookies.toString()
        );
        refreshTimes++;
    }

    public void crawl(Integer pageStart, Integer pageEnd) throws InterruptedException, IOException {
//        log.info("-------获取初始cookies--------");
//        List<String> cookies = HttpUtils.sendForGettingCookie(START_URL, HEADERS, null);
//        cookies.forEach(log::info);


        for (int page=pageStart; page<=pageEnd; page++){
            log.info("-------抓取第{}页--------", page);
            Map<String,String> searchParams = ImmutableMap.of(
                    "period", String.valueOf(4), //最近15天的
                    "page", String.valueOf(page),
                    "query", gaodeProperties.getKd());

            String parse_url = PARSE_URL;
            if(!searchParams.isEmpty()){
                parse_url += "?";
                for (Map.Entry<String,String> searchEntry: searchParams.entrySet()) {
                    parse_url +=  searchEntry.getKey() + "=" + searchEntry.getValue() + "&";
                }
                parse_url = parse_url.substring(0, parse_url.length()-1);
            }

            Connection.Response res = Jsoup.connect(parse_url)
                    .headers(HEADERS)
                    .timeout(10000)
                    .method(Connection.Method.GET)
                    .execute();
            String resBody = res.body();
            Document doc = Jsoup.parse(resBody);
                    //.get();
            doc.outerHtml();
            doc.select(".job-list > ul > li div.job-primary").forEach(element -> {
                Element detailLink = element.select("h3 a").first();
                PositionInfoZhipin positionInfo = new PositionInfoZhipin();
                positionInfo.setPositionId(Long.valueOf(detailLink.attr("data-jobid")));
                positionInfo.setPositionName(detailLink.select(".job-title").text());
                positionInfo.setSalary(detailLink.select("span").text());
                Element primaryInfo = element.select(".info-primary p").first();
                List<Node> nodes = primaryInfo.childNodes();
                String[] primaryInfoArr = nodes.get(0).toString().split(" ");
                positionInfo.setCity(primaryInfoArr[0]);
                if(primaryInfoArr.length>1){
                    positionInfo.setDistrict(primaryInfoArr[1]);
                }
                if(primaryInfoArr.length>2){
                    positionInfo.setStationname(primaryInfoArr[2]);
                }

                PositionDetail positionDetail = new PositionDetail();
                String publishTimeStr = element.select(".info-publis p").text();
                if(publishTimeStr.indexOf("昨天")>-1){
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DATE, -1);
                    publishTimeStr.replace("昨天", DateUtils.format(calendar.getTime(), "MM月dd日"));
                }else if(publishTimeStr.indexOf("月")<0){
                    publishTimeStr = "发布于" + DateUtils.format(new Date(), "MM月dd日");
                }
                positionDetail.setPublishTime(publishTimeStr);

                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(publishTimeStr);
                int count = 0;
                Calendar calendar = Calendar.getInstance();
                while (matcher.find()) {
                    Integer integer = Integer.valueOf(matcher.group(0));
                    if(count==0){
                        calendar.set(Calendar.MONTH, integer-1);
                    }else if(count==1){
                        calendar.set(Calendar.DAY_OF_MONTH, integer);
                    }
                    count++;
                }
                positionInfo.setCreateTime(calendar.getTime());

                String detailUrl = "https://www.zhipin.com" + detailLink.attr("href");
                crawlDetail(positionInfo, positionDetail, detailUrl);
                positionInfo.setPositionDetail(positionDetail);

                RouteInfo routeInfo = exist(positionInfo.getPositionId());
                if(routeInfo==null){
                    log.info("------抓取通勤路线规划{}", positionInfo.getPositionId());
                    getRouteInfo(positionInfo);
                }else{
                    positionInfo.setRouteInfo(routeInfo);
                }

                //TODO 持久化
                mongoTemplate.save(positionInfo);
            })  ;

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            refreshCookies();

            log.info("------------");
        }


        System.out.println("完毕");
    }

    public void crawlDetail(PositionInfo positionInfo, PositionDetail positionDetail, String detailUrl) {
        log.info(" 抓取明细 {}", detailUrl);
        try {
            Document doc = Jsoup.connect(detailUrl)
                    .headers(HEADERS)
                    .timeout(50000)
                    .get();

            Element first = doc.select(".job-primary .info-primary p").first();
            List<Node> nodes = first.childNodes();
            positionInfo.setCity(nodes.get(0).toString());  //所在城市
            positionInfo.setWorkYear(nodes.get(2).toString());  //工作年限
            positionInfo.setEducation(nodes.get(4).toString()); //学历
            //优势
            List<String> adventages = doc.select(".tag-more .tag-all span").eachText();
            positionInfo.setPositionAdvantage(StringUtils.join(adventages, ","));
            //发布人
            positionDetail.setPublisher(doc.select(".detail-op h2.name").text());
            String publisherStr = doc.select(".detail-op .gray").text();
            positionDetail.setPublisherJob(publisherStr.split("·")[0]);
            //职位描述
            String postionDescHtml = doc.select(".job-detail .job-sec").first().html();
            postionDescHtml = postionDescHtml.replace("<br>", "#####");
            Document doc2 = Jsoup.parse(postionDescHtml);
            positionDetail.setDescription(doc2.select("div.text").text().replace("#####", "\n\t"));
            //公司信息
            positionInfo.setCompanyFullName(doc.select(".job-sec:contains(工商信息) .name").text());
            positionInfo.setCompanyShortName(doc.select(".sider-company .company-info a").last().text());
            positionInfo.setFinanceStage(doc.select(".sider-company p:has(i.icon-stage)").text());
            positionInfo.setCompanySize(doc.select(".sider-company p:has(i.icon-scale)").text());
            positionInfo.setIndustryField(doc.select(".sider-company p:has(i.icon-industry)").text());
            positionDetail.setOfficialWebsite(doc.select(".sider-company p:has(i.icon-net)").text());
            if(StringUtils.isNotBlank(positionDetail.getOfficialWebsite())){
                positionDetail.setOfficialWebsite(positionDetail.getOfficialWebsite().replaceAll("\\s*", ""));
            }
            //公司地址
            positionDetail.setCompanyAddress(doc.select(".job-location .location-address").text());
            String addressSrc = doc.select(".job-location .job-location-map img").attr("src");
            if(StringUtils.isNotBlank(addressSrc)){
                Map<String, String> urlParams = getUrlParams(addressSrc);
                String markers = urlParams.get("markers");
                String longtitude = markers.split(",")[2].substring(2);
                String latitude =  markers.split(",")[3];
                positionInfo.setLongitude(Double.valueOf(longtitude));
                positionInfo.setLatitude(Double.valueOf(latitude));
            }

            positionDetail.setClickUrl(detailUrl);

            Thread.sleep(10000);

        } catch (InterruptedException | IOException e) {
            log.info("重试抓取明细：{}", detailUrl);
            crawlDetail(positionInfo, positionDetail, detailUrl);
            e.printStackTrace();
        }
    }

    public void getRouteInfo(PositionInfo positionInfo){
        Double longtitude = positionInfo.getLongitude(); //经度
        Double latitude = positionInfo.getLatitude(); //纬度
        if(longtitude==null || latitude==null){
            return;
        }
        Map<String, String> data = (Map)ImmutableMap.builder()
                .put("key", gaodeProperties.getAppKey())
                .put("origin", gaodeProperties.getCoordirate())
                .put("destination", longtitude + "," + latitude)
                .put("city", gaodeProperties.getCity())
                .put("cityd", gaodeProperties.getCity())
                .put("strategy", "0") //时间短优先
                .put("nightflag", "0")
                .build();
        String distanceResult = HttpUtils.sendGet(LagouSpider.GAODE_URL, ImmutableMap.of(
                "accept", "application/json, text/javascript, */*; q=0.01",
                "content-type", "application/x-www-form-urlencoded; charset=UTF-8",
                "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36"), data);
        log.info(distanceResult);
        JSONObject resultObject = JSON.parseObject(distanceResult);
        Integer status = resultObject.getInteger("status");
        if(status==1){
            RouteInfo routeInfo = JSONObject.parseObject(resultObject.getJSONObject("route").toJSONString(), RouteInfo.class);
            if(!CollectionUtils.isEmpty(routeInfo.getTransits())){
                routeInfo.setBestTransit(routeInfo.getTransits().get(0));
            }
            positionInfo.setRouteInfo(routeInfo);
        }
    }

    /**是否已持久化**/
    public RouteInfo exist(Long positionId){
        Query query = Query.query(Criteria.where("positionId").is(positionId));
        PositionInfoZhipin one = mongoTemplate.findOne(query, PositionInfoZhipin.class);
        return one==null? null: one.getRouteInfo();
    }

    public void exportExcel(){
        Query query = new Query();
        query.with(Sort.by(
                Sort.Order.asc("routeInfo.bestTransit.duration"),
                Sort.Order.desc("createTime")
        ));
        List<PositionInfoZhipin> positions = this.mongoTemplate.find(query, PositionInfoZhipin.class);
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
        ExportParams params = new ExportParams(gaodeProperties.getKd().toUpperCase() + "职位信息（BOSS直聘）", DateUtils.todayDateStr(), ExcelType.XSSF);
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
            fos = new FileOutputStream(String.format("job-excel/%s职位信息（BOSS直聘-%s）.xlsx", gaodeProperties.getKd().toUpperCase(), DateUtils.todayDateStr()));
            workbook.write(fos);
            fos.close();
        } catch (IOException  e) {
            e.printStackTrace();
        }

    }

    private static Map<String, String> getUrlParams(String url) {
        Map<String, String> map = new HashMap<String, String>();
        int start = url.indexOf("?");
        if (start >= 0) {
            String str = url.substring(start + 1);
            log.info(str);
            String[] paramsArr = str.split("&");
            for (String param : paramsArr) {
                String[] temp = param.split("=");
                map.put(temp[0], temp[1]);
            }
        }
        return map;
    }
}
