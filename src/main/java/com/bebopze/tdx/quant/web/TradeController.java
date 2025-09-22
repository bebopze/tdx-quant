package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.trade.RevokeOrderResultDTO;
import com.bebopze.tdx.quant.common.domain.param.QuickBuyPositionParam;
import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.param.TradeRevokeOrdersParam;
import com.bebopze.tdx.quant.common.domain.trade.resp.GetOrdersDataResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 交易（融资账户） -  BS / 撤单 / 持仓 / ...
 *
 * @author: bebopze
 * @date: 2025/5/6
 */
@RestController
@RequestMapping("/api/trade")
@Tag(name = "交易（融资账户）", description = "交易（仅限：融资账户） -  BS/撤单/持仓/...")
public class TradeController {


    @Autowired
    private TradeService tradeService;


    @Operation(summary = "我的持仓", description = "我的持仓")
    @GetMapping(value = "/queryCreditNewPosV2")
    public Result<QueryCreditNewPosResp> queryCreditNewPosV2() {
        return Result.SUC(tradeService.queryCreditNewPosV2(true));
    }


    @Operation(summary = "实时行情：买5/卖5", description = "实时行情：买5/卖5")
    @GetMapping(value = "/SHSZQuoteSnapshot")
    public Result<SHSZQuoteSnapshotResp> SHSZQuoteSnapshot(@Schema(description = "证券代码", example = "300059")
                                                           @RequestParam String stockCode) {

        return Result.SUC(tradeService.SHSZQuoteSnapshot(stockCode));
    }


    @Operation(summary = "买入/卖出", description = "买入/卖出")
    @PostMapping(value = "/bs")
    public Result<Integer> bs(@RequestBody TradeBSParam param) {
        return Result.SUC(tradeService.bs(param));
    }


    @Operation(summary = "当日 委托单列表", description = "当日 委托单列表")
    @GetMapping(value = "/getOrdersData")
    public Result<List<GetOrdersDataResp>> getOrdersData() {
        return Result.SUC(tradeService.getOrdersData());
    }


    @Operation(summary = "全部 可撤单列表", description = "全部 可撤单列表")
    @GetMapping(value = "/getRevokeList")
    public Result<List<GetOrdersDataResp>> getRevokeList() {
        // https://jywg.18.cn/MarginTrade/GetRevokeList?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
        return Result.SUC(tradeService.getRevokeList());
    }


    @Operation(summary = "批量撤单", description = "批量撤单")
    @PostMapping(value = "/revokeOrders")
    public Result<List<RevokeOrderResultDTO>> revokeOrders(@RequestBody List<TradeRevokeOrdersParam> paramList) {
        return Result.SUC(tradeService.revokeOrders(paramList));
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                        tips：以下均为  融资账户（特别是 BUY！！！）            暂不考虑 普通账户 BS逻辑
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    @Operation(summary = "一键清仓", description = "一键清仓")
    @GetMapping(value = "/quickOption/clearPosition")
    public Result<Void> quickClearPosition() {
        tradeService.quickClearPosition();
        return Result.SUC();
    }


    @Operation(summary = "一键 等比卖出 - 支持勾选", description = "一键 等比卖出 - 支持勾选")
    @GetMapping(value = "/quickOption/sellPosition")
    public Result<Void> quickSellPosition(@Schema(description = "卖出 个股code列表（逗号分隔）", example = "1,2,3")
                                          @RequestParam String sellStockCodeList,

                                          @Schema(description = "卖出 持仓比例%（100% -> 清仓，50% -> 减半仓）", example = "100.0")
                                          @RequestParam(required = false, defaultValue = "100.0") double sellPosPct,

                                          @Schema(description = "（当前价格）涨跌幅比例%（0% -> 实时价格，5% -> 当前价格x105% 挂S单）", example = "0.0")
                                          @RequestParam(required = false, defaultValue = "0.0") double currPricePct,

                                          @Schema(description = "（昨日收盘价）涨跌幅比例%（0% -> 昨日收盘价，10%/20%/30% -> 涨停价 挂S单）", example = "0.0")
                                          @RequestParam(required = false, defaultValue = "0.0") double changePricePct) {

        Set<String> _sellStockCodeList = Arrays.stream(sellStockCodeList.split(",")).collect(Collectors.toSet());

        tradeService.quickSellPosition(_sellStockCodeList, sellPosPct, currPricePct, changePricePct);
        return Result.SUC();
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Operation(summary = "一键买入（调仓换股）", description = "一键买入（调仓换股）  =>   清仓（old） ->  买入（new）")
    @PostMapping(value = "/quickOption/buyNewPosition")
    public Result<Void> quickBuyNewPosition(@RequestBody List<QuickBuyPositionParam> newPositionList) {
        tradeService.quickClearAndBuyNewPosition(newPositionList);
        return Result.SUC();
    }

    @Operation(summary = "一键 等比买入（调仓换股）", description = "一键 等比买入（调仓换股）  =>   清仓（old） ->  买入（new）")
    @PostMapping(value = "/quickOption/equalRatio/buyNewPosition")
    public Result<Void> avgBuyNewPosition(@RequestBody List<QuickBuyPositionParam> newPositionList) {
        tradeService.quickClearAndAvgBuyNewPosition(newPositionList);
        return Result.SUC();
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Operation(summary = "账户总仓位（以此刻 融+担=净x2 为100%基准）  ->   一键 等比减仓（等比卖出）", description = "将 账户总仓位（以此刻 融+担=净x2 为100%基准）  -降至->   指定仓位（0~1）")
    @GetMapping(value = "/quickOption/totalAccount/equalRatio/sellPosition")
    public Result<Void> totalAccount__equalRatioSellPosition(@Schema(description = "新仓位     =>     将 账户总仓位（以此刻 融+担=净x2 为100%基准）  -降至->   指定仓位（0~1）", example = "1")
                                                             @RequestParam(defaultValue = "1") double newPositionRate) {

        tradeService.totalAccount__equalRatioSellPosition(newPositionRate);
        return Result.SUC();
    }


    @Operation(summary = "当前持仓   ->   一键 等比减仓（等比卖出）", description = "将 当前持仓（以 此刻持仓市值 为100%基准）  -降至->   指定仓位")
    @GetMapping(value = "/quickOption/currPosition/equalRatio/sellPosition")
    public Result<Void> currPosition__equalRatioSellPosition(@Schema(description = "新仓位     =>     将 当前持仓（以 此刻持仓市值 为100%基准）  -降至->   指定仓位（0~1）", example = "1")
                                                             @RequestParam(defaultValue = "1") double newPositionRate) {

        tradeService.currPosition__equalRatioSellPosition(newPositionRate);
        return Result.SUC();
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Operation(summary = "一键撤单", description = "一键撤单   =>   撤除所有 [未成交 -> 未报/已报/部成] 委托单")
    @GetMapping(value = "/quickOption/cancelOrder")
    public Result<Void> quickCancelOrder() {
        tradeService.quickCancelOrder();
        return Result.SUC();
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Operation(summary = "一键再融资", description = "一键清仓 -> 重置融资   =>   一键融资再买入 -> 一键担保再买入   =>   新剩余 担保资金")
    @GetMapping(value = "/quickOption/resetFinancing")
    public Result<Void> quickResetFinancing() {
        tradeService.quickResetFinancing();
        return Result.SUC();
    }


    @Operation(summary = "一键取款（担保比例 >= 300%     ->     隔日 可取款）    // 已替换成 一键【等比减仓】（减免 先清仓 -> 2次再融资 -> 交易费）",
            description = "计算 新仓位   ->   一键 等比减仓   =>   手动【现金还款】  ->   担保比例 >= 300%" + "   \r\n   " +
                    "等比减仓（只涉及到 SELL   ->   无2次重复买入     =>     减免2次BS的 交易费）")
    // @Operation(summary = "已替换成 一键 等比减仓（减免 再融资 -> 手续费）  // 一键取款（担保比例 >= 300%     ->     隔日 可取款）",
    //        description = "控制担保比例 -> 计算 最大融资额   =>   一键清仓  ->  limit_融资 再买入 -> 一键担保再买入   =>   新剩余 担保资金（-> 可取金额）")
    @GetMapping(value = "/quickOption/lowerFinancing")
    public Result<Void> quickLowerFinancing(@Schema(description = "取款金额（T+1 隔日7点可取）", example = "50000")
                                            @RequestParam double transferAmount) {

        tradeService.quickLowerFinancing(transferAmount);
        return Result.SUC();
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                      ETF
    // -----------------------------------------------------------------------------------------------------------------


    @Operation(summary = "ETF快捷下单   ->   按照 指定价格区间", description = "ETF快捷下单   ->   按照 指定价格区间，逐档 梯次等比下单")
    @GetMapping(value = "/quickOption/quickETF")
    public Result<Void> quickETF(@Schema(description = "证券代码", example = "588000")
                                 @RequestParam String stockCode,

                                 @Schema(description = "价格间隔（%）", example = "0.5")
                                 @RequestParam double priceRangePct,

                                 @Schema(description = "价格间隔 -> 总档数", example = "10")
                                 @RequestParam int rangeTotal,

                                 @Schema(description = "每档 买入/卖出金额", example = "1000")
                                 @RequestParam double amount,

                                 @Schema(description = "交易类型（RZ_BUY-融资买入；ZY_BUY-担保买入；SELL-担保卖出）", example = "RZ_BUY")
                                 @RequestParam TradeTypeEnum tradeTypeEnum) {


        tradeService.quickETF(stockCode, priceRangePct, rangeTotal, amount, tradeTypeEnum);

        return Result.SUC();
    }


}