package com.bebopze.tdx.quant.indicator;

import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
        // super();


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


        // 收盘价 - 实时
        // double C = realtimequote.getCurrentPrice().doubleValue();


        // 历史行情
        List<KlineDTO> klineDTOList = ConvertStockKline.str2DTOList(baseBlockDO.getKlineHis(), limit);


        Object[] date_arr = ConvertStockKline.objFieldValArr(klineDTOList, "date");
        double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");
        double[] high_arr = ConvertStockKline.fieldValArr(klineDTOList, "high");


        // TODO   RPS（预计算） -> DB获取


//        double[] rps50_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 50);
//        double[] rps120_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 120);
//        double[] rps250_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 250);


        // --------------------------- init data


        this.stockCode = blockCode;
        this.stockName = baseBlockDO.getName();


        // this.shszQuoteSnapshotResp = shszQuoteSnapshotResp;
        this.klineDTOList = klineDTOList;

        this.C = C;

        this.date_arr = date_arr;
        this.close_arr = close_arr;
        this.high_arr = high_arr;


        this.ssf_arr = SSF(close_arr);


        this.rps50_arr = rps50_arr;
        this.rps120_arr = rps120_arr;
        this.rps250_arr = rps250_arr;
    }


}