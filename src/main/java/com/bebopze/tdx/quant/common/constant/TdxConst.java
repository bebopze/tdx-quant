package com.bebopze.tdx.quant.common.constant;

import com.bebopze.tdx.quant.common.util.PropsUtil;


/**
 * @author: bebopze
 * @date: 2025/5/4
 */
public class TdxConst {


    /**
     * 通达信 - 根目录
     */
    public static final String TDX_PATH = PropsUtil.getTdxPath();


    /**
     * 通达信88（数据 起始日期   2005-06-07）
     *
     *
     * 基准板块（代替 -> 大盘指数）   =>     交易日 基准
     */
    public static final String INDEX_BLOCK = "880515";


    /**
     * 沪深300ETF（数据 起始日期   2012-5-28）                    // 宽基指数ETF   ->   不会停牌
     *
     *
     * 基准个股（代替 -> 大盘指数）   =>     交易日 基准
     */
    public static final String INDEX_STOCK = "510300";


}