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


        long vol = result.getLongValue("Volume", 0L);
        // 对于 沪深京 个股/ETF   ->   Volume、NowVol 的 单位为：手
        if (StockTypeEnum.isAStock(code) || StockTypeEnum.isETF(code)) {
            // 股 = 手 x 100
            vol *= 100;
        }


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
        return getBatchByApi(codes);
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

            long vol = data.getLongValue("Volume", 0L);
            if (StockTypeEnum.isAStock(rawCode) || StockTypeEnum.isETF(rawCode)) {
                vol *= 100;
            }

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

        // TQ 批量快照：每批 500 只，避免单次响应过大；批次失败才退化为逐只请求。
        final int batchSize = 500;
        List<TdxRealtimeQuoteDTO> quotes = new ArrayList<>(codes.size());
        for (int from = 0; from < codes.size(); from += batchSize) {
            int to = Math.min(from + batchSize, codes.size());
            List<String> batch = codes.subList(from, to);
            try {
                quotes.addAll(getBatchByApi(batch));
            } catch (Exception e) {
                log.warn("TdxTqServiceImpl.getAll batch fallback     >>>     from : {} , to : {} , err : {}",
                         from, to, e.getMessage());
                quotes.addAll(batch.parallelStream().map(this::get).toList());
            }
        }
        return quotes;
    }



}