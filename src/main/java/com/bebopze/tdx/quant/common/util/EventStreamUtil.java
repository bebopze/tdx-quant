package com.bebopze.tdx.quant.common.util;

import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * EventStream
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
public class EventStreamUtil {


    /**
     * 发起 GET 请求并返回响应文本
     *
     * @param urlString 完整的 URL（包含查询参数）
     * @param headers   额外的请求头（可为 null）
     * @return 响应体字符串
     * @throws IOException 如果发生 I/O 错误
     */
    public static String get(String urlString,
                             Map<String, String> headers) throws IOException {
        HttpURLConnection conn = null;
        try {
            // 1. 创建 URL 并打开连接
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // 2. 设置 SSE/流式请求所需的默认请求头
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br, zstd");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Cache-Control", "no-cache");

            // 3. 如果传入了自定义请求头，则一并设置
            if (headers != null) {
                headers.forEach(conn::setRequestProperty);
            }

            // 4. 发起连接并检查响应码
            conn.connect();
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP 响应异常: " + code + " " + conn.getResponseMessage());
            }

            // 5. 读取响应体
            try (InputStream in = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }


    // 简单演示
    public static void main(String[] args) {
        // String sseData = fetch();

        String sseData = fetchOnce("");
        System.out.println("收到的数据：");
        System.out.println(sseData);
    }


    /**
     * 只读取第一条或 N 条“data:” 然后退出       ->       等效于 HTTP请求
     *
     * @param _url
     * @return
     */
    @SneakyThrows
    public static String fetchOnce(String _url) {

//        String base = "https://76.push2.eastmoney.com/api/qt/stock/trends2/sse";
//        String params = String.join("&",
//                                    "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f14,f17",
//                                    "fields2=f51,f52,f53,f54,f55,f56,f57,f58",
//                                    "mpi=1000",
//                                    "ut=fa5fd1943c7b386f172d6893dbfba10b",
//                                    "secid=0.300059",
//                                    "ndays=1",
//                                    "iscr=0",
//                                    "iscca=0",
//                                    "wbp2u=1849325530509956|0|1|0|web"
//        );
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
                    return line.substring(5).trim();
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
    public static String fetch(String url) {

        // 如果需要额外请求头，可构造 Map 后传入

        return get(url, null);
    }

}
