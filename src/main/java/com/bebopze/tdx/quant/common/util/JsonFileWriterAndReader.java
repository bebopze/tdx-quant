package com.bebopze.tdx.quant.common.util;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.google.common.collect.Lists;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


/**
 * json文件 - 读/写
 *
 * @author: bebopze
 * @date: 2025/5/24
 */
@Slf4j
public class JsonFileWriterAndReader {


    // -----------------------------------------------------------------------------------------------------------------
    //                                           1G以上   超大文件 读/写
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 大对象 写入
     *
     * @param dataList
     * @param filePath
     */
    public static void writeLargeListToFile(List<BaseStockDO> dataList, String filePath) {

        try (JsonWriter writer = new JsonWriter(new FileWriter(filePath))) {
            writer.beginArray();


            for (BaseStockDO data : dataList) {
                writer.beginObject();


                // 通用 -> 反射
                writer.name("id").value(data.getId());
                writer.name("code").value(data.getCode());
                writer.name("name").value(data.getName());
                writer.name("tdxMarketType").value(data.getTdxMarketType());
                writer.name("klineHis").value(data.getKlineHis());
                writer.name("extDataHis").value(data.getExtDataHis());

                // 基金北向
                writer.name("amount").value(data.getAmount());


                writer.endObject();
            }


            writer.endArray();


        } catch (IOException e) {
            log.error("写入文件失败: {}", e.getMessage(), e);
        }
    }


    public static List<BaseStockDO> readLargeJsonFile(String filePath) {
        List<BaseStockDO> entityList = Lists.newArrayList();


        try (JsonReader reader = new JsonReader(new FileReader(filePath))) {
            reader.beginArray();
            while (reader.hasNext()) {
                entityList.add(readKLine(reader));
            }
            reader.endArray();
        } catch (Exception e) {
            log.error("读取文件失败: {}", e.getMessage(), e);
        }


        return entityList;
    }

    @SneakyThrows
    private static BaseStockDO readKLine(JsonReader reader) {
        BaseStockDO entity = new BaseStockDO();


        try {
            reader.beginObject();


            while (reader.hasNext()) {
                String filedName = reader.nextName();
                switch (filedName) {

                    case "id":
                        Long id = reader.nextLong();
                        entity.setId(id);
                        break;

                    case "code":
                        String code = reader.nextString();
                        entity.setCode(code);
                        break;

                    case "name":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull();
                            log.warn("stockName 为空     >>>     entity : {}", JSON.toJSONString(entity));
                        } else {
                            String name = reader.nextString();
                            entity.setName(name);
                        }
                        break;

                    case "tdxMarketType":
                        Integer tdxMarketType = reader.nextInt();
                        entity.setTdxMarketType(tdxMarketType);
                        break;

                    case "klineHis":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull();
                            log.warn("klineHis 为空     >>>     entity : {}", JSON.toJSONString(entity));
                        } else {
                            String klineHis = reader.nextString();
                            entity.setKlineHis(klineHis);
                        }
                        break;

                    case "extDataHis":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull();
                            log.warn("extDataHis 为空     >>>     entity : {}", JSON.toJSONString(entity));
                        } else {
                            String extDataHis = reader.nextString();
                            entity.setExtDataHis(extDataHis);
                        }
                        break;

                    case "amount":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull();
                            log.warn("amount 为空     >>>     entity : {}", JSON.toJSONString(entity));
                        } else {
                            BigDecimal amount = new BigDecimal(reader.nextString());
                            entity.setAmount(amount);
                        }
                        break;

                    default:
                        // 跳过未知字段
                        reader.skipValue();
                        break;
                }
            }


            reader.endObject();


        } catch (Exception e) {
            log.error("entity : {} , reader : {}, exMsg : {}", JSON.toJSONString(entity), JSON.toJSONString(reader), e.getMessage(), e);
            reader.endObject();
        }

        return entity;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                           100M以下   普通小文件 读/写
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 将字符串内容  写入  指定路径的 JSON 文件
     *
     * @param content  JSON 字符串内容
     * @param filePath 文件路径（包括文件名和扩展名）
     */
    public static void writeStringToFile(String content, String filePath) {
        Path path = Paths.get(filePath);


        try {

            // 创建父目录（如果不存在）
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            // 写入文件
            Files.write(path, content.getBytes());
            log.info("成功写入文件到: {}", filePath);


        } catch (IOException e) {
            log.error("写入文件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从指定路径的 JSON 文件   读取  内容为字符串
     *
     * @param filePath 文件路径（包括文件名和扩展名）
     * @return 文件内容字符串
     */
    public static String readStringFromFile(String filePath) {
        Path path = Paths.get(filePath);


        try {

            // 检查文件是否存在
            if (!Files.exists(path)) {
                log.error("文件不存在: {}", filePath);
                return null;
            }

            // 读取文件内容为字符串
            String content = new String(Files.readAllBytes(path));
            log.info("成功读取文件: {}", filePath);

            return content;

        } catch (IOException e) {
            log.error("读取文件失败: {}", e.getMessage(), e);
            return null;
        }
    }


    // ------------------------------------------------------------


    public static void main(String[] args) {


        // writeStringToFile___listAllKline();


        List<BaseStockDO> baseStockDOList = readStringFromFile___stock_listAllKline();
        System.out.println(baseStockDOList.size());


//        String jsonContent = "{\"name\":\"张三\",\"age\":25,\"city\":\"北京\"}";
//        String filePath = "output.json";  // 当前目录下的 output.json 文件
//
//
//        // 写入文件
//        writeStringToFile(jsonContent, filePath);
//
//        // 读取文件
//        String readContent = readStringFromFile(filePath);
//        log.info("读取的内容: {}", readContent);
    }


    /**
     * test 指标   用
     */
    public static void writeStringToFile___stock_listAllKline() {

        String filePath = System.getProperty("user.dir") + "/wiki/DB/all_stock_kline.json";

        List<BaseStockDO> stockDOList = MybatisPlusUtil.getBaseStockService().listAllKline();


        writeLargeListToFile(stockDOList, filePath);
        // writeStringToFile(JSON.toJSONString(baseStockDOList, JSONWriter.Feature.LargeObject), filePath);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                             stockDOList
    // -----------------------------------------------------------------------------------------------------------------


    private static final String stock_filePath = System.getProperty("user.dir") + "/wiki/DB/all_stock_kline.json";


    /**
     * stockDOList   ->   write
     *
     * @param stockDOList
     */
    public static void writeStringToFile___stock_listAllKline(List<BaseStockDO> stockDOList) {
        writeLargeListToFile(stockDOList, stock_filePath);
        log.debug("disk cache write  -  writeStringToFile___stock_listAllKline     >>>     stock size : {}", stockDOList.size());
    }


    /**
     * stockDOList   ->   read
     */
    public static List<BaseStockDO> readStringFromFile___stock_listAllKline() {

        List<BaseStockDO> stockDOList = readLargeJsonFile(stock_filePath);
        log.debug("disk cache read  -  readStringFromFile___stock_listAllKline     >>>     stock size : {}", stockDOList.size());

        return stockDOList;
    }


}
