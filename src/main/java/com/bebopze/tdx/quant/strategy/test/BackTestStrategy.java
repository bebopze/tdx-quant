package com.bebopze.tdx.quant.strategy.test;

import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.BuyStockStrategyResultDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockNewRelaStockService;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.strategy.buy.BuyStockStrategy;
import com.bebopze.tdx.quant.strategy.sell.DownMASellStrategy;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.bebopze.tdx.quant.service.impl.ExtDataServiceImpl.fillNaN;


/**
 * BS策略 - 回测
 *
 * @author: bebopze
 * @date: 2025/5/27
 */
@Slf4j
@Component
public class BackTestStrategy {


    // 加载  最近N日   行情数据
    int DAY_LIMIT = 2000;

    List<BaseStockDO> baseStockDOList;


    // -----------------------------------------------------------------------------------------------------------------


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;

    @Autowired
    private IBaseBlockNewRelaStockService baseBlockNewRelaStockService;


    @Autowired
    private BuyStockStrategy buyStockStrategy;

    @Autowired
    private DownMASellStrategy sellStockStrategy;


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {


        // testStrategy_01();
    }


    // -----------------------------------------------------------------------------------------------------------------


    public void testStrategy_01() {


        Map<String, double[]> codeCloseMap = loadAllStockKline();


        // 0、起始日期 - 起始金额


        // -------------------------------------------------- B
        // 1.1、当日 B策略 -> stockCodeList
        // 1.2、昨日 B策略 -> stockCodeList


        // 买入策略
        BuyStockStrategyResultDTO resultDTO = buyStockStrategy.buyStockRule();


        List<String> stockCodeList = resultDTO.getStockCodeList();


        // -------------------------------------------------- S


        // B策略 - S策略     =>     彼此不关联 -> 解耦


        // 获取 -> 持仓列表

        // 卖出策略
        sellStockStrategy.holdingStockRule(null);


        // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）

        // 2.2 每日 淘汰策略（S策略 - 2）[排名]走弱 -> 末位淘汰 ->  stockCodeList（对昨日 持股 -> 末位淘汰[设置末尾淘汰 - 分数线/排名线 ]）


        // -------------------------------------------------- 账户金额


        // 3、每日 - S金额计算


        // 4、每日 - B金额计算


        // 5、每日 - BS汇总


    }


    /**
     * 从本地DB   加载全部（5000+支）个股的 收盘价序列
     *
     * @return stock - close_arr
     */
    private Map<String, double[]> loadAllStockKline() {
        Map<String, double[]> stockCloseArrMap = Maps.newHashMap();


        // List<BaseStockDO> baseStockDOList = baseStockService.listAllKline();
        baseStockDOList = baseStockService.listAllKline();


        // TODO   停牌 - 日期-行情 问题（待验证   ->   暂忽略【影响基本为 0】）


        baseStockDOList.forEach(e -> {

            String stockCode = e.getCode();
            List<KlineDTO> klineDTOList = e.getKLineHis();


            double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");
            // String[] date_arr = ConvertStockKline.strFieldValArr(klineDTOList, "date");


            // 上市1年
            if (close_arr.length > 200) {
                stockCloseArrMap.put(stockCode, fillNaN(close_arr, DAY_LIMIT));


//                double[] fillNaN_arr = stockCloseArrMap.get(stockCode);
//                if (Double.isNaN(fillNaN_arr[0])) {
//                    log.debug("fillNaN     >>>     stockCode : {}", stockCode);
//                }
            }
        });


        return stockCloseArrMap;
    }


}
