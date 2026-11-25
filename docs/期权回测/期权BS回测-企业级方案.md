# 期权 BS 回测 —— 企业级方案（后端 + 前端）

> 版本：v1.0 ｜ 日期：2026-11-26 ｜ 适用代码库：`tdx-quant`（Spring Boot 4.1 / Java 25 / MyBatis-Plus / MySQL / 静态 HTML + ECharts）
>
> 关联设计稿：[三套企业级 UI 提案](../../src/main/resources/static/option-backtest/design/index.html)

---

## 0. 结论摘要（TL;DR）

> **已落地（2026-11-26）**：引擎、9 张表、异步 API、A/B 双主题三页（创建/列表/详情）均已完成，并基于真实数据跑出基线任务 `OBT20261126-0019`（DB id=22）：
> 总收益 **+3,352.71%**、年化 **+67.12%**、最大回撤 **-24.78%**、夏普 **0.60**、交易 **22,973** 笔。
> 详见 [实跑报告](./实跑报告.md)。
>
> 页面：`/option-backtest/create.html` · `/option-backtest/list.html` · `/option-backtest/detail.html?id=22`（右上角切换 A 终端 / B 专业模式）。


| 项 | 结论 |
|---|---|
| 信号 | `qa_market_mid_cycle.market_mid_status`：1=底部、3=顶部，均为**单日事件**；回测按「状态跃迁」识别信号，而非按状态持续天数 |
| 标的 | 创业板ETF易方达 `159915`、科创50ETF华夏 `588000`、中证500ETF南方 `510500`、沪深300ETF华泰柏瑞 `510300`、中证1000 `000852`（中金所指数期权，替代不存在的 1000ETF 期权） |
| 数据 | `base_option` 31,738 个合约 / 2,321,163 根日K；`qa_market_mid_cycle` 3,553 条（2012-03-02 ~ 2026-10-20） |
| 默认区间 | 2017-01-01 ~ 至今；**实际首个可交易日为 2019-12-23**（510300 期权首挂），页面必须显式提示这一点 |
| 本金 | ¥1,000,000；仓位上限 100%；每日建仓 5%、逆势加仓 10% |
| 引擎 | 日频事件驱动；收盘价成交；ETF 期权乘数 10,000，指数期权乘数 100；支持佣金/滑点/到期结算 |
| 落库 | 新增 `obt_task` / `obt_daily_equity` / `obt_daily_position` / `obt_trade` / `obt_close_record` / `obt_signal_event` / `obt_task_log` 七张表（+ 可选 `obt_index_kline` / `obt_template`） |
| 前端 | **创建页 + 列表页 + 详情页**；创建页含参数模板/实时预估/校验；列表页含筛选/排序/分页/搜索/导出；详情页含净值/回撤/日收益、16 项风险指标、信号时间线、月度热力、卖出原因、收益归因、每日持仓/清仓/交易/净值、参数/日志 |
| UI | 已出 3 套设计稿（A 深色终端 / B 浅色中台 / C 驾驶舱），待选型后实施 |
| 交付 | 引擎 + DDL + API + 前端 + 真实数据实跑报告 + 字段/接口文档 + 验收清单 |

---

## 1. 目标与设计原则

### 1.1 目标

1. 严格按用户给定的「大盘中期底/顶 → 期权认购/认沽」买卖规则做**可复现、可审计**的日频回测。
2. 支持自定义回测区间、本金、标的集合、月份/行权价档位、费用模型、规则开关。
3. 结果可查询、可排序、可分页、可导出，并保留每一条交易/持仓/清仓的**触发原因**。
4. 单次回测（2019-12 ~ 2026-11，5 标的）在 60 秒内完成并落库；支持异步任务、进度、取消、重跑。
5. 与企业现有代码风格、权限体系、静态页面体系一致，不引入额外前端框架。

### 1.2 设计原则

- **可解释**：每个买卖动作必须落 `reason_code + reason_desc + trigger_value`，能回答“为什么买/卖”。
- **可复现**：同一参数 + 同一数据版本 → 同一结果；`engine_version`、数据快照日期写入任务。
- **可配置**：所有比例、阈值、费用、档位模式进 `params_json`，不写死。
- **失败可恢复**：任务状态机 + 幂等落库（先删后插）+ 断点日志。
- **性能可控**：只加载所需标的/区间/合约；K 线解析为原始数组；批量落库。
- **先算后存**：引擎在内存中完成，结果一次性落库，避免边算边写。

---

## 2. 现状与数据事实（已实测）

### 2.1 信号数据

```sql
SELECT MIN(date), MAX(date), COUNT(*) FROM qa_market_mid_cycle;
-- 2012-03-02 | 2026-10-20 | 3553

SELECT market_mid_status, COUNT(*) FROM qa_market_mid_cycle GROUP BY market_mid_status;
-- 1(底部) 180 | 2(底->顶) 1281 | 3(顶部) 687 | 4(顶->底) 1405
```

关键结论：

- `1` 与 `3` 绝大多数是**单日**出现（例如 2019-12-03 为 1，次日起为 2）。
- `3/4` 会在震荡期反复切换（例如 2022-06-24 ~ 2022-07-22 多次 3↔4）。因此不能用“状态=3 的每一天”当信号，否则会重复触发。
- 回测信号识别规则：`prev_status != 1 && status == 1` → 中期底；`prev_status != 3 && status == 3` → 中期顶。**一个底/顶周期内只认第一次**（见 §4.5）。

### 2.2 期权合约与行情

```sql
SELECT underlying_code, underlying_name, underlying_type, COUNT(*), MIN(start_date), MAX(trade_date)
FROM base_option GROUP BY underlying_code, underlying_name, underlying_type;
```

| 标的 | 名称 | 类型 | 合约数 | 期权起始 | 说明 |
|---|---|---|---:|---|---|
| 159915 | 创业板ETF易方达 | ETF | 1,808+206 | 2022-09-19 | 深交所 ETF 期权 |
| 588000 | 科创50ETF华夏 | ETF | 1,250+118 | 2023-06-05 | 上交所 ETF 期权 |
| 510500 | 中证500ETF南方 | ETF | 1,692+148 | 2022-09-19 | 上交所 ETF 期权 |
| 510300 | 沪深300ETF华泰柏瑞 | ETF | 2,826+34 | 2019-12-23 | 上交所 ETF 期权 |
| 000852 | 中证1000指数 | 指数 | 3,366 | 2022-07-22 | 中金所指数期权，乘数 100 |

> 「+206」等为程序每日增量写入的“当前存续合约”行（`underlying_name` 为简称），与历史全量导入行**按 code 不重叠**，查询时直接 UNION 即可；已实测同一日期同一 `(month, call_put, strike)` 无重复合约。

### 2.3 标的行情

- ETF 日K：`base_stock.kline_his`（`159915 / 588000 / 510500 / 510300 / 512100` 均有）。
- `000852` 在 `base_stock` 中是**石化机械（A股）**，不是中证1000指数；不能直接复用。
  - 方案：`OptionBsDataLoader` 优先查 `base_stock`，按 name 包含「中证1000指数」且 type=2/3 匹配；查不到则调用东方财富 `push2his` 拉取 `secid=1.000852` 日K，并落 `obt_index_kline` 缓存表（可复现、可离线）。
- 基准：`510300` 收盘价归一化；若区间早于其上市则退化为等权标的组合。

### 2.4 数据缺口（必须在页面披露）

| 缺口 | 影响 | 处理 |
|---|---|---|
| 2017-01-01 ~ 2019-12-22 无任何目标标的期权 | 默认区间前 3 年空仓 | 任务详情显示「实际起始 2019-12-23」与空仓原因 |
| 159915/510500 自 2022-09-19、588000 自 2023-06-05 才有期权 | 早期仅 510300 参与 | 按标的生效日动态纳入，前期等权只分给有效标的 |
| 000852 指数K线不在库 | 中证1000 无法计算 MA/SSF | 东财拉取 + `obt_index_kline` 缓存 + 失败时降级剔除并告警 |
| 部分合约零成交/占位K线（导入报告：6.33%） | 收盘价可能不真实 | 用收盘价估值/成交；`volume=0` 标记 `low_liquidity`，仍按收盘价成交但在质量报告计数 |
| 合约到期 | 持仓需处理 | 到期日按内在价值结算（见 §4.7） |

---

## 3. 总体架构

```
┌────────────────────────────────────────────────────────────────────────────┐
│ 前端（static/option-backtest/）                                              │
│  list.html  →  筛选/排序/分页/搜索/导出/收藏/新建                              │
│  detail.html?id=  →  KPI/图表/信号/持仓/清仓/交易/净值/参数日志/导出            │
│  原生 HTML + CSS + ECharts（沿用项目 static 体系，无构建步骤）                   │
└───────────────────────────────┬────────────────────────────────────────────┘
                                │ REST / JSON（Result<T> / PageResult<T>）+ SSE 进度
┌───────────────────────────────▼────────────────────────────────────────────┐
│ 接入层  OptionBsBacktestController                                          │
│  /api/option/backtest/*    参数校验、权限、限流、导出                          │
├────────────────────────────────────────────────────────────────────────────┤
│ 应用层  OptionBsBacktestService                                             │
│  任务创建/状态机/异步执行/进度/取消/重跑/对比/导出编排                          │
├────────────────────────────────────────────────────────────────────────────┤
│ 引擎层  OptionBsEngine（纯 Java，可单测、可 CLI）                             │
│  SignalResolver → DataLoader → ContractSelector → Portfolio → SellRules      │
│  → BuyRules → MarkToMarket → Metrics → Recorder                              │
├────────────────────────────────────────────────────────────────────────────┤
│ 数据层  OptionBsDataLoader / OptionBsResultPersister                         │
│  base_option · base_stock · qa_market_mid_cycle · obt_* · obt_index_kline     │
└────────────────────────────────────────────────────────────────────────────┘
```

### 3.1 技术选型

| 层 | 选型 | 理由 |
|---|---|---|
| Web | Spring MVC + `Result<T>`（项目现有） | 与现有接口一致 |
| 异步 | `ThreadPoolTaskExecutor`（专用 `optionBacktestExecutor`，2~4 线程） | 回测 CPU/内存密集，隔离 Tomcat 线程 |
| 锁 | 现有 `ConfDistributedLock` / Redis 锁 | 防止同一任务重复执行 |
| 持久化 | MyBatis-Plus + 批量 `insert` | 与项目一致；明细量大，用 JDBC batch |
| 前端 | 原生 HTML + ECharts（本地 `static/js/echarts.min.js`） | 无 Node 构建，部署简单；与现有期权页一致 |
| 导出 | CSV（流式）+ EasyExcel（可选，若已有依赖） | 大表不 OOM；PDF 用现有模板/前端打印 |

### 3.2 任务状态机

```
QUEUED(0) ──claim──> RUNNING(1) ──success──> SUCCESS(2)
   │                     │
   │                     ├──exception──> FAILED(3)  （error_msg + 日志）
   │                     └──cancel─────> CANCELED(4)
   └──queue timeout────> FAILED(3)
```

- 幂等：同一 `task_no` 唯一；重复提交返回已有任务。
- 重跑：创建新 `task_no`（`-R1` 后缀）或原地清空结果重算（默认新任务，保留历史可比）。
- 取消：引擎每个交易日检查 `cancelFlag`，安全退出。

---

## 4. 回测引擎（核心口径）

### 4.1 时间与日历

- 交易日历：取 `510300` 在区间内的日K日期；若 510300 缺失，取 5 个标的日期并集。
- 信号日期不在交易日历时，顺延到**下一个交易日**执行（记录 `signal_date` 与 `exec_date`）。
- 所有买卖按**当日收盘价**成交（用户明确要求）；`close` 缺失时：
  - 该合约当日不可买/卖（买：跳过该合约；卖：顺延到下一有价日）；
  - 估值沿用最近有效收盘价，并标记 `stale=true`。

### 4.2 生命周期与周期（Cycle）

```
中期底(1) 首次出现 ──> BOTTOM_CYCLE 开始：
    连续 20 个交易日每日买 5%，其间每 -5% 加买 10%，累计到 100% 停止
         │
         └──> 中期顶(3) 首次出现 ──> TOP_CYCLE 开始：
                  持仓进入卖出阶段：逐日 -10%、强势/左侧/右侧规则
                        │
                        └──> 中期底(1) 再次出现 ──> 新 BOTTOM_CYCLE（预算重置）
```

- **底周期内再次出现 1（如 2019-12-03 → 12-04 的 1/2/1 抖动）不重置预算、不重置参考价**，避免重复建仓。
- **顶周期内再次出现 3（3/4 反复）不重置顶部参考价**，只沿用第一次顶信号的参考价；`右侧卖` 清仓后本顶周期结束。
- 认购与认沽共用同一套周期；底周期买认购、顶周期买认沽；卖出规则按持仓类型镜像。

### 4.3 合约选择

**月份（用户已选：当月 + 次月 + 当季，按实际挂牌顺序取前 3 个）**

1. 取信号日存续合约（`start_date <= date <= end_date`）。
2. 按 `contract_month >= 当月` 升序去重。
3. 取前 3 个：通常为 `当月 / 次月 / 当季`。若某月无合约（数据缺失）→ 跳过并向后顺延。
4. 备选模式（参数可切）：
   - `NEAREST`：前 3 个挂牌月（默认）；
   - `QUARTER`：当月 / 当季 / 下季；
   - `NEXT2`：当月 / 次月 / 次2月。

**行权价（5 档）**

以信号日标的收盘价 `S`、该月该类型（C/P）的**去重行权价升序列表** `K[]`：

| 档位 | 认购（C） | 认沽（P） |
|---|---|---|
| 实值1（ITM1） | 小于 `S` 的最近一档 | 大于 `S` 的最近一档 |
| 平值（ATM） | `argmin |K-S|`（并列取较小 K） | 同 |
| 虚值1（OTM1） | 大于 `S` 的第 1 档 | 小于 `S` 的第 1 档 |
| 虚值2（OTM2） | 大于 `S` 的第 2 档 | 小于 `S` 的第 2 档 |
| 虚值3（OTM3） | 大于 `S` 的第 3 档 | 小于 `S` 的第 3 档 |

- 档位不足（例如虚3 不存在）→ 该档不买，资金在同月内**按剩余档位等比再分配**。
- 若整月缺失 → 该月资金按剩余月份等比再分配。
- 合约集合在**一个周期内固定**（避免每天换合约导致碎片化）；到期/退市时移除，后续买入在剩余月份/档位内再分配。
- 可选参数 `dailyReselect=true`：每日按最新标的价重新选档（默认 false，保证与“首次 5 档 × 3 月”语义一致）。
- **到期保护（硬约束）**：只选择、只买入距到期 **≥30 个自然日** 的合约；当月/次月若不足 30 天，自动跳过并顺延到当季/次季。

### 4.4 买入规则

| 场景 | 触发 | 金额 | 分配 |
|---|---|---|---|
| 底信号建仓 | 底周期每个交易日 | 初始本金的 **5%**（50,000） | 标的等权 → 月份等权 → 行权价等权 |
| 下跌加仓 | 自本周期**首次买入日**起，该标的 ETF 每再下跌 **-5%** | **10%**（100,000） | 同上（默认全标的等权） |
| 顶信号建仓（认沽） | 顶周期每个交易日 | **5%** | 同上 |
| 上涨加仓（认沽） | 自本周期首次买入日起，该标的 ETF 每再上涨 **+5%** | **10%** | 同上 |
| 上限 | 累计买入成本 ≤ 本金的 **100%** | — | 买满即停 |

补充口径：

- “每下跌 -5%”按**阶梯**计算：`-5%` 触发第 1 次，`-10%` 触发第 2 次……每个阶梯只触发一次；价格回升后再次下跌不重复触发同一阶梯。
- 参考价：本周期**首次实际买入日**该标的的收盘价（每个标的独立）；若首次买入因无合约未成交，则参考价顺延到首次成交日。
- 加仓作用范围默认 `ALL`（全标的等权），可配置 `TRIGGERED_ONLY`（只加触发下跌/上涨的那个标的）。
- 买入预算：以**成本**计，不含手续费；手续费从现金扣除。
- 整手：`qty = floor(金额 / (price × multiplier))`；若 `qty=0` 则该合约本次不买，余额进入下一档/留现金。
- 份额分配使用“最大余数法”保证总额贴合预算且各合约尽量等权。

### 4.5 卖出规则（认购为例，认沽镜像）

| 优先级 | 规则 | 触发条件 | 动作 | 频率 |
|---:|---|---|---|---|
| 1 | 右侧卖 · 清仓 | 标的 `MA10空` 或 `SSF空` 或 `下MA20` 或 `MA20空` | 该标的全部期权清仓 | 顶周期内一次 |
| 2 | 左侧卖2 | 标的 `下MA5` 或 `MA5空` 或 `下MA10` | 该标的全部期权减 **30%** | 顶周期内一次 |
| 3 | 左侧卖1 | 自首次顶信号起，标的每下跌 **-5%** | 该标的全部期权减 **30%** | 每个 -5% 阶梯一次 |
| 4 | 强势卖出 | 自首次顶信号起，标的每上涨 **+5%** | 该标的全部期权减 **20%** | 每个 +5% 阶梯一次 |
| 5 | 信号卖出 | 顶周期内每个交易日 | 全部期权减 **10%** | 每日 |
| 6 | 账户止盈 | 账户权益 / 初始本金 每跨过 **+50%** | 全部期权各减 **10%** | 每个 +50% 阶梯一次 |
| 7 | 单票止盈 | 单合约价格 / 均价成本 每跨过 **+100%** | 该合约减 **10%** | 每个 +100% 阶梯一次 |

认沽镜像：

| 规则 | 认沽触发 |
|---|---|
| 右侧卖 | `MA10多` 或 `SSF多` 或 `上MA20` 或 `MA20多` |
| 左侧卖2 | `上MA5` 或 `MA5多` 或 `上MA10` |
| 左侧卖1 | 标的每上涨 **+5%** |
| 强势卖出 | 标的每下跌 **-5%** |

指标定义（与 `TdxExtFun` 一致）：

- `下MA5`：`close < MA5`；`MA5空`：`close < MA5 && MA5_t < MA5_{t-1}`。
- `上MA5`：`close >= MA5`；`MA5多`：`close >= MA5 && MA5_t >= MA5_{t-1}`。
- `下MA10/MA10空`、`上MA10/MA10多`、`下MA20/MA20空`、`上MA20/MA20多` 同理。
- `SSF`：复用 `com.bebopze.tdx.quant.common.tdxfun.TdxExtFun.SSF(close)`；`SSF空` = `close < SSF && SSF_t < SSF_{t-1}`，`SSF多` 反之。

执行顺序与去重：

1. 同一交易日按优先级依次执行；`右侧卖` 触发后该标的当日不再执行其他卖出。
2. 同一规则同一阶梯只触发一次（`step` 状态持久化在内存；重跑一致）。
3. 每次减仓数量：`max(1, floor(qty × ratio))`，且不超过 `qty`；清仓直接全卖。
4. 卖出价 = 当日收盘价；若当日无价，顺延到下一有价交易日（记录 `deferred=true`）。
5. 账户止盈基于**卖出前**的收盘权益计算；同一天可同时触发一次账户止盈与若干单票止盈。

### 4.6 资金与成本

- 初始现金 = 本金；买入 `cash -= amount + fee`；卖出 `cash += amount - fee`。
- `amount = price × qty × multiplier`。
- 费用：默认 `commissionPerContract=2.5` 元/张/边；`slippageBps=0`（严格收盘价）；均可配置。
- 不使用融资/杠杆；现金不足时按可用现金缩减买单（记录 `cash_limited=true`）。
- 买入手续费也计入成本，但**仓位预算**按成交金额（不含费）计。

### 4.7 到期、退市与异常

| 场景 | 处理 |
|---|---|
| 合约到期（`end_date`） | **正常不应发生**：持仓 DTE <15 时已自动换月/平仓。仅作为数据缺口的兜底，按最后交易日收盘内在价值结算，`reason=EXPIRY` |
| 临近到期换月 | 持仓 DTE <15：按收盘价卖出旧合约，买入同标的/同类型/同行权价（或最近行权价）的下一可用月份（次月→当季→次季），目标 DTE 必须 ≥30；无目标则只平仓 |
| 合约退市/停牌 | 无当日收盘价 → 估值沿用最后有效价；连续 5 日无价 → 按最后价强制平仓并告警 |
| 合约无K线 | 买入跳过；持仓估值用最近有效价；质量报告计数 |
| 标的数据缺失 | 该标的当日跳过买卖；MA/SSF 用可得数据计算；任务级 warning |
| 000852 指数K线拉取失败 | 剔除中证1000 继续回测；任务详情显示 `DATA_MISSING_000852` |

### 4.8 净值与指标

每个交易日收盘后：

```
equity_t = cash_t + Σ(qty_i × close_i × multiplier_i)
nav_t    = equity_t / initial_capital
daily_return_t = equity_t / equity_{t-1} - 1
cum_return_t   = equity_t / initial_capital - 1
drawdown_t     = equity_t / max(equity_0..t) - 1
```

指标（页面全部展示）：

| 指标 | 公式/口径 |
|---|---|
| 总收益率 | `final/initial - 1` |
| 年化收益率 | `(final/initial)^(365.25/自然日) - 1` |
| 最大回撤 | `min(drawdown)`；同时记录起止日、持续天数 |
| 年化波动率 | `std(daily_return) × sqrt(252)` |
| 夏普 | `(mean(daily_return) - rf/252) / std(daily_return) × sqrt(252)`，`rf=2%` |
| 索提诺 | 分母仅下行波动 |
| 卡玛 | `年化 / |最大回撤|` |
| 胜率（日） | 上涨交易日 / 总交易日 |
| 胜率（交易） | 盈利平仓记录 / 平仓记录数 |
| 盈亏比 | `Σ盈利 / |Σ亏损|` |
| 平均持仓天数 | 平仓记录按数量加权 |
| 月胜率 | 正收益月份 / 有交易月份 |
| VaR 95% | 日收益 5% 分位数 |
| CVaR 95% | 尾部 5% 条件均值 |
| Beta / Alpha | 对 510300 日收益回归；Alpha 年化 |
| 换手率 | 成交额 / 平均权益（年化） |
| 交易成本 | 佣金 + 滑点合计 |
| 仓位利用率 | 平均 `market_value/equity`；满仓天数占比 |
| 信号胜率 | 底信号后 20 日标的上涨次数 / 底信号数；顶信号反之 |

---

## 5. 数据模型（DDL）

> 库：`tdx`；字符集 `utf8mb4`；引擎 `InnoDB`。若走 ShardingSphere，需在 `shardingsphere-*.yml` 中把 `obt_*` 加入默认库/分片规则；明细表按 `task_id` 分片即可。

### 5.1 `obt_task` 回测任务

```sql
CREATE TABLE obt_task (
  id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  task_no             VARCHAR(40)  NOT NULL COMMENT '任务号 OBTyyyyMMdd-0001',
  task_name           VARCHAR(128) NOT NULL COMMENT '任务名称',
  status              TINYINT      NOT NULL DEFAULT 0 COMMENT '0排队 1运行 2成功 3失败 4取消',
  progress            TINYINT      NOT NULL DEFAULT 0 COMMENT '进度 0-100',
  start_date          DATE         NOT NULL COMMENT '回测起始日',
  end_date            DATE         NOT NULL COMMENT '回测结束日',
  actual_start_date   DATE         NULL COMMENT '实际首个有信号/有合约的交易日',
  initial_capital     DECIMAL(18,2) NOT NULL COMMENT '初始本金',
  underlying_codes    VARCHAR(255) NOT NULL COMMENT '标的 code 逗号分隔',
  signal_mode         VARCHAR(32)  NOT NULL DEFAULT 'BOTH' COMMENT 'BOTTOM/TOP/BOTH',
  month_mode          VARCHAR(32)  NOT NULL DEFAULT 'NEAREST' COMMENT 'NEAREST/QUARTER/NEXT2',
  strike_mode         VARCHAR(32)  NOT NULL DEFAULT 'ITM1_ATM_OTM3',
  params_json         JSON         NULL COMMENT '全部参数快照',
  engine_version      VARCHAR(16)  NOT NULL COMMENT '引擎版本',
  data_version        VARCHAR(32)  NULL COMMENT '数据快照日期/批次',
  final_equity        DECIMAL(18,2) NULL,
  total_return_pct    DECIMAL(12,4) NULL,
  annual_return_pct   DECIMAL(12,4) NULL,
  max_drawdown_pct    DECIMAL(12,4) NULL,
  max_drawdown_start  DATE          NULL,
  max_drawdown_end    DATE          NULL,
  max_drawdown_days   INT           NULL,
  sharpe_ratio        DECIMAL(12,4) NULL,
  sortino_ratio       DECIMAL(12,4) NULL,
  calmar_ratio        DECIMAL(12,4) NULL,
  volatility_pct      DECIMAL(12,4) NULL,
  win_rate_pct        DECIMAL(12,4) NULL,
  profit_factor       DECIMAL(12,4) NULL,
  total_trades        INT           NULL,
  buy_trades          INT           NULL,
  sell_trades         INT           NULL,
  total_days          INT           NULL,
  avg_position_pct    DECIMAL(12,4) NULL,
  total_fee           DECIMAL(18,2) NULL,
  signal_count        INT           NULL,
  started_at          DATETIME      NULL,
  finished_at         DATETIME      NULL,
  duration_ms         BIGINT        NULL,
  error_msg           VARCHAR(1000) NULL,
  creator             VARCHAR(64)   NOT NULL,
  favorite_flag       TINYINT       NOT NULL DEFAULT 0,
  deleted_flag        TINYINT       NOT NULL DEFAULT 0,
  gmt_create          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modify          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_no (task_no),
  KEY idx_status_create (status, gmt_create),
  KEY idx_creator (creator),
  KEY idx_favorite (favorite_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期权BS回测-任务';
```

### 5.2 `obt_daily_equity` 每日净值

```sql
CREATE TABLE obt_daily_equity (
  id                   BIGINT NOT NULL AUTO_INCREMENT,
  task_id              BIGINT NOT NULL,
  trade_date           DATE   NOT NULL,
  nav                  DECIMAL(16,6) NOT NULL COMMENT '单位净值',
  equity               DECIMAL(18,2) NOT NULL COMMENT '总权益',
  cash                 DECIMAL(18,2) NOT NULL,
  market_value         DECIMAL(18,2) NOT NULL,
  position_pct         DECIMAL(10,4) NOT NULL COMMENT '仓位%',
  daily_return_pct     DECIMAL(12,4) NOT NULL,
  cum_return_pct       DECIMAL(12,4) NOT NULL,
  drawdown_pct         DECIMAL(12,4) NOT NULL,
  benchmark_close      DECIMAL(16,4) NULL COMMENT '510300 收盘',
  benchmark_nav        DECIMAL(16,6) NULL,
  benchmark_return_pct DECIMAL(12,4) NULL,
  signal_type          TINYINT NULL COMMENT '1底 3顶 0无',
  signal_status        VARCHAR(32) NULL COMMENT 'BOTTOM_HOLD/TOP_SELL/IDLE',
  position_count       INT NOT NULL DEFAULT 0,
  fee                  DECIMAL(18,2) NOT NULL DEFAULT 0,
  stale_flag           TINYINT NOT NULL DEFAULT 0 COMMENT '含缺失行情估值',
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_date (task_id, trade_date),
  KEY idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期权BS回测-每日净值';
```

### 5.3 `obt_daily_position` 每日持仓快照

```sql
CREATE TABLE obt_daily_position (
  id                BIGINT NOT NULL AUTO_INCREMENT,
  task_id           BIGINT NOT NULL,
  trade_date        DATE   NOT NULL,
  underlying_code   VARCHAR(16) NOT NULL,
  underlying_name   VARCHAR(64) NULL,
  option_code       VARCHAR(32) NOT NULL,
  option_name       VARCHAR(64) NULL,
  call_put_type     CHAR(1) NOT NULL COMMENT 'C/P',
  strike_price      DECIMAL(12,4) NOT NULL,
  contract_month    INT NOT NULL COMMENT 'yyyyMM',
  expiry_date       DATE NULL,
  multiplier        INT NOT NULL,
  qty               INT NOT NULL COMMENT '持仓张数',
  avg_cost          DECIMAL(12,4) NOT NULL COMMENT '加权平均成本',
  close_price       DECIMAL(12,4) NOT NULL COMMENT '当日收盘/估值',
  cost_amount       DECIMAL(18,2) NOT NULL,
  market_value      DECIMAL(18,2) NOT NULL,
  unrealized_pnl    DECIMAL(18,2) NOT NULL,
  pnl_pct           DECIMAL(12,4) NOT NULL,
  weight_pct        DECIMAL(10,4) NOT NULL COMMENT '占组合市值%',
  hold_days         INT NOT NULL,
  open_reason       VARCHAR(64) NULL COMMENT '建仓来源',
  moneyness         VARCHAR(8) NULL COMMENT 'ITM/ATM/OTM',
  stale_flag        TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_date_code (task_id, trade_date, option_code),
  KEY idx_task_date (task_id, trade_date),
  KEY idx_task_code (task_id, option_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期权BS回测-每日持仓';
```

### 5.4 `obt_trade` 交易明细

```sql
CREATE TABLE obt_trade (
  id                BIGINT NOT NULL AUTO_INCREMENT,
  task_id           BIGINT NOT NULL,
  trade_date        DATE   NOT NULL,
  trade_time        TIME   NULL,
  seq               INT    NOT NULL COMMENT '当日序号',
  trade_type        TINYINT NOT NULL COMMENT '1买入 2卖出',
  underlying_code   VARCHAR(16) NOT NULL,
  underlying_name   VARCHAR(64) NULL,
  option_code       VARCHAR(32) NOT NULL,
  option_name       VARCHAR(64) NULL,
  call_put_type     CHAR(1) NOT NULL,
  strike_price      DECIMAL(12,4) NOT NULL,
  contract_month    INT NOT NULL,
  price             DECIMAL(12,4) NOT NULL,
  qty               INT NOT NULL,
  multiplier        INT NOT NULL,
  amount            DECIMAL(18,2) NOT NULL,
  fee               DECIMAL(12,2) NOT NULL,
  reason_code       VARCHAR(32) NOT NULL COMMENT 'BUY_DAILY/BUY_DIP/TP_SINGLE/TP_ACCOUNT/SIGNAL_SELL/STRONG_SELL/LEFT1/LEFT2/RIGHT_CLEAR/EXPIRY',
  reason_desc       VARCHAR(128) NULL,
  trigger_value     DECIMAL(16,4) NULL COMMENT '触发值（如 -5.0%、+100%）',
  cash_after        DECIMAL(18,2) NULL,
  qty_after         INT NULL,
  cycle_id          INT NULL,
  signal_date       DATE NULL,
  deferred_flag     TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_task_date (task_id, trade_date),
  KEY idx_task_code (task_id, option_code),
  KEY idx_reason (task_id, reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期权BS回测-交易明细';
```

### 5.5 `obt_close_record` 平仓记录

```sql
CREATE TABLE obt_close_record (
  id              BIGINT NOT NULL AUTO_INCREMENT,
  task_id         BIGINT NOT NULL,
  cycle_id        INT NULL,
  underlying_code VARCHAR(16) NOT NULL,
  option_code     VARCHAR(32) NOT NULL,
  option_name     VARCHAR(64) NULL,
  call_put_type   CHAR(1) NOT NULL,
  open_date       DATE NOT NULL,
  close_date      DATE NOT NULL,
  qty             INT NOT NULL COMMENT '本次平仓张数（累计）',
  avg_cost        DECIMAL(12,4) NOT NULL,
  avg_sell_price  DECIMAL(12,4) NOT NULL,
  realized_pnl    DECIMAL(18,2) NOT NULL,
  pnl_pct         DECIMAL(12,4) NOT NULL,
  hold_days       INT NOT NULL,
  max_pnl_pct     DECIMAL(12,4) NULL COMMENT '持有期最大浮盈',
  min_pnl_pct     DECIMAL(12,4) NULL,
  close_reason    VARCHAR(32) NOT NULL,
  close_reason_desc VARCHAR(128) NULL,
  PRIMARY KEY (id),
  KEY idx_task_close (task_id, close_date),
  KEY idx_task_code (task_id, option_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期权BS回测-平仓记录';
```

> 同一合约可能分多次减仓，`obt_close_record` 在**完全平仓**时汇总写入；部分减仓的已实现盈亏记在 `obt_trade` 的卖出记录中，二者通过 `option_code` 关联。

### 5.6 `obt_signal_event` 信号事件

```sql
CREATE TABLE obt_signal_event (
  id               BIGINT NOT NULL AUTO_INCREMENT,
  task_id          BIGINT NOT NULL,
  signal_date      DATE NOT NULL,
  exec_date        DATE NULL,
  prev_status      TINYINT NULL,
  signal_type      TINYINT NOT NULL COMMENT '1底 3顶',
  cycle_id         INT NOT NULL,
  ref_underlying   VARCHAR(16) NULL COMMENT '参考标的',
  ref_close        DECIMAL(16,4) NULL,
  market_low_day   INT NULL,
  market_high_day  INT NULL,
  ma50_pct         DECIMAL(10,2) NULL,
  position_pct     DECIMAL(10,2) NULL,
  processed_flag   TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_task_date (task_id, signal_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期权BS回测-信号事件';
```

### 5.7 `obt_task_log` 运行日志

```sql
CREATE TABLE obt_task_log (
  id        BIGINT NOT NULL AUTO_INCREMENT,
  task_id   BIGINT NOT NULL,
  log_time  DATETIME(3) NOT NULL,
  level     VARCHAR(8) NOT NULL,
  logger    VARCHAR(64) NULL,
  message   VARCHAR(1000) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_task_time (task_id, log_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期权BS回测-运行日志';
```

### 5.8 `obt_index_kline` 指数K线缓存（000852）

```sql
CREATE TABLE obt_index_kline (
  id         BIGINT NOT NULL AUTO_INCREMENT,
  code       VARCHAR(16) NOT NULL,
  trade_date DATE NOT NULL,
  open       DECIMAL(16,4), high DECIMAL(16,4), low DECIMAL(16,4), close DECIMAL(16,4) NOT NULL,
  volume     BIGINT, amount DECIMAL(20,2),
  source     VARCHAR(16) NOT NULL DEFAULT 'EASTMONEY',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code_date (code, trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指数K线缓存（中证1000等）';
```

---

### 5.9 `obt_template` 参数模板（可选）

```sql
CREATE TABLE obt_template (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  template_no VARCHAR(40) NOT NULL,
  name        VARCHAR(64) NOT NULL,
  description VARCHAR(255) NULL,
  params_json JSON NOT NULL,
  creator     VARCHAR(64) NOT NULL,
  public_flag TINYINT NOT NULL DEFAULT 0 COMMENT '0私有 1公开',
  use_count   INT NOT NULL DEFAULT 0,
  deleted_flag TINYINT NOT NULL DEFAULT 0,
  gmt_create  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modify  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_no (template_no),
  KEY idx_creator (creator)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期权BS回测-参数模板';
```

## 6. API 设计

统一返回：`Result<T>`（项目现有）；分页返回 `Result<PageResult<T>>`，`PageResult` 含 `list/total/page/size/pages`。

### 6.1 任务

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/option/backtest/run` | 创建并异步执行 |
| GET | `/api/option/backtest/templates` | 参数模板列表 |
| POST | `/api/option/backtest/templates` | 保存参数模板 |
| GET | `/api/option/backtest/page` | 列表分页 |
| GET | `/api/option/backtest/{id}` | 任务详情 + KPI + 参数 |
| GET | `/api/option/backtest/{id}/progress` | SSE 进度（`text/event-stream`） |
| POST | `/api/option/backtest/{id}/cancel` | 取消 |
| POST | `/api/option/backtest/{id}/rerun` | 重跑（新任务） |
| POST | `/api/option/backtest/{id}/favorite?flag=1` | 收藏 |
| DELETE | `/api/option/backtest/{id}` | 逻辑删除 |
| GET | `/api/option/backtest/compare?ids=1,2,3` | 多任务对比 |

**创建请求**：

```json
{
  "taskName": "中期信号 · 全标的 · 当月/次月/当季",
  "startDate": "2017-01-01",
  "endDate": "2026-11-25",
  "initialCapital": 1000000,
  "underlyingCodes": ["159915", "588000", "510500", "510300", "000852"],
  "signalMode": "BOTH",
  "monthMode": "NEAREST",
  "strikeMode": "ITM1_ATM_OTM3",
  "dailyBuyPct": 0.05,
  "addBuyPct": 0.10,
  "addStepPct": 0.05,
  "maxPositionPct": 1.0,
  "singleTpStepPct": 1.0,
  "singleTpReducePct": 0.10,
  "accountTpStepPct": 0.5,
  "accountTpReducePct": 0.10,
  "signalSellDailyPct": 0.10,
  "strongSellPct": 0.20,
  "left1SellPct": 0.30,
  "left2SellPct": 0.30,
  "commissionPerContract": 2.5,
  "slippageBps": 0,
  "benchmarkCode": "510300",
  "rf": 0.02
}
```

**列表查询参数**：

`page`、`size`、`keyword`、`status`、`signalMode`、`underlyingCode`、`startDate`、`endDate`、`creator`、`favorite`、`sortBy`（`totalReturn|annualReturn|maxDD|sharpe|createdAt`）、`sortOrder`（`asc|desc`）。

**列表返回（示例）**：

```json
{
  "list": [{
    "id": 10248,
    "taskNo": "OBT20261125-0048",
    "taskName": "中期信号 · 全标的 · 当月/次月/当季",
    "startDate": "2017-01-01",
    "endDate": "2026-11-25",
    "actualStartDate": "2019-12-23",
    "initialCapital": 1000000,
    "finalEquity": 3846200,
    "totalReturnPct": 284.62,
    "annualReturnPct": 28.94,
    "maxDrawdownPct": -28.43,
    "sharpeRatio": 1.42,
    "calmarRatio": 1.02,
    "winRatePct": 61.8,
    "totalTrades": 2846,
    "status": 2,
    "progress": 100,
    "favorite": true,
    "creator": "bebopze",
    "durationMs": 184260,
    "gmtCreate": "2026-11-25 21:42:18"
  }],
  "total": 48, "page": 1, "size": 8
}
```

### 6.2 详情数据

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/option/backtest/{id}/equity?from=&to=` | 净值/回撤/基准曲线 |
| GET | `/api/option/backtest/{id}/stats` | 风险指标 + 分月/归因/卖出原因 |
| GET | `/api/option/backtest/{id}/signals` | 信号事件 |
| GET | `/api/option/backtest/{id}/positions?date=&underlying=&page=&size=` | 每日持仓；`date` 为空取最新 |
| GET | `/api/option/backtest/{id}/trades?from=&to=&type=&reason=&page=&size=` | 交易明细 |
| GET | `/api/option/backtest/{id}/closes?from=&to=&reason=&page=&size=` | 平仓明细 |
| GET | `/api/option/backtest/{id}/daily?from=&to=` | 每日净值表 |
| GET | `/api/option/backtest/{id}/logs?level=` | 运行日志 |
| GET | `/api/option/backtest/{id}/quality` | 数据质量报告 |
| GET | `/api/option/backtest/{id}/export?type=&format=` | 导出 |

导出：

- `type`: `task | equity | positions | trades | closes | all`
- `format`: `csv | xlsx`（xlsx 需要 EasyExcel 或 POI；若依赖不便，前端用 SheetJS 亦可）
- 文件名：`期权BS回测_{taskNo}_{type}_{yyyyMMddHHmm}.csv`
- 大表流式写出，`Content-Disposition: attachment`。

### 6.3 SSE 进度

```
event: progress
data: {"taskId":10248,"status":1,"progress":42,"phase":"BUY","currentDate":"2022-06-15","message":"建仓中"}

event: done
data: {"taskId":10248,"status":2,"finalEquity":3846200}
```

前端 `EventSource` 订阅；失败自动回退到轮询（5s）。

---

## 7. 前端设计

### 7.1 页面与路由

| 页面 | 路径 | 说明 |
|---|---|---|
| 创建 | `/option-backtest/create.html` | **已上线**：参数表单 + 规则模板 + 实时预估 + 校验 |
| 列表 | `/option-backtest/list.html` | **已上线**：分页/排序/搜索/导出/收藏 |
| 详情 | `/option-backtest/detail.html?id={taskId}` | **已上线**：KPI/图表/信号/持仓/清仓/交易/净值/参数日志 |

> 也可复用现有 `static/backtest/` 目录风格；建议独立目录，避免与股票回测混淆。

### 7.2 创建页模块与字段（补齐）

**布局**：两栏（左表单 + 右粘性摘要）。A 为长表单；B 为 5 步向导；C 为左侧步骤导航 + Mission Blueprint。三套字段完全一致，仅组织方式不同。

| 分组 | 字段 | 控件 | 默认值 | 校验/说明 |
|---|---|---|---|---|
| 01 基础 | 任务名称 | input | 中期信号 · 全标的 · 当月/次月/当季 | 必填，≤64 字 |
| 01 基础 | 标签 | input | 中期信号, 全标的, 基线 | 最多 5 个，可空 |
| 01 基础 | 备注 | textarea | 默认口径说明 | ≤500 字 |
| 02 区间 | 开始日期 | date | 2017-01-01 | 必填，≤结束日 |
| 02 区间 | 结束日期 | date | 今天 | 必填，≥开始日 |
| 02 区间 | 快捷区间 | 按钮组 | 2017 至今 | 近 1/3/5 年、全部；只改开始日 |
| 02 区间 | 初始本金 | number | 1,000,000 | ≥10,000，步长 1 万；50/100/200/500 万快捷 |
| 03 标的 | 标的集合 | 多选卡片 | 5 个全选 | 至少 1 个；卡片显示 code/交易所/期权起始/合约数/覆盖率 |
| 04 信号 | 信号模式 | 分段 | BOTH | BOTH / BOTTOM / TOP |
| 04 合约 | 合约月份 | select | 当月+次月+当季 | NEAREST / QUARTER / NEXT2 |
| 04 合约 | 行权价档位 | 多选 chips | ITM1/ATM/OTM1/OTM2/OTM3 | 至少 1 档；默认 5 档 |
| 04 合约 | 合约集合 | select | 周期内固定 | 固定 / 每日重选 |
| 04 合约 | 平值判定 | select | 最接近标的价 | 最近 / 并列取低 / 并列取高 |
| 04 合约 | 到期处理 | select | 内在价值结算 | 内在价值 / 最后收盘平仓 |
| 04 合约 | 买入最低剩余天数 | number | 30 | 严禁买入 DTE<30 的合约 |
| 04 合约 | 自动换月阈值 | number | 15 | 持仓 DTE<15 自动换月，目标 DTE≥30 |
| 05 建仓 | 每日建仓 | number(%) | 5 | 1~100 |
| 05 建仓 | 逆势加仓 | number(%) | 10 | 1~100 |
| 05 建仓 | 触发步长 | number(%) | 5 | 1~50 |
| 05 建仓 | 仓位上限 | number(%) | 100 | 10~100 |
| 05 建仓 | 加仓范围 | select | 全标的等权 | 全标的 / 仅触发标的 |
| 05 建仓 | 买满后止盈 | select | 不再买入 | 不再买 / 下一轮重新建仓 |
| 05 建仓 | 备用现金 | number(%) | 0 | 0~50 |
| 06 卖出 | 7 条规则 | 开关 + 比例 | 全开 | 右侧100 / 左侧2 30 / 左侧1 30 / 强势20 / 信号10 / 账户10 / 单票10 |
| 06 卖出 | MA/SSF 参数 | 只读 | MA5/10/20 + SSF | 与引擎一致，暂不开放改 |
| 07 执行 | 佣金 | number | 2.5 元/张/边 | ≥0 |
| 07 执行 | 滑点 | number(bps) | 0 | 0~20 |
| 07 执行 | 基准 | select | 510300 | 510300 / 等权组合 |
| 07 执行 | 无风险利率 | number(%) | 2 | 0~10 |
| 07 执行 | 引擎/数据版本 | 只读 | v1.0.0 / base_option @ 日期 | 写入任务快照 |
| 07 执行 | 年化口径 | select | 365 自然日 | 365 / 252 |

**实时预估（右栏）**：

| 预估项 | 公式 |
|---|---|
| 理论合约数 | `Σ标的 × 3 月 × 已选档位数`（默认 5×3×5=75） |
| 每日建仓金额 | `本金 × 每日建仓%` |
| 逆势单次加仓 | `本金 × 逆势加仓%` |
| 满仓交易日 | `ceil(仓位上限% / 每日建仓%)` |
| 资金分配 | 标的等权；月份等权；档位等权；显示环形/条形图 |
| 首批有效信号 | 查询信号表 + 各标的期权起始日，取第一个“有合约可交易”的底/顶信号（默认 2020-02-03） |
| 预计数据量 | 标的数 × 平均合约数 × 区间交易日（粗估，仅提示） |

**参数模板**：

| 模板 | 每日建仓 | 逆势加仓 | 步长 | 上限 | 档位 |
|---|---:|---:|---:|---:|---|
| 默认方案 | 5% | 10% | 5% | 100% | 实1/平/虚1/虚2/虚3 |
| 保守 | 5% | 5% | 8% | 60% | 实1/平 |
| 激进 | 8% | 15% | 5% | 100% | 5 档 |

**校验规则**：

1. 任务名称非空、≤64 字。
2. 开始日 ≤ 结束日；结束日 ≤ 今天。
3. 本金 ≥ 10,000；仓位上限 ≤ 100%；备用现金 + 上限 ≤ 100%（若启用）。
4. 至少 1 个标的、至少 1 个行权价档位。
5. 标的必须存在期权数据；若某标的在区间内完全无合约，提示并允许剔除。
6. 卖出规则至少启用 1 条（否则持仓无法退出）。
7. 校验失败时：字段标红 + 右侧汇总提示 + 禁用“创建”按钮。

**创建动作**：

- `POST /api/option/backtest/run` → 返回 `{taskId, taskNo, status}`。
- 成功后跳转 `/option-backtest/detail.html?id={taskId}`，详情页顶部显示进度条（SSE）。
- 支持“保存草稿”（localStorage + 服务端草稿两种）、“保存为模板”（写入 `obt_template` 或 `params_json` 模板库）、“重置”。
- 离开页面时若有未保存修改，弹出确认。

### 7.3 列表页模块

1. **页头**：标题、说明、导出、新建回测。
2. **统计卡**：累计任务、运行中、平均年化、最佳总收益（可按当前筛选动态计算）。
3. **筛选**：关键字（任务名/任务号/创建人）、信号类型、标的多选、回测区间、状态、本金区间、仅看收藏。
4. **表格**（可排序/分页/列设置/密度切换）：
   - 收藏、任务名称+任务号、回测区间、实际起始、标的 chips、本金、期末权益、总收益率、年化、最大回撤、夏普、卡玛、胜率、交易笔数、状态、耗时、创建人、创建时间、操作。
5. **批量操作**：导出所选、删除、对比。
6. **状态与空态**：加载骨架、无数据、接口异常重试。
7. **导出**：当前筛选全部或选中行，CSV。

### 7.4 详情页模块与字段

| 模块 | 字段 |
|---|---|
| 任务头 | 任务名、任务号、状态、创建人、创建时间、耗时、引擎版本、区间、实际起始、本金、信号模式、月份模式、行权价模式、标的列表 |
| 核心 KPI | 期末权益、总收益率、年化、最大回撤（起止/天数）、夏普、卡玛、胜率、交易笔数（买/卖） |
| 净值/回撤/日收益 | 日期、策略净值、基准净值、回撤、日收益；支持缩放、十字光标、导出 PNG |
| 风险指标（16 项） | 年化波动、夏普、索提诺、卡玛、最大回撤/最长回撤、盈亏比、VaR95、CVaR95、Beta/Alpha、年化换手、交易成本、最大连胜/连亏、收益偏度、月胜率 |
| 信号时间线 | 信号日、类型（底/顶）、参考标的收盘、执行日、周期号、当时 low_day/high_day、ma50_pct |
| 月度热力 | 年 × 月收益，红涨绿跌，悬浮明细 |
| 卖出原因 | 各规则触发次数、占比、平均收益贡献 |
| 收益归因 | 按标的、按月份、按行权价档位、按持有天数区间 |
| 每日持仓 | 日期、标的、合约、C/P、行权价、月份、到期日、数量、成本价、现价、成本金额、市值、浮盈、收益率、权重、持仓天数、建仓来源、虚实值 |
| 每日清仓 | 标的、合约、开/平仓日、数量、均价成本、均价卖出、持有天数、已实现盈亏、收益率、最大浮盈、平仓原因 |
| 每日交易 | 日期、时间、标的、合约、方向、成交价、数量、成交额、费用、触发规则、触发值、仓位比例、现金余额、成交后持仓 |
| 每日净值/资金 | 日期、单位净值、总权益、当日收益、累计收益、回撤、现金、持仓市值、仓位、信号状态、持仓数、费用 |
| 参数与日志 | 全量参数（分组）、运行日志（级别/时间/内容）、数据版本、引擎版本 |
| 数据质量 | 实际起始日、缺失K线合约数、零成交占比、无行情天数、指数K线来源、告警列表 |
| 导出 | PDF 报告、Excel（持仓/交易/清仓/净值分 sheet）、CSV |

### 7.5 交互细节

- 表格：服务端分页/排序/筛选；前端保留 URL 参数（`?id=&tab=&date=`），可分享。
- 图表与表格联动：点击净值某日 → 下方持仓/交易切换到该日。
- 大表虚拟滚动或分页（默认 20/50/100）。
- 所有金额千分位、收益率带正负号与红涨绿跌；缺失值显示 `—` 而非 0。
- 导出按钮带 loading + 失败重试。
- 移动端：列表卡片化，详情单列。

### 7.6 三套 UI 设计稿

已交付 3 套可交互设计稿，每套均含**创建 / 列表 / 详情**三页（详见 `src/main/resources/static/option-backtest/design/index.html`）：

| 方案 | 风格 | 适用 |
|---|---|---|
| A 深色量化终端 | 深色 + 青色 + 高密度 + 双栏导航 | 专业投研、日常盯盘 |
| B 浅色企业数据中台 | 白色卡片 + 蓝色 + 结论面板 | 团队协作、管理层汇报、PDF |
| C 深蓝驾驶舱 | 玻璃拟态 + 紫青渐变 + 实时监控 | 大屏、路演、品牌展示 |

---

## 8. 性能、可靠性与安全

### 8.1 数据量与内存

- 全量 31,738 合约、2.3M 根日K；但单次只加载 5 个标的、目标区间内合约（约 1.5 万合约 / 120 万根）。
- K 线解析为 `long[] date + float[] close`，单合约约 1~2 KB；1.5 万合约约 30 MB；避免保留 JSON 字符串。
- 按 `(underlying, month)` 预分组；仅在需要时解析该合约 K 线（惰性）。
- 峰值内存目标 < 1 GB；任务串行度 2，避免并发打爆。

### 8.2 SQL 与索引

- 合约加载：`underlying_code IN (...) AND start_date <= end AND end_date >= start`，走 `idx_underlying_date`（需补索引）。
- 持仓/交易/净值查询全部 `task_id` 前缀索引。
- 批量插入：`rewriteBatchedStatements=true`，每批 1000 行。
- 大表归档：`obt_daily_position` 单任务 ~5 万行；保留 1 年，超期转冷存储/删除。

### 8.3 可靠性

- 任务幂等：`task_no` 唯一；落库前 `DELETE FROM obt_* WHERE task_id=?`。
- 失败重试：数据加载失败重试 3 次（指数K线拉取）；引擎异常记录堆栈到 `obt_task_log` 并置 FAILED。
- 超时：单任务 10 分钟；超时置 FAILED，`error_msg=TIMEOUT`。
- 审计：参数快照 + 引擎版本 + 数据版本 + 日志。

### 8.4 安全

- 接口鉴权沿用现有体系；任务仅创建人/管理员可删除。
- 导出接口校验任务归属，防越权。
- SQL 全部参数化；`params_json` 做白名单校验。
- 限流：同一用户同时运行任务 ≤ 2。

---

## 9. 测试与验收

### 9.1 单元测试（JUnit 5）

| 用例 | 断言 |
|---|---|
| 信号识别 | 1/2/1 抖动只产生 1 个底周期；3/4/3 只产生 1 个顶周期 |
| 行权价选择 | 给定 S=3.00、K=[2.8,2.9,3.0,3.1,3.2,3.3]，C 选 2.9/3.0/3.1/3.2/3.3；P 镜像 |
| 月份选择 | 2026-11 挂牌 [202611,202612,202703,202706] → 前 3 个 |
| 5%/10% 预算 | 买满 100 万后不再买；卖后不再买 |
| 阶梯加仓 | -5.1% 触发 1 次；-10.2% 触发第 2 次；回升再跌不重复 |
| 单票止盈 | 价格 2.0×成本触发 10%；2.9× 不触发 2.0 档；3.0× 触发第 2 档 |
| 账户止盈 | 权益 1.5× 触发；1.49× 不触发 |
| 卖出优先级 | 右侧清仓当日不再执行左侧/强势 |
| 到期结算 | 认购内在价值 = max(S-K,0)×qty×multiplier |
| 费用 | 买入/卖出佣金正确，现金不为负 |
| 幂等 | 重跑同参数结果一致（浮点容差 1e-6） |

### 9.2 集成测试

- 用 2020-02-03 ~ 2020-03-23（疫情底）跑通一个完整周期，校验：首次建仓日、20 日 5% 节奏、顶部信号卖出、右侧清仓。
- 用 2024-01-31 ~ 2024-09-26（最大回撤区间）校验回撤计算与最大回撤日期。
- 5 标的 + 000852 全量跑通，确认无空指针/除零/负数量。

### 9.3 验收标准

1. 默认参数（2017-01-01 ~ 2026-11-25，100 万，5 标的）单次运行 < 60s，落库成功。
2. 创建页可完成参数配置、校验、预估并成功创建任务；列表能查询到任务；排序/分页/搜索/导出正确。
3. 详情页所有图表与 5 个表格有数据；导出 CSV/Excel 可打开且行数一致。
4. 任意交易可追溯到 `reason_code` 与触发值。
5. 数据质量页明确显示「实际起始 2019-12-23」及缺失合约统计。
6. 重跑同参数结果一致（除时间戳）。

---

## 10. 实施计划

| 阶段 | 内容 | 产出 | 预估 |
|---|---|---|---|
| P1 | 引擎骨架 + 数据加载 + 合约选择 + 买卖规则 + 指标 | `OptionBsEngine` + 单测 + CLI 实跑 | 3 人日 |
| P2 | DDL + 持久化 + 任务状态机 + API（含创建/校验/预估） | 7 张表 + REST + SSE | 2 人日 |
| P3 | 创建页 + 列表页（选中 UI 方案） | `create.html` + `list.html` + 接口联调 | 2.5 人日 |
| P4 | 详情页（图表 + 5 个表格 + 导出） | `detail.html` + 实跑报告 | 3 人日 |
| P5 | 数据质量/对比/模板/文档/验收 | 文档 + 验收清单 | 1.5 人日 |

> 实跑报告包含：净值曲线、年度收益、最大回撤区间、交易统计、分标的贡献、典型周期复盘（建仓→加仓→止盈→清仓）。

---

## 11. 待确认 / 可配置决策点

| # | 决策 | 默认 | 备选 |
|---|---|---|---|
| D1 | 次1/次2 月份口径 | 当月+次月+当季 | 当月+当季+下季 / 当月+次月+次2月 |
| D2 | 中证1000 标的 | 000852 中金所指数期权 | 剔除 / 512100 ETF（无期权，不可行） |
| D3 | 加仓作用范围 | 全标的等权 | 仅触发标的 |
| D4 | 合约集合 | 周期内固定 | 每日按最新价重选 |
| D5 | 滑点 | 0（严格收盘价） | 1~5 bps |
| D6 | 到期处理 | 内在价值结算 | 实物交割/移仓 |
| D7 | 账户止盈基准 | 权益/本金 | 权益/投入成本 |
| D8 | 新底周期预算 | 重置 100% | 仅补足至 100% |
| D9 | 顶周期内再次 3 | 不重置参考价 | 重置 |
| D10 | 000852 指数K线 | 东财拉取 + 缓存 | 手工导入 CSV |

---

## 12. 已落地实现（2026-11-26）

| 层 | 实现 |
|---|---|
| 引擎 | `com.bebopze.tdx.quant.optionbs.OptionBsEngine`：日频事件循环、周期去抖、5 档×3 月、5%/10% 建仓、7 条卖出规则、到期结算、指标 |
| 数据 | `OptionBsDataLoader`：`base_option` / `base_stock` / `qa_market_mid_cycle`；ETF 改用**未复权**腾讯日K并缓存 `obt_index_kline`；000852 指数K线缓存 |
| 落库 | `OptionBsRepository`：`obt_task` / `obt_daily_equity` / `obt_daily_position` / `obt_trade` / `obt_close_record` / `obt_signal_event` / `obt_task_log`（+ `obt_index_kline` / `obt_template`） |
| API | `OptionBsBacktestController`：`/api/option/backtest/run`、`/page`、`/{id}`、`/stats`、`/equity`、`/positions`、`/trades`、`/closes`、`/signals`、`/logs`、`/export`、`/underlyings`、`/favorite`、`/cancel` |
| 前端 | `static/option-backtest/{create,list,detail}.html` + `assets/common.css|js`；A 深色终端为默认，右上角切换 B 浅色专业模式；ECharts 图表随主题重绘 |
| 校验 | 前后端双重校验；创建后异步执行 + 轮询进度；失败返回 `error_msg` |
| 数据质量 | 未复权价、成交量>0、价格>0.001、距到期≥3 天、信号去抖；页面展示实际数据起始 |
| 基线 | [实跑报告](./实跑报告.md)：+3,352.71% / 年化 67.12% / 最大回撤 -24.78% |

**启动/复现**：`mvn -DskipTests package` → `bash scripts/deploy/restart.sh run`；命令行见实跑报告。

## 13. 附：规则速查表（一页版）

### 13.1 中期底 → 认购

| 规则 | 触发 | 动作 |
|---|---|---|
| 建仓 | 底周期每交易日 | 5% 本金，5 档 × 3 月 × 标的等权 |
| 下跌加仓 | 每 -5% | 10% 本金，等比 |
| 上限 | 成本 100% | 停止买入（卖后不再买） |
| 单票止盈 | 每 +100% | 该合约 -10% |
| 账户止盈 | 每 +50% | 全部 -10% |
| 信号卖出 | 顶周期每日 | 全部 -10% |
| 强势卖出 | 每 +5% | 该标的 -20% |
| 左侧卖1 | 每 -5% | 该标的 -30% |
| 左侧卖2 | 下MA5 / MA5空 / 下MA10 | 该标的 -30%（一次） |
| 右侧卖 | MA10空 / SSF空 / 下MA20 / MA20空 | 该标的清仓 |
| 到期换月 | 持仓 DTE <15 天 | 同标的/同行权价 → 次月/当季/次季（目标 DTE≥30） |

### 13.2 中期顶 → 认沽

| 规则 | 触发 | 动作 |
|---|---|---|
| 建仓 | 顶周期每交易日 | 5% 本金，5 档 × 3 月 × 标的等权 |
| 上涨加仓 | 每 +5% | 10% 本金，等比 |
| 单票止盈 | 每 +100% | 该合约 -10% |
| 账户止盈 | 每 +50% | 全部 -10% |
| 信号卖出 | 底周期每日 | 全部 -10% |
| 强势卖出 | 每 -5% | 该标的 -20% |
| 左侧卖1 | 每 +5% | 该标的 -30% |
| 左侧卖2 | 上MA5 / MA5多 / 上MA10 | 该标的 -30%（一次） |
| 右侧卖 | MA10多 / SSF多 / 上MA20 / MA20多 | 该标的清仓 |

---

*文档结束。UI 选型见 [design/index.html](../../src/main/resources/static/option-backtest/design/index.html)。*
