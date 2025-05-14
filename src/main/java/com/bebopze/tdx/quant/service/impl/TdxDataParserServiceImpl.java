package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bebopze.tdx.quant.client.EastMoneyKlineHttpClient;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.parser.tdxdata.*;
import com.bebopze.tdx.quant.service.TdxDataParserService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
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

    @Autowired
    private IBaseBlockNewService iBaseBlockNewService;

    @Autowired
    private IBaseStockRelaBlockNewService iBaseStockRelaBlockNewService;
    @Qualifier("conversionService")
    @Autowired
    private ConversionService conversionService;


    // -----------------------------------------------------------------------------------------------------------------
    //                                           tdx 配置文件 - 导入
    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public void tdxData() {


        // 全部板块（含 板块-父子关系）
        List<Tdxzs3Parser.Tdxzs3DTO> tdxzs3DTOList = Tdxzs3Parser.parse();


        // 个股 - 行业板块（研究行业 + 普通行业）
        List<TdxhyParser.TdxhyDTO> tdxhyDTOList = TdxhyParser.parse();


        // 概念板块 - 个股code列表
        List<BlockGnParser.BlockDatDTO> blockGnDTOList = BlockGnParser.parse_gn();


        // -------------------------------------------------------------------------------------------------------------


        // 个股 - 交易所（深沪京）
        Map<String, Integer> stockCode_marketType_map = Maps.newHashMap();


        // （行业）关联code - 板块code
        Map<String, String> txCode_blockCode_map = Maps.newHashMap();


        // 个股 - 板块列表
        Map<String, Set<String>> stockCode_blockCodeSet_map = Maps.newHashMap();


        // all 个股code
        Set<String> allStockCodeSet = Sets.newHashSet();


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------
        //                                              关联关系
        // -------------------------------------------------------------------------------------------------------------


        // 全量板块   ->   关联code_TX - 板块code
        fill___txCode_blockCode_map(txCode_blockCode_map,

                                    tdxzs3DTOList);


        // 行业   ->   个股-行业板块          个股-交易所（深沪京）
        fill___stockCode_marketType_map___stockCode_blockCodeSet_map(stockCode_marketType_map, stockCode_blockCodeSet_map,

                                                                     tdxhyDTOList, txCode_blockCode_map);


        // 概念   ->   个股-概念板块          全量个股code
        fill___stockCode_blockCodeSet_map(stockCode_blockCodeSet_map, allStockCodeSet,

                                          blockGnDTOList);


        // -------------------------------------------------------------------------------------------------------------


        log.info("all 板块code     >>>     板块size : {}", tdxzs3DTOList.size());
        log.info("all 个股code     >>>     个股size : {}", allStockCodeSet.size());


        // -------------------------------------------------------------------------------------------------------------


        // 个股code          从小到大  排列
        List<String> sortAllStockCodeList = Lists.newArrayList(allStockCodeSet);
        Collections.sort(sortAllStockCodeList);


        // -------------------------------------------------------------------------------------------------------------


        Map<String, Long> stock__codeIdMap = Maps.newHashMap();
        Map<String, Long> block__codeIdMap = Maps.newHashMap();
        Map<String, String> hyBlock__code_pCode_map = Maps.newHashMap();


        // -------------------------------------------------------------------------------------------------------------
        //                                              save2DB
        // -------------------------------------------------------------------------------------------------------------


        // 个股
        save2DB___stock(sortAllStockCodeList, stockCode_marketType_map, stock__codeIdMap);


        // 板块
        save2DB___block(tdxzs3DTOList, block__codeIdMap, hyBlock__code_pCode_map);
        // 行业板块 - 父ID
        save2DB___hyBlock_pId(hyBlock__code_pCode_map, block__codeIdMap);


        // 个股 - 板块
        save2DB___stock_rela_block(sortAllStockCodeList, stock__codeIdMap, stockCode_blockCodeSet_map, block__codeIdMap);
    }


    // -------------------------------------------------------------------------------------------------------------
    //                                              关联关系
    // -------------------------------------------------------------------------------------------------------------


    private void fill___txCode_blockCode_map(Map<String, String> txCodeBlockCodeMap,

                                             List<Tdxzs3Parser.Tdxzs3DTO> tdxzs3DTOList) {

        // 全量板块
        tdxzs3DTOList.forEach(e -> {

            String txCode = e.getTXCode();
            String blockCode = e.getCode();

            txCodeBlockCodeMap.put(txCode, blockCode);
        });
    }


    private void fill___stockCode_marketType_map___stockCode_blockCodeSet_map(Map<String, Integer> stockCode_marketType_map,
                                                                              Map<String, Set<String>> stockCode_blockCodeSet_map,

                                                                              List<TdxhyParser.TdxhyDTO> tdxhyDTOList,
                                                                              Map<String, String> txCode_blockCode_map) {


        // 个股 - 行业
        tdxhyDTOList.forEach(e -> {


            // 个股 - 交易所（深沪京）
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

    }


    private void fill___stockCode_blockCodeSet_map(Map<String, Set<String>> stockCode_blockCodeSet_map,
                                                   Set<String> allStockCodeSet,

                                                   List<BlockGnParser.BlockDatDTO> blockGnDTOList) {


        // 概念板块
        blockGnDTOList.forEach(e -> {

            String blockCode = e.getCode();
            List<String> stockCodeList = e.getStockCodeList();


            allStockCodeSet.addAll(stockCodeList);


            stockCodeList.forEach(stockCode -> {


                Set<String> exist_blockCodeList = stockCode_blockCodeSet_map.get(stockCode);
                if (CollectionUtils.isEmpty(exist_blockCodeList)) {

                    stockCode_blockCodeSet_map.put(stockCode, Sets.newTreeSet(Lists.newArrayList(blockCode)));

                } else {

                    exist_blockCodeList.add(blockCode);
                }
            });
        });

    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                              save2DB
    // -----------------------------------------------------------------------------------------------------------------


    private void save2DB___stock(List<String> sortAllStockCodeList,
                                 Map<String, Integer> stockCode_marketType_map,

                                 Map<String, Long> stock__codeIdMap) {


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
            List<LdayParser.LdayDTO> ldayDTOList = LdayParser.parseByStockCode(stockCode);


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
    }


    private void save2DB___block(List<Tdxzs3Parser.Tdxzs3DTO> tdxzs3DTOList,

                                 Map<String, Long> block__codeIdMap,
                                 Map<String, String> hyBlock__code_pCode_map) {


        // 从小到大   排列
        Collections.sort(tdxzs3DTOList, Comparator.comparing(Tdxzs3Parser.Tdxzs3DTO::getCode));


        // ----------------------------------- 遍历 - save

        tdxzs3DTOList.forEach(e -> {

            String blockCode = e.getCode();
            String blockName = e.getName();

            String pCode = e.getPCode();


            BaseBlockDO baseBlockDO = new BaseBlockDO();
            baseBlockDO.setCode(blockCode);
            baseBlockDO.setName(blockName);
            baseBlockDO.setType(e.getBlockType());
            baseBlockDO.setLevel(e.getLevel());
            // 父级
            baseBlockDO.setParentId(null);


            // 板块 - 实时行情


            // 板块 - 历史行情
            List<LdayParser.LdayDTO> ldayDTOList = LdayParser.parseByStockCode(blockCode);


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


            // 父级（行业板块）
            if (StringUtils.isNotEmpty(pCode)) {
                hyBlock__code_pCode_map.put(blockCode, e.getPCode());
            }


            if (blockId == null) {
                log.error("base_block - insertOrUpdate   err     >>>     blockCode : {} , blockId : {} , baseBlockDO : {}",
                          blockCode, blockId, JSON.toJSONString(baseBlockDO));
            }
        });
    }

    private void save2DB___hyBlock_pId(Map<String, String> hyBlock__code_pCode_map,
                                       Map<String, Long> block__codeIdMap) {

        // p_id（行业板块）
        hyBlock__code_pCode_map.forEach((blockCode, pCode) -> {


            BaseBlockDO baseBlockDO = new BaseBlockDO();
            baseBlockDO.setId(block__codeIdMap.get(blockCode));
            baseBlockDO.setParentId(block__codeIdMap.get(pCode));

            iBaseBlockService.updateById(baseBlockDO);
        });
    }


    private void save2DB___stock_rela_block(List<String> sortAllStockCodeList,
                                            Map<String, Long> stock__codeIdMap,
                                            Map<String, Set<String>> stockCode_blockCodeSet_map,
                                            Map<String, Long> block__codeIdMap) {


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
            iBaseStockRelaBlockService.saveBatch(doList, 500);
        });

    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                              报表-导入
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public void exportBlock() {


        List<ExportBlockParser.ExportBlockDTO> dtoList = ExportBlockParser.parseAllTdxBlock();


        Map<String, Long> blockCode_blockId_map = Maps.newHashMap();


        Set<String> stockCodeSet = Sets.newHashSet();
        dtoList.forEach(e -> {

            String blockCode = e.getBlockCode();
            String blockName = e.getBlockName();

            String stockCode = e.getStockCode();
            String stockName = e.getStockName();


            Long blockId = blockCode_blockId_map.get(blockCode);
            if (blockId == null) {

                blockId = iBaseBlockService.getIdByCode(blockCode);
                if (blockId == null) {

                    BaseBlockDO blockEntity = new BaseBlockDO();
                    blockEntity.setCode(blockCode);
                    blockEntity.setName(blockName);

                    iBaseBlockService.save(blockEntity);

                    blockId = blockEntity.getId();
                }


                blockCode_blockId_map.put(blockCode, blockId);
            }

            stockCodeSet.add(stockCode);
        });


        Map<String, Long> sotock__codeIdMap = iBaseStockService.codeIdMap(stockCodeSet);


        List<BaseStockRelaBlockDO> relaDOList = Lists.newArrayList();
        dtoList.forEach(e -> {

            String blockCode = e.getBlockCode();
            String blockName = e.getBlockName();

            String stockCode = e.getStockCode();
            String stockName = e.getStockName();


            BaseStockRelaBlockDO baseStockRelaBlockDO = new BaseStockRelaBlockDO();
            baseStockRelaBlockDO.setBlockId(blockCode_blockId_map.get(blockCode));
            baseStockRelaBlockDO.setStockId(sotock__codeIdMap.get(stockCode));

            if (baseStockRelaBlockDO.getStockId() == null) {

                log.warn("stockId不存在     >>>     zdy_e : {}", JSON.toJSONString(e));


                // BaseStockDO baseStockDO = new BaseStockDO();
                // baseStockDO.setCode(stockCode);
                // baseStockDO.setName(stockName);
                //
                // iBaseStockService.save(baseStockDO);
                // baseStockRelaBlockNewDO.setStockId(baseStockDO.getId());


            } else {

                relaDOList.add(baseStockRelaBlockDO);
            }

        });


        iBaseStockRelaBlockService.deleteAll();
        iBaseStockRelaBlockService.saveBatch(relaDOList, 500);
    }


    @Override
    public void exportBlockNew() {


        List<ExportBlockParser.ExportBlockDTO> zdy_dtoList = ExportBlockParser.parse_zdy();


        Map<String, Long> blockCode_blockNewId_map = Maps.newHashMap();


        Set<String> stockCodeSet = Sets.newHashSet();
        zdy_dtoList.forEach(e -> {

            String blockCode = e.getBlockCode();
            String blockName = e.getBlockName();

            String stockCode = e.getStockCode();
            String stockName = e.getStockName();


            Long blockNewId = blockCode_blockNewId_map.get(blockCode);
            if (blockNewId == null) {

                BaseBlockNewDO baseBlockNewDO = new BaseBlockNewDO();
                baseBlockNewDO.setCode(blockCode);
                baseBlockNewDO.setName(blockName);

                iBaseBlockNewService.save(baseBlockNewDO);

                blockNewId = baseBlockNewDO.getId();
                blockCode_blockNewId_map.put(blockCode, blockNewId);
            }

            stockCodeSet.add(stockCode);
        });


        Map<String, Long> sotock__codeIdMap = iBaseStockService.codeIdMap(stockCodeSet);


        List<BaseStockRelaBlockNewDO> relaDOList = Lists.newArrayList();
        zdy_dtoList.forEach(e -> {

            String blockCode = e.getBlockCode();
            String blockName = e.getBlockName();

            String stockCode = e.getStockCode();
            String stockName = e.getStockName();


            BaseStockRelaBlockNewDO baseStockRelaBlockNewDO = new BaseStockRelaBlockNewDO();
            baseStockRelaBlockNewDO.setBlockNewId(blockCode_blockNewId_map.get(blockCode));
            baseStockRelaBlockNewDO.setStockId(sotock__codeIdMap.get(stockCode));

            if (baseStockRelaBlockNewDO.getStockId() == null) {

                // TODO   自定义板块     - 关联了 ->     大盘指数 / 板块 / ETF / 港股 / 美股 / ...     =>     忽略
                log.warn("stockId不存在     >>>     zdy_e : {}", JSON.toJSONString(e));


                // BaseStockDO baseStockDO = new BaseStockDO();
                // baseStockDO.setCode(stockCode);
                // baseStockDO.setName(stockName);
                //
                // iBaseStockService.save(baseStockDO);
                // baseStockRelaBlockNewDO.setStockId(baseStockDO.getId());


            } else {

                relaDOList.add(baseStockRelaBlockNewDO);
            }

        });


        iBaseStockRelaBlockNewService.deleteAll();
        iBaseStockRelaBlockNewService.saveBatch(relaDOList, 500);
    }


    @Override
    public void fillStockKline(String stockCode) {


        // ---------------------  东方财富 API     ->     拉取数据

        StockKlineHisResp stockKlineHisResp = EastMoneyKlineHttpClient.stockKlineHis(KlineTypeEnum.DAY, stockCode);

        String name = stockKlineHisResp.getName();
        List<String> klines = stockKlineHisResp.getKlines();


        // --------------------- entity

        BaseStockDO entity = new BaseStockDO();
        entity.setId(iBaseStockService.getIdByCode(stockCode));
        entity.setName(name);
        // 历史行情
        entity.setKlineHis(JSON.toJSONString(klines));


        // 实时行情   -   last kline
        KlineDTO lastKlineDTO = ConvertStock.str2DTO(klines.get(klines.size() - 1));

        entity.setTradeDate(lastKlineDTO.getDate());

        entity.setOpenPrice(lastKlineDTO.getOpen());
        entity.setClosePrice(lastKlineDTO.getClose());
        entity.setHighPrice(lastKlineDTO.getHigh());
        entity.setLowPrice(lastKlineDTO.getLow());

        entity.setVolume(lastKlineDTO.getVol());
        entity.setAmount(lastKlineDTO.getAmo());

        entity.setRangePct(lastKlineDTO.getRange_pct());
        entity.setChangePct(lastKlineDTO.getChange_pct());
        entity.setTurnoverPct(lastKlineDTO.getTurnover_pct());


        // --------------------- DB

        iBaseStockService.updateById(entity);
    }

    @Override
    public void fillStockKlineAll() {

        // Map<String, Long> codeIdMap = iBaseStockService.codeIdMap();

    }


    @Override
    public void xgcz() {


        // 60日新高
        String filePath_60rxg = TDX_PATH + "/T0002/blocknew/60rxg.blk";

        List<BlockNewParser.BlockNewDTO> dtoList_60rxg = BlockNewParser.parse(filePath_60rxg);


        List<BaseStockRelaBlockNewDO> entityList = Lists.newArrayList();


        BaseBlockNewDO baseBlockNewDO = iBaseBlockNewService.getByCode("60rxg");
        Long blockNewId = baseBlockNewDO.getId();


        List<String> stockCodeList = dtoList_60rxg.stream().map(BlockNewParser.BlockNewDTO::getStockCode).collect(Collectors.toList());
        Map<String, Long> codeIdMap = iBaseStockService.codeIdMap(stockCodeList);


        dtoList_60rxg.forEach(e -> {

            String stockCode = e.getStockCode();
            Integer tdxMarketType = e.getTdxMarketType();


            BaseStockRelaBlockNewDO entity = new BaseStockRelaBlockNewDO();
            entity.setStockId(codeIdMap.get(stockCode));
            entity.setBlockNewId(blockNewId);

            entityList.add(entity);
        });


        iBaseStockRelaBlockNewService.delByBlockNewId(blockNewId);

        iBaseStockRelaBlockNewService.saveBatch(entityList);
    }


    @Override
    public Map<String, List<String>> marketRelaStockCodePrefixList() {

        Map<String, List<String>> market_stockCodeList_map = iBaseStockService.market_stockCodePrefixList_map();

        return market_stockCodeList_map;
    }


    @Override
    public JSONObject check() {

        return null;
    }

}