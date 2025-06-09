package com.bebopze.tdx.quant.common.domain.dto;

import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.google.common.collect.Maps;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;


/**
 * 扩展数据（预计算 指标） -  RPS/...
 *
 * @author: bebopze
 * @date: 2025/5/15
 */
@Data
public class ExtDataDTO implements Serializable {


    // 2025-05-01
    private LocalDate date;


    // ---------------------------------------------------
    private Double rps10;  // -> 板块 rps5
    private Double rps20;  // -> 板块 rps10
    private Double rps50;  // -> 板块 rps15
    private Double rps120; // -> 板块 rps20
    private Double rps250; // -> 板块 rps50


    // ---------------------------------------------------


    private Boolean SSF多;


    private Boolean N日新高;
    private Boolean 均线预萌出;
    private Boolean 均线萌出;
    private Boolean 大均线多头;


    private Boolean 月多;
    private Boolean RPS三线红;


    // ---------------------------------------------------


//    public static void main(String[] args) {
//
//        Field[] fields = ExtDataDTO.class.getDeclaredFields();
//
//        // 可选：按声明（自定义）顺序排序（JVM 通常已保留顺序，但非强制）
//        // Arrays.sort(fields, Comparator.comparing(Field::getName)); // 如果你用字段名排序
//
//        Map<String, Integer> map = Maps.newHashMap();
//        for (int i = 0; i < fields.length; i++) {
//            Field field = fields[i];
//            map.put(field.getName(), i);
//            System.out.println("Index: " + i + " | Name: " + field.getName() + " | Type: " + field.getType().getSimpleName());
//        }
//
//        System.out.println("字段：[" + map.keySet() + "]");
//    }

}