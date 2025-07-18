package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.EastMoneyKlineAPI;
import com.bebopze.tdx.quant.common.constant.BlockNewTypeEnum;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.parser.tdxdata.*;
import com.bebopze.tdx.quant.service.TdxDataParserService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
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
@Service
public class TdxDataParserServiceImpl implements TdxDataParserService {


    @Autowired
    private IBaseStockService iBaseStockService;

    @Autowired
    private IBaseBlockService iBaseBlockService;

    @Autowired
    private IBaseBlockRelaStockService iBaseBlockRelaStockService;

    @Autowired
    private IBaseBlockNewService iBaseBlockNewService;

    @Autowired
    private IBaseBlockNewRelaStockService iBaseBlockNewRelaStockService;


    /**
     * 通达信 - （股票/板块/自定义板块）数据初始化   一键导入
     */
    @Override
    public void importAll() {


        // ------------------------------------------------------------------------ 配置导入（缓存数据）


        // 初始数据：     板块（行业/概念） - 个股   关联关系
        importTdxBlockCfg();


        // ------------------------------------------------------------------------ 报表导入（板块导出）


        //（行业/概念）板块 - 个股     关联关系
        importBlockReport();
        //      自定义板块 - 个股     关联关系
        importBlockNewReport();


        // ------------------------------------------------------------------------ 行情（通达信-行情数据 / 东方财富/同花顺/雪球-API）


        // 行情
        refreshKlineAll();
    }


    /**
     * 板块/个股 - 行情     一键刷新
     */
    @Override
    public void refreshKlineAll() {

        // 行情-板块
        Future<?> task1 = Executors.newCachedThreadPool().submit(() -> {
            fillBlockKlineAll();
        });


        // 行情-个股
        Future<?> task2 = Executors.newCachedThreadPool().submit(() -> {
            fillStockKlineAll();
        });
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                           tdx 配置文件 - 导入
    // -----------------------------------------------------------------------------------------------------------------


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


        log.info("all 板块code     >>>     板块size : {}", tdxzs3DTOList.size());
        log.info("all 个股code     >>>     个股size : {}", allStockCodeSet.size());


        // -------------------------------------------------------------------------------------------------------------


        // 个股code          从小到大  排列
        List<String> sortAllStockCodeList = Lists.newArrayList(allStockCodeSet);
        Collections.sort(sortAllStockCodeList);


        // -------------------------------------------------------------------------------------------------------------


        Map<String, Long> stock__codeIdMap = Maps.newConcurrentMap();
        Map<String, Long> block__codeIdMap = Maps.newHashMap();
        Map<String, String> block__codeNameMap = Maps.newHashMap();
        Map<String, String> hyBlock__code_pCode_map = Maps.newHashMap();


        // -------------------------------------------------------------------------------------------------------------
        //                                              save2DB
        // -------------------------------------------------------------------------------------------------------------


        // 个股
        save2DB___stock(sortAllStockCodeList, stockCode_marketType_map, stockCode_stockName_map, stock__codeIdMap);


        // 板块
        save2DB___block(tdxzs3DTOList, block__codeIdMap, block__codeNameMap, hyBlock__code_pCode_map);
        // 行业板块 - 父ID
        save2DB___hyBlock_pId(hyBlock__code_pCode_map, block__codeIdMap, block__codeNameMap);


        // 板块 - 个股
        save2DB___block_rela_stock(sortAllStockCodeList, stock__codeIdMap, stockCode_blockCodeSet_map, block__codeIdMap);
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
                                                   Map<String, String> stockCode_stockName_map,
                                                   Set<String> allStockCodeSet,

                                                   List<BlockReportParser.ExportBlockDTO> blockGnDTOList) {


        // 概念板块
        blockGnDTOList.forEach(e -> {


            String blockCode = e.getBlockCode();
            String blockName = e.getBlockName();

            String stockCode = e.getStockCode();
            String stockName = e.getStockName();

            // List<String> stockCodeList = e.getStockCodeList();


            allStockCodeSet.add(stockCode);
            stockCode_stockName_map.put(stockCode, stockName);


            // stockCodeList.forEach(stockCode -> {


            Set<String> exist_blockCodeList = stockCode_blockCodeSet_map.get(stockCode);
            if (CollectionUtils.isEmpty(exist_blockCodeList)) {

                stockCode_blockCodeSet_map.put(stockCode, Sets.newTreeSet(Lists.newArrayList(blockCode)));

            } else {

                exist_blockCodeList.add(blockCode);
            }
            // });
        });

    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                              save2DB
    // -----------------------------------------------------------------------------------------------------------------


    private void save2DB___stock(List<String> sortAllStockCodeList,
                                 Map<String, Integer> stockCode_marketType_map,
                                 Map<String, String> stockCode_stockName_map,

                                 Map<String, Long> stock__codeIdMap) {


        Map<String, Long> stock__codeIdMap_DB = iBaseStockService.codeIdMap();


        if (MapUtils.isEmpty(stock__codeIdMap_DB)) {

            // 初次 init   ->   有序 insert
            sortAllStockCodeList.forEach(stockCode -> {
                exec___save2DB___stock(stockCode, stock__codeIdMap_DB, stockCode_marketType_map, stockCode_stockName_map, stock__codeIdMap);
            });

        } else {

            // 并行 update（无序）
            sortAllStockCodeList.parallelStream().forEach(stockCode -> {
                exec___save2DB___stock(stockCode, stock__codeIdMap_DB, stockCode_marketType_map, stockCode_stockName_map, stock__codeIdMap);
            });
        }
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
        BaseStockDO baseStockDO = new BaseStockDO();
        baseStockDO.setCode(stockCode);
        baseStockDO.setName(stockCode_stockName_map.get(stockCode));
        baseStockDO.setTdxMarketType(marketType);


        // Long stockId = iBaseStockService.getIdByCode(stockCode);
        Long stockId = stock__codeIdMap_DB.get(stockCode);
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
    }


    private void save2DB___block(List<Tdxzs3Parser.Tdxzs3DTO> tdxzs3DTOList,

                                 Map<String, Long> block__codeIdMap,
                                 Map<String, String> block__codeNameMap,
                                 Map<String, String> hyBlock__code_pCode_map) {


        // 从小到大   排列
        Collections.sort(tdxzs3DTOList, Comparator.comparing(Tdxzs3Parser.Tdxzs3DTO::getCode));


        // 概念 - 行业
        Map<String, String> gnCode__hyCode__map = GnRelaHyParser.gnCode__hyCode__map();


        // ----------------------------------- 遍历 - save

        tdxzs3DTOList.forEach(e -> {

            String blockCode = e.getCode();
            String blockName = e.getName();

            String pCode = e.getPCode();


            // ----------------------------------- 概念 - 行业
            if (e.getBlockType() == 4) {
                pCode = gnCode__hyCode__map.get(blockCode);
            }


            // -----------------------------------


            BaseBlockDO baseBlockDO = new BaseBlockDO();
            baseBlockDO.setCode(blockCode);
            baseBlockDO.setName(blockName);
            baseBlockDO.setType(e.getBlockType());
            baseBlockDO.setLevel(e.getLevel());
            baseBlockDO.setEndLevel(e.getEndLevel());
            // 父级
            baseBlockDO.setParentId(null);


            Long blockId = iBaseBlockService.getIdByCode(blockCode);
            if (blockId != null) {
                baseBlockDO.setId(blockId);
                iBaseBlockService.updateById(baseBlockDO);
            } else {
                iBaseBlockService.save(baseBlockDO);
                blockId = baseBlockDO.getId();
            }


            block__codeIdMap.put(blockCode, blockId);
            block__codeNameMap.put(blockCode, blockName);


            // 父级（行业板块）
            if (StringUtils.isNotEmpty(pCode)) {
                hyBlock__code_pCode_map.put(blockCode, pCode);
            }


            if (blockId == null) {
                log.error("base_block - insertOrUpdate   err     >>>     blockCode : {} , blockId : {} , baseBlockDO : {}",
                          blockCode, blockId, JSON.toJSONString(baseBlockDO));
            }
        });
    }


    private void save2DB___hyBlock_pId(Map<String, String> hyBlock__code_pCode_map,
                                       Map<String, Long> block__codeIdMap,
                                       Map<String, String> block__codeNameMap) {

        // p_id（行业板块）
        hyBlock__code_pCode_map.forEach((blockCode, pCode) -> {


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


            iBaseBlockService.updateById(baseBlockDO);
        });
    }


    private void save2DB___block_rela_stock(List<String> sortAllStockCodeList,
                                            Map<String, Long> stock__codeIdMap,
                                            Map<String, Set<String>> stockCode_blockCodeSet_map,
                                            Map<String, Long> block__codeIdMap) {


        sortAllStockCodeList.forEach(stockCode -> {

            Long stockId = stock__codeIdMap.get(stockCode);

            Set<String> blockCodeSet = stockCode_blockCodeSet_map.get(stockCode);
            List<BaseBlockRelaStockDO> doList = Lists.newArrayList();


            if (stockId == null || CollectionUtils.isEmpty(blockCodeSet)) {
                log.error("个股-板块   err     >>>     stockId : {} , blockCodeSet : {}", stockId, JSON.toJSONString(blockCodeSet));
                return;
            }


            blockCodeSet.forEach(blockCode -> {

                // 板块 - 个股
                BaseBlockRelaStockDO baseBlockRelaStockDO = new BaseBlockRelaStockDO();
                baseBlockRelaStockDO.setStockId(stockId);
                baseBlockRelaStockDO.setBlockId(block__codeIdMap.get(blockCode));


                doList.add(baseBlockRelaStockDO);
            });


            // del All
            iBaseBlockRelaStockService.deleteByStockId(stockId);
            // batch insert
            iBaseBlockRelaStockService.saveBatch(doList, 500);
        });

    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                              报表-导入
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public void importBlockReport() {


        // -------------------------------------------------------------------------------------------------------------
        //                                              数据读取（txt）
        // -------------------------------------------------------------------------------------------------------------


        // 数据读取   ->   txt报表（系统板块 报表）
        List<BlockReportParser.ExportBlockDTO> dtoList = BlockReportParser.parseAllTdxBlock();


        // -------------------------------------------------------------------------------------------------------------
        //                                              数据读取（DB -> 已导入过的 数据 [板块/个股] ）
        // -------------------------------------------------------------------------------------------------------------


        // DB读取     ->     板块code - 板块ID
        Map<String, Long> block__codeIdMap = iBaseBlockService.codeIdMap();


        // DB读取     ->     code_id_map   code_name_map
        List<BaseStockDO> allBaseStockDOList = iBaseStockService.listAllSimple();

        Map<String, Long> stock__codeIdMap = allBaseStockDOList.stream().collect(Collectors.toMap(BaseStockDO::getCode, BaseStockDO::getId));
        Map<String, String> stock__codeNameMap = allBaseStockDOList.stream().collect(Collectors.toMap(BaseStockDO::getCode, stock -> stock.getName() != null ? stock.getName() : ""));


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


        importBlockReport_____save2DB___block_rela_stock(blockCode_stockCodeSet_map,

                                                         block__codeIdMap,
                                                         stock__codeIdMap);


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

            String stockCode = e.getStockCode();
            String stockName = e.getStockName();


            // ------------------------------------------------------


            // block -> save2DB
            Long blockId = block__codeIdMap.get(blockCode);
            if (blockId == null) {

                BaseBlockDO blockEntity = new BaseBlockDO();
                blockEntity.setCode(blockCode);
                blockEntity.setName(blockName);

                iBaseBlockService.save(blockEntity);


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
                stockEntity.setTdxMarketType(StockMarketEnum.getTdxMarketType(stockCode));


                // 等待 排序后，再insert
                if (null != stockEntity.getTdxMarketType()) {
                    baseStockDOList.add(stockEntity);
                } else {
                    // 非A股     ==>     B股 + ST   ->   忽略          （900957 - *ST凌云B）
                    isAStock = false;
                    log.error("importBlockReport_____save2DB___block   -   个股 -> 未知类型     >>>     stockCode : {} , e : {}", stockCode, JSON.toJSONString(e));
                }


            } else {

                // 股票name - 补齐
                if (StringUtils.isBlank(stock__codeNameMap.get(stockCode))) {
                    BaseStockDO baseStockDO = new BaseStockDO();
                    baseStockDO.setId(stockId);
                    baseStockDO.setName(stockName);

                    iBaseStockService.updateById(baseStockDO);


                    // 实在 找不到 stockName     =>     上市失败的   ->   688688 - [蚂蚁集团]
                }
            }


            // ------------------------------------------------------


            // blockCode - stockCodeSet     关联关系

            if (isAStock) {

                Set<String> stockCodeSet = blockCode_stockCodeSet_map.get(blockCode);
                if (CollectionUtils.isEmpty(stockCodeSet)) {
                    blockCode_stockCodeSet_map.put(blockCode, Sets.newTreeSet(Sets.newHashSet(stockCode)));
                } else {
                    stockCodeSet.add(stockCode);
                }
            }


        });


        // -------------------------------------------------------------------------------------------------------------


        // stock -> save2DB


        // sort
        baseStockDOList.sort(Comparator.comparing(BaseStockDO::getCode));
        // batch insert
        iBaseStockService.saveBatch(baseStockDOList, 500);

        baseStockDOList.forEach(e -> {
            stock__codeIdMap.put(e.getCode(), e.getId());
        });


    }


    private void importBlockReport_____save2DB___block_rela_stock(Map<String, Set<String>> blockCode_stockCodeSet_map,

                                                                  Map<String, Long> block__codeIdMap,
                                                                  Map<String, Long> stock__codeIdMap) {


        // 有序
        blockCode_stockCodeSet_map.forEach((blockCode, stockCodeSet) -> {


            Long blockId = block__codeIdMap.get(blockCode);


            // blockId - stockId   关联列表
            List<BaseBlockRelaStockDO> relaEntityList = Lists.newArrayList();


            // 有序
            stockCodeSet.forEach(stockCode -> {

                BaseBlockRelaStockDO relaEntity = new BaseBlockRelaStockDO();
                relaEntity.setBlockId(blockId);
                relaEntity.setStockId(stock__codeIdMap.get(stockCode));

                relaEntityList.add(relaEntity);
            });


            // del All
            iBaseBlockRelaStockService.deleteByBlockId(blockId);
            // batch insert
            iBaseBlockRelaStockService.saveBatch(relaEntityList, 500);
        });
    }


    @Override
    public void importBlockNewReport() {


        // 数据读取   ->   txt报表（自定义板块 报表）
        List<BlockReportParser.ExportBlockDTO> zdy_dtoList = BlockReportParser.parse_zdy();


        // -------------------------------------------------------------------------------------------------------------
        //                                              关联关系
        // -------------------------------------------------------------------------------------------------------------


        // 自定义板块   code-ID   （DB库）
        Map<String, Long> blockNew__codeIdMap = iBaseBlockNewService.codeIdMap();


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
        Map<String, Long> sotock__codeIdMap = iBaseStockService.codeIdMap(allStockCodeSet);
        // 板块   code-Id
        Map<String, Long> block__codeIdMap = iBaseBlockService.codeIdMap(allStockCodeSet);


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

                iBaseBlockNewService.save(baseBlockNewDO);


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

                    BaseBlockNewRelaStockDO baseBlockNewRelaStockDO = new BaseBlockNewRelaStockDO();
                    baseBlockNewRelaStockDO.setBlockNewId(blockNewId);
                    baseBlockNewRelaStockDO.setStockId(relaId);
                    baseBlockNewRelaStockDO.setType(type);

                    relaDOList.add(baseBlockNewRelaStockDO);
                }
            });


            iBaseBlockNewRelaStockService.delByBlockNewId(blockNewId);
            iBaseBlockNewRelaStockService.saveBatch(relaDOList);
        });
    }


    @Override
    public void fillBlockKline(String blockCode) {
        fillBlockKline(blockCode, null);
    }

    public void fillBlockKline(String blockCode, Long blockId) {

        BaseBlockDO baseBlockDO = new BaseBlockDO();
        baseBlockDO.setCode(blockCode);


        // 板块 - 历史行情
        List<LdayParser.LdayDTO> ldayDTOList = LdayParser.parseByStockCode(blockCode);


        List<String> klines = Lists.newArrayList();

        ldayDTOList.forEach(x -> {


            // 历史行情-JSON（日期：[O,H,L,C,VOL,AMO,涨幅,振幅,换手率]）
            // List<Number> kline = Lists.newArrayList(x.getOpen(), x.getHigh(), x.getLow(), x.getClose(), x.getVol(), x.getAmount(), x.getChangePct(), null, null);


            // 2025-05-13,21.06,21.97,20.89,21.45,8455131,18181107751.03,5.18,2.98,0.62,6.33
            // 日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率

            // 历史行情-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）
            List<Object> kline = Lists.newArrayList(String.valueOf(x.getTradeDate()), x.getOpen(), x.getHigh(), x.getLow(), x.getClose(), x.getVol(), x.getAmount(),
                                                    x.getRangePct(), x.getChangePct(), x.getChangePrice(), null);


            String klineStr = kline.stream().map(obj -> obj != null ? obj.toString() : "").collect(Collectors.joining(","));
            klines.add(klineStr);
        });

        baseBlockDO.setKlineHis(JSON.toJSONString(klines));


        if (CollectionUtils.isEmpty(ldayDTOList)) {
            return;
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
        baseBlockDO.setChangePct(last.getChangePct());


        if (blockId != null) {
            baseBlockDO.setId(blockId);
            iBaseBlockService.updateById(baseBlockDO);
        } else {
            blockId = iBaseBlockService.getIdByCode(blockCode);
            baseBlockDO.setId(blockId);

            iBaseBlockService.saveOrUpdate(baseBlockDO);
        }


        log.info("fillBlockKline suc     >>>     blockCode : {}", blockCode);
    }


    @Override
    public void fillBlockKlineAll() {
        long[] start = {System.currentTimeMillis()};
        AtomicInteger count = new AtomicInteger(0);


        Map<String, Long> codeIdMap = iBaseBlockService.codeIdMap();


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
    }


    @Override
    public void fillStockKline(String stockCode, Integer apiType) {
        fillStockKline(stockCode, null, apiType);
        log.info("fillStockKline suc     >>>     stockCode : {}", stockCode);
    }

    @Override
    public void fillStockKlineAll() {
        long[] start = {System.currentTimeMillis()};
        AtomicInteger count = new AtomicInteger(0);


        Map<String, Long> codeIdMap = iBaseStockService.codeIdMap();


        codeIdMap.keySet().parallelStream().forEach(stockCode -> {
            Long stockId = codeIdMap.get(stockCode);


            fillStockKline(stockCode, stockId, 1);


            // ------------------------------------------- 计时（频率）   29ms/次   x 5500     ->     总耗时：161s


            int countVal = count.incrementAndGet();
            long time = System.currentTimeMillis() - start[0];


            long r1 = time / countVal;
            long r2 = countVal * 1000 / time;
            String r3 = String.format("%s次 - %s", countVal, DateTimeUtil.format2Hms(time));


            // stockCode : 300154, stockId : 1630 , count : 5424 , r1 : 29ms/次 , r2 : 33次/s , r3 : 5424次 - 161s
            log.info("fillStockKlineAll suc     >>>     stockCode : {}, stockId : {} , count : {} , r1 : {}ms/次 , r2 : {}次/s , r3 : {}",
                     stockCode, stockId, countVal, r1, r2, r3);
        });

    }


    private void fillStockKline(String stockCode, Long stockId, Integer apiType) {


        // --------------------- ID

        stockId = stockId == null ? iBaseStockService.getIdByCode(stockCode) : stockId;
        Assert.notNull(stockId, "个股信息不存在：" + stockCode);


        // ---------------------  拉取数据     ->     通达信-本地读取   /   东方财富 API


        List<String> klines = null;


        // 1、通达信   本地读取
        if (apiType == null || apiType == 1) {

            klines = klinesFromTdx(stockCode);

        } else if (apiType == 2) {

            // 2、东方财富 API
            StockKlineHisResp stockKlineHisResp = EastMoneyKlineAPI.stockKlineHis(stockCode, KlineTypeEnum.DAY);

            // String name = stockKlineHisResp.getName();
            klines = stockKlineHisResp.getKlines();
        }


        // 3、同花顺 API


        // 4、雪球 API


        // --------------------- entity

        BaseStockDO entity = new BaseStockDO();
        entity.setId(stockId);
        // TODO   entity.setName(name);
        // 历史行情
        entity.setKlineHis(JSON.toJSONString(klines));


        if (CollectionUtils.isEmpty(klines)) {
            return;
        }


        // 实时行情   -   last kline
        KlineDTO lastKlineDTO = ConvertStockKline.kline2DTO(klines.get(klines.size() - 1));

        entity.setTradeDate(lastKlineDTO.getDate());

        entity.setOpen(of(lastKlineDTO.getOpen()));
        entity.setClose(of(lastKlineDTO.getClose()));
        entity.setHigh(of(lastKlineDTO.getHigh()));
        entity.setLow(of(lastKlineDTO.getLow()));

        entity.setVolume(lastKlineDTO.getVol());
        entity.setAmount(of(lastKlineDTO.getAmo()));

        entity.setRangePct(of(lastKlineDTO.getRange_pct()));
        entity.setChangePct(of(lastKlineDTO.getChange_pct()));
        entity.setTurnoverPct(of(lastKlineDTO.getTurnover_pct()));


        // --------------------- DB

        iBaseStockService.updateById(entity);
    }


    private List<String> klinesFromTdx(String stockCode) {

        // 个股 - 历史行情
        List<LdayParser.LdayDTO> ldayDTOList = LdayParser.parseByStockCode(stockCode);


        List<String> klines = Lists.newArrayList();
        ldayDTOList.forEach(e -> {


            // 2025-05-13,21.06,21.97,20.89,21.45,8455131,18181107751.03,5.18,2.98,0.62,6.33
            // 日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率

            // 历史行情-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）

            List<Object> kline = Lists.newArrayList(String.valueOf(e.getTradeDate()), e.getOpen(), e.getHigh(), e.getLow(), e.getClose(), e.getVol(), e.getAmount(),
                                                    e.getRangePct(), e.getChangePct(), e.getChangePrice(), null);


            String klineStr = kline.stream().map(obj -> obj != null ? obj.toString() : "").collect(Collectors.joining(","));
            klines.add(klineStr);
        });


        return klines;
    }


    private BigDecimal of(Double val) {
        return val == null || Double.isNaN(val) ? null : new BigDecimal(val);
    }


    @Override
    public Map<String, List<String>> marketRelaStockCodePrefixList() {
        Map<String, List<String>> market_stockCodeList_map = iBaseStockService.market_stockCodePrefixList_map();
        return market_stockCodeList_map;
    }


}
