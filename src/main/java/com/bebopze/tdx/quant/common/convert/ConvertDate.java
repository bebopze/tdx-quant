package com.bebopze.tdx.quant.common.convert;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;

import static com.bebopze.tdx.quant.common.constant.TdxConst.INDEX_BLOCK;


/**
 * 交易日 - 基准
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Deprecated
public class ConvertDate {


    /**
     * 倒序 idx（今日  ->  上市 第1天）
     */
    public static final Map<LocalDate, Integer> reverse__dateIndexMap = Maps.newTreeMap();


    public static final Map<LocalDate, Integer> dateIndexMap = Maps.newHashMap();
    public static final List<LocalDate> dateList = Lists.newArrayList();


    private static boolean initialized = false;


    private static synchronized void init() {
        if (initialized) return;


        try {
            BaseBlockMapper mapper = MybatisPlusUtil.getMapper(BaseBlockMapper.class);
            BaseBlockDO baseBlockDO = mapper.getByCode(INDEX_BLOCK);

            if (baseBlockDO != null) {

                List<KlineDTO> klineDTOList = baseBlockDO.getKlineDTOList();
                LocalDate[] date_arr = ConvertStockKline.dateFieldValArr(klineDTOList, "date");


                for (int i = 0; i < date_arr.length; i++) {
                    LocalDate date = date_arr[i];

                    dateIndexMap.put(date, i);
                    dateList.add(date);
                }


//                // 倒序
//                Collections.reverse(Arrays.asList(date_arr));
//
//
//                // 倒序 idx（今日  ->  上市 第1天）
//                for (int i = 0; i < date_arr.length; i++) {
//                    reverse__dateIndexMap.put(date_arr[i], i);
//                }
            }


            initialized = true;

        } catch (Exception e) {
            throw new RuntimeException("ConvertDate 初始化失败", e);
        }
    }


    public static boolean getByDate(boolean[] arr, LocalDate date) {

        // 第一次使用时 初始化
        init();


        try {

            // // 倒序 idx
            // Integer reverse_idx = reverse__dateIndexMap.get(date);
            // // 正序 idx
            // int idx = arr.length - reverse_idx - 1;
            //
            // return arr[idx];


            Integer idx = dateIndexMap.get(date);
            if (idx == null) {
                log.error("");
                return false;
            }

            return arr[idx];


        } catch (Exception e) {
            log.error("getByDate error     >>>     reverse__dateIndexMap size : {} , arr size : {} , date : {} , exMsg : {}",
                      reverse__dateIndexMap.size(), arr.length, date, e.getMessage(), e);
        }


        return false;
    }


    public static LocalDate tradeDateIncr(LocalDate tradeDate) {

        // 第一次使用时 初始化
        init();


        Integer idx = dateIndexMap.get(tradeDate);

        // 非交易日
        while (idx == null) {
            // 下一自然日   ->   直至 交易日
            tradeDate = tradeDate.plusDays(1);
            idx = dateIndexMap.get(tradeDate);


            if (!DateTimeUtil.between(tradeDate, dateList.get(0), dateList.get(dateList.size() - 1))) {
                throw new BizException(String.format("[日期：%s]非法，超出有效交易日范围", tradeDate));
            }
        }


        return dateList.get(idx + 1);
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 两个交易日   间隔天数(交易日)
     *
     * @param start
     * @param end
     * @param dateIndexMap 交易日-idx
     * @return
     */
    public static int between(LocalDate start, LocalDate end, Map<LocalDate, Integer> dateIndexMap) {
        Assert.isTrue(!start.isAfter(end), String.format("start[%s]不能大于end[%s]", start, end));
        // Assert.isTrue(!start.isBefore(dateList.get(0)), "start非法");
        // Assert.isTrue(!end.isAfter(dateList.get(dateList.size() - 1)), "end非法");


        // start = DateTimeUtil.max(start, dateList.get(0));
        // end = DateTimeUtil.min(end, dateList.get(dateList.size() - 1));


        Integer idx1 = dateIndexMap.get(start);
        Integer idx2 = dateIndexMap.get(end);

        Assert.notNull(idx1, String.format("start[%s]非交易日", start));
        Assert.notNull(idx2, String.format("end[%s]非交易日", end));


        return idx2 - idx1;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {


        init();


        TreeMap<LocalDate, Integer> sort__dateIndexMap = new TreeMap<>(reverse__dateIndexMap);
        System.out.println(JSON.toJSONString(reverse__dateIndexMap));
        System.out.println(JSON.toJSONString(sort__dateIndexMap));


        // ------------------------------------------------------------------------------------


        List<Integer> list = Lists.newArrayList(1, 2, 3, 4, 5);


        Map<Integer, Integer> reverse_idxMap = Maps.newHashMap();


        for (int i = list.size() - 1; i >= 0; i--) {
            // 倒序 idx
            reverse_idxMap.put(i, list.get(i));
        }


        for (Integer reverse_idx : reverse_idxMap.keySet()) {
            // 正序 idx
            int idx = list.size() - reverse_idx - 1;
            Integer val = list.get(idx);

            System.out.println(reverse_idx + "  " + idx + "     " + val);
        }
    }


}