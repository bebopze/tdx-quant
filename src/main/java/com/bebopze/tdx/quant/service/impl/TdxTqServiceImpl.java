package com.bebopze.tdx.quant.service.impl;

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

import java.time.LocalDateTime;
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


        return new TdxRealtimeQuoteDTO(
                code,
                NumUtil.of(result.getDouble("Open")),
                NumUtil.of(result.getDouble("Max")),
                NumUtil.of(result.getDouble("Min")),
                NumUtil.of(result.getDouble("Now")),
                NumUtil.of(result.getDouble("Volume")),
                NumUtil.of(result.getDouble("Amount")),
                NumUtil.of(result.getDouble("LastClose"))
                // LocalDateTime.now()
        );
    }


    @Override
    public List<TdxRealtimeQuoteDTO> getBatch(List<String> codes) {
        return codes.stream().map(this::get).toList();
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

            // 个股/ETF
            codes = baseStockService.listAllSimple().stream()
                                    .filter(e -> Objects.equals(stockTypeEnum.getType(), e.getType()))
                                    .map(BaseStockDO::getCode)
                                    .collect(Collectors.toList());
        }


        // 27s / 1.6s / 3.7s
        // return ParallelCalcUtil.map(codes, this::get, ThreadPoolType.IO_INTENSIVE);

        // 27s / 1.6s / 3.7s
        return codes.parallelStream().map(this::get).toList();
    }


    public List<TdxRealtimeQuoteDTO> getBatch_2(List<String> codes) {


        List<String> formatCodes = TdxFormatCodeUtil.formatCodes(codes);


        // 批量获取快照数据get_market_snapshot_batch
        //
        // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhthaq5qmg/mindoc-1hlnpjm512efk.html


        // 参数          是否必选     参数类型          参数说明
        // stock_list     Y	      list[str]     证券代码列表，须带市场后缀，例如 ["600000.SH", "688318.SH"]
        // field_list     N	      list[str]     指定返回字段，为空 [] 返回全部字段，未知字段静默忽略
        // return_df      N	      bool	        为 True 且已安装 pandas 时返回 DataFrame（行索引为证券代码），否则返回 dict


        JSONObject result = call("get_market_snapshot_batch",

                                 Map.of("stock_code", formatCodes,
                                        "field_list", SNAPSHOT_FIELDS,
                                        "return_df", false));


        // {
        //   "600000.SH": {
        //   "Amount": "46996.94",
        //   "Average": "9.08",
        //   "Before5MinNow": "9.09",
        //   "Buyp": ["9.07", "9.06", "9.05", "9.04", "9.03"],
        //   "Buyv": ["4467", "11048", "7788", "5161", "4586"],
        //   "DownHome": "0",
        //   "InOutFlag": "2",
        //   "Inside": "247892",
        //   "ItemNum": "3716",
        //   "Jjjz": "3330583.75",
        //   "LastClose": "9.06",
        //   "Max": "9.15",
        //   "Min": "9.00",
        //   "Now": "9.07",
        //   "NowVol": "53978",
        //   "Open": "9.05",
        //   "Outside": "269701",
        //   "RefreshTime": "153058",
        //   "Sellp": ["9.08", "9.09", "9.10", "9.11", "9.12"],
        //   "Sellv": ["2564", "1316", "2574", "5680", "6306"],
        //   "TickDiff": "0.00",
        //   "UpHome": "0",
        //   "Volume": "517592",
        //   "XsFlag": "2",
        //   "ZAFPre3": "1.01",
        //   "Zangsu": "-0.22"
        //   }
        // }


        LocalDateTime now = LocalDateTime.now();


        List<TdxRealtimeQuoteDTO> quotes = result.entrySet()
                                                 .stream()
                                                 .map(entry -> {

                                                     String code = entry.getKey();
                                                     JSONObject data = (JSONObject) entry.getValue();

                                                     return new TdxRealtimeQuoteDTO(
                                                             code,
                                                             NumUtil.of(data.getDouble("Open")),
                                                             NumUtil.of(data.getDouble("Max")),
                                                             NumUtil.of(data.getDouble("Min")),
                                                             NumUtil.of(data.getDouble("Now")),
                                                             NumUtil.of(data.getDouble("Volume")),
                                                             NumUtil.of(data.getDouble("Amount")),
                                                             NumUtil.of(data.getDouble("LastClose"))
                                                             // now.toLocalDate().atTime(DateTimeUtil.parseTime__HH_mm_ss(data.getString("RefreshTime")))
                                                     );
                                                 })
                                                 .toList();


        return quotes;
    }


}