package com.bebopze.tdx.quant.client;

import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.tq.TdxRealtimeQuoteDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.domain.tq.SimpleStockDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.service.TdxTqService;
import com.bebopze.tdx.quant.service.impl.TdxTqServiceImpl;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 通达信 TQ   实时行情/历史行情   数据接口（无限速）
 *
 * @author: bebopze
 * @date: 2026/9/26
 */
@Slf4j
public class TdxTqKlineAPI {


    /**
     * 通达信 TQ   ->   批量拉取   全A + ETF + 板块   盘中实时行情（无限速）
     *
     * @return
     */
    public static List<StockSnapshotKlineDTO> pullAllSnapshotKline() {

        List<StockSnapshotKlineDTO> all_AStock = pullAllStockSnapshotKline();
        List<StockSnapshotKlineDTO> all_etf = pullAllETFSnapshotKline();
        List<StockSnapshotKlineDTO> all_block = pullAllBlockSnapshotKline();


        List<StockSnapshotKlineDTO> allSnapshotKlineDTOS = Lists.newArrayList(all_AStock);
        allSnapshotKlineDTOS.addAll(all_etf);
        allSnapshotKlineDTOS.addAll(all_block);


        Assert.notEmpty(allSnapshotKlineDTOS,
                        String.format("pullAllSnapshotKline - err     >>>     通达信TQ -> 批量拉取 全A（ETF）实时行情 异常 , AStock_size : %s , etf_size : %s , block_size : %s , total_size : %s",
                                      all_AStock.size(), all_etf.size(), all_block.size(), allSnapshotKlineDTOS.size()));


        return allSnapshotKlineDTOS;
    }


    public static List<StockSnapshotKlineDTO> pullAllStockSnapshotKline() {
        return pullAllSnapshotKline(StockTypeEnum.A_STOCK);
    }

    public static List<StockSnapshotKlineDTO> pullAllETFSnapshotKline() {
        return pullAllSnapshotKline(StockTypeEnum.ETF);
    }

    public static List<StockSnapshotKlineDTO> pullAllBlockSnapshotKline() {
        return pullAllSnapshotKline(StockTypeEnum.TDX_BLOCK);
    }


    /**
     * 通达信 TQ   ->   批量拉取   指定类型   盘中实时行情（无限速）
     *
     * @param stockTypeEnum 股票类型（A股/ETF/港股/美股/板块）
     * @return
     */
    public static List<StockSnapshotKlineDTO> pullAllSnapshotKline(StockTypeEnum stockTypeEnum) {

        TdxTqService tdxTqService = new TdxTqServiceImpl(MybatisPlusUtil.getBaseStockService(), MybatisPlusUtil.getBaseBlockService());

        List<StockSnapshotKlineDTO> klineDTOList = tdxTqService.getAll(stockTypeEnum)
                                                               .stream()
                                                               .map(TdxRealtimeQuoteDTO::toStockSnapshotKlineDTO)
                                                               .collect(Collectors.toList());

        return klineDTOList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 获取A股 全部板块 代码列表
     *
     * @return
     */
    @Deprecated
    public static List<SimpleStockDTO> listAllCode() {
        List<SimpleStockDTO> sectorList = TdxTqAPI.get_sector_list();
        return sectorList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {
        long start = System.currentTimeMillis();


        // List<StockSnapshotKlineDTO> stockSnapshotKlineDTOS_stock = pullAllStockSnapshotKline();
        // List<StockSnapshotKlineDTO> stockSnapshotKlineDTOS_ETF = pullAllETFSnapshotKline();
        // List<StockSnapshotKlineDTO> stockSnapshotKlineDTOS_block = pullAllBlockSnapshotKline();


        List<StockSnapshotKlineDTO> allSnapshotKlineDTOS = pullAllSnapshotKline();


        log.info(String.valueOf(allSnapshotKlineDTOS.size()));
        log.info("耗时：{}", DateTimeUtil.formatNow2Hms(start));
    }


}