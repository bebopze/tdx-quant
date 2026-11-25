# 期权 BS 回测（Option BS Backtest）

> 基于大盘中期顶底信号，交易 ETF / 指数期权的日频回测模块。
>
> 后端：`com.bebopze.tdx.quant.optionbs` ｜ 前端：`src/main/resources/static/option-backtest/`

---

## 1. 页面

| 页面 | 地址 | 说明 |
|---|---|---|
| 创建 | `/option-backtest/create.html` | 参数配置、模板、实时预估、校验、创建并异步执行 |
| 列表 | `/option-backtest/list.html` | 分页、排序、搜索、筛选、收藏、CSV 导出 |
| 详情 | `/option-backtest/detail.html?id=22` | KPI、净值/回撤/日收益、风险指标、信号、月度热力、卖出原因、收益归因、每日持仓/清仓/交易/净值、参数日志 |

右上角 **A 终端 / B 专业** 切换主题，选择保存在 localStorage；也可用 `?theme=b` 直接打开专业模式。

基线任务：`OBT20261126-0019`（id=22），详见 [实跑报告](./实跑报告.md)。

---

## 2. 代码结构

```
src/main/java/com/bebopze/tdx/quant/optionbs/
├── OptionBsParams.java                 参数（创建页字段）
├── OptionBsModels.java                 合约/标的/信号/持仓/交易/净值模型
├── OptionBsData.java                   输入数据
├── OptionBsDataLoader.java             信号/标的/合约/指数K线加载（MyBatis-Plus Mapper）
├── OptionBsEngine.java                 回测引擎（日频）
├── OptionBsRepository.java             落库 / 查询 / 导出编排（Mapper 调用）
├── OptionBsBacktestService.java        任务编排 / 异步 / 校验
├── OptionBsBacktestController.java     REST API
├── OptionBsConfig.java                 Spring Bean
└── OptionBsCli.java                    命令行实跑入口
```

持久化分层（与项目现有风格一致）：

```
dal/entity/Obt*DO.java                   9 张表的 MyBatis-Plus 实体
dal/mapper/Obt*Mapper.java               BaseMapper + 自定义方法
resources/com/bebopze/.../mapper/Obt*Mapper.xml   自定义 SQL
```

表结构：`src/main/resources/db/optionbs.sql`

---

## 3. 启动

```bash
mvn -DskipTests package
bash scripts/deploy/restart.sh run
```

服务：`http://localhost:7001`

### 命令行实跑（不启动 Spring、不触发交易任务）

```bash
mvn dependency:build-classpath -Dmdep.outputFile=/tmp/obt-cp.txt -DincludeScope=runtime
java -Xms2g -Xmx12g \
  -cp "target/classes:$(cat /tmp/obt-cp.txt)" \
  com.bebopze.tdx.quant.optionbs.OptionBsCli 2017-01-01 2026-11-25 1000000
```

---

## 4. 核心口径

1. 信号：`qa_market_mid_cycle.market_mid_status` 1=底、3=顶，按状态跃迁识别；同一底/顶周期内去抖。
2. 合约：当月 + 次月 + 当季；实值1 / 平值 / 虚值1 / 虚值2 / 虚值3；周期内固定。
3. 建仓：每日 5%，逆势 ±5% 加 10%，累计成本 ≤ 100%。
4. 卖出：单票 +100% 减 10%；账户 +50% 减 10%；信号逐日 -10%；强势 ±5% -20%；左侧 -5% -30%；左侧 MA5/MA10 -30%；右侧 MA10/SSF/MA20 清仓。
5. 成交：收盘价，佣金 2.5 元/张/边，滑点默认 0；到期按内在价值结算。
6. 数据：ETF 使用**未复权**日K（腾讯，缓存 `obt_index_kline`），避免复权价与期权行权价错配；000852 使用中金所指数期权 + 指数日K。
7. 到期约束：**买入 DTE ≥30 天**；持仓 **DTE <15 天自动换月**（次月/当季/次季）；价格 ≤0.001、成交量 0 的合约不买。

---

## 5. 文档

- [企业级方案（架构 / 表 / API / 字段 / 规则）](./期权BS回测-企业级方案.md)
- [实跑报告（基线结果 / 年度 / 归因 / 数据修正）](./实跑报告.md)
- [设计稿（A/B/C 三套 UI 原型）](../../src/main/resources/static/option-backtest/design/index.html)

---

## 6. 已知限制

- 按收盘价成交，未建模盘口深度、涨跌停、真实成交量；深度虚值合约的成交量可能远低于回测数量。
- ETF 期权为实物交割，本回测按内在价值现金结算。
- 2020 年 COVID 行情下认沽期权贡献了大部分收益，属于尾部收益，不宜线性外推。
- 周期内合约固定；到期后不再补选同月新合约。
- 账户止盈按“权益/初始本金”计算。
