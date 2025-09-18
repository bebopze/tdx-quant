package com.bebopze.tdx.quant.common.config.aspect;

import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.domain.trade.req.RevokeOrdersReq;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;


/**
 * - @BSLimiter 拦截器       ->       BS限流（量化监管）
 *
 * @author: bebopze
 * @date: 2025/9/13
 */
@Slf4j
@Aspect
@Component
public class BSLimiterAspect {


    private static final StockTradingRateLimiter limiter = new StockTradingRateLimiter();


    /**
     * 拦截 @BSLimiter       ->       BS限流（量化监管）
     *
     * @param point
     */
    @Around("@annotation(com.bebopze.tdx.quant.common.config.anno.BSLimiter)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();


        // Method method = ((MethodSignature) point.getSignature()).getMethod();
        // Parameter[] parameters = method.getParameters();


        Object arg = point.getArgs()[0];


        String stockCode = null;

        if (arg instanceof SubmitTradeV2Req) {

            // B/S
            SubmitTradeV2Req req = (SubmitTradeV2Req) arg;
            stockCode = req.getStockCode();

        } else if (arg instanceof RevokeOrdersReq) {

            // 批量撤单
            RevokeOrdersReq req = (RevokeOrdersReq) arg;
            // 简化模型
            stockCode = "-1";

        } else {
            throw new BizException("异常");
        }


        // -------------------------------------------------------------------------------------------------------------


        // 获取当前速率信息
        StockTradingRateLimiter.RateInfo rateInfo = limiter.getRateInfo(stockCode);
        log.info("[" + stockCode + "] " + rateInfo.toString());


        // 限流 -> 等待许可后执行交易
        // limiter.waitForPermit(stockCode);
        limiter.tryAcquireWithTimeout(stockCode, 60 * 1000);
        log.info("[" + stockCode + "] 在 " + DateTimeUtil.formatNow2Hms(start) + " 执行交易");


        // -------------------------------------------------------------------------------------------------------------


        // 放行
        return point.proceed();
    }

}