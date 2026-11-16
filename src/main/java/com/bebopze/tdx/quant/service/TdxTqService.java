package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.constant.tq.PeriodEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;

import java.time.LocalDate;
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
     * 获取历史行情数据
     *
     * @param code       通达信 代码（股票/ETF/板块/期权）
     * @param periodEnum K线周期
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return
     */
    List<KlineDTO> get_market_data(String code,
                                   PeriodEnum periodEnum,
                                   LocalDate startDate,
                                   LocalDate endDate);


    /**
     * 读取单个实时行情
     *
     * @param code 通达信 代码（股票/ETF/板块/期权）
     * @return
     */
    StockSnapshotKlineDTO get_market_snapshot(String code);


    /**
     * 批量读取实时行情
     *
     * @param codeList 通达信 代码（股票/ETF/板块/期权）列表
     * @return
     */
    List<StockSnapshotKlineDTO> get_market_snapshot_batch(List<String> codeList);


    /**
     * 按 股票类型，读取全部实时行情
     *
     * @param stockTypeEnum 可选 股票类型，例如：A_STOCK,ETF,TDX_BLOCK（传空 -> 拉取全部类型）
     * @return
     */
    List<StockSnapshotKlineDTO> getAll(StockTypeEnum stockTypeEnum);

}