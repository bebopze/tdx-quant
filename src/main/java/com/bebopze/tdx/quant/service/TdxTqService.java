package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.tq.TdxRealtimeQuoteDTO;

import java.util.List;
import java.util.Map;


/**
 * 通达信 tqcenter 只读数据接口透传服务
 *
 *
 * <p>
 * 明确排除 下单、撤单、账户、发送消息、修改板块 和 执行客户端命令 等方法，避免通用 HTTP入口 意外扩大 交易 或 桌面控制权限。
 * </p>
 *
 * @author: bebopze
 * @date: 2026/9/26
 */
public interface TdxTqService {


    /**
     * 调用 tqcenter 的接口
     *
     * @param method
     * @param params
     * @return
     */
    Object call(String method, Map<String, Object> params);


    /**
     * 读取单个实时行情
     *
     * @param code 通达信 代码（股票/ETF/板块）
     * @return
     */
    TdxRealtimeQuoteDTO get(String code);


    /**
     * 批量读取实时行情
     *
     * @param codeList 通达信 代码（股票/ETF/板块）列表
     * @return
     */
    List<TdxRealtimeQuoteDTO> getBatch(List<String> codeList);


    /**
     * 按 股票类型，读取全部实时行情
     *
     * @param stockTypeEnum 可选 股票类型，例如：A_STOCK,ETF,TDX_BLOCK（传空 -> 拉取全部类型）
     * @return
     */
    List<TdxRealtimeQuoteDTO> getAll(StockTypeEnum stockTypeEnum);


}