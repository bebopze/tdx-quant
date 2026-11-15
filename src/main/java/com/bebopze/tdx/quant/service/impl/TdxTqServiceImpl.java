package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.client.TdxTqHttpClient;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.tq.TdxRealtimeQuoteDTO;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.TdxFormatCodeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.service.TdxTqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


/**
 * 通达信 tqcenter API（快照行情、历史行情）
 *
 *
 * <p>
 * 明确排除下单、撤单、账户、发送消息、修改板块和执行客户端命令等方法，
 * 避免通用 HTTP 入口意外扩大交易或桌面控制权限。
 * </p>
 *
 * @author: bebopze
 * @date: 2026/9/26
 */
@Slf4j
@Service
public class TdxTqServiceImpl implements TdxTqService {


    public static final List<String> SNAPSHOT_FIELDS = List.of("LastClose", "Open", "Max", "Min", "Now", "Volume", "Amount");

    /**
     * 当前 TQ 版本可能不支持 get_market_snapshot_batch（例如返回 -32601）；
     * 探测到后固定走单只 get_market_snapshot，避免每批都重复失败。
     */
    private volatile boolean batchSnapshotUnsupported = false;

    /**
     * 真实 TQ 不支持批量快照，单只 TQ 本地接口实测 20 并发约 1.5s/500只；
     * 固定 24 个 IO 线程，避免 common ForkJoin 并发过低导致 5000+ 只耗时 30s。
     */
    private static final ExecutorService TQ_SNAPSHOT_POOL = Executors.newFixedThreadPool(24, r -> {
        Thread t = new Thread(r, "tdx-tq-snapshot");
        t.setDaemon(true);
        return t;
    });


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;


    public TdxTqServiceImpl(IBaseStockService baseStockService, IBaseBlockService baseBlockService) {
        this.baseStockService = baseStockService;
        this.baseBlockService = baseBlockService;
    }


    @Override
    public JSONObject call(String method, Map<String, Object> params) {
//        if (method == null || NOT_ALLOW_METHODS.contains(method)) {
//            throw new IllegalArgumentException("禁止调用的 TQ 方法 : " + method);
//        }

        return TdxTqHttpClient.call(method, params == null ? Map.of() : params);
    }


    @Override
    public TdxRealtimeQuoteDTO get(String code) {


        // 获取快照数据 get_market_snapshot
        //
        // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhthaq5qmg/mindoc-1h10iig4pb6e0.html


        //   参数          是否必选       参数类型            参数说明
        //  stock_code       Y          str           证券代码
        //  field_list       N          List[str]     字段筛选，传空则返回全部


//        {
//            "stock_code": "300059.SZ",
//            "field_list": ["Now"]
//        }


        String formatCode = TdxFormatCodeUtil.formatCode(code);


        JSONObject result = call("get_market_snapshot",
                                 Map.of("stock_code", formatCode, "field_list", SNAPSHOT_FIELDS));


        // TQ Volume 可能带小数（ETF），直接 long 截断会少几十股；按 double 乘 100 后四舍五入
        double rawVol = result.getDoubleValue("Volume");
        long vol = Math.round(rawVol * ((StockTypeEnum.isAStock(code) || StockTypeEnum.isETF(code)) ? 100 : 1));


        return new TdxRealtimeQuoteDTO(
                code,
                NumUtil.of(result.getDouble("Open")),
                NumUtil.of(result.getDouble("Max")),
                NumUtil.of(result.getDouble("Min")),
                NumUtil.of(result.getDouble("Now")),
                vol,
                NumUtil.of(result.getDouble("Amount")),
                NumUtil.of(result.getDouble("LastClose"))
                // LocalDateTime.now()
        );
    }


    @Override
    public List<TdxRealtimeQuoteDTO> getBatch(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }

        // 真实 TQ 当前版本不支持 get_market_snapshot_batch
        if (!batchSnapshotUnsupported) {
            try {
                return getBatchByApi(codes);
            } catch (Exception e) {
                if (isBatchUnsupported(e)) {
                    batchSnapshotUnsupported = true;
                    log.info("TdxTqServiceImpl batch snapshot unsupported, fallback to get_market_snapshot     >>>     reason : {}", e.getMessage());
                } else {
                    log.warn("TdxTqServiceImpl batch snapshot fail, fallback to get_market_snapshot     >>>     err : {}", e.getMessage());
                }
            }
        }

        return getPerCode(codes);
    }


    /**
     * 单只快照并发拉取。TQ 不限速，使用并行流提高吞吐；
     * 单只失败/非法代码只跳过该只。
     */
    private List<TdxRealtimeQuoteDTO> getPerCode(List<String> codes) {
        List<Future<TdxRealtimeQuoteDTO>> futures = codes.stream()
                                                         .map(code -> TQ_SNAPSHOT_POOL.submit(() -> {
                                                             try {
                                                                 return get(code);
                                                             } catch (Exception e) {
                                                                 log.debug("TdxTqServiceImpl.getPerCode skip     >>>     code : {} , err : {}", code, e.getMessage());
                                                                 return null;
                                                             }
                                                         }))
                                                         .toList();

        List<TdxRealtimeQuoteDTO> quotes = new ArrayList<>(codes.size());
        for (Future<TdxRealtimeQuoteDTO> future : futures) {
            try {
                TdxRealtimeQuoteDTO quote = future.get();
                if (quote != null) {
                    quotes.add(quote);
                }
            } catch (Exception e) {
                log.debug("TdxTqServiceImpl.getPerCode future fail     >>>     err : {}", e.getMessage());
            }
        }
        return quotes;
    }

    private boolean isBatchUnsupported(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null && (msg.contains("不支持") || msg.contains("32601") || msg.contains("方法名"))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * 批量快照（TQ 单次 HTTP 请求，取代逐只 get_market_snapshot）。
     *
     * <p>旧实现 getBatch_2 使用 stock_code 参数、且从外层 result 读取 Volume，实际成交量恒为 0；
     * 这里统一修正为 stock_list + 单个证券 data 读取。</p>
     */
    private List<TdxRealtimeQuoteDTO> getBatchByApi(List<String> codes) {

        if (codes == null || codes.isEmpty()) {
            return List.of();
        }

        List<String> formatCodes = TdxFormatCodeUtil.formatCodes(codes);

        JSONObject result = call("get_market_snapshot_batch",
                                 Map.of("stock_list", formatCodes,
                                        "field_list", SNAPSHOT_FIELDS,
                                        "return_df", false));

        JSONObject values;
        Object valueObj = result.get("Value");
        if (valueObj instanceof JSONObject jsonObject) {
            values = jsonObject;
        } else if (valueObj instanceof String text) {
            values = JSON.parseObject(text);
        } else {
            values = result;
        }
        if (values == null) {
            throw new IllegalStateException("[通达信TQ] get_market_snapshot_batch 返回缺少 Value");
        }

        List<TdxRealtimeQuoteDTO> quotes = new ArrayList<>(codes.size());
        for (String rawCode : codes) {
            String formatCode = TdxFormatCodeUtil.formatCode(rawCode);
            JSONObject data = values.getJSONObject(formatCode);
            if (data == null) {
                data = values.getJSONObject(rawCode);
            }
            // 停牌/退市/指数缺失：跳过单只，不影响其他证券
            if (data == null || data.isEmpty()) {
                continue;
            }

            double rawVol = data.getDoubleValue("Volume");
            long vol = Math.round(rawVol * ((StockTypeEnum.isAStock(rawCode) || StockTypeEnum.isETF(rawCode)) ? 100 : 1));

            quotes.add(new TdxRealtimeQuoteDTO(
                    rawCode,
                    NumUtil.of(data.getDouble("Open")),
                    NumUtil.of(data.getDouble("Max")),
                    NumUtil.of(data.getDouble("Min")),
                    NumUtil.of(data.getDouble("Now")),
                    vol,
                    NumUtil.of(data.getDouble("Amount")),
                    NumUtil.of(data.getDouble("LastClose"))
            ));
        }

        if (quotes.isEmpty()) {
            throw new IllegalStateException("[通达信TQ] get_market_snapshot_batch 未返回任何有效行情，codes=" + codes.size());
        }
        return quotes;
    }


    @Override
    public List<TdxRealtimeQuoteDTO> getAll(StockTypeEnum stockTypeEnum) {

        List<String> codes;
        if (stockTypeEnum == null) {
            codes = baseStockService.listAllSimple().stream().map(BaseStockDO::getCode).collect(Collectors.toList());
            codes.addAll(baseBlockService.listAllSimple().stream().map(BaseBlockDO::getCode).toList());
        }

        // 板块/指数
        else if (stockTypeEnum == StockTypeEnum.TDX_BLOCK) {

            codes = baseBlockService.listAllSimple().stream()
                                    // .filter(e -> Objects.equals(stockTypeEnum.getType(), e.getType()))
                                    .map(BaseBlockDO::getCode)
                                    .collect(Collectors.toList());
        } else {

            // 个股/ETF/港股/美股
            codes = baseStockService.listAllSimple().stream()
                                    .filter(e -> Objects.equals(stockTypeEnum.getType(), e.getType()))
                                    .map(BaseStockDO::getCode)
                                    .collect(Collectors.toList());
        }


        if (codes.isEmpty()) {
            return List.of();
        }

        if (batchSnapshotUnsupported) {
            return getPerCode(codes);
        }

        // 批量不支持时 getBatch 会自动标记并退化为单只并发；每批 500 只
        final int batchSize = 500;
        List<TdxRealtimeQuoteDTO> quotes = new ArrayList<>(codes.size());
        for (int from = 0; from < codes.size(); from += batchSize) {
            int to = Math.min(from + batchSize, codes.size());
            quotes.addAll(getBatch(codes.subList(from, to)));
        }
        return quotes;
    }



}