package com.bebopze.tdx.quant.common.constant.tq;

import lombok.AllArgsConstructor;


/**
 * order_type 类型                            - https://help.tdx.com.cn/quant/docs/markdown/Dict.html#order-type类型
 *
 * @author: bebopze
 * @date: 2026/10/1
 */
@AllArgsConstructor
public enum OrderTypeEnum {


    //       名称                 类型      数值       说明
    //
    //   STOCK_BUY               int       0        买
    //   STOCK_SELL              int       1        卖
    //   CREDIT_BUY              int       0        担保品买入
    //   CREDIT_SELL             int       1        担保品卖出
    //   CREDIT_FIN_BUY          int       69       融资买入
    //   CREDIT_SLO_SELL         int       70       融券卖出
    //   CREDIT_COV_BUY          int       71       买券还券
    //   CREDIT_STK_REPAY        int       76       卖券还款
    //   ETF_PURCHASE            int       45       基金申购
    //   ETF_REDEMPTION          int       46       基金赎回
    //   FUTURE_OPEN_LONG        int       101      期货开多
    //   FUTURE_OPEN_SHORT       int       102      期货开空
    //   FUTURE_CLOSE_LONG       int       103      期货平多
    //   FUTURE_CLOSE_SHORT      int       104      期货平空
    //   FUTURE_CLOSEJ_LONG      int       105      期货平多(上期所平今)
    //   FUTURE_CLOSEJ_SHORT     int       106      期货平空(上期所平今)
    //   OPTION_OPEN_LONG        int       201      期权开多
    //   OPTION_OPEN_SHORT       int       202      期权开空
    //   OPTION_CLOSE_LONG       int       203      期权平多
    //   OPTION_CLOSE_SHORT      int       204      期权平空
    //   OPTION_CLOSEJ_LONG      int       205      期权平多(上期所平今)
    //   OPTION_CLOSEJ_SHORT     int       206      期权平空(上期所平今)


    STOCK_BUY(0, "买"),
    STOCK_SELL(1, "卖"),


    CREDIT_BUY(0, "担保品买入"),
    CREDIT_SELL(1, "担保品卖出"),


    CREDIT_FIN_BUY(69, "融资买入"),
    CREDIT_SLO_SELL(70, "融券卖出"),


    CREDIT_COV_BUY(71, "买券还券"),
    CREDIT_STK_REPAY(76, "卖券还款"),


    ETF_PURCHASE(45, "基金申购"),
    ETF_REDEMPTION(46, "基金赎回"),


    FUTURE_OPEN_LONG(101, "期货开多"),
    FUTURE_OPEN_SHORT(102, "期货开空"),

    FUTURE_CLOSE_LONG(103, "期货平多"),
    FUTURE_CLOSE_SHORT(104, "期货平空"),

    FUTURE_CLOSEJ_LONG(105, "期货平多(上期所平今)"),
    FUTURE_CLOSEJ_SHORT(106, "期货平空(上期所平今)"),


    OPTION_OPEN_LONG(201, "期权开多"),
    OPTION_OPEN_SHORT(202, "期权开空"),

    OPTION_CLOSE_LONG(203, "期权平多"),
    OPTION_CLOSE_SHORT(204, "期权平空"),

    OPTION_CLOSEJ_LONG(205, "期权平多(上期所平今)"),
    OPTION_CLOSEJ_SHORT(206, "期权平空(上期所平今)");


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 交易类型
     */
    public final int type;

    /**
     * 类型描述
     */
    public final String desc;


}