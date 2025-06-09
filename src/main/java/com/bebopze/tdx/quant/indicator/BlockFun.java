package com.bebopze.tdx.quant.indicator;

import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.bebopze.tdx.quant.common.tdxfun.TdxExtFun.SSF;


/**
 * 基础指标   -   序列化（返回 数组）
 *
 * @author: bebopze
 * @date: 2025/5/25
 */
@Slf4j
public class BlockFun extends StockFun {


    private BaseBlockDO baseBlockDO;


    // -----------------------------------------------------------------------------------------------------------------
    //                                            个股/板块 - 行情数据 init
    // -----------------------------------------------------------------------------------------------------------------


    public BlockFun(String blockCode, BaseBlockDO baseBlockDO) {
        // super(null);


        this.baseBlockDO = baseBlockDO;

        // 板块
        initData(blockCode, null);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                            板块 - 行情数据 init
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 加载   板块-行情数据
     *
     * @param blockCode 板块code
     * @param limit     N日
     */
    @Override
    public void initData(String blockCode, Integer limit) {


        limit = limit == null ? 500 : limit;


        // --------------------------- HTTP 获取   个股行情 data

        // 实时行情 - API
        // SHSZQuoteSnapshotResp shszQuoteSnapshotResp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
        // SHSZQuoteSnapshotResp.RealtimequoteDTO realtimequote = shszQuoteSnapshotResp.getRealtimequote();


        // 历史行情 - API
        // StockKlineHisResp stockKlineHisResp = EastMoneyKlineAPI.stockKlineHis(stockCode, KlineTypeEnum.DAY);


        // -------------------------------------------------------------------------------------------------------------

        // --------------------------- resp -> DTO


        String blockName = baseBlockDO.getName();


        // 历史行情
        List<KlineDTO> klineDTOList = baseBlockDO.getKlineDTOList();
        // 扩展数据
        List<ExtDataDTO> extDataDTOList = baseBlockDO.getExtDataDTOList();


        // last
        KlineDTO klineDTO = klineDTOList.get(klineDTOList.size() - 1);


        // 收盘价 - 实时
        double C = klineDTO.getClose().doubleValue();


        LocalDate[] date_arr = ConvertStockKline.dateFieldValArr(klineDTOList, "date");
        double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");
        double[] high_arr = ConvertStockKline.fieldValArr(klineDTOList, "high");
        double[] low_arr = ConvertStockKline.fieldValArr(klineDTOList, "low");
        double[] open_arr = ConvertStockKline.fieldValArr(klineDTOList, "open");
        long[] vol_arr = ConvertStockKline.longFieldValArr(klineDTOList, "vol");
        double[] amo_arr = ConvertStockKline.fieldValArr(klineDTOList, "amo");


        double[] rps5_arr = ConvertStockExtData.fieldValArr(extDataDTOList, "rps10");
        double[] rps10_arr = ConvertStockExtData.fieldValArr(extDataDTOList, "rps20");
        double[] rps15_arr = ConvertStockExtData.fieldValArr(extDataDTOList, "rps50");
        double[] rps20_arr = ConvertStockExtData.fieldValArr(extDataDTOList, "rps120");
        double[] rps50_arr = ConvertStockExtData.fieldValArr(extDataDTOList, "rps250");


        Map<LocalDate, Integer> dateIndexMap = Maps.newHashMap();
        for (int i = 0; i < date_arr.length; i++) {
            dateIndexMap.put(date_arr[i], i);
        }


        // --------------------------- init data


        this.code = blockCode;
        this.name = blockName;


        this.shszQuoteSnapshotResp = null;
        this.klineDTOList = klineDTOList;

        this.C = C;


        this.dateIndexMap = dateIndexMap;


        this.date_arr = date_arr;
        this.open_arr = open_arr;
        this.high_arr = high_arr;
        this.low_arr = low_arr;
        this.close_arr = close_arr;
        this.vol_arr = vol_arr;
        this.amo_arr = amo_arr;


        this.ssf_arr = SSF(close_arr);


        this.rps10_arr = rps5_arr;
        this.rps20_arr = rps10_arr;
        this.rps50_arr = rps15_arr;
        this.rps120_arr = rps20_arr;
        this.rps250_arr = rps50_arr;
    }


}