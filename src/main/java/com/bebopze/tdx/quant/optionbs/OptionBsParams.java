package com.bebopze.tdx.quant.optionbs;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/**
 * 期权 BS 回测 - 参数（与创建页字段一一对应，写入 obt_task.params_json）
 *
 * @author bebopze
 * @date 2026/11/26
 */
@Data
public class OptionBsParams {

    // ---------------- 基础 ----------------
    private String taskName = "中期信号 · 全标的 · 当月/次月/当季";
    private List<String> tags = new ArrayList<>();
    private String remark = "";

    private LocalDate startDate = LocalDate.of(2017, 1, 1);
    private LocalDate endDate = LocalDate.now();
    private double initialCapital = 1_000_000D;

    // ---------------- 标的 ----------------
    private List<String> underlyingCodes = new ArrayList<>(List.of("159915", "588000", "510500", "510300", "000852"));

    // ---------------- 信号 / 合约 ----------------
    /**
     * BOTH / BOTTOM / TOP
     */
    private String signalMode = "BOTH";
    /**
     * NEAREST / QUARTER / NEXT2
     */
    private String monthMode = "NEAREST";
    /**
     * ITM1,ATM,OTM1,OTM2,OTM3
     */
    private List<String> strikes = new ArrayList<>(List.of("ITM1", "ATM", "OTM1", "OTM2", "OTM3"));
    /**
     * 平值并列：NEAREST / LOWER / HIGHER
     */
    private String atmMode = "NEAREST";
    /**
     * 合约集合：固定 / 每日重选
     */
    private boolean fixedContracts = true;
    /**
     * 到期：INTRINSIC / CLOSE
     */
    private String expiryMode = "INTRINSIC";

    /**
     * 严禁买入：距到期 < N 个自然日的合约（默认 30）
     */
    private int minBuyDteDays = 30;

    /**
     * 严禁持有到期：距到期 < N 个自然日时自动换月（默认 15）
     */
    private int rollDteDays = 15;

    // ---------------- 建仓 ----------------
    private double dailyBuyPct = 0.05D;
    private double addBuyPct = 0.10D;
    private double addStepPct = 0.05D;
    private double maxPositionPct = 1.00D;
    private double cashReservePct = 0D;
    /**
     * ALL / TRIGGERED
     */
    private String addScope = "ALL";
    /**
     * 买满后止盈卖出，是否允许下一轮底/顶信号重新建仓
     */
    private boolean reenterOnNewCycle = true;

    // ---------------- 卖出 ----------------
    private boolean enableRight = true;
    private boolean enableLeft2 = true;
    private boolean enableLeft1 = true;
    private boolean enableStrong = true;
    private boolean enableSignalSell = true;
    private boolean enableAccountTp = true;
    private boolean enableSingleTp = true;

    private double singleTpStepPct = 1.00D;   // +100%
    private double singleTpReducePct = 0.10D;
    private double accountTpStepPct = 0.50D;  // +50%
    private double accountTpReducePct = 0.10D;
    private double signalSellDailyPct = 0.10D;
    private double strongSellPct = 0.20D;
    private double left1SellPct = 0.30D;
    private double left2SellPct = 0.30D;

    // ---------------- 执行 ----------------
    private double commissionPerContract = 2.5D;
    private double slippageBps = 0D;
    private String benchmarkCode = "510300";
    private double rf = 0.02D;
    private boolean annualize365 = true;

    private String engineVersion = "v1.0.0";
    private String dataVersion = "";
    private String creator = "bebopze";


    public boolean isBoth() {
        return "BOTH".equalsIgnoreCase(signalMode);
    }

    public boolean allowCalls() {
        return !"TOP".equalsIgnoreCase(signalMode);
    }

    public boolean allowPuts() {
        return !"BOTTOM".equalsIgnoreCase(signalMode);
    }
}
