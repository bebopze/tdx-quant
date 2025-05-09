package com.bebopze.tdx.quant.tdxdata;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 通达信   block_gn.dat / block_fg.dat / block_zs.dat   -   解析
 * -
 * -   /T0002/hq_cache/block_gn.dat               概念板块 - 个股列表
 * -   /T0002/hq_cache/block_fg.dat               风格板块 - 个股列表
 * -   /T0002/hq_cache/block_zs.dat               指数    - 个股列表
 *
 * @author: bebopze
 * @date: 2025/5/6
 */
@Slf4j
public class BlockGnParser {


    private static final String BASE_PATH = TDX_PATH + "/T0002/hq_cache/";


    // ---------------------------------------------------------------------------
    private static final String filePath_gn = TDX_PATH + "/T0002/hq_cache/block_gn.dat";


    public static void main(String[] args) {

        parse_gn();
        parse_fg();
        parse_zs();


        System.out.println();
    }


    /**
     * /T0002/hq_cache/block_gn.dat               概念板块 - 个股列表
     *
     * @return
     */
    public static List<BlockDatDTO> parse_gn() {
        return parse("gn");
    }

    public static List<BlockDatDTO> parse_fg() {
        return parse("fg");
    }


    public static List<BlockDatDTO> parse_zs() {
        return parse("zs");
    }


    /**
     * 解析“gn”等板块：加载 .cfg 和 .dat 并合并
     *
     * @param blk ->   gn / fg / zs
     */
    @SneakyThrows
    public static List<BlockDatDTO> parse(String blk) {

        // 读取解析
        List<BlockDatDTO> dtoList = execParse(blk);


        // 板块name - 板块code
        Map<String, String> name_code_map = name_code_map(blk);
        // System.out.println(JSON.toJSONString(name_code_map));


        // 补齐   板块code
        for (BlockDatDTO dto : dtoList) {

            // 锂电池概念 - 锂电池
            // 核污染防治 - 核污防治
            // 车路云（tdxzs3.cfg 不存在）   -   车联网（？？？）
            name_code_map.forEach((name, code) -> {

                if (name.contains(dto.name)) {
                    dto.setCode(code);
                }
            });


            if (dto.code == null) {
                log.warn("BlockGnParser#parse warn     >>>     name : {} , block : {}", dto.name, JSON.toJSONString(dto));
            }
        }


        return dtoList;
    }


    /**
     * 读取二进制  block_<blk>.dat   并解析每个板块条目
     *
     * @param blk ->   gn / fg / zs
     * @return
     * @throws IOException
     */
    public static List<BlockDatDTO> execParse(String blk) throws IOException {

        byte[] buff = Files.readAllBytes(Paths.get(BASE_PATH + "block_" + blk + ".dat"));
        ByteBuffer bbCount = ByteBuffer.wrap(buff, 384, 2).order(ByteOrder.LITTLE_ENDIAN);


        // 板块 总数
        int blockCount = bbCount.getShort();


        List<BlockDatDTO> list = Lists.newArrayList();


        int offset = 386;
        for (int i = 0; i < blockCount; i++) {
            int start = offset + i * 2813;
            byte[] blockBytes = Arrays.copyOfRange(buff, start, start + 2813);


            // 板块名称
            String name = new String(blockBytes, 0, 8, "GBK").trim();
            ByteBuffer bb = ByteBuffer.wrap(blockBytes, 9, 4).order(ByteOrder.LITTLE_ENDIAN);


            // 板块  -  关联 个股数量
            int stockNum = bb.getShort();


            // 板块  -  关联 个股code列表
            List<String> stockCodeList = Lists.newArrayList();
            for (int j = 0; j < stockNum; j++) {
                String stockCode = new String(blockBytes, 13 + 7 * j, 7, "GBK").trim();
                if (!stockCode.isEmpty()) {
                    stockCodeList.add(stockCode);
                }
            }


            list.add(new BlockDatDTO(name, null, blk, stockNum, stockCodeList));
        }


        return list;
    }


    /**
     * 解析 tdxzs3.cfg，返回指定板块类型的列表
     */
    public static Map<String, String> name_code_map(String blk) {
        Map<String, String> name_code_map = Maps.newHashMap();


        List<Tdxzs3Parser.Tdxzs3DTO> dtoList = Tdxzs3Parser.parse();


        Map<String, Integer> mapping = Maps.newHashMap();
        // 普通行业（细分行业）
        mapping.put("hy", 2);
        // 地区
        mapping.put("dq", 3);
        // 概念
        mapping.put("gn", 4);
        // 风格
        mapping.put("fg", 5);
        // 研究行业
        mapping.put("yjhy", 12);
        // 指数
        mapping.put("zs", 6);
        Integer type = mapping.get(blk);


        dtoList.stream().filter(e -> e.getBlockType().equals(type)).forEach(e -> {

            // if ("gn".equals(blk)) {

            // 锂电池概念 - 锂电池
            // 核污染防治 - 核污防治
            name_code_map.put(e.name + "|" + e.TXCode, e.code);


            // 车路云（tdxzs3.cfg 不存在）   ->   车联网（？？？）
            if ("车联网".equals(e.name)) {
                name_code_map.put(e.name + "|车路云", e.code);
            }


            // } else {
            //
            //    // 活跃可转债|880677|5|2|0|活跃转债          5-风格板块
            //    name_code_map.put(e.name, e.code);
            // }
        });


        return name_code_map;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @AllArgsConstructor
    public static class BlockDatDTO {
        /**
         * 板块-name
         */
        String name;
        /**
         * 板块-code
         */
        String code;

        /**
         * 板块类型：gn-概念；fg-风格；zs-指数；
         */
        String type;

        /**
         * 板块 - 个股数量
         */
        int num;
        /**
         * 板块 - 个股code列表
         */
        List<String> stockCodeList;
    }

}