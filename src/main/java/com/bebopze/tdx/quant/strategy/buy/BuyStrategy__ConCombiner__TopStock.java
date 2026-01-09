package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.common.util.StepRoundUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.service.impl.ExtDataServiceImpl.earlyClearStockCache__OOM;


/**
 * B策略 组合生成器（手动指定  超级大牛股 起涨点[code + date]     ->     B策略）
 *
 * @author: bebopze
 * @date: 2025/12/17
 */
@Slf4j
@Component
public class BuyStrategy__ConCombiner__TopStock {


    private static volatile List<Map<String, Boolean>> buyStrategy__conMap__list = null;


    @Autowired
    private IBaseStockService baseStockService;


    public void init__buy_strategy__conMap__list() {
        if (CollectionUtils.isNotEmpty(buyStrategy__conMap__list)) {
            return;
        }


        synchronized (BuyStrategy__ConCombiner__TopStock.class) {

            if (CollectionUtils.isEmpty(buyStrategy__conMap__list)) {

                buyStrategy__conMap__list = generate();
                Assert.notEmpty(buyStrategy__conMap__list, "[B策略] 组合Map 为空，请确认是否 OOM（内存爆炸）进行了 date 范围截取！");
            }
        }
    }


    public boolean anyMatch__buyStrategy(ExtDataDTO extDataDTO) {
        Map<String, Boolean> extData__conMap = convertExtDataDTO(extDataDTO);
        return anyMatch__buyStrategy(extData__conMap);
    }


    /**
     * 检查 extData__conMap  是否 完全匹配（buyStrategy__conMap__list 中的）  任一 B策略组合
     *
     * @param extData__conMap
     * @return
     */
    public boolean anyMatch__buyStrategy(Map<String, Boolean> extData__conMap) {
        return anyMatch__buyStrategy(buyStrategy__conMap__list, extData__conMap);
    }

    /**
     * 检查 extData__conMap  是否 完全匹配 buyStrategy__conMap__list 中的 任一组合
     *
     * @param buyStrategy__conMap__list
     * @param extData__conMap
     * @return
     */
    public boolean anyMatch__buyStrategy(List<Map<String, Boolean>> buyStrategy__conMap__list,
                                         Map<String, Boolean> extData__conMap) {


        // 仅初始化1次
        init__buy_strategy__conMap__list();


        // 检查 extData__conMap  是否 完全匹配 buyStrategy__conMap__list 中的 任一组合
        return extData__conMap.entrySet()
                              .stream()
                              .allMatch(entry -> buyStrategy__conMap__list.stream()
                                                                          .anyMatch(b_conMap -> b_conMap.containsKey(entry.getKey())
                                                                                  && Objects.equals(b_conMap.get(entry.getKey()), entry.getValue())));
    }


    public List<Map<String, Boolean>> generate() {
        List<Map<String, Boolean>> conMapList = Lists.newArrayList();


        // 1、从缓存中获取指定日期的股票
        Map<String, Set<LocalDate>> topStock__buy___codeDateSetMap = read();


        // 2、获取指定的股票（扩展数据）
        // tips：data.stockDOList  ->  经过date截取  不能用（数据不完整）  =>   这里必须重新加载 原始数据（未被date 截取过的）
        List<BaseStockDO> stockDOList = baseStockService.listByCodeList(topStock__buy___codeDateSetMap.keySet());


        // 3、遍历股票，获取指定日期的扩展数据   ->   并转换为 组合Map
        topStock__buy___codeDateSetMap.forEach((stockCode, dateSet) -> {

            stockDOList.parallelStream()
                       .filter(stockDO -> Objects.equals(stockDO.getCode(), stockCode))
                       .forEach(stockDO -> {
                           StockFun fun = new StockFun(stockDO);

                           fun.getExtDataDTOList()
                              .parallelStream()
                              .filter(e -> dateSet.contains(e.getDate()))
                              .forEach(extDataDTO -> {

                                  Map<String, Boolean> conMap = convertExtDataDTO(extDataDTO);
                                  // 强制
                                  conMap.put("XZZB", true);
                                  conMap.put("SSF多", true);
//                                  conMap.put("MA200多", true);
//                                  conMap.put("RPS红", true);

                                  conMapList.add(conMap);


                                  // OOM  ->  提前清理
                                  earlyClearStockCache__OOM(stockDO);
                              });
                       });
        });


        return conMapList;
    }


    private static Map<String, Set<LocalDate>> read() {
        Map<String, Set<LocalDate>> codeDateMap = Maps.newHashMap();

        try {
            String filePath = System.getProperty("user.dir") + "/src/main/resources/txt/牛市__topStock__B_signal___code_date_map.txt";
            FileUtils.readLines(new File(filePath))
                     .stream()
                     .filter(StringUtils::isNotBlank)
                     .forEach(line -> {
                         // 300502|2025-08-11,2025-08-12
                         String[] split = line.split("\\|");
                         Set<LocalDate> dateSet = codeDateMap.computeIfAbsent(split[0], k -> Sets.newHashSet());
                         dateSet.addAll(Lists.newArrayList(split[1].split(",")).stream().map(LocalDate::parse).collect(Collectors.toSet()));
                     });
        } catch (IOException e) {
            log.error("read file error", e);
            throw new RuntimeException(e);
        }


        return codeDateMap;
    }


    public static Map<String, Boolean> convertExtDataDTO(ExtDataDTO dto) {


        // Boolean
        Map<String, Boolean> conMap = ConvertStockExtData.toBooleanMap(dto);

        // C_MA_偏离率/H_MA_偏离率
        Map<String, Boolean> c_ma__conMap = C_MA(dto);

        // 中期涨幅
        Map<String, Boolean> 中期涨幅__conMap = 中期涨幅(dto);

        // N日涨幅
        Map<String, Boolean> N日涨幅__conMap = N日涨幅(dto);

        // 支撑线
        Map<String, Boolean> 支撑线__conMap = 支撑线(dto);


        conMap.putAll(c_ma__conMap);
        conMap.putAll(中期涨幅__conMap);
        conMap.putAll(N日涨幅__conMap);
        conMap.putAll(支撑线__conMap);


        return conMap;
    }


    @SneakyThrows
    private static Map<String, Boolean> C_MA(ExtDataDTO dto) {
        // C_MA_偏离率/H_MA_偏离率
        Map<String, Boolean> map = Maps.newHashMap();
        if (dto == null) {
            return map;
        }


        Class<?> clazz = dto.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {

            // 只处理 Double 类型
            if ((field.getType() != Double.class && field.getType() != double.class) || !field.getName().contains("偏离率")) {
                continue;
            }

            field.setAccessible(true);
            Double value = (Double) field.get(dto);


            // key：C_MA_偏离率/H_MA_偏离率     5/10/20/30/50/60/100/120/150/200/250
            String key = field.getName();
            // value：偏离率  ->  向上取整到 步长5
            // value = value == null ? 0 : StepRoundUtil.ceilToStep(value, 5);

            // value：偏离率
            if (value < 5) {
                map.put(key + "<5", true);
            } else if (value < 10) {
                map.put(key + "<10", true);
            } else if (value < 15) {
                map.put(key + "<15", true);
            } else if (value < 20) {
                map.put(key + "<20", true);
            } else if (value < 25) {
                map.put(key + "<25", true);
            } else if (value < 30) {
                map.put(key + "<30", true);
            } else if (value < 35) {
                map.put(key + "<35", true);
            } else if (value < 40) {
                map.put(key + "<40", true);
            } else if (value < 45) {
                map.put(key + "<45", true);
            } else if (value < 50) {
                map.put(key + "<50", true);
            } else if (value < 55) {
                map.put(key + "<55", true);
            } else if (value < 60) {
                map.put(key + "<60", true);
            } else if (value < 65) {
                map.put(key + "<65", true);
            } else if (value < 70) {
                map.put(key + "<70", true);
            } else if (value < 75) {
                map.put(key + "<75", true);
            } else if (value < 80) {
                map.put(key + "<80", true);
            } else if (value < 85) {
                map.put(key + "<85", true);
            } else if (value < 90) {
                map.put(key + "<90", true);
            } else if (value < 95) {
                map.put(key + "<95", true);
            } else if (value < 100) {
                map.put(key + "<100", true);
            }
        }


        return map;
    }


    @SneakyThrows
    private static Map<String, Boolean> 中期涨幅(ExtDataDTO dto) {
        // 中期涨幅
        Map<String, Boolean> map = Maps.newHashMap();
        if (dto == null) {
            return map;
        }


        Class<?> clazz = dto.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {

            // 只处理 Double 类型
            if ((field.getType() != Double.class && field.getType() != double.class) || !field.getName().contains("中期涨幅")) {
                continue;
            }

            field.setAccessible(true);
            Double value = (Double) field.get(dto);


            // key：中期涨幅N5/10/20/30/50/60/100/120/150/200/250
            String key = field.getName();
            // value：中期涨幅  ->  向上取整到 步长5
            // value = value == null ? 0 : StepRoundUtil.ceilToStep(value, 5);

            // value：中期涨幅
            int MA_N = Integer.valueOf(key.split("N")[1]);


            // 中期涨幅N5/10/20/30/50/60
            if (MA_N <= 60) {
                if (value < 5) {
                    map.put(key + "<5", true);
                } else if (value < 10) {
                    map.put(key + "<10", true);
                } else if (value < 15) {
                    map.put(key + "<15", true);
                } else if (value < 20) {
                    map.put(key + "<20", true);
                } else if (value < 25) {
                    map.put(key + "<25", true);
                } else if (value < 30) {
                    map.put(key + "<30", true);
                } else if (value < 35) {
                    map.put(key + "<35", true);
                } else if (value < 40) {
                    map.put(key + "<40", true);
                } else if (value < 45) {
                    map.put(key + "<45", true);
                } else if (value < 50) {
                    map.put(key + "<50", true);
                } else if (value < 55) {
                    map.put(key + "<55", true);
                } else if (value < 60) {
                    map.put(key + "<60", true);
                } else if (value < 65) {
                    map.put(key + "<65", true);
                } else if (value < 70) {
                    map.put(key + "<70", true);
                } else if (value < 75) {
                    map.put(key + "<75", true);
                } else if (value < 80) {
                    map.put(key + "<80", true);
                } else if (value < 85) {
                    map.put(key + "<85", true);
                } else if (value < 90) {
                    map.put(key + "<90", true);
                } else if (value < 95) {
                    map.put(key + "<95", true);
                } else if (value < 100) {
                    map.put(key + "<100", true);
                }
            }


            // 中期涨幅N100/120/150/200/250
            if (value < 150) {
                map.put(key + "<150", true);
            } else if (value < 350) {
                map.put(key + "<350", true);
            } else if (value < 700) {
                map.put(key + "<700", true);
            } else if (value < 1500) {
                map.put(key + "<1500", true);
            }
        }


        return map;
    }


    @SneakyThrows
    private static Map<String, Boolean> N日涨幅(ExtDataDTO dto) {
        // N日涨幅
        Map<String, Boolean> map = Maps.newHashMap();
        if (dto == null) {
            return map;
        }


        Class<?> clazz = dto.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {

            // 只处理 Double 类型
            if ((field.getType() != Double.class && field.getType() != double.class) || !field.getName().contains("日涨幅")) {
                continue;
            }

            field.setAccessible(true);
            Double value = (Double) field.get(dto);


            // key：N3日涨幅/5/10/20/30/50/60/100/120/150/200/250
            String key = field.getName();

            // value：N日涨幅
            if (value < 5) {
                map.put(key + "<5", true);
            } else if (value < 10) {
                map.put(key + "<10", true);
            } else if (value < 15) {
                map.put(key + "<15", true);
            } else if (value < 20) {
                map.put(key + "<20", true);
            } else if (value < 25) {
                map.put(key + "<25", true);
            } else if (value < 30) {
                map.put(key + "<30", true);
            } else if (value < 35) {
                map.put(key + "<35", true);
            } else if (value < 40) {
                map.put(key + "<40", true);
            } else if (value < 45) {
                map.put(key + "<45", true);
            } else if (value < 50) {
                map.put(key + "<50", true);
            } else if (value < 55) {
                map.put(key + "<55", true);
            } else if (value < 60) {
                map.put(key + "<60", true);
            } else if (value < 65) {
                map.put(key + "<65", true);
            } else if (value < 70) {
                map.put(key + "<70", true);
            } else if (value < 75) {
                map.put(key + "<75", true);
            } else if (value < 80) {
                map.put(key + "<80", true);
            } else if (value < 85) {
                map.put(key + "<85", true);
            } else if (value < 90) {
                map.put(key + "<90", true);
            } else if (value < 95) {
                map.put(key + "<95", true);
            } else if (value < 100) {
                map.put(key + "<100", true);
            }
        }


        return map;
    }


    @SneakyThrows
    private static Map<String, Boolean> 支撑线(ExtDataDTO dto) {
        // 支撑线
        Map<String, Boolean> map = Maps.newHashMap();
        if (dto == null) {
            return map;
        }


        Class<?> clazz = dto.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {

            // 只处理 int 类型
            if ((field.getType() != Integer.class && field.getType() != int.class) || !field.getName().contains("支撑线")) {
                continue;
            }

            field.setAccessible(true);
            Integer value = (Integer) field.get(dto);


            // key：短/中/长期支撑线
            String key = field.getName();

            // value：支撑线=5/10/20/30/50/60/100/120/150/200/250
            if (value == 5) {
                map.put(key + "=5", true);
            } else if (value == 10) {
                map.put(key + "=10", true);
            } else if (value == 20) {
                map.put(key + "=20", true);
            } else if (value == 30) {
                map.put(key + "=30", true);
            } else if (value == 50) {
                map.put(key + "=50", true);
            } else if (value == 60) {
                map.put(key + "=60", true);
            } else if (value == 100) {
                map.put(key + "=100", true);
            } else if (value == 120) {
                map.put(key + "=120", true);
            } else if (value == 150) {
                map.put(key + "=150", true);
            } else if (value == 200) {
                map.put(key + "=200", true);
            } else if (value == 250) {
                map.put(key + "=250", true);
            }
        }


        return map;
    }


    @SneakyThrows
    private Map<String, Double> C_MA__2(ExtDataDTO dto) {
        // C_MA_偏离率
        Map<String, Double> map = Maps.newHashMap();
        if (dto == null) {
            return map;
        }


        Class<?> clazz = dto.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {

            // 只处理 Double 类型
            if ((field.getType() != Double.class && field.getType() != double.class) || !field.getName().contains("偏离率")) {
                continue;
            }

            field.setAccessible(true);
            Double value = (Double) field.get(dto);


            // key：C_MAx_偏离率
            // value：偏离率  ->  向上取整到 步长5
            map.put(field.getName(), value == null ? 0 : StepRoundUtil.ceilToStep(value, 5));
        }


        return map;
    }


// -----------------------------------------------------------------------------------------------------------------

    public static void main(String[] args) {

        BuyStrategy__ConCombiner__TopStock buyStrategy__conCombiner__topStock = new BuyStrategy__ConCombiner__TopStock();
        buyStrategy__conCombiner__topStock.baseStockService = MybatisPlusUtil.getBaseStockService();


        List<Map<String, Boolean>> conMap__list = buyStrategy__conCombiner__topStock.generate();
        System.out.println("conMap__list.size() : " + conMap__list.size());
    }


}