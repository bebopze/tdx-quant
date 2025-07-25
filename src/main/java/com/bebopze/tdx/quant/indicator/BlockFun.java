package com.bebopze.tdx.quant.indicator;

import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * 基础指标   -   序列化（返回 数组）
 *
 * @author: bebopze
 * @date: 2025/5/25
 */
@Slf4j
public class BlockFun extends StockFun {


    private BaseBlockDO blockDO;


    // -----------------------------------------------------------------------------------------------------------------
    //                                            个股/板块 - 行情数据 init
    // -----------------------------------------------------------------------------------------------------------------


    public BlockFun(String code, BaseBlockDO blockDO) {
        Assert.notNull(blockDO, String.format("blockDO:[%s] is null  ->  请检查 dataCache 是否为null", code));


        // super(null);


        this.blockDO = blockDO;


        String blockName = blockDO.getName();


        // 历史行情
        klineDTOList = blockDO.getKlineDTOList();
        // 扩展数据（预计算 指标）
        extDataDTOList = blockDO.getExtDataDTOList();


        // last
        KlineDTO klineDTO = klineDTOList.get(klineDTOList.size() - 1);


        // 收盘价 - 实时
        C = klineDTO.getClose();


        // -----------------------------------------------


        klineArrDTO = ConvertStock.dtoList2Arr(klineDTOList);
        extDataArrDTO = ConvertStock.dtoList2Arr2(extDataDTOList);


        // -----------------------------------------------

        date = klineArrDTO.date;

        open = klineArrDTO.open;
        high = klineArrDTO.high;
        low = klineArrDTO.low;
        close = klineArrDTO.close;

        vol = klineArrDTO.vol;
        amo = klineArrDTO.amo;


        // -----------------------------------------------


        rps10 = extDataArrDTO.rps10;
        rps20 = extDataArrDTO.rps20;
        rps50 = extDataArrDTO.rps50;
        rps120 = extDataArrDTO.rps120;
        rps250 = extDataArrDTO.rps250;


        // -----------------------------------------------


        dateIndexMap = Maps.newHashMap();
        for (int i = 0; i < date.length; i++) {
            dateIndexMap.put(date[i], i);
        }


        // --------------------------- init data


        this.code = code;
        this.name = blockName;


        this.shszQuoteSnapshotResp = null;


        ssf = extDataArrDTO.SSF;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                            板块 - 行情数据 init
    // -----------------------------------------------------------------------------------------------------------------


    public BlockFun(String blockCode) {
        // super(null);


        // 板块
        initData(blockCode, null);
    }


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


        String blockName = blockDO.getName();


        // 历史行情
        List<KlineDTO> klineDTOList = blockDO.getKlineDTOList();
        // 扩展数据
        List<ExtDataDTO> extDataDTOList = blockDO.getExtDataDTOList();


        // last
        KlineDTO klineDTO = klineDTOList.get(klineDTOList.size() - 1);


        // 收盘价 - 实时
        double C = klineDTO.getClose();


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


        this.date = date_arr;
        this.open = open_arr;
        this.high = high_arr;
        this.low = low_arr;
        this.close = close_arr;
        this.vol = vol_arr;
        this.amo = amo_arr;


        this.ssf = SSF();


        this.rps10 = rps5_arr;
        this.rps20 = rps10_arr;
        this.rps50 = rps15_arr;
        this.rps120 = rps20_arr;
        this.rps250 = rps50_arr;
    }


}