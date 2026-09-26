package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.tq.TdxRealtimeQuoteDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.service.TdxTqService;
import com.bebopze.tdx.quant.service.impl.TdxTqServiceImpl;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
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
     * 以下API   通达信   TQ HTTP服务   暂不可用
     *
     *
     * - TdxTqHttpClient - TdxTqHttpClient.call     >>>     method : get_stock_list ,
     *
     * -        params : {"market":"5","list_type":"1"} ,
     *
     * -        result : {"Error":"RPC处理异常:TPyth_TdxWServer_Main_New","ErrorId":"10","run_id":"-1"} , time : 58ms
     *
     * @return
     */
    @Deprecated
    public static List<StockSnapshotKlineDTO> listAllCode() {


        // 获取系统分类成份股 get_stock_list
        //
        // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhttn72svo/mindoc-1h10qo3uj48fg.html


        // 参数          是否必选     参数类型        参数说明
        // market         Y         str         指定代码（5-全部A股；10-所有板块指数；35-所有沪深基金；102-港股；103-美股；）
        // list_type      Y         int         返回数据类型（0-只返回代码；1-返回代码和名称；）


        JSONObject result_1 = TdxTqHttpClient.call("get_stock_list",
                                                   Map.of("market", 5, "list_type", 1));


        // 获取A股板块代码列表 get_sector_list
        //
        // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhttn72svo/mindoc-1h10r5907noko.html


        // 参数           是否必选    参数类型    参数说明
        // list_type        Y        int      返回数据类型（0-只返回代码；1-返回代码和名称；）


        JSONObject result_2 = TdxTqHttpClient.call("get_sector_list",
                                                   Map.of("list_type", 1));


        // 获取板块成份股 get_stock_list_in_sector
        //
        // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhttn72svo/mindoc-1h10r92mchgug.html


        //     参数     是否必选    参数类型       参数说明
        // block_code    Y        str         板块代码（block_type=0 表示传入 系统板块 代码 或 名称【880952/芯片】）
        // block_type    N        str         板块类型（block_type=1 表示传入 自定义板块简称【YD/月多】）
        // list_type     Y        int         返回数据类型（0-只返回代码；1-返回代码和名称；）


        JSONObject result_3 = TdxTqHttpClient.call("get_stock_list_in_sector",
                                                   Map.of("block_code", "880952.SH", "block_type", 0, "list_type", 1));

        return null;
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