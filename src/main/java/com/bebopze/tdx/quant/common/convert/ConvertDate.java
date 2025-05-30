package com.bebopze.tdx.quant.common.convert;

import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockMapper;
import com.google.common.collect.Maps;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.bebopze.tdx.quant.common.constant.TdxConst.INDEX_BLOCK;


/**
 * 交易日 - 基准
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
public class ConvertDate {


    public static final Map<String, Integer> dateIndexMap = Maps.newHashMap();


    static {
        BaseBlockMapper mapper = MybatisPlusUtil.getMapper(BaseBlockMapper.class);
        BaseBlockDO baseBlockDO = mapper.getByCode(INDEX_BLOCK);


        if (baseBlockDO != null) {

            List<KlineDTO> klineDTOList = ConvertStockKline.klineHis2DTOList(baseBlockDO.getKlineHis());
            String[] date_arr = ConvertStockKline.strFieldValArr(klineDTOList, "date");


            for (int i = 0; i < date_arr.length; i++) {
                dateIndexMap.put(date_arr[i], i);
            }
        }
    }


    public static boolean getByDate(boolean[] arr, LocalDate date) {
        int idx = dateIndexMap.get(DateTimeUtil.format_yyyy_MM_dd(date));
        return arr[idx];
    }

}