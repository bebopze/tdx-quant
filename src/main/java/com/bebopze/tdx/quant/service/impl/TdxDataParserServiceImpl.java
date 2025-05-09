package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson.JSON;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockRelaBlockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockRelaBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.service.TdxDataParserService;
import com.bebopze.tdx.quant.tdxdata.BlockGnParser;
import com.bebopze.tdx.quant.tdxdata.LdayParser;
import com.bebopze.tdx.quant.tdxdata.TdxhyParser;
import com.bebopze.tdx.quant.tdxdata.Tdxzs3Parser;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * @author: bebopze
 * @date: 2025/5/7
 */
@Slf4j
@Service
public class TdxDataParserServiceImpl implements TdxDataParserService {


    @Autowired
    private IBaseStockService iBaseStockService;

    @Autowired
    private IBaseBlockService iBaseBlockService;

    @Autowired
    private IBaseStockRelaBlockService iBaseStockRelaBlockService;


    @Override
    public void tdxData() {


        // 全部板块（含 板块-父子关系）
        List<Tdxzs3Parser.Tdxzs3DTO> tdxzs3DTOList = Tdxzs3Parser.parse();


        // 个股 - 行业板块（研究行业 + 普通行业）
        List<TdxhyParser.TdxhyDTO> tdxhyDTOList = TdxhyParser.parse();


        // 概念板块 - 个股code列表
        List<BlockGnParser.BlockDatDTO> blockGnDTOList = BlockGnParser.parse_gn();


        // -------------------------------------------------------------------------------------------------------------


        Map<String, Integer> stockCode_marketType_map = Maps.newHashMap();

        Map<String, String> txCode_blockCode_map = Maps.newHashMap();

        Map<String, Set<String>> stockCode_blockCodeSet_map = Maps.newHashMap();
        // Map<String, Set<String>> blockCode_stockCodeSet_map = Maps.newHashMap();


        // -------------------------------------------------------------------------------------------------------------

        tdxzs3DTOList.forEach(e -> {

            String txCode = e.getTXCode();
            String blockCode = e.getCode();

            txCode_blockCode_map.put(txCode, blockCode);
        });


        // 个股 - 行业
        tdxhyDTOList.forEach(e -> {


            // 个股 - 市场（沪深京）
            stockCode_marketType_map.put(e.getStockCode(), e.getTdxMarketType());


            // 个股 - 行业code
            String stockCode = e.getStockCode();

            String hy_TCode = e.getHyCode_T();
            String hy_XCode = e.getHyCode_X();

            String blockCode_T = txCode_blockCode_map.get(hy_TCode);
            String blockCode_X = txCode_blockCode_map.get(hy_XCode);


            Set<String> exist_blockCodeSet = stockCode_blockCodeSet_map.get(stockCode);
            if (CollectionUtils.isEmpty(exist_blockCodeSet)) {

                Set<String> blockCodeSet = Sets.newHashSet(blockCode_T, blockCode_X).stream().filter(Objects::nonNull).collect(Collectors.toSet());
                stockCode_blockCodeSet_map.put(stockCode, blockCodeSet);

            } else {

                Set<String> blockCodeSet = Sets.newHashSet(blockCode_T, blockCode_X).stream().filter(Objects::nonNull).collect(Collectors.toSet());
                exist_blockCodeSet.addAll(blockCodeSet);
            }
        });


        // all 个股code
        HashSet<String> allStockCodeSet = Sets.newHashSet();
        blockGnDTOList.forEach(e -> {

            String blockCode = e.getCode();
            List<String> stockCodeList = e.getStockCodeList();


            allStockCodeSet.addAll(stockCodeList);


            // blockCode_stockCodeList_map.put(blockCode, stockCodeList);


            stockCodeList.forEach(stockCode -> {


                Set<String> exist_blockCodeList = stockCode_blockCodeSet_map.get(stockCode);
                if (CollectionUtils.isEmpty(exist_blockCodeList)) {

                    stockCode_blockCodeSet_map.put(stockCode, Sets.newTreeSet(Lists.newArrayList(blockCode)));

                } else {

                    exist_blockCodeList.add(blockCode);
                }
            });
        });


        log.info("all 板块code     >>>     板块size : {}", tdxzs3DTOList.size());
        log.info("all 个股code     >>>     个股size : {}", allStockCodeSet.size());


        // -------------------------------------------------------------------------------------------------------------


        // 从小到大   排列
        List<String> sortAllStockCodeList = Lists.newArrayList(allStockCodeSet);
        Collections.sort(sortAllStockCodeList);


        // -------------------------------------------------------------------------------------------------------------


        Map<String, Long> stock__codeIdMap = Maps.newHashMap();
        Map<String, Long> block__codeIdMap = Maps.newHashMap();


        sortAllStockCodeList.forEach(stockCode -> {


            Integer marketType = stockCode_marketType_map.get(stockCode);
            if (marketType == null) {
                log.warn("marketType为空     >>>     stockCode : {}", stockCode);
                return;
            }


            // 个股
            BaseStockDO baseStockDO = new BaseStockDO();
            baseStockDO.setCode(stockCode);
            baseStockDO.setName("");
            baseStockDO.setTdxMarketType(marketType);


            // 个股 - 实时行情
            // ...


            // 个股 - 历史行情
            String market = StockMarketEnum.getMarketSymbol(marketType);
            // String filePath_a = TDX_PATH + "/vipdoc/sh/lday/sh600519.day";
            String filePath_a = TDX_PATH + String.format("/vipdoc/%s/lday/%s%s.day", market, market, stockCode);
            List<LdayParser.LdayDTO> ldayDTOList = LdayParser.parse(filePath_a);


            Map<String, List<Number>> date_kline_map = Maps.newLinkedHashMap();
            ldayDTOList.forEach(e -> {

                // 历史行情-JSON（日期：[O,H,L,C,VOL,AMO,涨幅,振幅,换手率]）
                List<Number> kline = Lists.newArrayList(e.getOpen(), e.getHigh(), e.getLow(), e.getClose(), e.getVol(), e.getAmount(), e.getChangePct(), null, null);

                date_kline_map.put(String.valueOf(e.getTradeDate()), kline);
            });

            baseStockDO.setKlineHis(JSON.toJSONString(date_kline_map));


            LdayParser.LdayDTO last = ldayDTOList.get(ldayDTOList.size() - 1);
            baseStockDO.setTradeDate(last.getTradeDate());
            baseStockDO.setOpenPrice(last.getOpen());
            baseStockDO.setHighPrice(last.getHigh());
            baseStockDO.setLowPrice(last.getLow());
            baseStockDO.setClosePrice(last.getClose());
            baseStockDO.setVolume(Long.valueOf(last.getVol()));
            baseStockDO.setAmount(last.getAmount());
            baseStockDO.setChangePct(last.getChangePct());


            Long stockId = iBaseStockService.getIdByCode(stockCode);
            if (stockId != null) {
                baseStockDO.setId(stockId);
                iBaseStockService.updateById(baseStockDO);
            } else {
                iBaseStockService.save(baseStockDO);
                stockId = baseStockDO.getId();
            }


            stock__codeIdMap.put(stockCode, stockId);


            if (stockId == null) {
                log.error("base_stock - insertOrUpdate   err     >>>     stockCode : {} , stockId : {} , baseStockDO : {}",
                          stockCode, stockId, JSON.toJSONString(baseStockDO));
            }
        });


        // -------------------------------------------------------------------------------------------------------------

        // 从小到大   排列
        Collections.sort(tdxzs3DTOList, Comparator.comparing(Tdxzs3Parser.Tdxzs3DTO::getCode));


        tdxzs3DTOList.forEach(e -> {

            String blockCode = e.getCode();
            String blockName = e.getName();


            BaseBlockDO baseBlockDO = new BaseBlockDO();
            baseBlockDO.setCode(blockCode);
            baseBlockDO.setName(blockName);
            baseBlockDO.setType(e.getBlockType());
            baseBlockDO.setParentId(null);
            baseBlockDO.setLevel(e.getLevel());


            // 板块 - 实时行情


            // 板块 - 历史行情

            // 清一色：   1-上海
            String market = StockMarketEnum.getMarketSymbol(1);
            // String filePath_a = TDX_PATH + "/vipdoc/sh/lday/sh880904.day";
            String filePath_a = TDX_PATH + String.format("/vipdoc/%s/lday/%s%s.day", market, market, blockCode);
            List<LdayParser.LdayDTO> ldayDTOList = LdayParser.parse(filePath_a);


            Map<String, List<Number>> date_kline_map = Maps.newLinkedHashMap();
            ldayDTOList.forEach(x -> {

                // 历史行情-JSON（日期：[O,H,L,C,VOL,AMO,涨幅,振幅,换手率]）
                List<Number> kline = Lists.newArrayList(x.getOpen(), x.getHigh(), x.getLow(), x.getClose(), x.getVol(), x.getAmount(), x.getChangePct(), null, null);

                date_kline_map.put(String.valueOf(x.getTradeDate()), kline);
            });

            baseBlockDO.setKlineHis(JSON.toJSONString(date_kline_map));


            LdayParser.LdayDTO last = ldayDTOList.get(ldayDTOList.size() - 1);
            baseBlockDO.setTradeDate(last.getTradeDate());
            baseBlockDO.setOpenPrice(last.getOpen());
            baseBlockDO.setHighPrice(last.getHigh());
            baseBlockDO.setLowPrice(last.getLow());
            baseBlockDO.setClosePrice(last.getClose());
            baseBlockDO.setVolume(Long.valueOf(last.getVol()));
            baseBlockDO.setAmount(last.getAmount());
            baseBlockDO.setChangePct(last.getChangePct());


            Long blockId = iBaseBlockService.getIdByCode(blockCode);
            if (blockId != null) {
                baseBlockDO.setId(blockId);
                iBaseBlockService.updateById(baseBlockDO);
            } else {
                iBaseBlockService.save(baseBlockDO);
                blockId = baseBlockDO.getId();
            }


            block__codeIdMap.put(blockCode, blockId);


            if (blockId == null) {
                log.error("base_block - insertOrUpdate   err     >>>     blockCode : {} , blockId : {} , baseBlockDO : {}",
                          blockCode, blockId, JSON.toJSONString(baseBlockDO));
            }
        });


        // -------------------------------------------------------------------------------------------------------------


        sortAllStockCodeList.forEach(stockCode -> {

            Long stockId = stock__codeIdMap.get(stockCode);

            Set<String> blockCodeSet = stockCode_blockCodeSet_map.get(stockCode);
            List<BaseStockRelaBlockDO> doList = Lists.newArrayList();


            if (stockId == null || CollectionUtils.isEmpty(blockCodeSet)) {
                log.error("个股-板块   err     >>>     stockId : {} , blockCodeSet : {}", stockId, JSON.toJSONString(blockCodeSet));
                return;
            }


            blockCodeSet.forEach(blockCode -> {

                // 个股 - 板块
                BaseStockRelaBlockDO baseStockRelaBlockDO = new BaseStockRelaBlockDO();
                baseStockRelaBlockDO.setStockId(stockId);
                baseStockRelaBlockDO.setBlockId(block__codeIdMap.get(blockCode));


                doList.add(baseStockRelaBlockDO);
            });


            // del All
            iBaseStockRelaBlockService.deleteByStockId(stockId);
            // batch insert
            iBaseStockRelaBlockService.saveBatch(doList);
        });


    }

    @Override
    public Map<String, List<String>> marketRelaStockCodePrefixList() {

        Map<String, List<String>> market_stockCodeList_map = iBaseStockService.market_stockCodePrefixList_map();

        return market_stockCodeList_map;
    }

}