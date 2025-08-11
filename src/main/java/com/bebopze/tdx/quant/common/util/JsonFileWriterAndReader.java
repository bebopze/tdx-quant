package com.bebopze.tdx.quant.common.util;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.google.common.collect.Lists;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


/**
 * json文件 - 读/写（1G以上 超大文件）
 *
 * @author: bebopze
 * @date: 2025/5/24
 */
@Slf4j
public class JsonFileWriterAndReader {


    // -----------------------------------------------------------------------------------------------------------------
    //                                           1G以上   超大文件 读/写
    // -----------------------------------------------------------------------------------------------------------------


    // ---------------------------------------------------- write


    /**
     * 超大对象 写入       -       stock
     *
     * @param entityList
     * @param filePath
     */
    public static void stock__writeLargeListToFile(List<BaseStockDO> entityList, String filePath) {


        // del oldFile
        del__writeLargeListToFile(filePath);


        // write newFile
        try (JsonWriter writer = new JsonWriter(new FileWriter(filePath))) {
            writer.beginArray();


            for (BaseStockDO entity : entityList) {
                writer.beginObject();

                // 通用 -> 反射
                writeSetField(writer, entity);

                writer.endObject();
            }


            writer.endArray();


        } catch (Exception e) {
            log.error("写入文件失败: {}", e.getMessage(), e);
        }
    }


    /**
     * 超大对象 写入       -       block
     *
     * @param entityList
     * @param filePath
     */
    public static void block__writeLargeListToFile(List<BaseBlockDO> entityList, String filePath) {


        // del oldFile
        del__writeLargeListToFile(filePath);


        // write newFile
        try (JsonWriter writer = new JsonWriter(new FileWriter(filePath))) {
            writer.beginArray();


            for (BaseBlockDO entity : entityList) {
                writer.beginObject();

                // 通用 -> 反射
                writeSetField(writer, entity);

                writer.endObject();
            }


            writer.endArray();


        } catch (Exception e) {
            log.error("写入文件失败: {}", e.getMessage(), e);
        }
    }


    // ---------------------------------------------------- read


    /**
     * 超大对象 读取       -       stock
     *
     * @param filePath
     * @return
     */
    public static List<BaseStockDO> stock__readLargeJsonFile(String filePath) {
        List<BaseStockDO> entityList = Lists.newArrayList();


        try (JsonReader reader = new JsonReader(new FileReader(filePath))) {
            reader.beginArray();
            while (reader.hasNext()) {
                entityList.add(readStockKLine(reader));
            }
            reader.endArray();
        } catch (Exception e) {
            log.error("读取文件失败: {}", e.getMessage(), e);
        }


        return entityList;
    }


    /**
     * 超大对象 读取       -       block
     *
     * @param filePath
     * @return
     */
    public static List<BaseBlockDO> block__readLargeJsonFile(String filePath) {
        List<BaseBlockDO> entityList = Lists.newArrayList();


        try (JsonReader reader = new JsonReader(new FileReader(filePath))) {
            reader.beginArray();
            while (reader.hasNext()) {
                entityList.add(readBlockKLine(reader));
            }
            reader.endArray();
        } catch (Exception e) {
            log.error("读取文件失败: {}", e.getMessage(), e);
        }


        return entityList;
    }


    /**
     * 行数据 - read
     *
     * @param reader
     * @return
     */
    @SneakyThrows
    private static BaseStockDO readStockKLine(JsonReader reader) {
        BaseStockDO entity = new BaseStockDO();

        try {
            reader.beginObject();


            while (reader.hasNext()) {
                String filedName = reader.nextName();
                // 通用 -> 反射
                readSetField(reader, entity, filedName);
            }


            reader.endObject();

        } catch (Exception e) {
            log.error("entity : {} , reader : {}, exMsg : {}", JSON.toJSONString(entity), JSON.toJSONString(reader), e.getMessage(), e);
            reader.endObject();
        }

        return entity;
    }


    /**
     * 行数据 - read
     *
     * @param reader
     * @return
     */
    @SneakyThrows
    private static BaseBlockDO readBlockKLine(JsonReader reader) {
        BaseBlockDO entity = new BaseBlockDO();

        try {
            reader.beginObject();


            while (reader.hasNext()) {
                String fieldName = reader.nextName();
                // 通用 -> 反射
                readSetField(reader, entity, fieldName);
            }


            reader.endObject();

        } catch (Exception e) {
            log.error("entity : {} , reader : {}, exMsg : {}", JSON.toJSONString(entity), JSON.toJSONString(reader), e.getMessage(), e);
            reader.endObject();
        }

        return entity;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * write     =>     通用 -> 反射
     *
     * @param writer
     * @param entity
     * @throws Exception
     */
    private static void writeSetField(JsonWriter writer, Object entity) throws Exception {

        // writer.name("id").value(data.getId());


        Field[] fields = entity.getClass().getDeclaredFields();


        for (Field field : fields) {
            field.setAccessible(true);


            String key = field.getName();
            if (key.equals("serialVersionUID")) {
                continue;
            }


            Object val = field.get(entity);
            String valStr = val == null ? null : val.toString();


            Class<?> type = field.getType();
            if (null != val) {
                if (type.equals(LocalDate.class)) {
                    valStr = DateTimeUtil.format_yyyy_MM_dd((LocalDate) val);
                } else if (type.equals(LocalDateTime.class)) {
                    valStr = DateTimeUtil.formatTime_yyyy_MM_dd((LocalDateTime) val);
                }
            }


            writer.name(key).value(valStr);
        }
    }

    /**
     * read     =>     通用 -> 反射
     *
     * @param reader
     * @param entity
     * @param fieldName
     * @param <T>
     * @throws Exception
     */
    private static <T> void readSetField(JsonReader reader, Object entity, String fieldName) throws Exception {


        Field field = entity.getClass().getDeclaredField(fieldName);

        // 设置字段可访问（如果字段是 private）
        field.setAccessible(true);


        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            log.warn("{} 为空     >>>     entity : {}", fieldName, JSON.toJSONString(entity));
        } else {

            String valStr = reader.nextString();
            Object value = null;


            Class<?> type = field.getType();
            if (type.equals(String.class)) {
                value = valStr;
            } else if (type.equals(Long.class)) {
                value = Long.parseLong(valStr);
            } else if (type.equals(Integer.class)) {
                value = Integer.parseInt(valStr);
            } else if (type.equals(BigDecimal.class)) {
                value = BigDecimal.valueOf(Double.parseDouble(valStr));
            } else if (type.equals(LocalDate.class)) {
                value = DateTimeUtil.parseDate_yyyy_MM_dd(valStr);
            } else if (type.equals(LocalDateTime.class)) {
                value = DateTimeUtil.parseTime_yyyy_MM_dd(valStr);
            }


            field.set(entity, value);
        }
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


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                             stockDOList
    // -----------------------------------------------------------------------------------------------------------------


    private static final String stock_filePath = System.getProperty("user.dir") + "/wiki/DB/all_stock_kline.json";


    private static void del__writeLargeListToFile(String filePath) {
        new File(filePath).delete();
        log.info("disk cache DELETE  -  del__writeLargeListToFile     >>>     filePath : {}", filePath);
    }


    /**
     * stockDOList   ->   write
     *
     * @param stockDOList
     */
    public static void writeStringToFile___stock_listAllKline(List<BaseStockDO> stockDOList) {
        // write
        stock__writeLargeListToFile(stockDOList, stock_filePath);
        log.info("disk cache WRITE  -  writeStringToFile___stock_listAllKline     >>>     stock size : {}", stockDOList.size());
    }


    /**
     * stockDOList   ->   read
     */
    public static List<BaseStockDO> readStringFromFile___stock_listAllKline() {

        List<BaseStockDO> stockDOList = stock__readLargeJsonFile(stock_filePath);
        log.info("disk cache READ  -  readStringFromFile___stock_listAllKline     >>>     stock size : {}", stockDOList.size());

        return stockDOList;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                             blockDOList
    // -----------------------------------------------------------------------------------------------------------------


    private static final String block_filePath = System.getProperty("user.dir") + "/wiki/DB/all_block_kline.json";


    /**
     * blockDOList   ->   write
     *
     * @param blockDOList
     */
    public static void writeStringToFile___block_listAllKline(List<BaseBlockDO> blockDOList) {
        block__writeLargeListToFile(blockDOList, block_filePath);
        log.info("disk cache WRITE  -  writeStringToFile___block_listAllKline     >>>     block size : {}", blockDOList.size());
    }


    /**
     * blockDOList   ->   read
     */
    public static List<BaseBlockDO> readStringFromFile___block_listAllKline() {

        List<BaseBlockDO> blockDOList = block__readLargeJsonFile(block_filePath);
        log.info("disk cache READ  -  readStringFromFile___block_listAllKline     >>>     block size : {}", blockDOList.size());

        return blockDOList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {


        // writeStringToFile___stock_listAllKline();


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


        // List<BaseStockDO> stockDOList = MybatisPlusUtil.getBaseStockService().listAllKline();

        List<BaseStockDO> stockDOList = MybatisPlusUtil.getBaseStockService().getBaseMapper()
                                                       .selectList(new QueryWrapper<BaseStockDO>().last("LIMIT 10"));


        stock__writeLargeListToFile(stockDOList, stock_filePath);
    }


}
