package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.client.KlineAPI;
import com.bebopze.tdx.quant.client.TdxIncrClient;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.*;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.tdxfun.BlockKlineFun;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.parser.tdxdata.*;
import com.bebopze.tdx.quant.service.ExtDataService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.service.TdxDataParserService;
import com.bebopze.tdx.quant.task.TdxTask;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * 通达信 - 数据初始化（个股-板块-大盘   关联关系 / 行情）   ->   解析入库
 *
 * @author: bebopze
 * @date: 2025/5/7
 */
@Slf4j
@Primary
@Service
public class TdxDataParserServiceImpl implements TdxDataParserService {


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;

    @Autowired
    private IBaseBlockNewService baseBlockNewService;

    @Autowired
    private IBaseBlockNewRelaStockService baseBlockNewRelaStockService;


    @Autowired
    private MarketService marketService;

    @Autowired
    private ExtDataService extDataService;

    @Autowired
    private InitDataService initDataService;


    @Autowired
    private TdxTask tdxTask;

    @Autowired
    private TdxIncrClient tdxIncrClient;

    @Autowired
    private BlockKlineFun blockKlineFun;


    /**
     * 通达信 - （股票/板块/自定义板块）数据初始化   一键导入     +     Kline/ext_data 解析入库
     */
    @TotalTime
    @Override
    public void importAll() {


        // 板块/个股/ETF/自定义板块/关联关系   ->   一键 初始化/刷新
        importAll__blockRelaStock();


        // ------------------------------------------------------------------------ 大盘量化


        marketService.importMarketMidCycle();


        // ------------------------------------------------------------------------ 行情（通达信-行情数据 / 东方财富/同花顺/雪球-API）


        refreshKlineAll(UpdateTypeEnum.ALL);

        extDataService.refreshExtDataAll(null);


        // tdxTask.execTask__refreshAll();
    }


    /**
     * 通达信 - 板块/个股/ETF/自定义板块/关联关系   ->   一键 初始化/刷新
     */
    @TotalTime
    @Override
    public void importAll__blockRelaStock() {


        // ------------------------------------------------------------------------ 配置导入（缓存数据）


        // 初始数据：     板块（行业/概念） - 个股   关联关系
        importTdxBlockCfg();


        // ------------------------------------------------------------------------ 报表导入（板块导出）


        //（行业/概念）板块 - 个股     关联关系
        importBlockReport();
        //      自定义板块 - 个股     关联关系
        importBlockNewReport();


        // ------------------------------------------------------------------------ 导入ETF/港股/美股


        importETF();


        importHkStock();
        importUsStock();


        // ------------------------------------------------------------------------ 补充info（股本/流通市值/总市值）


        // fillMoreInfo();


        // ------------------------------------------------------------------------ 大盘量化


        // marketService.importMarketMidCycle();
    }


    private void fillMoreInfo() {


        // List<GetGbInfoDTO> gbDTOList = TdxTqAPI.get_gb_info_by_date("000001", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                           tdx 配置文件 - 导入
    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @Override
    public void importTdxBlockCfg() {


        // 全部板块（含 板块-父子关系）
        List<Tdxzs3Parser.Tdxzs3DTO> tdxzs3DTOList = Tdxzs3Parser.parse();


        // 仅导入   ->   2/12-行业；4-概念；
        tdxzs3DTOList = tdxzs3DTOList.stream().filter(e -> {
            // tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；
            Integer blockType = e.getBlockType();
            return blockType.equals(2) || blockType.equals(12) || blockType.equals(4);
        }).collect(Collectors.toList());


        // 个股 - 行业板块（研究行业 + 普通行业）
        List<TdxhyParser.TdxhyDTO> tdxhyDTOList = TdxhyParser.parse();


        // 概念板块 - 个股code列表
        // List<BlockGnParser.BlockDatDTO> blockGnDTOList = BlockGnParser.parse_gn();
        List<BlockReportParser.ExportBlockDTO> blockGnDTOList = BlockReportParser.parse_gn();


        // -------------------------------------------------------------------------------------------------------------


        // 个股 - 交易所（深沪京）
        Map<String, Integer> stockCode_marketType_map = Maps.newHashMap();

        // 个股code - 个股name
        Map<String, String> stockCode_stockName_map = Maps.newHashMap();


        // （行业）关联code - 板块code
        Map<String, String> txCode_blockCode_map = Maps.newHashMap();


        // 个股 - 板块列表
        Map<String, Set<String>> stockCode_blockCodeSet_map = Maps.newHashMap();


        // all 个股code
        Set<String> allStockCodeSet = Sets.newTreeSet();


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
        fill___stockCode_blockCodeSet_map(stockCode_blockCodeSet_map, stockCode_stockName_map, allStockCodeSet,

                                          blockGnDTOList);


        // -------------------------------------------------------------------------------------------------------------
        allStockCodeSet.addAll(stockCode_blockCodeSet_map.keySet());


        // 排除 B股
        allStockCodeSet.removeIf(stockCode -> !StockTypeEnum.isAStock_ETF_block(stockCode));
        stockCode_blockCodeSet_map.keySet().removeIf(key -> !StockTypeEnum.isAStock_ETF_block(key));


        log.info("all 板块code     >>>     板块size : {}", tdxzs3DTOList.size());
        log.info("all 个股code     >>>     个股size : {}", allStockCodeSet.size());


        // -------------------------------------------------------------------------------------------------------------


        // 个股code          从小到大  排列
        List<String> sortAllStockCodeList = Lists.newArrayList(allStockCodeSet);
        Collections.sort(sortAllStockCodeList);


        // -------------------------------------------------------------------------------------------------------------


        Map<String, String> block__codeNameMap = Maps.newHashMap();
        Map<String, String> hyBlock__code_pCode_map = Maps.newHashMap();


        // 板块 - 个股列表
        Map<String, Set<String>> blockCode_stockCodeSet_map = Maps.newHashMap();
        stockCode_blockCodeSet_map.forEach((stockCode, blockCodeSet) -> {
            blockCodeSet.forEach(blockCode -> {
                blockCode_stockCodeSet_map.computeIfAbsent(blockCode, k -> Sets.newHashSet()).add(stockCode);
            });
        });


        // -------------------------------------------------------------------------------------------------------------
        //                                              save2DB
        // -------------------------------------------------------------------------------------------------------------


        // 个股
        save2DB___stock(sortAllStockCodeList, stockCode_marketType_map, stockCode_stockName_map);


        // 板块
        save2DB___block(tdxzs3DTOList, block__codeNameMap, hyBlock__code_pCode_map);
        // 行业板块 - 父ID
        save2DB___hyBlock_pId(hyBlock__code_pCode_map, block__codeNameMap);


        // 板块 - 个股
        importBlockReport_____save2DB___block_rela_stock(blockCode_stockCodeSet_map, StockTypeEnum.A_STOCK.type);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                              关联关系
    // -----------------------------------------------------------------------------------------------------------------


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


            Set<String> blockCodeSet = Sets.newHashSet(blockCode_T, blockCode_X).stream().filter(Objects::nonNull).collect(Collectors.toSet());


            stockCode_blockCodeSet_map.computeIfAbsent(stockCode, k -> Sets.newHashSet())
                                      .addAll(blockCodeSet);
        });

    }


    private void fill___stockCode_blockCodeSet_map(Map<String, Set<String>> stockCode_blockCodeSet_map,
                                                   Map<String, String> stockCode_stockName_map,
                                                   Set<String> allStockCodeSet,

                                                   List<BlockReportParser.ExportBlockDTO> blockGnDTOList) {


        // 概念板块
        blockGnDTOList.forEach(e -> {


            String blockCode = e.getBlockCode();
            String blockName = e.getBlockName();

            String stockCode = e.getStockCode();
            String stockName = e.getStockName();


            allStockCodeSet.add(stockCode);
            stockCode_stockName_map.put(stockCode, stockName);

            stockCode_blockCodeSet_map.computeIfAbsent(stockCode, k -> Sets.newTreeSet()).add(blockCode);
        });

    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                              save2DB
    // -----------------------------------------------------------------------------------------------------------------


    private void save2DB___stock(List<String> sortAllStockCodeList,
                                 Map<String, Integer> stockCode_marketType_map,
                                 Map<String, String> stockCode_stockName_map) {


        Map<String, Long> stock__codeIdMap_DB = baseStockService.codeIdMap(StockTypeEnum.A_STOCK.type);


        // -------------------------------------------------------------------------------------------------------------


        long start_1 = System.currentTimeMillis();


        List<BaseStockDO> entityList = Lists.newArrayList();
        sortAllStockCodeList.forEach(stockCode -> {

            Integer marketType = stockCode_marketType_map.get(stockCode);
            if (marketType == null) {
                log.warn("marketType为空     >>>     stockCode : {}", stockCode);
                return;
            }


            // 个股
            BaseStockDO entity = new BaseStockDO();
            entity.setCode(stockCode);
            entity.setName(stockCode_stockName_map.get(stockCode));
            entity.setType(StockTypeEnum.A_STOCK.type);
            entity.setTdxMarketType(marketType);


            entity.setId(stock__codeIdMap_DB.get(stockCode));
            entityList.add(entity);
        });


        // 3min 4s（狗屎🐶💩 Mybatis-plus）  ->   1.5s
        baseStockService.batchInsertOrUpdate(entityList);
        log.info("save2DB___stock     >>>     size : {} , 耗时 : {}", entityList.size(), DateTimeUtil.formatNow2Hms(start_1));


        // -------------------------------------------------------------------------------------------------------------


//        Map<String, Long> stock__codeIdMap_DB2 = baseStockService.codeIdMap(StockTypeEnum.A_STOCK.type);
//        stock__codeIdMap.putAll(stock__codeIdMap_DB2);
    }

    private void exec___save2DB___stock(String stockCode,
                                        Map<String, Long> stock__codeIdMap_DB,
                                        Map<String, Integer> stockCode_marketType_map,
                                        Map<String, String> stockCode_stockName_map,

                                        Map<String, Long> stock__codeIdMap) {


        Integer marketType = stockCode_marketType_map.get(stockCode);
        if (marketType == null) {
            log.warn("marketType为空     >>>     stockCode : {}", stockCode);
            return;
        }


        // 个股
        BaseStockDO entity = new BaseStockDO();
        entity.setCode(stockCode);
        entity.setName(stockCode_stockName_map.get(stockCode));
        entity.setType(StockTypeEnum.A_STOCK.type);
        entity.setTdxMarketType(marketType);


        entity.setId(stock__codeIdMap_DB.get(stockCode));
        baseStockService.saveOrUpdate(entity);


        Long stockId = entity.getId();
        stock__codeIdMap.put(stockCode, stockId);


        if (stockId == null) {
            log.error("base_stock - insertOrUpdate   err     >>>     stockCode : {} , stockId : {} , baseStockDO : {}",
                      stockCode, stockId, JSON.toJSONString(entity));
        }
    }


    private void save2DB___block(List<Tdxzs3Parser.Tdxzs3DTO> tdxzs3DTOList,

                                 Map<String, String> block__codeNameMap,
                                 Map<String, String> hyBlock__code_pCode_map) {


        long start = System.currentTimeMillis();


        // 从小到大   排列
        tdxzs3DTOList.sort(Comparator.comparing(Tdxzs3Parser.Tdxzs3DTO::getCode));


        // 概念 - 行业
        // Map<String, String> gnCode__hyCode__map = GnRelaHyParser.gnCode__hyCode__map();


        Map<String, Long> block__codeIdMap_DB = baseBlockService.codeIdMap();


        // ----------------------------------- 遍历 - save


        List<BaseBlockDO> entityList = Lists.newArrayList();
        tdxzs3DTOList.forEach(e -> {

            String blockCode = e.getCode();
            String blockName = e.getName();

            String pCode = e.getPCode();


//            // ----------------------------------- 概念 - 行业
//            if (e.getBlockType() == 4) {
//                pCode = gnCode__hyCode__map.get(blockCode);
//            }


            // -----------------------------------


            BaseBlockDO entity = new BaseBlockDO();

            entity.setCode(blockCode);
            entity.setName(blockName);
            entity.setType(e.getBlockType());
            entity.setLevel(e.getLevel());
            entity.setEndLevel(e.getEndLevel());
            // 父级
            entity.setParentId(null);


            Long blockId = block__codeIdMap_DB.get(blockCode);
            entity.setId(blockId);
            entityList.add(entity);


            block__codeNameMap.put(blockCode, blockName);


            // 父级（行业板块）
            if (StringUtils.isNotEmpty(pCode)) {
                hyBlock__code_pCode_map.put(blockCode, pCode);
            }
        });


        baseBlockService.batchInsertOrUpdate(entityList);


        log.info("save2DB___block     >>>     size : {} , 耗时 : {}", entityList.size(), DateTimeUtil.formatNow2Hms(start));
    }


    private void save2DB___hyBlock_pId(Map<String, String> hyBlock__code_pCode_map,
                                       Map<String, String> block__codeNameMap) {


        Map<String, Long> block__codeIdMap = baseBlockService.codeIdMap();


        long start = System.currentTimeMillis();


        // p_id（行业板块）
        hyBlock__code_pCode_map.entrySet().parallelStream().forEach(entry -> {
            String blockCode = entry.getKey();
            String pCode = entry.getValue();


            BaseBlockDO baseBlockDO = new BaseBlockDO();
            baseBlockDO.setId(block__codeIdMap.get(blockCode));
            baseBlockDO.setParentId(block__codeIdMap.get(pCode));


            // ------------------------------------------------------------------------ code/name   path


            String lv3_blockName = block__codeNameMap.get(blockCode);
            String lv2_blockName = block__codeNameMap.get(pCode);


            String codePath = pCode + "-" + blockCode;
            String namePath = lv2_blockName + "-" + lv3_blockName;

            String lv1_blockCode = hyBlock__code_pCode_map.get(pCode);
            if (StringUtils.isNotEmpty(lv1_blockCode)) {
                String lv1_blockName = block__codeNameMap.get(lv1_blockCode);

                codePath = lv1_blockCode + "-" + codePath;
                namePath = lv1_blockName + "-" + namePath;
            }

            baseBlockDO.setCodePath(codePath);
            baseBlockDO.setNamePath(namePath);


            // ------------------------------------------------------------------------
            baseBlockService.updateById(baseBlockDO);
        });


        log.info("save2DB___hyBlock_pId     >>>     size : {} , 耗时 : {}", hyBlock__code_pCode_map.size(), DateTimeUtil.formatNow2Hms(start));
    }


//    private void save2DB___block_rela_stock(List<String> sortAllStockCodeList,
//                                            Map<String, Long> stock__codeIdMap,
//                                            Map<String, Set<String>> stockCode_blockCodeSet_map,
//                                            Map<String, Long> block__codeIdMap) {
//
//        long start = System.currentTimeMillis();
//
//
//        Set<Long> stockIdSet = Sets.newHashSet();
//        List<BaseBlockRelaStockDO> entityList = Lists.newArrayList();
//
//
//        sortAllStockCodeList.forEach(stockCode -> {
//
//            Long stockId = stock__codeIdMap.get(stockCode);
//            Set<String> blockCodeSet = stockCode_blockCodeSet_map.get(stockCode);
//
//
//            if (stockId == null || CollectionUtils.isEmpty(blockCodeSet)) {
//                log.error("个股-板块   err     >>>     stockCode : {} , stockId : {} , blockCodeSet : {}",
//                          stockCode, stockId, JSON.toJSONString(blockCodeSet));
//                return;
//            }
//
//
//            blockCodeSet.forEach(blockCode -> {
//                Long blockId = block__codeIdMap.get(blockCode);
//
//
//                // 板块 - 个股
//                BaseBlockRelaStockDO entity = new BaseBlockRelaStockDO();
//                entity.setStockId(stockId);
//                entity.setBlockId(blockId);
//
//
//                if (null == blockId) {
//                    log.error("板块-个股   NPE     >>>     stockCode : {} , stockId : {} , blockCode : {} , blockId : {}",
//                              stockCode, stockId, blockCode, blockId);
//                    return;
//                }
//
//
//                entityList.add(entity);
//            });
//            stockIdSet.add(stockId);
//        });
//
//
//        // del All
//        stockIdSet.parallelStream().forEach(baseBlockRelaStockService::deleteByStockId);
//        // batch insert
//        baseBlockRelaStockService.batchInsert(entityList);
//
//
//        log.info("save2DB___block_rela_stock     >>>     size : {} , 耗时 : {}", entityList.size(), DateTimeUtil.formatNow2Hms(start));
//    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                              报表-导入
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @Override
    public void importBlockReport() {


        // -------------------------------------------------------------------------------------------------------------
        //                                              数据读取（系统板块 txt）
        // -------------------------------------------------------------------------------------------------------------


        // 数据读取   ->   txt报表（系统板块 报表）
        List<BlockReportParser.ExportBlockDTO> dtoList = BlockReportParser.parseAllTdxBlock();


        // -------------------------------------------------------------------------------------------------------------
        //                                              数据读取（DB -> 已导入过的 数据 [板块/个股] ）
        // -------------------------------------------------------------------------------------------------------------


        // DB读取（板块）    ->     code_id_map
        Map<String, Long> block__codeIdMap = baseBlockService.codeIdMap();


        // DB读取（个股）    ->     code_id_map  +  code_name_map
        List<BaseStockDO> allStockDOList = baseStockService.listAllSimple();

        Map<String, Long> stock__codeIdMap = allStockDOList.stream().collect(Collectors.toMap(BaseStockDO::getCode, BaseStockDO::getId));
        Map<String, String> stock__codeNameMap = allStockDOList.stream().collect(Collectors.toMap(BaseStockDO::getCode, stock -> stock.getName() != null ? stock.getName() : ""));


        // -------------------------------------------------------------------------------------------------------------
        //                                              关联关系
        // -------------------------------------------------------------------------------------------------------------


        // blockCode - stockCode列表   关联关系
        Map<String, Set<String>> blockCode_stockCodeSet_map = Maps.newTreeMap();


        // -------------------------------------------------------------------------------------------------------------
        //                                              save2DB
        // -------------------------------------------------------------------------------------------------------------


        importBlockReport_____save2DB___block(dtoList,

                                              block__codeIdMap,
                                              stock__codeIdMap,
                                              stock__codeNameMap,

                                              blockCode_stockCodeSet_map);


        // -------------------------------------------------------------------------------------------------------------


        importBlockReport_____save2DB___block_rela_stock(blockCode_stockCodeSet_map, StockTypeEnum.A_STOCK.type);
    }


    private void importBlockReport_____save2DB___block(List<BlockReportParser.ExportBlockDTO> dtoList,

                                                       Map<String, Long> block__codeIdMap,
                                                       Map<String, Long> stock__codeIdMap,
                                                       Map<String, String> stock__codeNameMap,

                                                       Map<String, Set<String>> blockCode_stockCodeSet_map) {


        // blockCode   ->   从小到大   排列
        dtoList.sort(Comparator.comparing(BlockReportParser.ExportBlockDTO::getBlockCode));


        List<BaseStockDO> baseStockDOList = Lists.newArrayList();


        dtoList.forEach(e -> {


            String blockCode = e.getBlockCode();
            String blockName = e.getBlockName();
            Integer blockType = e.getBlockType();

            String stockCode = e.getStockCode();
            String stockName = e.getStockName();


            // ------------------------------------------------------


            // block -> save2DB
            Long blockId = block__codeIdMap.get(blockCode);
            if (blockId == null) {

                BaseBlockDO blockEntity = new BaseBlockDO();
                blockEntity.setCode(blockCode);
                blockEntity.setName(blockName);
                blockEntity.setType(blockType);

                baseBlockService.save(blockEntity);


                blockId = blockEntity.getId();
                block__codeIdMap.put(blockCode, blockId);
            }


            // ------------------------------------------------------


            // 是否为 A股       （非A股 => B股 -> 忽略）
            boolean isAStock = true;


            // stock -> save2DB
            Long stockId = stock__codeIdMap.get(stockCode);
            if (stockId == null) {


                BaseStockDO stockEntity = new BaseStockDO();
                stockEntity.setCode(stockCode);
                stockEntity.setName(stockName);
                stockEntity.setType(StockTypeEnum.getTypeByStockCode(stockCode));
                stockEntity.setTdxMarketType(StockMarketEnum.getTdxMarketType(stockCode));


                // 等待 排序后，再insert
                if (null != stockEntity.getTdxMarketType()) {
                    baseStockDOList.add(stockEntity);
                } else {
                    // 非A股     ==>     B股 + ST   ->   忽略          （900957 - *ST凌云B）
                    isAStock = false;
                    log.error("importBlockReport_____save2DB___block   -   个股 -> 未知类型（B股[非A股] -> 忽略）    >>>     stockCode : {} , e : {}", stockCode, JSON.toJSONString(e));
                }


            } else {

                // 股票name - 补齐
                if (StringUtils.isBlank(stock__codeNameMap.get(stockCode))) {
                    BaseStockDO baseStockDO = new BaseStockDO();
                    baseStockDO.setId(stockId);
                    baseStockDO.setName(stockName);

                    baseStockService.updateById(baseStockDO);


                    // 实在 找不到 stockName     =>     上市失败的   ->   688688 - [蚂蚁集团]
                }
            }


            // ------------------------------------------------------


            // blockCode - stockCodeSet     关联关系
            if (isAStock) {
                blockCode_stockCodeSet_map.computeIfAbsent(blockCode, k -> Sets.newTreeSet()).add(stockCode);
            }
        });


        // -------------------------------------------------------------------------------------------------------------


        // stock -> save2DB


        // sort
        baseStockDOList.sort(Comparator.comparing(BaseStockDO::getCode));
        // batch insert
        baseStockService.batchInsert(baseStockDOList);

        baseStockDOList.forEach(e -> {
            stock__codeIdMap.put(e.getCode(), e.getId());
        });
    }


    private void importBlockReport_____save2DB___block_rela_stock(Map<String, Set<String>> blockCode_stockCodeSet_map,
                                                                  Integer stockType) {


        Map<String, Long> block__codeIdMap = baseBlockService.codeIdMap();
        Map<String, Long> stock__codeIdMap = baseStockService.codeIdMap(stockType);


        // -------------------------------------------------------------------------------------------------------------


        // blockId - stockId   关联列表
        List<BaseBlockRelaStockDO> new__relaEntityList = Lists.newArrayList();


        // 新增    ->  insert
        Set<String> new__keySet = Sets.newHashSet();
        // 已存在  ->  略过
        Set<String> exist__keySet = Sets.newHashSet();


        // old（DB）  =>   K : blockId-stockId       V : ID
        Map<String, Long> old__blockIdStockId_ID_Map = baseBlockRelaStockService.listAll(stockType)
                                                                                .stream()
                                                                                .collect(Collectors.toMap(
                                                                                        // K : blockId-stockId
                                                                                        e -> e.getBlockId() + "-" + e.getStockId(),
                                                                                        // V : ID
                                                                                        BaseBlockRelaStockDO::getId));


        // -------------------------------------------------------------------------------------------------------------


        // DEL  ->  仅对比  IN txt中的板块（行业  =>  细分行业  +  研究行业[未导出 -> 否则，会被删除]）
        Set<Long> txt__blockIdSet = Sets.newHashSet();


        // -------------------------------------------------------------------------------------------------------------


        blockCode_stockCodeSet_map.forEach((blockCode, stockCodeSet) -> {
            Long blockId = block__codeIdMap.get(blockCode);
            txt__blockIdSet.add(blockId);


//            if (null == blockId) {
//                log.error("importBlockReport_____save2DB___block_rela_stock   -   [板块] 不存在   ->   忽略     >>>     blockCode : {}", blockCode);
//                return;
//            }


            stockCodeSet.forEach(stockCode -> {
                Long stockId = stock__codeIdMap.get(stockCode);


                if (null == stockId) {
                    log.error("importBlockReport_____save2DB___block_rela_stock   -   [{}] 不存在   ->   忽略     >>>     stockCode : {}", StockTypeEnum.getDescByType(stockType), stockCode);
                    return;
                }


                // K : blockId-stockId
                String key = blockId + "-" + stockId;
                Long old_ID = old__blockIdStockId_ID_Map.get(key);


                // 不存在  ->  新增
                if (null == old_ID) {
                    BaseBlockRelaStockDO new_relaEntity = new BaseBlockRelaStockDO();
                    new_relaEntity.setBlockId(blockId);
                    new_relaEntity.setStockId(stockId);
                    new_relaEntity.setStockType(stockType);

                    new__relaEntityList.add(new_relaEntity);
                    new__keySet.add(key);

                } else {
                    // 已存在  ->  略过
                    exist__keySet.add(key);
                }
            });
        });


        // -------------------------------------------------------------------------------------------------------------


        // del old
        List<Long> del_ID_List = Lists.newArrayList();


        Map<Long, String> debug___block__idCodeMap = baseBlockService.idCodeMap();
        Map<Long, String> debug___stock__idCodeMap = baseStockService.idCodeMap();


        // IN old   +   NOT IN (new + exist)     =>     DEL
        old__blockIdStockId_ID_Map.forEach((key, old_ID) -> {
            Long blockId = Long.parseLong(key.split("-")[0]);
            Long stockId = Long.parseLong(key.split("-")[1]);
            if (!new__keySet.contains(key) && !exist__keySet.contains(key) && txt__blockIdSet.contains(blockId)) {
                del_ID_List.add(old_ID);

                log.warn("系统板块-{} 删除   >>>     blockCode : {} , stockCode : {} , ID : {}", StockTypeEnum.getDescByType(stockType), debug___block__idCodeMap.get(blockId), debug___stock__idCodeMap.get(stockId), old_ID);
            }
        });


        long start_del = System.currentTimeMillis();
        baseBlockRelaStockService.removeBatchByIds(del_ID_List);
        log.info("系统板块-{} 关联   -   [del_old]     >>>     size : {} , time : {}", StockTypeEnum.getDescByType(stockType), del_ID_List.size(), DateTimeUtil.formatNow2Hms(start_del));


        // -------------------------------------------------------------------------------------------------------------


        // insert new
        log.info("系统板块-{} 关联   -   [insert_new] start     >>>     size : {}", StockTypeEnum.getDescByType(stockType), new__relaEntityList.size());
        long start_insert = System.currentTimeMillis();
        baseBlockRelaStockService.batchInsert(new__relaEntityList);
        log.info("系统板块-{} 关联   -   [insert_new] end       >>>     size : {} , time : {}", StockTypeEnum.getDescByType(stockType), new__relaEntityList.size(), DateTimeUtil.formatNow2Hms(start_insert));


        print__new__relaEntityList(new__relaEntityList);


        // -------------------------------------------------------------------------------------------------------------


        log.info("importBlockReport_____save2DB___block_rela_stock     >>>     exist_size : {} , del_old_size : {} , insert_new_size : {}",
                 exist__keySet.size(), del_ID_List.size(), new__relaEntityList.size());
    }


    private void print__new__relaEntityList(List<BaseBlockRelaStockDO> new__relaEntityList) {
        if (CollectionUtils.isEmpty(new__relaEntityList)) return;


        Map<Long, BaseBlockDO> idBlockMap = baseBlockService.listAllSimple().stream().collect(Collectors.toMap(BaseBlockDO::getId, e -> e));
        Map<Long, BaseStockDO> idStockMap = baseStockService.listAllSimple().stream().collect(Collectors.toMap(BaseStockDO::getId, e -> e));

        new__relaEntityList.forEach(e -> {
            BaseBlockDO blockDO = idBlockMap.get(e.getBlockId());
            BaseStockDO stockDO = idStockMap.get(e.getStockId());

            System.out.println(blockDO.getCode() + "-" + blockDO.getName() + "   " + stockDO.getCode() + "-" + stockDO.getName());
        });
    }


    @TotalTime
    @Override
    public void importBlockNewReport() {


        // 数据读取   ->   txt报表（自定义板块 报表）
        List<BlockReportParser.ExportBlockDTO> zdy_dtoList = BlockReportParser.parse_zdy();


        // -------------------------------------------------------------------------------------------------------------
        //                                              关联关系
        // -------------------------------------------------------------------------------------------------------------


        // 自定义板块   code-ID   （DB库）
        Map<String, Long> blockNew__codeIdMap = baseBlockNewService.codeIdMap();


        // 自定义板块 - 个股（板块/指数）列表
        Map<String, Set<String>> blockNewCode_stockCodeSet_map = Maps.newTreeMap();


        // 全部   个股（板块/指数）code
        Set<String> allStockCodeSet = Sets.newHashSet();


        // -------------------------------------------------------------------------------------------------------------
        //                                              save2DB
        // -------------------------------------------------------------------------------------------------------------


        save2DB___blockNew(zdy_dtoList,
                           blockNew__codeIdMap,

                           allStockCodeSet,
                           blockNewCode_stockCodeSet_map);


        // -------------------------------------------------------------------------------------------------------------


        // 个股   code-Id
        Map<String, Long> sotock__codeIdMap = baseStockService.codeIdMap(allStockCodeSet);
        // 板块   code-Id
        Map<String, Long> block__codeIdMap = baseBlockService.codeIdMap(allStockCodeSet);


        // -------------------------------------------------------------------------------------------------------------
        //                                              save2DB
        // -------------------------------------------------------------------------------------------------------------


        // 3min 13s
        save2DB___stock_rela_blockNew(blockNewCode_stockCodeSet_map,
                                      blockNew__codeIdMap,
                                      sotock__codeIdMap,
                                      block__codeIdMap);
    }


    private void save2DB___blockNew(List<BlockReportParser.ExportBlockDTO> zdy_dtoList,
                                    Map<String, Long> blockNew__codeIdMap,

                                    Set<String> allStockCodeSet,
                                    Map<String, Set<String>> blockNewCode_stockCodeSet_map) {


        zdy_dtoList.forEach(e -> {


            // 自定义板块
            String blockNewCode = e.getBlockCode();
            String blockNewName = e.getBlockName();

            // 关联 个股/板块/指数   /   ETF/港美股/...
            String stockCode = e.getStockCode();
            String stockName = e.getStockName();


            // 自定义板块 - 创建
            Long blockNewId = blockNew__codeIdMap.get(blockNewCode);
            if (blockNewId == null) {

                BaseBlockNewDO baseBlockNewDO = new BaseBlockNewDO();
                baseBlockNewDO.setCode(blockNewCode);
                baseBlockNewDO.setName(blockNewName);

                baseBlockNewService.save(baseBlockNewDO);


                blockNewId = baseBlockNewDO.getId();
                blockNew__codeIdMap.put(blockNewCode, blockNewId);
            }


            Set<String> stockCodeSet = blockNewCode_stockCodeSet_map.get(blockNewCode);
            if (CollectionUtils.isEmpty(stockCodeSet)) {
                stockCodeSet = Sets.newHashSet(stockCode);
                blockNewCode_stockCodeSet_map.put(blockNewCode, stockCodeSet);
            } else {
                stockCodeSet.add(stockCode);
            }


            // 所有 关联code：个股/板块/指数
            allStockCodeSet.add(stockCode);
        });

    }


    private void save2DB___stock_rela_blockNew(Map<String, Set<String>> blockNewCode_stockCodeSet_map,
                                               Map<String, Long> blockNew__codeIdMap,
                                               Map<String, Long> sotock__codeIdMap,
                                               Map<String, Long> block__codeIdMap) {


        blockNewCode_stockCodeSet_map.keySet().parallelStream().forEach(blockNewCode -> {
            Set<String> stockCodeSet = blockNewCode_stockCodeSet_map.get(blockNewCode);


            Long blockNewId = blockNew__codeIdMap.get(blockNewCode);


            List<BaseBlockNewRelaStockDO> relaDOList = Lists.newArrayList();
            stockCodeSet.forEach(stockCode -> {


                // 个股
                Long relaId = sotock__codeIdMap.get(stockCode);
                int type = BlockNewTypeEnum.STOCK.getType();
                if (relaId == null) {
                    // 板块
                    relaId = block__codeIdMap.get(stockCode);
                    type = BlockNewTypeEnum.BLOCK.getType();
                }


                if (relaId == null) {

                    // TODO   自定义板块     - 关联了 ->     大盘指数 / ETF / 港股 / 美股 / ...     =>     忽略
                    log.warn("exportBlockNew - stockId不存在     >>>     blockNewCode : {} , stockCode : {}", blockNewCode, stockCode);


                    // BaseStockDO baseStockDO = new BaseStockDO();
                    // baseStockDO.setCode(stockCode);
                    // baseStockDO.setName(stockName);
                    //
                    // iBaseStockService.save(baseStockDO);
                    // baseBlockNewRelaStockDO.setStockId(baseStockDO.getId());


                } else {

                    BaseBlockNewRelaStockDO entity = new BaseBlockNewRelaStockDO();
                    entity.setBlockNewId(blockNewId);
                    entity.setStockId(relaId);
                    entity.setType(type);

                    relaDOList.add(entity);
                }
            });


            baseBlockNewRelaStockService.delByBlockNewId(blockNewId);
            baseBlockNewRelaStockService.batchInsert(relaDOList);
        });
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  ETF
    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @Override
    public void importETF() {

        // ETF列表
        import__ETF();


        // ETF - 板块  关联关系
        import___block_rela_ETF();
    }


    @TotalTime
    @Override
    public void importHkStock() {
        import__hkOrUs_Stock(BlockPoolEnum.GG, StockTypeEnum.HK_STOCK);
    }


    @TotalTime
    @Override
    public void importUsStock() {
        import__hkOrUs_Stock(BlockPoolEnum.MG, StockTypeEnum.US_STOCK);
    }


    private void import__hkOrUs_Stock(BlockPoolEnum GG, StockTypeEnum HK_STOCK) {


        // ----------------------------------------- 港美股列表   解析


        // 数据读取   ->   txt报表（自定义板块 报表）
        List<BlockReportParser.ExportBlockDTO> zdy_dtoList = BlockReportParser.parse_zdy();


        // ,港股(自动),00700,腾讯控股
        // ,美股(自动),SPY,标普500 ETF-SPDR
        List<BlockReportParser.ExportBlockDTO> hkStockList = zdy_dtoList.stream()
                                                                        .filter(dto -> Objects.equals(dto.getBlockCode(), GG.getBlockNewCode()))
                                                                        .sorted(Comparator.comparing(BlockReportParser.ExportBlockDTO::getStockCode))
                                                                        .toList();


        // ----------------------------------------- 港美股列表   save2DB


        Map<String, Long> old__hkStock_codeIdMap = baseStockService.codeIdMap(HK_STOCK.type);


        List<BaseStockDO> entityList = Lists.newArrayList();
        hkStockList.forEach(hkStock -> {


            String stockCode = hkStock.getStockCode();
            String stockName = hkStock.getStockName();
            Integer tdxMarketType = StockMarketEnum.getTdxMarketType(stockCode);


            // 港股
            BaseStockDO entity = new BaseStockDO();
            entity.setCode(stockCode);
            entity.setName(stockName);
            entity.setType(HK_STOCK.type);
            entity.setTdxMarketType(tdxMarketType);


            entity.setId(old__hkStock_codeIdMap.get(stockCode));
            entityList.add(entity);
        });


        // insert new
        baseStockService.batchInsertOrUpdate(entityList);


        // --------------------------------------------------------


        // DEL old（NOT IN -> new）
        Set<String> new__hkStockCodeSet = entityList.stream().map(BaseStockDO::getCode).collect(Collectors.toSet());
        old__hkStock_codeIdMap.forEach((code, id) -> {
            if (!new__hkStockCodeSet.contains(code)) {
                baseStockService.removeById(id);
                log.warn("DEL old     >>>     code : {}", code);
            }
        });
    }


    private void import__ETF() {


        // ----------------------------------------- ETF列表   解析（.txt）
        List<HyETFReportParser.TdxETFDTO> etfList = HyETFReportParser.parseAndDistinct();


        // ----------------------------------------- ETF列表   save2DB


        Map<String, Long> old__ETF_codeIdMap = baseStockService.codeIdMap(StockTypeEnum.ETF.type);


        List<BaseStockDO> ETF_entityList = Lists.newArrayList();
        etfList.forEach(etf -> {


            String stockCode = etf.getCode();
            String stockName = StringUtils.isBlank(etf.getName()) ? EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode).getName() : etf.getName();
            Integer tdxMarketType = StockMarketEnum.getTdxMarketType(stockCode);
            String 细分行业 = etf.get细分行业();


            // ETF
            BaseStockDO entity = new BaseStockDO();
            entity.setCode(stockCode);
            entity.setName(stockName);
            entity.setType(StockTypeEnum.ETF.type);
            entity.setTdxMarketType(tdxMarketType);
            entity.setBlockName(细分行业);


            entity.setId(old__ETF_codeIdMap.get(stockCode));
            ETF_entityList.add(entity);
        });


        // insert new
        baseStockService.batchInsertOrUpdate(ETF_entityList);


        // --------------------------------------------------------


        // DEL old（NOT IN -> new）
        Set<String> new__etfCodeSet = ETF_entityList.stream().map(BaseStockDO::getCode).collect(Collectors.toSet());
        old__ETF_codeIdMap.forEach((code, id) -> {
            if (!new__etfCodeSet.contains(code)) {
                baseStockService.removeById(id);
                log.warn("DEL old     >>>     code : {}", code);
            }
        });
    }


    private void import___block_rela_ETF() {


        // ----------------------------------------- ETF - 板块  关联关系
        List<EtfRelaBlockParser.EtfRelaBlockDTO> etf_rela_blockList = EtfRelaBlockParser.parse();


        // 板块 - ETF_Set
        Map<String, Set<String>> blockCode_stockCodeSet_map = Maps.newTreeMap();
        etf_rela_blockList.forEach(e -> {
            blockCode_stockCodeSet_map.computeIfAbsent(e.getBlockCode(), k -> Sets.newTreeSet()).add(e.getEtfCode());
        });


        // ----------------------------------------- 关联关系   save2DB
        importBlockReport_____save2DB___block_rela_stock(blockCode_stockCodeSet_map, StockTypeEnum.ETF.type);
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  Kline
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 板块+个股 - 行情（kline_his）     一键刷新
     */
    @TotalTime
    @SneakyThrows
    @Override
    public void refreshKlineAll(UpdateTypeEnum updateTypeEnum) {

        // 增量：个股/ETF 走 TQ 实时快照，板块由个股实时计算（不再读本地 TDX 板块文件）
        if (updateTypeEnum == UpdateTypeEnum.INCR) {
            fillStockKlineAll(UpdateTypeEnum.INCR);
            calcAndFillBlockKlineAll();
            return;
        }


        // 行情-板块
        Future<?> task1 = Executors.newSingleThreadExecutor().submit(() -> {
            // refresh  ->  block kline
            fillBlockKlineAll();
        });


        // 行情-个股/ETF
        Future<?> task2 = Executors.newSingleThreadExecutor().submit(() -> {
            // refresh  ->  stock/ETF kline
            fillStockKlineAll(updateTypeEnum);
        });


        task1.get();
        task2.get();
    }


    @TotalTime
    @Override
    public void fillBlockKline(String blockCode) {
        fillBlockKline(blockCode, null);
    }

    public void fillBlockKline(String blockCode, Long blockId) {

        BaseBlockDO baseBlockDO = new BaseBlockDO();
        baseBlockDO.setCode(blockCode);


        // 板块 - K线数据
        List<LdayParser.LdayDTO> ldayDTOList = LdayParser.parseByStockCode(blockCode);


        List<String> klines = Lists.newArrayList();

        ldayDTOList.forEach(x -> {


            // K线数据-JSON（日期：[O,H,L,C,VOL,AMO,涨幅,振幅,换手率]）
            // List<Number> kline = Lists.newArrayList(x.getOpen(), x.getHigh(), x.getLow(), x.getClose(), x.getVol(), x.getAmount(), x.getChangePct(), null, null);


            // 2025-05-13,21.06,21.97,20.89,21.45,8455131,18181107751.03,5.18,2.98,0.62,6.33
            // 日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率,流通股本,总股本

            // K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率,流通股本,总股本]）
            List<Object> kline = Lists.newArrayList(String.valueOf(x.getTradeDate()), x.getOpen(), x.getHigh(), x.getLow(), x.getClose(), x.getVol(), x.getAmount(),
                                                    x.getRangePct(), x.getChangePct(), x.getChangePrice(), null, x.getLtgb(), x.getZgb());


            String klineStr = kline.stream().map(obj -> obj != null ? obj.toString() : "").collect(Collectors.joining(","));
            klines.add(klineStr);
        });

        baseBlockDO.setKlineHisStr(JSON.toJSONString(klines));


        if (CollectionUtils.isEmpty(ldayDTOList)) {
            return;
        }


        // 昨日
        if (ldayDTOList.size() >= 2) {
            LdayParser.LdayDTO prev = ldayDTOList.get(ldayDTOList.size() - 2);
            baseBlockDO.setPrevClose(prev.getClose());
        }


        // 板块 - 实时行情
        LdayParser.LdayDTO last = ldayDTOList.get(ldayDTOList.size() - 1);

        baseBlockDO.setTradeDate(last.getTradeDate());
        baseBlockDO.setOpen(last.getOpen());
        baseBlockDO.setHigh(last.getHigh());
        baseBlockDO.setLow(last.getLow());
        baseBlockDO.setClose(last.getClose());
        baseBlockDO.setVolume(last.getVol());
        baseBlockDO.setAmount(last.getAmount());
        baseBlockDO.setRangePct(last.getRangePct());
        baseBlockDO.setChangePct(last.getChangePct());
        baseBlockDO.setChangePrice(last.getChangePrice());
        baseBlockDO.setTurnoverPct(last.getTurnoverPct());


        if (blockId != null) {
            baseBlockDO.setId(blockId);
            baseBlockService.updateById(baseBlockDO);
        } else {
            blockId = baseBlockService.getIdByCode(blockCode);
            baseBlockDO.setId(blockId);

            baseBlockService.saveOrUpdate(baseBlockDO);
        }


        log.info("fillBlockKline suc     >>>     blockCode : {}", blockCode);
    }


    @TotalTime
    @Override
    public void fillBlockKlineAll() {
        long[] start = {System.currentTimeMillis()};
        AtomicInteger count = new AtomicInteger(0);


        Map<String, Long> codeIdMap = baseBlockService.codeIdMap();


        codeIdMap.keySet().parallelStream().forEach((blockCode) -> {
            Long blockId = codeIdMap.get(blockCode);


            fillBlockKline(blockCode, blockId);


            // ------------------------------------------- 计时（频率）   26ms/次   x 881     ->     总耗时：23s


            int countVal = count.incrementAndGet();
            long time = System.currentTimeMillis() - start[0];


            long r1 = time / countVal;
            long r2 = countVal * 1000L / time;
            String r3 = String.format("%s次 - %s", countVal, DateTimeUtil.format2Hms(time));


            // blockCode : 880444, blockId : 94 , count : 881 , r1 : 26ms/次 , r2 : 37次/s , r3 : 881次 - 23s
            log.info("fillBlockKlineAll suc     >>>     blockCode : {}, blockId : {} , count : {} , r1 : {}ms/次 , r2 : {}次/s , r3 : {}",
                     blockCode, blockId, countVal, r1, r2, r3);
        });


        initDataService.deleteDiskCache();
    }


    @TotalTime
    @Override
    public void fillStockKline(String stockCode, Integer apiType, UpdateTypeEnum updateTypeEnum) {
        fillStockKline(stockCode, null, apiType, updateTypeEnum);
        log.info("fillStockKline suc     >>>     stockCode : {}", stockCode);
    }

    @TotalTime
    @Override
    public void fillStockKlineAll(UpdateTypeEnum updateTypeEnum) {


        Map<String, Long> codeIdMap = baseStockService.codeIdMap((Integer) null);


        // 1-全量更新
        if (updateTypeEnum == UpdateTypeEnum.ALL) {
            fullUpdate__fillStockKlineAll(codeIdMap);
        }

        // 2-增量更新（实时行情，最后 1 日）
        else if (updateTypeEnum == UpdateTypeEnum.INCR) {
            fillStockKlineLastNDaysInternal(1);
        }


        initDataService.deleteDiskCache();
    }

    @Override
    public void fillStockKlineLastNDays(int days) {
        Assert.isTrue(days >= 1 && days <= 250, "增量更新天数必须在 1~250 之间：" + days);

        fillStockKlineLastNDaysInternal(days);
        initDataService.deleteDiskCache();
    }


    /**
     * 15s 级实时快照：只更新 base_stock 实时字段（小字段 update），不读/写 kline_his。
     *
     * <p>K线历史由 5min 扩展链路、N 日补偿或盘后全量链路负责；这样实时链路不会被
     * “每只股票读取+重写全历史 JSON”拖到分钟级。</p>
     */
    @Override
    public void fillStockRealtimeSnapshot() {

        Map<String, Long> allCodeIdMap = baseStockService.codeIdMap((Integer) null);
        Map<String, Long> codeIdMap = allCodeIdMap.entrySet().stream()
                                                  .filter(e -> StockTypeEnum.isAStock(e.getKey()) || StockTypeEnum.isETF(e.getKey()))
                                                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        if (codeIdMap.isEmpty()) {
            return;
        }

        List<StockSnapshotKlineDTO> snapshotList = KlineAPI.pullAllStockETFSnapshotKline();
        if (CollectionUtils.isEmpty(snapshotList)) {
            throw new IllegalStateException("TQ 实时快照为空，无法实时刷新");
        }

        LocalDate tradeDate = latestTradingDate();
        List<BaseStockDO> entities = snapshotList.parallelStream()
                                                 .map(snapshot -> {
                                                     if (snapshot == null || snapshot.getStockCode() == null) {
                                                         return null;
                                                     }
                                                     Long stockId = codeIdMap.get(snapshot.getStockCode());
                                                     if (stockId == null) {
                                                         return null;
                                                     }
                                                     if (!(snapshot.getOpen() > 0) || !(snapshot.getHigh() > 0)
                                                             || !(snapshot.getLow() > 0) || !(snapshot.getClose() > 0)) {
                                                         return null;
                                                     }
                                                     if (snapshot.getHigh() < Math.max(snapshot.getOpen(), snapshot.getClose())
                                                             || snapshot.getLow() > Math.min(snapshot.getOpen(), snapshot.getClose())
                                                             || snapshot.getVol() < 0 || !Double.isFinite(snapshot.getAmo()) || snapshot.getAmo() < 0) {
                                                         return null;
                                                     }

                                                     BaseStockDO entity = new BaseStockDO();
                                                     entity.setId(stockId);
                                                     entity.setCode(snapshot.getStockCode());
                                                     entity.setTradeDate(tradeDate);

                                                     if (snapshot.getPrevClose() > 0) {
                                                         entity.setPrevClose(BigDecimal.valueOf(snapshot.getPrevClose()));
                                                     }
                                                     entity.setOpen(of(snapshot.getOpen()));
                                                     entity.setHigh(of(snapshot.getHigh()));
                                                     entity.setLow(of(snapshot.getLow()));
                                                     entity.setClose(of(snapshot.getClose()));
                                                     entity.setVolume(snapshot.getVol());
                                                     entity.setAmount(of(snapshot.getAmo()));

                                                     if (Double.isFinite(snapshot.getRangePct())) {
                                                         entity.setRangePct(of(snapshot.getRangePct()));
                                                     }
                                                     if (Double.isFinite(snapshot.getChangePct())) {
                                                         entity.setChangePct(of(snapshot.getChangePct()));
                                                     }
                                                     if (Double.isFinite(snapshot.getChangePrice())) {
                                                         entity.setChangePrice(of(snapshot.getChangePrice()));
                                                     }

                                                     return entity;
                                                 })
                                                 .filter(Objects::nonNull)
                                                 .toList();


        // 关键：不能逐行/逐条 update（base_stock 含大字段，实测单行 2~5s）；
        // 使用现有批量 upsert，COALESCE(NULL, old) 保留 kline_his/ext_data_his。
        baseStockService.batchInsertOrUpdate(entities);


        log.info("fillStockRealtimeSnapshot     >>>     tradeDate : {} , snapshot : {} , update : {}",
                 tradeDate, snapshotList.size(), entities.size());
    }


    /**
     * 1-全量更新
     *
     * @param codeIdMap
     */
    private void fullUpdate__fillStockKlineAll(Map<String, Long> codeIdMap) {
        long[] start = {System.currentTimeMillis()};
        AtomicInteger count = new AtomicInteger(0);


        codeIdMap.keySet().parallelStream().forEach(stockCode -> {
            try {
                Long stockId = codeIdMap.get(stockCode);


                // -------------------------------------------


                int apiType = apiType(null, UpdateTypeEnum.ALL);


                fillStockKline(stockCode, stockId, apiType, UpdateTypeEnum.ALL);


                // ------------------------------------------- 计时（频率）   29ms/次   x 5500     ->     总耗时：161s


                int countVal = count.incrementAndGet();
                long time = System.currentTimeMillis() - start[0];


                long r1 = time / countVal;
                long r2 = countVal * 1000 / time;
                String r3 = String.format("%s次 - %s", countVal, DateTimeUtil.format2Hms(time));


                // stockCode : 300154, stockId : 1630 , count : 5424 , r1 : 29ms/次 , r2 : 33次/s , r3 : 5424次 - 161s
                log.info("fillStockKlineAll suc     >>>     stockCode : {}, stockId : {} , count : {} , r1 : {}ms/次 , r2 : {}次/s , r3 : {}",
                         stockCode, stockId, countVal, r1, r2, r3);


            } catch (Exception ex) {

                // 不要影响 其他正常个股
                log.error("fillStockKlineAll error     >>>     stockCode : {}, stockId : {} , count : {}",
                          stockCode, codeIdMap.get(stockCode), count.incrementAndGet(), ex);
            }
        });
    }

    /**
     * 2-增量更新
     *
     * @param days
     */
    private void fillStockKlineLastNDaysInternal(int days) {
        Map<String, Long> allCodeIdMap = baseStockService.codeIdMap((Integer) null);
        Map<String, Long> codeIdMap = allCodeIdMap.entrySet().stream()
                                                  .filter(e -> StockTypeEnum.isAStock(e.getKey()) || StockTypeEnum.isETF(e.getKey()))
                                                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

        if (codeIdMap.isEmpty()) {
            log.warn("fillStockKlineLastNDaysInternal     >>>     无 A股/ETF 基础数据，跳过");
            return;
        }

        if (days == 1) {
            // 盘中：TQ 批量快照 -> 最后 1 日实时行情
            fillStockKlineBySnapshot(codeIdMap);
            return;
        }

        // 自定义最近 N 日：TQ 批量日线（含日期）
        fillStockKlineByDailyBars(codeIdMap, days);

        // 再叠加一次实时快照，保证 N>1 时最后 1 日也是盘中最新价；快照失败不回滚已写入的日线
        try {
            fillStockKlineBySnapshot(codeIdMap);
        } catch (Exception e) {
            log.warn("fillStockKlineLastNDaysInternal snapshot overlay fail     >>>     days : {} , err : {}", days, e.getMessage(), e);
        }
    }


    /**
     * TQ 实时快照 -> 最后 1 日。
     *
     * <p>TQ 快照本身不带日期，日期必须从 TQ 交易日历获取；禁止使用本机日期。</p>
     */
    private void fillStockKlineBySnapshot(Map<String, Long> codeIdMap) {

        List<StockSnapshotKlineDTO> snapshotList = KlineAPI.pullAllStockETFSnapshotKline();
        if (CollectionUtils.isEmpty(snapshotList)) {
            throw new IllegalStateException("TQ 实时快照为空，无法增量更新");
        }

        LocalDate tradeDate = latestTradingDate();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger skip = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        snapshotList.parallelStream().forEach(snapshot -> {
            if (snapshot == null || snapshot.getStockCode() == null) {
                skip.incrementAndGet();
                return;
            }
            Long stockId = codeIdMap.get(snapshot.getStockCode());
            if (stockId == null) {
                skip.incrementAndGet();
                return;
            }

            KlineDTO bar = new KlineDTO();
            BeanUtils.copyProperties(snapshot, bar);
            bar.setDate(tradeDate);

            if (!validKline(bar)) {
                skip.incrementAndGet();
                return;
            }

            try {
                boolean updated = updateStockKlineIncr(stockId, snapshot.getStockName(), List.of(bar), snapshot.getPrevClose());
                if (updated) {
                    success.incrementAndGet();
                } else {
                    skip.incrementAndGet();
                }
            } catch (Exception e) {
                fail.incrementAndGet();
                log.warn("fillStockKlineBySnapshot fail     >>>     stockCode : {} , err : {}", snapshot.getStockCode(), e.getMessage());
            }
        });

        log.info("fillStockKlineBySnapshot     >>>     tradeDate : {} , total : {} , success : {} , skip : {} , fail : {}",
                 tradeDate, snapshotList.size(), success.get(), skip.get(), fail.get());
    }


    /**
     * TQ 批量日线 -> 最近 N 个交易日（含日期、成交量/成交额单位已标准化）。
     */
    private void fillStockKlineByDailyBars(Map<String, Long> codeIdMap, int days) {

        List<LocalDate> tradeDates = latestTradingDates(days);
        LocalDate startDate = tradeDates.getFirst();
        LocalDate endDate = tradeDates.getLast();

        List<String> stockCodes = new ArrayList<>(codeIdMap.keySet());
        int batchSize = 500;

        AtomicInteger success = new AtomicInteger();
        AtomicInteger skip = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        for (int from = 0; from < stockCodes.size(); from += batchSize) {
            int to = Math.min(from + batchSize, stockCodes.size());
            List<String> batchCodes = stockCodes.subList(from, to);

            Map<String, List<KlineDTO>> barsMap = tdxIncrClient.dailyBars(batchCodes, startDate, endDate);
            barsMap.entrySet().parallelStream().forEach(entry -> {
                String stockCode = entry.getKey();
                List<KlineDTO> bars = entry.getValue();
                Long stockId = codeIdMap.get(stockCode);

                if (stockId == null || CollectionUtils.isEmpty(bars)) {
                    skip.incrementAndGet();
                    return;
                }
                try {
                    boolean updated = updateStockKlineIncr(stockId, null, bars, null);
                    if (updated) {
                        success.incrementAndGet();
                    } else {
                        skip.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail.incrementAndGet();
                    log.warn("fillStockKlineByDailyBars fail     >>>     stockCode : {} , err : {}", stockCode, e.getMessage());
                }
            });
        }

        log.info("fillStockKlineByDailyBars     >>>     range : {} ~ {} , days : {} , total : {} , success : {} , skip : {} , fail : {}",
                 startDate, endDate, days, stockCodes.size(), success.get(), skip.get(), fail.get());
    }


    /**
     * 按日期合并增量 K 线；保留旧数据中未被新数据覆盖的日期，避免停牌/缺失导致历史丢失。
     */
    private boolean updateStockKlineIncr(Long stockId, String stockName,
                                         List<KlineDTO> newBars, Double snapshotPrevClose) {

        if (stockId == null || CollectionUtils.isEmpty(newBars)) {
            return false;
        }

        BaseStockDO old = baseStockService.getById(stockId);
        if (old == null) {
            return false;
        }

        List<KlineDTO> oldBars = old.getKlineDTOList() == null ? List.of() : old.getKlineDTOList();
        TreeMap<LocalDate, KlineDTO> merged = new TreeMap<>();
        for (KlineDTO oldBar : oldBars) {
            if (oldBar != null && oldBar.getDate() != null) {
                merged.put(oldBar.getDate(), oldBar);
            }
        }

        int applied = 0;
        for (KlineDTO bar : newBars) {
            if (!validKline(bar)) {
                continue;
            }

            KlineDTO oldBar = merged.get(bar.getDate());

            // 快照/日线不提供股本、换手率：保留旧值，避免被 0 覆盖
            if (oldBar != null) {
                if (bar.getLtgb() <= 0) {
                    bar.setLtgb(oldBar.getLtgb());
                }
                if (bar.getZgb() <= 0) {
                    bar.setZgb(oldBar.getZgb());
                }
                if (!Double.isFinite(bar.getTurnoverPct()) || bar.getTurnoverPct() == 0) {
                    bar.setTurnoverPct(oldBar.getTurnoverPct());
                }
            }
            if (!Double.isFinite(bar.getTurnoverPct())) {
                bar.setTurnoverPct(0);
            }
            if (bar.getLtgb() < 0) {
                bar.setLtgb(0);
            }
            if (bar.getZgb() < 0) {
                bar.setZgb(0);
            }

            if (!Double.isFinite(bar.getRangePct()) || (bar.getRangePct() == 0 && bar.getHigh() > bar.getLow())) {
                bar.setRangePct(calcRangePct(bar));
            }

            Double prevClose = null;
            Map.Entry<LocalDate, KlineDTO> lower = merged.lowerEntry(bar.getDate());
            if (lower != null) {
                prevClose = lower.getValue().getClose();
            }
            if (snapshotPrevClose != null && snapshotPrevClose > 0) {
                prevClose = snapshotPrevClose;
            }

            if (prevClose != null && prevClose > 0) {
                boolean needChange = !Double.isFinite(bar.getChangePrice())
                        || !Double.isFinite(bar.getChangePct())
                        || (bar.getChangePrice() == 0 && Math.abs(bar.getClose() - prevClose) > 0.005);
                if (needChange) {
                    bar.setChangePrice(NumUtil.of(bar.getClose() - prevClose));
                    bar.setChangePct(NumUtil.of((bar.getClose() - prevClose) / prevClose * 100));
                }
            } else {
                if (!Double.isFinite(bar.getChangePrice())) {
                    bar.setChangePrice(0);
                }
                if (!Double.isFinite(bar.getChangePct())) {
                    bar.setChangePct(0);
                }
            }

            merged.put(bar.getDate(), bar);
            applied++;
        }

        if (applied == 0) {
            return false;
        }

        List<KlineDTO> all = new ArrayList<>(merged.values());
        all.sort(Comparator.comparing(KlineDTO::getDate));
        KlineDTO last = all.getLast();

        List<String> klines = ConvertStockKline.dtoList2StrList(all);
        BaseStockDO entity = convertStockDO(stockId,
                                            StringUtils.isNotBlank(stockName) ? stockName : old.getName(),
                                            klines, last);

        // 前收：优先实时快照的 LastClose，其次倒数第二根 K 线
        if (snapshotPrevClose != null && snapshotPrevClose > 0) {
            entity.setPrevClose(BigDecimal.valueOf(snapshotPrevClose));
        } else if (all.size() >= 2) {
            entity.setPrevClose(of(all.get(all.size() - 2).getClose()));
        } else if (old.getPrevClose() != null) {
            entity.setPrevClose(old.getPrevClose());
        }

        entity.setTradeDate(last.getDate());
        baseStockService.updateById(entity);
        return true;
    }


    private boolean validKline(KlineDTO bar) {
        if (bar == null || bar.getDate() == null) {
            return false;
        }
        if (!(bar.getOpen() > 0) || !(bar.getHigh() > 0) || !(bar.getLow() > 0) || !(bar.getClose() > 0)) {
            return false;
        }
        if (bar.getHigh() < Math.max(bar.getOpen(), bar.getClose())
                || bar.getLow() > Math.min(bar.getOpen(), bar.getClose())) {
            return false;
        }
        return bar.getVol() >= 0 && Double.isFinite(bar.getAmo()) && bar.getAmo() >= 0;
    }

    private static double calcRangePct(KlineDTO bar) {
        return bar.getLow() > 0 ? NumUtil.of((bar.getHigh() - bar.getLow()) / bar.getLow() * 100) : 0;
    }

    private LocalDate latestTradingDate() {
        try {
            return tdxIncrClient.tradingDates(LocalDate.now(), 1).getLast();
        } catch (Exception e) {
            LocalDate fallback = KlineAPI.lastTradeDate();
            if (fallback == null) {
                throw new IllegalStateException("无法获取最新交易日", e);
            }
            log.warn("TQ 交易日历不可用，回退本地最新交易日 : {} , err : {}", fallback, e.getMessage());
            return fallback;
        }
    }

    private List<LocalDate> latestTradingDates(int days) {
        try {
            return tdxIncrClient.tradingDates(LocalDate.now(), days);
        } catch (Exception e) {
            LocalDate fallback = KlineAPI.lastTradeDate();
            if (fallback == null) {
                throw new IllegalStateException("无法获取最近 " + days + " 个交易日", e);
            }
            log.warn("TQ 交易日历不可用，回退本地最近交易日 : {} , days : {} , err : {}", fallback, days, e.getMessage());
            List<LocalDate> dates = new ArrayList<>();
            LocalDate date = fallback;
            while (dates.size() < days) {
                if (date.getDayOfWeek() != java.time.DayOfWeek.SATURDAY
                        && date.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                    dates.add(date);
                }
                date = date.minusDays(1);
            }
            dates.sort(Comparator.naturalOrder());
            return dates;
        }
    }

    private BaseStockDO convertStockDO(Long stockId, String stockName, List<String> klines, KlineDTO e) {

        BaseStockDO entity = new BaseStockDO();
        entity.setId(stockId);
        entity.setName(stockName);

        // K线数据
        entity.setKlineHisStr(JSON.toJSONString(klines));


        // 昨日
        if (klines.size() >= 2) {
            KlineDTO prevKlineDTO = ConvertStockKline.kline2DTO(klines.get(klines.size() - 2));
            entity.setPrevClose(of(prevKlineDTO.getClose()));
        }


        // 实时行情   -   last kline
        entity.setTradeDate(e.getDate());

        entity.setOpen(of(e.getOpen()));
        entity.setHigh(of(e.getHigh()));
        entity.setLow(of(e.getLow()));
        entity.setClose(of(e.getClose()));

        entity.setVolume(e.getVol());
        entity.setAmount(of(e.getAmo()));

        entity.setRangePct(of(e.getRangePct()));
        entity.setChangePct(of(e.getChangePct()));
        entity.setChangePrice(of(e.getChangePrice()));
        entity.setTurnoverPct(of(e.getTurnoverPct()));


        return entity;
    }


    /**
     * 个股行情 拉取 -> DB
     *
     * @param stockCode
     * @param stockId
     * @param apiType
     * @param updateTypeEnum
     */
    private void fillStockKline(String stockCode, Long stockId, Integer apiType, UpdateTypeEnum updateTypeEnum) {


        apiType = apiType(apiType, updateTypeEnum);


        // --------------------- ID


        stockId = stockId == null ? baseStockService.getIdByCode(stockCode) : stockId;
        Assert.notNull(stockId, "个股信息不存在：" + stockCode);


        // ---------------------  拉取数据     ->     通达信-本地读取   /   东方财富 API


        List<String> klines = null;


        // 1、通达信   本地读取
        if (apiType == null || apiType == 1) {

            klines = klinesFromTdx(stockCode);
        }

//        // 2、东方财富 API
//        else if (apiType == 2) {
//
//            if (updateTypeEnum == UpdateTypeEnum.ALL) {
//
//                // 全量更新     =>     全量 K线数据
//                StockKlineHisResp resp = EastMoneyKlineAPI.stockKlineHis(stockCode, KlineTypeEnum.DAY);
//                klines = resp.getKlines();
//
//            } else {
//
//                // 增量更新     =>     最后一日  实时行情数据
//                klines = EastMoneyKlineAPI.stockKlineLastN(stockCode);
//            }
//        }


        // 3、同花顺 API
        // 4、雪球 API
        // 5、新浪 API


        // -------------------------------------------------------------------------------------------------------------


        // klineHis   ->   全量更新 / 增量更新
        klines = klineHis__updateType(stockId, klines, updateTypeEnum);


        // -------------------------------------------------------------------------------------------------------------


        // --------------------- entity -> 实时行情

        if (CollectionUtils.isEmpty(klines)) {
            return;
        }


        // 实时行情   -   last kline
        KlineDTO lastKlineDTO = ConvertStockKline.kline2DTO(ListUtil.last(klines));


        // 实时行情
        BaseStockDO entity = convertStockDO(stockId, null, klines, lastKlineDTO);


        // --------------------- DB
        baseStockService.updateById(entity);
    }


    private int apiType(Integer apiType, UpdateTypeEnum updateTypeEnum) {

        if (apiType != null && apiType == 1) {
            return apiType;
        }


        // 1-全量更新   ->   1-本地TDX
        if (updateTypeEnum == UpdateTypeEnum.ALL) {
            apiType = 1;
        }

        // 2-增量更新   ->   2-券商API
        else if (updateTypeEnum == UpdateTypeEnum.INCR) {
            apiType = 2;
        }


        return apiType;
    }

    /**
     * klineHis   ->   1-全量更新 / 2-增量更新
     *
     * @param stockId
     * @param new_klines
     * @param updateTypeEnum 1-全量更新；2-增量更新；
     * @return
     */
    private List<String> klineHis__updateType(Long stockId, List<String> new_klines, UpdateTypeEnum updateTypeEnum) {

        // 1-全量更新
        if (updateTypeEnum == UpdateTypeEnum.ALL) {
            return new_klines;
        }


        // -------------------------------------------------------------------------------------------------------------


        // 2-增量更新     =>     1、获取 old__klineHis

        BaseStockDO stockDO = baseStockService.getById(stockId);
        List<KlineDTO> old_klineDTOList = stockDO.getKlineDTOList();


        // ----------------------------------


        // new__klineHis   ->   start_date
        LocalDate new__start_date = DateTimeUtil.parseDate_yyyy_MM_dd(new_klines.getFirst().split("\\,")[0]);


        // ----------------------------- remove + add     =>     2、从 old__klineHis 中   剔除全部 new__klineHis     =>     3、再 addAll  ->  new__klineHis


        // ------------------ 2、从 old__klineHis 中   剔除全部 new__klineHis


        // 从大到小排序索引，避免索引变化影响
        List<Integer> removeIdxList = Lists.newArrayList();


        for (int i = old_klineDTOList.size() - 1; i >= 0; i--) {
            KlineDTO old_klineDTO = old_klineDTOList.get(i);


            // 实际  new__start_date   永远 >=   old__last_klineDTO.date
            if (new__start_date.isAfter(old_klineDTO.getDate())) {
                break;
            } else {
                // 从后往前添加，自然就是从大到小
                removeIdxList.add(i);
            }
        }


        // 从大到小 删除，避免索引偏移          ->          默认已  从大到小 排序
        removeIdxList.forEach(idx -> old_klineDTOList.remove((int) idx));


        // ------------------ 3、再 addAll  ->  new__klineHis


        List<String> old_klines = ConvertStockKline.dtoList2StrList(old_klineDTOList);
        old_klines.addAll(new_klines);


        return old_klines;
    }


    private List<String> klinesFromTdx(String stockCode) {

        // 个股 - K线数据
        List<LdayParser.LdayDTO> ldayDTOList = LdayParser.parseByStockCode(stockCode);


        List<String> klines = Lists.newArrayList();
        ldayDTOList.forEach(e -> {


            // 2025-05-13,21.06,21.97,20.89,21.45,8455131,18181107751.03,5.18,2.98,0.62,6.33
            // 日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率,流通股本,总股本

            // K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率,流通股本,总股本]）

            List<Object> kline = Lists.newArrayList(String.valueOf(e.getTradeDate()), e.getOpen(), e.getHigh(), e.getLow(), e.getClose(), e.getVol(), e.getAmount(),
                                                    e.getRangePct(), e.getChangePct(), e.getChangePrice(), e.getTurnoverPct(), e.getLtgb(), e.getZgb());


            String klineStr = kline.stream().map(obj -> obj != null ? obj.toString() : "").collect(Collectors.joining(","));
            klines.add(klineStr);
        });


        return klines;
    }


    private BigDecimal of(Double val) {
        return NumUtil.double2Decimal(val);
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public Map<String, Set<String>> marketRelaStockCodePrefixList(int type, int N) {
        Map<String, Set<String>> market_stockCodeList_map = baseStockService.market_stockCodePrefixList_map(type, N);
        return market_stockCodeList_map;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @Override
    public void calcAndFillBlockKlineAll() {
        blockKlineFun.calcAndFillBlockKlineAll();
    }


}