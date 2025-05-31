package com.bebopze.tdx.quant.common.convert;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

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
public class ConvertDate {


    /**
     * 倒序 idx（今日  ->  上市 第1天）
     */
    public static final Map<String, Integer> reverse__dateIndexMap = Maps.newHashMap();

    private static boolean initialized = false;


    private static synchronized void init() {
        if (initialized) return;


        try {
            BaseBlockMapper mapper = MybatisPlusUtil.getMapper(BaseBlockMapper.class);
            BaseBlockDO baseBlockDO = mapper.getByCode(INDEX_BLOCK);

            if (baseBlockDO != null) {

                List<KlineDTO> klineDTOList = ConvertStockKline.klineHis2DTOList(baseBlockDO.getKlineHis());
                String[] date_arr = ConvertStockKline.strFieldValArr(klineDTOList, "date");


                // 倒序
                Collections.reverse(Arrays.asList(date_arr));


                // 倒序 idx（今日  ->  上市 第1天）
                for (int i = 0; i < date_arr.length; i++) {
                    reverse__dateIndexMap.put(date_arr[i], i);
                }
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

            // 倒序 idx
            int reverse_idx = reverse__dateIndexMap.get(DateTimeUtil.format_yyyy_MM_dd(date));
            // 正序 idx
            int idx = arr.length - reverse_idx - 1;

            return arr[idx];


        } catch (Exception e) {
            log.error("getByDate error     >>>     arr : {} , date : {} , exMsg : {}",
                      JSON.toJSONString(arr), date, e.getMessage(), e);
        }


        return false;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {


        init();


        TreeMap<String, Integer> sort__dateIndexMap = new TreeMap<>(reverse__dateIndexMap);
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