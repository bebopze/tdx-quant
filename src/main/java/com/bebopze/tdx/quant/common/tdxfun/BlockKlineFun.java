package com.bebopze.tdx.quant.common.tdxfun;

import com.bebopze.tdx.quant.client.KlineAPI;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.impl.InitDataServiceImpl;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.util.NumUtil.of;


/**
 * 板块-行情计算（根据 板块->个股列表   =>   计算 板块指数）
 *
 * @author: bebopze
 * @date: 2025/12/3
 */
@Slf4j
@Component
public class BlockKlineFun {


    private static final BacktestCache data = InitDataServiceImpl.data;


    @Autowired
    private InitDataService initDataService;

    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private InitDataServiceImpl initDataServiceImpl;


    @TotalTime
    public void calcAndFillBlockKlineAll() {

        List<BaseStockDO> baseStockDOList = baseStockService.listAllSimple();

        // 个股 codeEntityMap
        Map<String, BaseStockDO> codeStockMap = baseStockDOList.stream()
                                                               .collect(Collectors.toMap(
                                                                       BaseStockDO::getCode,  // 键：从对象中提取 code 字段
                                                                       e -> e,                // 值：从对象中提取 id 字段

                                                                       (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                               ));


        // 1、加载数据
//        BacktestCache data = initDataService.incrUpdateInitData();
        if (CollectionUtils.isEmpty(data.blockDOList)) {
            initDataServiceImpl.loadAllBlockKline(false);
            initDataServiceImpl.loadAllBlockRelaStock();
        }


//        List<BaseBlockDO> blockDOList = baseBlockService.listAllKline();
//        List<BaseBlockDO> blockDOList = baseBlockService.listAllRpsKline();


        // 2、当前交易日
        LocalDate date = KlineAPI.lastTradeDate();


        // 3、计算  板块指数 K线数据
        data.blockDOList.parallelStream().forEach(blockDO -> {


            // 板块-个股 code列表
            Set<String> stockCodeSet = data.blockCode_stockCodeSet_Map.get(blockDO.getCode());


            // 板块-个股 KlineDTO列表
            List<KlineDTO> allStock_klineDTOList = stockCodeSet.stream()
                                                               .map(stockCode -> {
//                                                                   BaseStockDO stockDO = data.codeStockMap.get(stockCode);
//                                                                   List<KlineDTO> stock_klineDTOList = stockDO.getKlineDTOList();
//
//                                                                   KlineDTO klineDTO = stock_klineDTOList.get(stock_klineDTOList.size() - 1);
//                                                                   if (klineDTO.getDate().isEqual(date)) {
//                                                                       return klineDTO;
//                                                                   }
//                                                                   return null;

                                                                   BaseStockDO stockDO = codeStockMap.get(stockCode);

                                                                   if (stockDO == null
                                                                           // 未上市/停牌/退市
                                                                           || stockDO.getTradeDate() == null
                                                                           || !stockDO.getTradeDate().isEqual(date)) {

                                                                       log.warn("未上市/停牌/退市     >>>     [{}-{}]", stockCode, stockDO != null ? stockDO.getName() : null);
                                                                       return null;
                                                                   }


                                                                   return convert2DTO(stockDO);
                                                               })
                                                               .filter(Objects::nonNull)
                                                               .collect(Collectors.toList());


            // ---------------------------------------------------------------------------------------------------------


            List<KlineDTO> old_klineDTOList = blockDO.getKlineDTOList();


            // prev_close
            double prevClose = blockDO.getTradeDate().isEqual(date) ? blockDO.getPrevClose().doubleValue() : 0;
            // 之前时刻计算的 当日 KlineDTO
            KlineDTO prevSnapshot__today_klineDTO = blockDO.getTradeDate().isEqual(date) ? ListUtil.last(old_klineDTOList) : null;


            // 从大到小排序索引，避免索引变化影响
            List<Integer> removeIdxList = Lists.newArrayList();

            for (int i = old_klineDTOList.size() - 1; i >= 0; i--) {
                KlineDTO old_klineDTO = old_klineDTOList.get(i);

                if (old_klineDTO.getDate().isBefore(date)) {
                    prevClose = old_klineDTO.getClose();
                    break;
                } else {
                    // 从后往前添加，自然就是从大到小
                    removeIdxList.add(i); // 实际只有1个（最新1日 实时行情）
                }
            }


            // ---------------------------------------------------------------------------------------------------------


            // 个股 KlineDTO列表   ->   板块指数 KlineDTO
            KlineDTO block_klineDTO = calcAndFillBlockKlineAll(allStock_klineDTOList, prevClose, prevSnapshot__today_klineDTO);
            if (block_klineDTO == null) {
                return;
            }


            // ---------------------------------------------------------------------------------------------------------


            // ------------------ 从 old__klineHis 中   替换[del + add] new__klineHis（当日计算1次后，就会add 当日kline，下次查询时  ->  old__klineHis 中就包含了 当日kline，需要del，再重新add 此时刻 最新计算的 kline）


            // 从大到小 删除，避免索引偏移          ->          默认已  从大到小 排序
            removeIdxList.forEach(idx -> old_klineDTOList.remove((int) idx));   // 必须DEL -> 否则每运行1次，同一日期 就会重复1次

            // add  ->  new
            old_klineDTOList.add(block_klineDTO);


            // ---------------------------------------------------------------------------------------------------------


            BaseBlockDO entity = new BaseBlockDO();

            entity.setId(blockDO.getId());
            entity.setTradeDate(block_klineDTO.getDate());

            entity.setOpen(of2(block_klineDTO.getOpen()));
            entity.setHigh(of2(block_klineDTO.getHigh()));
            entity.setLow(of2(block_klineDTO.getLow()));
            entity.setClose(of2(block_klineDTO.getClose()));

            entity.setPrevClose(of2(prevClose));

            entity.setVolume(block_klineDTO.getVol());
            entity.setAmount(of2(block_klineDTO.getAmo()));

            entity.setRangePct(of2(block_klineDTO.getRange_pct()));
            entity.setChangePct(of2(block_klineDTO.getChange_pct()));
            entity.setChangePrice(of2(block_klineDTO.getChange_price()));
            entity.setTurnoverPct(of2(block_klineDTO.getTurnover_pct()));


            entity.setKlineHis(ConvertStockKline.dtoList2JsonStr(old_klineDTOList));


            baseBlockService.updateById(entity);
        });
    }

    private KlineDTO convert2DTO(BaseStockDO stockDO) {
        KlineDTO klineDTO = new KlineDTO();

        klineDTO.setDate(stockDO.getTradeDate());

        klineDTO.setOpen(of(stockDO.getOpen()));
        klineDTO.setHigh(of(stockDO.getHigh()));
        klineDTO.setLow(of(stockDO.getLow()));
        klineDTO.setClose(of(stockDO.getClose()));

        klineDTO.setVol(stockDO.getVolume());
        klineDTO.setAmo(of(stockDO.getAmount()));

        klineDTO.setRange_pct(of(stockDO.getRangePct()));
        klineDTO.setChange_pct(of(stockDO.getChangePct()));
        klineDTO.setChange_price(of(stockDO.getChangePrice()));
        klineDTO.setTurnover_pct(of(stockDO.getTurnoverPct()));


        return klineDTO;
    }


    public static KlineDTO calcAndFillBlockKlineAll(List<KlineDTO> list,
                                                    double prevClose,
                                                    KlineDTO prevSnapshot__today_klineDTO) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }


        // AMO = 累加
        double sumAmo = list.stream().mapToDouble(KlineDTO::getAmo).sum();

        // VOL = 累加
        long sumVol = list.stream().mapToLong(KlineDTO::getVol).sum();

        // 涨幅 = 平均值
        double change_pct = list.stream()
                                .mapToDouble(k -> k.getChange_pct() == null ? 0 : k.getChange_pct())
                                .average()
                                .orElse(0);


        // 收盘价（现价）
        double close = prevClose * (1 + change_pct * 0.01);

        double open = prevSnapshot__today_klineDTO == null ? close : prevSnapshot__today_klineDTO.getOpen();
        double high = prevSnapshot__today_klineDTO == null ? close : Math.max(prevSnapshot__today_klineDTO.getHigh(), close);
        double low = prevSnapshot__today_klineDTO == null ? close : Math.min(prevSnapshot__today_klineDTO.getLow(), close);


        KlineDTO dto = new KlineDTO();

        dto.setDate(list.get(0).getDate());

        dto.setOpen(open);
        dto.setHigh(high);
        dto.setLow(low);
        dto.setClose(close);

        dto.setVol(sumVol);
        dto.setAmo(sumAmo);

        dto.setRange_pct(of(high / low * 100 - 100));
        dto.setChange_pct(of(change_pct));
        dto.setChange_price(of(close - prevClose));
        dto.setTurnover_pct(null);

        return dto;
    }


    private BigDecimal of2(Double val) {
        return NumUtil.double2Decimal(val);
    }

}