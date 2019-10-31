package com.example.demo.anti.proxy;

import com.example.demo.anti.UserAgent;
import com.example.demo.utils.SleepTools;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProxySpider {

    private final int capacity;
    private final CircleBlockingQueue<ProxyIp> proxyQueue;

    private static final String WEB_URL = "https://www.kuaidaili.com/free/inha/%d/";
    private static Map<String,String> HEADERS = ImmutableMap.of(
            "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3",
            "Accept-Language", "zh-CN,zh;q=0.9",
            "Host", "www.kuaidaili.com",
            "Referer", "https://www.kuaidaili.com/free/",
            "User-Agent", UserAgent.randomOne()
    );

    private static SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd HH:mm");

    public ProxySpider(int capacity) {
        this.capacity = capacity;
        this.proxyQueue = new CircleBlockingQueue<>(capacity);
    }

    public void startCollect(){
        ExecutorService proxyPool = Executors.newFixedThreadPool(2);
        proxyPool.submit(new CheckAliveThread());
        proxyPool.submit(new CollectThread(1));
    }

    /**拿一个代理*/
    public ProxyIp getProxy(){
        try {
            return proxyQueue.take(); //TODO 后期可以改成带超时的
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**还一个代理*/
    public void releaseProxy(ProxyIp proxyIp){
        try {
            proxyQueue.put(proxyIp);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**负责验活的线程***/
    private class CheckAliveThread implements Runnable{
        @Override
        public void run() {
            Iterator<ProxyIp> iterator = proxyQueue.iterator();
            while(true){
                ProxyIp next = iterator.next();
                boolean isAlive = checkAlive(next.getIp(), next.getPort());
                if(!isAlive){
                    log.info("验活 {}, 已失效，从代理池抛弃", next.getIp()+":"+next.getPort());
                    proxyQueue.remove(next);
                }
            }
        }

        private boolean checkAlive(String host, Integer port){
            Socket connection = new Socket();
            try {
                connection.connect(new InetSocketAddress(host, port), 500);
                boolean connected = connection.isConnected();
                log.info("验活 {}, 结果={}", host + ":" + port, connected?"存活":"失效");
                return connected;
            } catch (IOException e) {
                return Boolean.FALSE;
            } finally {
              try{
                  if (connection != null) {
                      connection.close();
                  }
              }catch (Exception e){e.printStackTrace();}
            }
        }
    }

    private class CollectThread implements Runnable {

        private final int pageStart;

        public CollectThread(int pageStart) {
            this.pageStart = pageStart;
        }

        @Override
        public void run() {
            int pageStart = this.pageStart;
            int pageEnd = Integer.MAX_VALUE;
            int page = pageStart;

            while(page <= pageEnd){
                String web_url = String.format(WEB_URL, page);
                Document doc = null;
                while (true){
                    try {
                        doc = Jsoup.connect(web_url)
                                .headers(HEADERS)
                                .timeout(3000)
                                .get();
                        if(doc!=null){
                            break;
                        }
                    } catch (IOException e) {
                        log.error("代理池抓取：{}失败", web_url);
                    }
                }



                String lastPage = doc.select("#listnav ul li:eq(8)").text();
                pageEnd = Integer.valueOf(lastPage);

                doc.select("#list tbody tr:has(td)").forEach(element -> {
                    ProxyIp proxyIp = new ProxyIp();
                    proxyIp.setIp(element.select("td:eq(0)").text());
                    proxyIp.setPort(Integer.valueOf(element.select("td:eq(1)").text()));
                    String isHiddenStr = element.select("td:eq(2)").text();
                    proxyIp.setHidden("高匿名".equals(isHiddenStr.trim()));
                    String isHttpsStr = element.select("td:eq(3)").text();
                    proxyIp.setHttps("HTTPS".equals(isHttpsStr.trim()));
                    proxyIp.setServerAddress(element.select("td:eq(4)").text());
                    String speedStr = element.select("td:eq(5)").text();
                    double speed = Double.valueOf(speedStr.replace("秒", ""));
                    proxyIp.setSpeed(speed);
//                        String linkSpeedStr = element.select("td:eq(7) div.bar").attr("title");
//                        double linkSpeed = Double.valueOf(linkSpeedStr.replace("秒", ""));
//                        proxyIp.setLinkSpeed(linkSpeed);
//                        String surviveTimeStr = element.select("td:eq(8)").text();
//                        proxyIp.setSurviveTime(parseSurviveDate(surviveTimeStr));
                    String checkTimeStr = element.select("td:eq(6)").text();
                    proxyIp.setCheckedTime(parseDf(checkTimeStr));

                    try {
                        proxyQueue.put(proxyIp);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log.info("填充代理池： {} 来自 {}", proxyIp.getIp() + ":" + proxyIp.getPort(), proxyIp.getServerAddress() );
                });
                page++;
            }
        }
    }


    private Date parseDf(String timeStr){
        try {
            return df.parse(timeStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Date parseSurviveDate(String surviveTimeStr){
        surviveTimeStr = surviveTimeStr.trim();
        if(surviveTimeStr.endsWith("分钟")){
            Integer minute = Integer.valueOf(surviveTimeStr.replace("分钟", ""));
            long time = new Date().getTime()  + TimeUnit.MINUTES.toSeconds(minute) * 1000;
            return new Date(time);
        }else if(surviveTimeStr.endsWith("小时")){
            Integer hour = Integer.valueOf(surviveTimeStr.replace("小时", ""));
            long time = new Date().getTime()  + TimeUnit.HOURS.toSeconds(hour) * 1000;
            return new Date(time);
        }else if(surviveTimeStr.endsWith("天")){
            Integer day = Integer.valueOf(surviveTimeStr.replace("天", ""));
            long time = new Date().getTime()  + TimeUnit.DAYS.toSeconds(day) * 1000;
            return new Date(time);
        }
        return null;
    }

    /**代理池是否充足**/
    public boolean isProxyReady(){
        return proxyQueue.size() >= capacity;
    }

    public static void main(String[] args) {
        ProxySpider proxySpider = new ProxySpider(100);
        proxySpider.startCollect();
        while(true){
            if(proxySpider.isProxyReady())
                break;
        }
        System.out.println("代理池就绪");
        while(true){
            ProxyIp proxy = proxySpider.getProxy();
            System.out.println("取得代理 " + proxy.getIp() + ":" + proxy.getPort());
            SleepTools.sleep(1);
            //proxySpider.releaseProxy(proxy);
            //System.out.println("释放代理 " + proxy.getIp() + ":" + proxy.getPort());
        }
    }
}
