package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.*;
import com.bebopze.tdx.quant.common.tdxfun.BlockKlineFun;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.parser.tdxdata.*;
import com.bebopze.tdx.quant.service.*;
import com.bebopze.tdx.quant.task.TdxTask;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
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
    private IBaseOptionService baseOptionService;


    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;

    @Autowired
    private IBaseBlockNewService baseBlockNewService;

    @Autowired
    private IBaseBlockNewRelaStockService baseBlockNewRelaStockService;


    @Autowired
    private MarketService marketService;

    @Autowired
    private KlineService klineService;

    @Autowired
    private ExtDataService extDataService;

    @Autowired
    private InitDataService initDataService;


    @Autowired
    private TdxTask tdxTask;

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


        klineService.refreshKlineAll(UpdateTypeEnum.HIS);

        extDataService.refreshExtDataAll(UpdateTypeEnum.HIS);


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


        // ------------------------------------------------------------------------ 导入期权


        importOption();


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


    @TotalTime
    @Override
    public void importOption() {


        // ----------------------------------------- 期权列表   解析（.txt）
        List<QqcodeParser.QqcodeDTO> dtoList = QqcodeParser.parse();


        // ----------------------------------------- 期权列表   save2DB


        Map<String, Long> old__codeIdMap = baseOptionService.codeIdMap();


        List<BaseOptionDO> entityList = Lists.newArrayList();
        dtoList.forEach(e -> {
            String code = e.getCode();


            // 期权
            BaseOptionDO entity = new BaseOptionDO();

            entity.setCode(code);
            entity.setSymbol(e.getSymbol());
            entity.setName(e.getName());
            entity.setCallPutType(e.getCallPutType());
            entity.setStrikePrice(e.getStrikePrice());
            entity.setMultiplier(e.getMultiplier());
            entity.setUnderlyingType(e.getUnderlyingType());
            entity.setUnderlyingCode(e.getUnderlyingCode());
            entity.setUnderlyingName(e.getUnderlyingName());
            entity.setStartDate(e.getStartDate());
            entity.setEndDate(e.getEndDate());
            entity.setTdxMarketType(StockMarketEnum.getTdxMarketType(code));


            entity.setId(old__codeIdMap.get(code));
            entityList.add(entity);
        });


        // insert new
        baseOptionService.batchInsertOrUpdate(entityList);


        // --------------------------------------------------------


        // 期权   ->   不能 DEL old（过期期权   ->   通达信 不提供）


//        // DEL old（NOT IN -> new）
//        Set<String> new__codeSet = entityList.stream().map(BaseOptionDO::getCode).collect(Collectors.toSet());
//        old__codeIdMap.forEach((code, id) -> {
//            if (!new__codeSet.contains(code)) {
//                baseOptionService.removeById(id);
//                log.warn("DEL old     >>>     code : {}", code);
//            }
//        });
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


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public Map<String, Set<String>> marketRelaStockCodePrefixList(int type, int N) {
        Map<String, Set<String>> market_stockCodeList_map = baseStockService.market_stockCodePrefixList_map(type, N);
        return market_stockCodeList_map;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Deprecated
    @TotalTime
    @Override
    public void calcAndFillBlockKlineAll() {
        blockKlineFun.calcAndFillBlockKlineAll();
    }


}