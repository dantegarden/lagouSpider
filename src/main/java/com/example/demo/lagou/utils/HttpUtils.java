package com.example.demo.lagou.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.lagou.model.PositionInfo;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * HTTP POST和GET处理工具类
 */
public class HttpUtils {

    /**
     * 向指定URL发送GET方法的请求
     * @param url 发送请求的URL
     * @param params 请求参数
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, Map<String,String> headers, Map<String,String> params) {
        String result = "";
        BufferedReader in = null;
        try {
            /**组装参数**/
            String param = parseParams(params);
            String urlNameString = url + param;
            URL realUrl = new URL(urlNameString);
            /**打开和URL之间的连接**/
            URLConnection connection = realUrl.openConnection();
            /**设置通用的请求属性**/
            if(!CollectionUtils.isEmpty(headers)){
                for(Entry<String,String> headerEntry: headers.entrySet()){
                    connection.setRequestProperty(headerEntry.getKey(), headerEntry.getValue());
                }
            }
            connection.setConnectTimeout(3000);
            /**建立实际的连接**/
            connection.connect();
            /**定义 BufferedReader输入流来读取URL的响应**/
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        } finally {/**使用finally块来关闭输入流**/
            try {
                if(in != null) { in.close(); }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 向指定URL发送GET方法的请求
     * @param url 发送请求的URL
     * @param params 请求参数
     * @return URL 所代表远程资源的响应结果
     */
    public static List<String> sendForGettingCookie(String url, Map<String,String> headers, Map<String,String> params) {
        List<String> cookies = null;
        BufferedReader in = null;
        try {
            /**组装参数**/
            String param = parseParams(params);
            String urlNameString = url + param;
            URL realUrl = new URL(urlNameString);
            /**打开和URL之间的连接**/
            URLConnection connection = realUrl.openConnection();
            /**设置header**/
            if(!CollectionUtils.isEmpty(headers)){
                for(Entry<String,String> headerEntry: headers.entrySet()){
                    connection.setRequestProperty(headerEntry.getKey(), headerEntry.getValue());
                }
            }
            connection.setConnectTimeout(3000);
            /**建立实际的连接**/
            connection.connect();
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            cookies = headerFields.get("Set-Cookie");
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        } finally {/**使用finally块来关闭输入流**/
            try {
                if(in != null) { in.close(); }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return cookies;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     * @param url 发送请求的 URL
     * @param params 请求参数
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, List<String> cookies, Map<String,String> headers, Map<String,String> params) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            /**打开和URL之间的连接**/
            URLConnection conn = realUrl.openConnection();
            /**设置通用的请求属性**/
            if(!CollectionUtils.isEmpty(headers)){
                for(Entry<String,String> headerEntry: headers.entrySet()){
                    conn.setRequestProperty(headerEntry.getKey(), headerEntry.getValue());
                }
            }
            conn.setConnectTimeout(3000);
            String cookie = StringUtils.join(cookies, " ");
            conn.setRequestProperty("Cookie", cookie);
            /**发送POST请求必须设置如下两行**/
            conn.setDoOutput(true);
            conn.setDoInput(true);
            /**获取URLConnection对象对应的输出流**/
            out = new PrintWriter(conn.getOutputStream());
            /**发送请求参数**/
            String param = parseParams(params);
            out.print(param);
            /**flush输出流的缓冲**/
            out.flush();
            /**定义BufferedReader输入流来读取URL的响应**/
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        } finally{ /**使用finally块来关闭输出流、输入流**/
            try{
                if(out!=null){ out.close();}
                if(in!=null){ in.close(); }
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 将HashMap参数组装成字符串
     * @param map
     * @return
     */
    private static String parseParams(Map<String,String> map){
        StringBuffer sb = new StringBuffer();
        if(map != null && !map.isEmpty()){
            sb.append("?");
            for (Entry<String, String> e : map.entrySet()) {
                sb.append(e.getKey());
                sb.append("=");
                sb.append(e.getValue());
                sb.append("&");
            }
            sb.substring(0, sb.length() - 1);
        }
        return sb.toString();
    }

}