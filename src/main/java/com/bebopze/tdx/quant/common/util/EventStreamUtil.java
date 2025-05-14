package com.bebopze.tdx.quant.common.util;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;


/**
 * EventStream
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
@Slf4j
public class EventStreamUtil {


    // 简单演示
    public static void main(String[] args) {

        // 0.300059
        String secid = String.format("0.%s", 300059);

        int ndays = 1;


        String url = "https://31.push2.eastmoney.com/api/qt/stock/trends2/sse?" +
                "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f14,f17" +
                "&fields2=f51,f52,f53,f54,f55,f56,f57,f58" +
                "&mpi=1000" +
                // "&ut=fa5fd1943c7b386f172d6893dbfba10b" +
                "&secid=" + secid +
                "&ndays=" + ndays +
                "&iscr=0" +
                "&iscca=0"
                // "&wbp2u=1849325530509956|0|1|0|we"
                ;


        String sseData = fetchOnce(url);
        System.out.println("收到的数据：");
        System.out.println(sseData);


        // fetch(url);
    }


    public static String fetchOnce(String _url) {
        return fetchN(_url, 1).get(0);
    }

    /**
     * 只读取  第1条 或 N条“data:”    然后退出       ->       等效于 HTTP请求
     *
     * @param _url
     * @return
     */
    @SneakyThrows
    public static List<String> fetchN(String _url, int limit) {

        List<String> dataList = Lists.newArrayList();


        URL url = new URL(_url);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(5_000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {

                    // data: 后面可能是 JSON
                    String data = line.substring(5).trim();
                    dataList.add(data);

                    if (dataList.size() >= limit) {
                        return dataList;
                    }
                }
            }
        } finally {
            conn.disconnect();
        }


        throw new IOException("没有读取到任何 data 行");
    }


    /**
     * 专门构建并获取东方财富 API 的行情趋势 SSE 流
     *
     * @return 原始的 SSE 文本
     * @throws IOException 如果获取失败
     */
    @SneakyThrows
    public static void fetch(String url) {

        // 如果需要额外请求头，可构造 Map 后传入

        List<String> resultDataList = Lists.newArrayList();
        get(url, null, resultDataList, () -> {
            System.out.println("do something ...     >>>     size : " + resultDataList.size());
        });
    }


    /**
     * 发起 GET 请求并返回响应文本
     *
     * @param urlString      完整的 URL（包含查询参数）
     * @param headers        额外的请求头（可为 null）
     * @param resultDataList 接收data 列表
     * @return 响应体字符串
     * @throws IOException 如果发生 I/O 错误
     */
    @SneakyThrows
    public static void get(String urlString,
                           Map<String, String> headers,
                           List<String> resultDataList,
                           Runnable task) {


        HttpURLConnection conn = null;
        try {
            // 1. 创建 URL 并打开连接
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // 2. 设置 SSE/流式请求所需的默认请求头
            conn.setRequestProperty("Accept", "text/event-stream");
            // conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br, zstd");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setConnectTimeout(5_000);
            // conn.setReadTimeout(5_000);


            // 3. 如果传入了自定义请求头，则一并设置
            if (headers != null) {
                headers.forEach(conn::setRequestProperty);
            }

//            // 4. 发起连接并检查响应码
//            conn.connect();
//            int code = conn.getResponseCode();
//            if (code != HttpURLConnection.HTTP_OK) {
//                throw new IOException("HTTP 响应异常: " + code + " " + conn.getResponseMessage());
//            }


            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {

                        // data: 后面可能是 JSON
                        String data = line.substring(5).trim();
                        log.info("data : {}", data);

                        resultDataList.add(data);


                        task.run();

                        // return data;
                    }
                }
            }


        } finally {

            if (conn != null) {
                conn.disconnect();
            }
        }
    }


}
