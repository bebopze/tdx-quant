# 大陆 A 股 ETF 期权真实交易终端

## 企业级产品需求规格说明书（PRD / SRS）

> 文档版本：V1.0  
> 编制日期：2026-11-12  
> 目标应用：大陆 A 股 ETF 期权  
> 前端技术：原生 HTML5 + CSS3 + 原生 JavaScript（ES Modules）  
> 后端系统：现有 `tdx-quant` Spring Boot 服务  
> 核心 API：`OptionTradeController`，基础路径 `/api/option/trade`  
> 交易通道：由现有后端封装并调用东方财富证券交易接口  
> 文档属性：开发、测试、验收及后续维护的统一需求基线

---

# 第一部分：可直接复用的最终开发 Prompt

以下 Prompt 可整体复制给后续代码生成/开发 Agent。开发时必须同时提供本文件及仓库源码，不能只依据本文件臆造 DTO 字段或业务逻辑。

```text
你是资深证券交易终端产品架构师、前端架构师和资深原生 JavaScript 工程师。

项目目标：为 bebopze/tdx-quant 仓库设计并实现一套面向中国大陆 A 股 ETF 期权市场的企业级真实交易 Web 终端。后端继续使用仓库现有 Spring Boot API；前端使用纯原生 HTML5、CSS3 和现代原生 JavaScript，不使用 React、Vue、Angular、jQuery、Bootstrap、Element、Ant Design 或其他前端 UI 框架。

一、源码审计要求
1. 必须先阅读用户提供的 tdx-quant 仓库源码，不得只阅读 OptionTradeController 后就开始实现。
2. 以 OptionTradeController 为入口，沿调用链阅读 OptionTradeService、OptionTradeServiceImpl、EastMoneyOptionTradeAPI、全部 Request/Response DTO、交易类型枚举、报价方式枚举、交易所枚举、委托状态枚举、统一 Result、异常处理、限流器、StockUtil 等相关实现。
3. 以仓库当前实际代码为准。所有字段名、字段类型、枚举值、URL、HTTP Method、参数默认值、返回结构和业务行为必须由源码证明。
4. 任何源码未确认的内容必须标记“待核实”，严禁自行猜测。
5. 必须区分：Controller 当前真实暴露的接口、Service 内部方法、已注释/Deprecated 的旧接口。只有当前真实暴露的 Controller 路由才能被标记为现有 API 功能。

二、不可删减的功能范围
必须 100% 覆盖 OptionTradeController 当前全部活动路由。不得以“简化 MVP”“暂不支持”“非核心功能”为理由删减：
- 账户资金汇总 + ETF 期权持仓列表
- 可交易期权合约列表搜索（标的 ETF、认购/认沽、合约编码/名称关键词）
- 期权实时行情、买 5/卖 5 五档行情和数量
- 最大可交易数量查询
- 买入/卖出四类交易业务的普通下单
- 五种报价方式
- 当日委托列表
- 历史委托列表（支持起止日期，包含当日数据）
- 全部可撤委托列表
- 批量撤单
- 一键清仓
- 勾选合约的一键等比卖出，包含卖出持仓比例、当前价偏移、昨收价偏移三个参数
- 账户总仓位一键等比减仓
- 当前持仓一键等比减仓
- 一键撤单
- 快捷分档/梯次下单

三、接口约束
1. 所有现有 API 均通过 `/api/option/trade` 调用。
2. 不得让浏览器直接请求东方财富交易站点，不得把 Cookie、SID、validatekey、券商账户凭据写进 HTML/JS、localStorage、sessionStorage 或构建产物。
3. 必须逐条遵循 Controller 的 HTTP Method 和参数位置；不得擅自把现有 GET 路由改成 POST，也不得把 GET 交易路由用于自动轮询、预取或浏览器缓存。
4. 统一解析 Result<T>：data、success、code、msg；同时兼容 HTTP 500 携带统一 Result 错误对象的情况。
5. 普通下单必须支持调用 getMaxTradeCount 进行下单前数量核验；不得将该查询伪装成券商已接受委托。
6. 下单成功返回委托编号仅代表提交委托，不代表成交。委托结果必须通过当日委托接口再次核验。
7. 撤单提交结果必须逐笔展示。撤单提交成功不代表最终撤单状态已经确认，必须刷新委托列表确认。

四、交易业务映射
交易类型：
- BUY_OPEN = 400 = 买入开仓
- SELL_CLOSE = 401 = 卖出平仓
- SELL_OPEN = 402 = 卖出开仓
- BUY_CLOSE = 403 = 买入平仓

报价方式：
- LMT = 130 = 限价
- LOK = 131 = 限价即全成否则撤
- FAL = 132 = 市价剩转限价
- FOK = 133 = 市价即全成否则撤
- FAK = 134 = 市价即成剩撤

交易所：
- 05 = 深交所
- 15 = 上交所

五、页面要求
设计为完整交易工作台，至少包含：
1. 顶部系统状态区：后端连通状态、账户数据刷新时间、行情刷新时间、交易时段提示、未确认操作提示。
2. 账户总览：账户总资产、持仓市值、可用资金、浮动盈亏、持仓比例等源 DTO 可提供指标。
3. 持仓区：全部期权持仓、合约方向、认购/认沽、持仓数量、可用数量、成本价、现价、成本、市值、盈亏、到期日/剩余天数、行权价、标的 ETF、交易所。
4. 合约检索区：标的 ETF、认购/认沽、合约代码或合约名称关键词检索，搜索结果可直接选中。
5. 实时行情区：当前价、昨收、开高低、涨跌、涨跌幅、成交量/额、更新时间、涨跌停/价格笼子信息，以及买 1~5、卖 1~5、各档数量。
6. 普通下单区：四种交易类型、五种报价方式、价格、数量、最大可交易数量提示、预计委托金额、订单确认摘要。
7. 当日委托区：全部字段、委托状态、成交数量、撤单数量、委托金额、成交金额、逐条勾选、批量撤单。
8. 历史委托区：日期范围、查询、结果列表、空数据和异常状态。
9. 可撤委托区：仅显示后端 getRevokeList 返回的可撤列表，支持全选、反选、批量撤单。
10. 快捷操作区：一键清仓、勾选合约等比卖出、账户总仓位等比减仓、当前持仓等比减仓、一键撤单、快捷分档下单。
11. 交易回执/操作审计区：用户操作、参数摘要、提交时间、接口响应、委托编号、逐笔成功/失败和后续确认状态。

六、原生前端工程要求
1. 纯原生 HTML/CSS/JavaScript；使用 ES Modules、fetch、AbortController、URLSearchParams、Intl.NumberFormat 等现代浏览器原生 API。
2. 推荐拆分为 index.html、assets/css、assets/js/api、services、components、pages、utils 等文件。
3. 页面必须支持桌面端主交易工作台与平板/窄屏降级布局；不能依赖第三方图标库或 CSS 框架。
4. 对所有交易操作使用显式确认弹窗，清楚显示合约、方向、数量、价格、报价方式、资金/持仓影响和参数单位。
5. 对一键清仓、一键撤单、等比卖出、分档下单等批量或不可逆操作，展示将影响的合约/委托数量及逐项结果；没有预览 API 的地方必须明确标记为估算值，不能伪装为后端精确预览。
6. 提交过程中禁用重复点击；网络超时后不得自动重发交易请求，必须提示用户先刷新委托确认是否已提交。
7. 不得把“接口请求成功”“委托已提交”“委托已成交”“撤单已确认”合并成一个状态。
8. 页面所有表格必须具备加载、空态、错误、刷新、排序/筛选、长列表滚动和响应式布局。

七、源码缺陷处理
源码若存在业务校验、数量取整、订单方向、状态判定、等待/重试等疑点：
- 必须列为源码核验项或阻断项；
- 不得在前端静默修正业务算法；
- 未经用户确认，不得擅自修改后端交易逻辑；
- 若某项源码行为有明显风险，必须在需求文档、页面交互和测试用例中显式呈现。

八、验收
1. 15 个活动 API 路由必须逐条可追踪到页面入口、调用函数和验收用例，覆盖率必须 100%。
2. 四类交易类型、五种报价方式均能正确映射。
3. 所有参数单位、默认值、日期格式、合约编码、交易所编码正确。
4. 交易和撤单必须有二次确认、请求防重、逐笔回执、委托刷新确认。
5. 不得出现模拟交易、虚拟资金、虚拟委托作为真实功能替代品。
6. 提交完整可运行源码、部署说明、接口映射、测试用例和问题清单。

先输出源码审计与功能映射，再输出页面结构与交互方案，最后实现全部代码。不得遗漏、弱化或虚构现有线上 API 功能。
```

---

# 第二部分：产品需求规格说明书

## 1. 项目概述

### 1.1 项目背景

本项目基于现有 `bebopze/tdx-quant` 后端工程，为大陆 A 股 ETF 期权交易提供一个统一、完整、可视化的 Web 操作终端。系统不重新实现券商交易协议，而是通过既有 Spring Boot Controller 调用已有 Service 和东方财富交易 API。

现有交易入口为：

- Controller：`com.bebopze.tdx.quant.web.OptionTradeController`
- API 根路径：`/api/option/trade`
- Service：`OptionTradeService` / `OptionTradeServiceImpl`
- 券商 API 封装：`EastMoneyOptionTradeAPI`

### 1.2 产品目标

1. 将期权账户资金、持仓、行情、合约、委托和交易操作集中在一个交易工作台。
2. 完整呈现 `OptionTradeController` 当前已经开放的所有线上真实交易能力。
3. 通过合约搜索、五档行情、最大可交易数量查询和订单确认降低误操作概率。
4. 对所有下单、撤单、清仓、减仓和快捷下单请求提供清晰的操作过程、逐笔结果和状态回查。
5. 前端保持轻量、原生、可独立部署，避免引入大型前端工程框架。

### 1.3 成功标准

| 指标 | 验收目标 |
|---|---|
| Controller 活动路由覆盖率 | 15 / 15，100% |
| 四种交易业务覆盖率 | 4 / 4，100% |
| 五种报价方式覆盖率 | 5 / 5，100% |
| 账户/持仓/委托/行情数据呈现 | 所有已由 DTO 提供的字段均有对应展示或详情入口 |
| 真实 API 接入 | 所有动作请求到现有后端，不伪造成功状态 |
| 交易回执 | 下单编号、逐笔撤单结果、后续委托状态可区分 |
| 前端技术 | 不引入 React/Vue/Angular/jQuery/Bootstrap 等框架 |

### 1.4 非目标

本项目不在前端重写东方财富交易协议，不在浏览器中持有券商 Cookie/SID，不新增未经确认的交易策略，不把内部 Service 私有方法误认为 Controller 已开放接口，也不把已注释的旧接口算作当前可调用 API。

---

## 2. 源码审计范围与可信度说明

### 2.1 本次已直接核对的源码

本次需求基线已通过仓库文件读取能力核对以下核心文件及调用链：

| 文件 | 核对目的 |
|---|---|
| `web/OptionTradeController.java` | 15 个活动 HTTP 路由、Method、参数、默认值 |
| `service/OptionTradeService.java` | Service 能力、方法签名、注释说明 |
| `service/impl/OptionTradeServiceImpl.java` | 下单、撤单、清仓、等比减仓、快捷分档逻辑 |
| `client/EastMoneyOptionTradeAPI.java` | 东方财富账户、持仓、行情、委托、下单、撤单 API 封装 |
| `OptionTradeBSParam.java` | 普通 B/S 下单参数 |
| `OptionTradeRevokeOrdersParam.java` | 撤单参数 |
| `OptionGetMaxTradeCountReq.java` | 最大可交易数量查询参数 |
| `OptionSubmitTradeReq.java` | 券商下单字段转换 |
| `OptionRevokeOrdersReq.java` | 批量撤单请求字段 |
| `SearchOptionDictResp.java` | 期权合约搜索响应 |
| `OptionGetAccountFundsResp.java` | 账户资金汇总响应及派生指标 |
| `OptionGetGGQQPositionResp.java` | 持仓列表字段和派生指标 |
| `OptionGetOrderResp.java` | 委托单字段 |
| `OptionGetMaxTradeCountResp.java` | 最大可交易数量响应 |
| `OptionRevokeOrderResultDTO.java` | 逐笔撤单结果 |
| `SHSZQuoteSnapshotResp.java` | 实时行情、买卖五档、价格限制 |
| `OptionTradeTypeEnum.java` | 四类期权交易类型 |
| `OptionPriceTypeEnum.java` | 五种报价方式 |
| `OptionMarketTypeEnum.java` | 沪深交易所编码 |
| `BSOrderStatusEnum.java` | 委托状态分类 |
| `Result.java` | 统一接口返回对象 |
| `GlobalExceptionHandler.java` | 统一异常返回 |
| `StockTradingRateLimiter.java` | 交易限流相关行为 |
| `StockUtil.java` | 数量取整相关行为 |

### 2.2 阅读范围限制

当前执行环境无法通过 `git clone` 完整拉取 GitHub 仓库（运行环境 DNS 无法解析 `github.com`）。因此，本次不是对仓库每一个无关模块、每一个 Java 文件的逐行完整审计；而是对 `OptionTradeController` 真实功能及其直接调用链、核心 DTO/枚举/异常/限流/数量工具进行了针对性源码审计。

开发进入实施阶段前，必须在可访问仓库的环境内补充完整源码审计，并锁定实际部署分支与 commit SHA。需求文档中的源码引用以仓库 `main` 分支链接为导航，最终实施以部署环境实际 commit 为准。

### 2.3 源码引用

- [tdx-quant 仓库](https://github.com/bebopze/tdx-quant)
- [OptionTradeController.java](https://github.com/bebopze/tdx-quant/blob/main/src/main/java/com/bebopze/tdx/quant/web/OptionTradeController.java)
- [OptionTradeService.java](https://github.com/bebopze/tdx-quant/blob/main/src/main/java/com/bebopze/tdx/quant/service/OptionTradeService.java)
- [OptionTradeServiceImpl.java](https://github.com/bebopze/tdx-quant/blob/main/src/main/java/com/bebopze/tdx/quant/service/impl/OptionTradeServiceImpl.java)
- [EastMoneyOptionTradeAPI.java](https://github.com/bebopze/tdx-quant/blob/main/src/main/java/com/bebopze/tdx/quant/client/EastMoneyOptionTradeAPI.java)
- [OptionTradeBSParam.java](https://github.com/bebopze/tdx-quant/blob/main/src/main/java/com/bebopze/tdx/quant/common/domain/param/OptionTradeBSParam.java)
- [OptionGetAccountFundsResp.java](https://github.com/bebopze/tdx-quant/blob/main/src/main/java/com/bebopze/tdx/quant/common/domain/trade/option/resp/OptionGetAccountFundsResp.java)
- [OptionGetGGQQPositionResp.java](https://github.com/bebopze/tdx-quant/blob/main/src/main/java/com/bebopze/tdx/quant/common/domain/trade/option/resp/OptionGetGGQQPositionResp.java)
- [OptionGetOrderResp.java](https://github.com/bebopze/tdx-quant/blob/main/src/main/java/com/bebopze/tdx/quant/common/domain/trade/option/resp/OptionGetOrderResp.java)
- [SHSZQuoteSnapshotResp.java](https://github.com/bebopze/tdx-quant/blob/main/src/main/java/com/bebopze/tdx/quant/common/domain/trade/resp/SHSZQuoteSnapshotResp.java)

---

## 3. 产品范围与功能边界

### 3.1 必须实现的全部活动功能

以下 15 个路由是 `OptionTradeController` 当前活动代码中实际暴露的接口，全部属于本项目强制范围。

| # | 功能 | HTTP | API 路径 | 级别 |
|---:|---|---|---|---|
| 1 | 账户资金 + 期权持仓 | GET | `/getAccountFundsAndPositions` | 查询 |
| 2 | 搜索可用期权合约 | GET | `/searchOptionDict` | 查询 |
| 3 | 期权实时行情/买卖五档 | GET | `/SHSZQuoteSnapshot` | 查询 |
| 4 | 最大可交易数量 | POST | `/getMaxTradeCount` | 查询/下单前校验 |
| 5 | 普通买入/卖出下单 | POST | `/bs` | 真实交易 |
| 6 | 当日委托列表 | GET | `/getTodayOrders` | 查询 |
| 7 | 历史委托列表（含当日） | GET | `/getHisOrders` | 查询 |
| 8 | 全部可撤委托列表 | GET | `/getRevokeList` | 查询 |
| 9 | 批量撤单 | POST | `/revokeOrders` | 真实交易操作 |
| 10 | 一键清仓 | GET | `/quick/clearPosition` | 批量真实交易 |
| 11 | 勾选合约等比卖出 | GET | `/quick/sellPosition` | 批量真实交易 |
| 12 | 账户总仓位等比减仓 | GET | `/quick/totalAccount/eqRatio/sellPosition` | 批量真实交易 |
| 13 | 当前持仓等比减仓 | GET | `/quick/currPos/eqRatio/sellPosition` | 批量真实交易 |
| 14 | 一键撤单 | GET | `/quick/cancelOrder` | 批量真实交易操作 |
| 15 | 快捷分档/梯次下单 | GET | `/quick/quickOption` | 批量真实交易 |

### 3.2 不属于当前 Controller 活动 API 的代码

Controller 中存在被注释的旧接口，例如旧版一键买入/调仓换股、等比买入、保留已有仓位再买入、成本估算等；这些代码当前不是可通过 `OptionTradeController` 路由调用的活动 API。不得把它们标成已经具备的线上功能，也不得自行将其加入页面并虚构请求地址。

Service 中还存在未由该 Controller 暴露的内部方法，例如 `quickClearPosition(Set<String>)`、`quickBuyPosition(...)` 等。它们可以作为源码理解材料，但不能作为现有 HTTP 页面按钮的直接调用目标。

---

## 4. 系统总体架构

### 4.1 调用关系

```text
浏览器（原生 HTML/CSS/JavaScript）
          |
          | fetch('/api/option/trade/...')
          v
Spring Boot Controller
OptionTradeController
          |
          v
OptionTradeService
OptionTradeServiceImpl
          |
          v
EastMoneyOptionTradeAPI
          |
          v
东方财富证券交易 API / 行情 API
```

### 4.2 前后端职责

**前端负责：** 页面结构、表单输入、交互校验、行情展示、资金持仓展示、订单列表、用户确认、请求防重、请求结果展示、状态回查、操作审计展示。

**后端负责：** 现有 DTO 转换、交易类型映射、报价方式映射、交易所映射、东方财富会话、Cookie/SID、券商协议请求、后端业务校验、委托提交、撤单执行和限流。

**明确禁止：** 浏览器直接访问 `jywg.18.cn` 或 `emhsmarketwgmix.eastmoneysec.com` 的交易接口；浏览器不得保存或拼接 `SID`、`COOKIE`、`validatekey`、券商账户账号或其他会话凭据。

### 4.3 后端基础地址

前端统一配置：

```js
const API_BASE_URL = '/api/option/trade';
```

如前后端跨域部署，必须由部署环境提供受控的 `API_BASE_URL` 配置及 CORS/凭据策略。优先采用同源部署或反向代理，避免把券商会话暴露到浏览器。

### 4.4 统一返回对象

现有 `Result<T>` 结构：

```json
{
  "data": {},
  "success": true,
  "code": 0,
  "msg": "操作成功",
  "totalNum": 0,
  "pageIndex": 0,
  "pageSize": 0,
  "totalPage": 0
}
```

`totalNum/pageIndex/pageSize/totalPage` 为可选字段。列表类接口可能包含 `totalNum`；前端不能假设每个响应都有分页元数据。

异常处理器在部分异常情况下仍以 HTTP 500 返回 `Result` JSON。因此 API 层必须同时检查 HTTP 状态和业务字段 `success`，并尽可能读取 JSON `msg`；不能只按 HTTP 200/非 200 判断。

---

## 5. 页面信息架构与导航

### 5.1 主工作台

主工作台采用桌面端优先的交易终端布局：

1. 顶部全局状态栏。
2. 账户资金总览卡片。
3. 主要内容区分为：持仓区、合约/行情区、下单区。
4. 底部委托管理区，支持当日委托、历史委托、可撤委托切换。
5. 快捷操作面板，提供一键撤单、清仓、等比卖出、等比减仓和分档下单。
6. 操作回执/错误信息区。

### 5.2 功能导航

| 导航 | 包含内容 |
|---|---|
| 交易总览 | 账户资金、持仓概览、行情状态、未完成委托摘要 |
| 我的持仓 | 全部 ETF 期权持仓、持仓筛选、勾选合约 |
| 合约与行情 | 标的 ETF 筛选、认购/认沽、合约搜索、买卖五档 |
| 普通下单 | 四类交易类型、五种报价方式、数量校验、委托确认 |
| 当日委托 | 当日委托、状态、成交情况、批量撤单 |
| 历史委托 | 起止日期查询、历史数据列表 |
| 可撤委托 | 后端可撤列表、选择撤单 |
| 快捷交易 | 一键清仓、勾选等比卖出、两种等比减仓、分档下单 |
| 操作记录 | 本次页面会话中的操作请求、回执、状态回查结果 |

### 5.3 顶部状态栏

应包含：

- 后端 API 连通状态（最近一次请求结果）。
- 账户数据最近成功刷新时间。
- 当前行情合约和行情时间。
- 最近一次交易/撤单操作状态。
- 请求执行中数量。
- 当前页面运行环境标识（生产/测试配置应来自部署配置，不能由用户随意切换到未知地址）。

注意：现有 `OptionTradeController` 没有独立的登录状态/会话健康检查接口。顶部状态不能在未调用接口时自行宣称券商已登录；只能显示“最近一次需要账户会话的接口调用成功/失败”。

---

## 6. 全部 API 接口规格与页面映射

以下路径均以 `/api/option/trade` 为前缀。

### 6.1 账户资金 + 持仓

**接口**

```http
GET /getAccountFundsAndPositions
```

**响应类型**：`OptionGetAccountFundsResp`

**主要账户字段**：

| 字段 | 含义 |
|---|---|
| `Zczz` | 账户总资产 |
| `Sz` | 账户持仓总市值 |
| `Fdyk` | 账户浮动总盈亏 |
| `Ccyk` | 持仓盈亏字段 |
| `Zjkyje` | 可用资金 |
| `Zjkqje` | 可用/可取资金相关字段，具体展示名应按真实业务口径确认 |
| `Zjdtky` | 资金冻结/可用相关字段，展示前按实际券商语义核对 |
| `optionPositionList` | 期权持仓列表 |
| `totalAccount__actTotalPosRatio` | 派生总持仓比例，源码按 `Sz / Zczz` 计算 |
| `max_buyPosPct` | 派生剩余可买仓位比例，源码按 `Zjkyje / Zczz * 100` 计算 |

**持仓字段**（`OptionGetGGQQPositionResp`）：

- 合约：`Hybm`、`Hydm`、`Hyjc`。
- 标的 ETF：`bdzqdm`。
- 交易所：`Jybk`、`Jybkmc`、`Jysc`。
- 持仓性质：`Ccfx`（权利仓/义务仓）、`Hylx`（认购/认沽）、`Mm`。
- 数量：`Hysjcc`（持仓数量）、`Hykysl`（可用数量）。
- 价值：`Hysz`（合约市值）、`Hymrcb`（合约买入成本）。
- 盈亏：`Fdyk`、`Zyk`、`Ljpcyk` 等源字段。
- 合约属性：`xqj`（行权价）、`qqxqr`（行权日）、`syts`（剩余自然日）。
- 派生：`buyPrice = Hymrcb / Hysjcc`、`currentPrice = Hysz / Hysjcc`、`posRatio = Hysz / accountAssets`。

**页面要求**

- 展示完整账户资金摘要和全部持仓。
- 持仓表支持按标的 ETF、合约代码、合约名称、认购/认沽、权利仓/义务仓、剩余天数和盈亏筛选。
- 支持勾选持仓行，为“勾选合约等比卖出”提供合约编码列表。
- 展示可用数量与总持仓数量，不得把二者混为一列。
- `posRatio` 是持仓市值除以账户总资产的比例，页面显示时换算成百分数；不要误标为持仓数量占比。

### 6.2 可用期权合约搜索

**接口**

```http
GET /searchOptionDict?targetCode={ETF代码}&callOrPut={C或P}&id={关键词}
```

**参数**

| 参数 | 必选 | 含义 |
|---|---|---|
| `targetCode` | 否 | 标的 ETF 代码，例如 `588000`、`159915`、`510500` |
| `callOrPut` | 否 | `C` 认购；`P` 认沽 |
| `id` | 否 | 合约编码或合约名称关键词模糊搜索 |

**响应类型**：`List<SearchOptionDictResp>`

| 字段 | 含义 |
|---|---|
| `s` | 合约编码，例如 `10011565` |
| `c` | 合约代码，例如 `588000C2612M01900` |
| `n` | 合约名称 |
| `cp` | `C`/`P` |
| `ep` | 行权价格 |
| `us` | 标的 ETF 代码 |
| `un` | 标的 ETF 名称 |
| `m`、`um`、`ut` | 上游原始扩展字段，需按实际数据含义展示或放入详情，不得擅自命名 |

**页面要求**

- 支持只按标的 ETF 检索、只按认购/认沽检索、按合约代码/名称关键词检索，以及多条件组合检索。
- 搜索结果展示标的 ETF、合约简称、合约代码、合约编码、认购/认沽、行权价。
- 点击结果后将所选 `s` 作为行情查询 `optionCode`，并填充普通下单和快捷分档下单的合约编码。
- 不允许将用户搜索关键词直接拼接到未编码 URL；使用 `URLSearchParams`。

### 6.3 期权实时行情 / 买五卖五

**接口**

```http
GET /SHSZQuoteSnapshot?optionCode={合约编码}
```

**响应类型**：`SHSZQuoteSnapshotResp`

**行情字段**

- 基础：`code`、`name`、`sname`。
- 价格限制：`topprice`、`bottomprice`、`nt`、`nb`。
- 买卖五档：`fivequote.sale1` ~ `sale5`、`fivequote.buy1` ~ `buy5`。
- 五档数量：`sale1_count` ~ `sale5_count`、`buy1_count` ~ `buy5_count`。
- 昨收/开盘：`yesClosePrice`、`yesSettlePrice`、`openPrice`。
- 实时：`currentPrice`、`open`、`high`、`low`、`avg`、`zd`、`zdf`、`turnover`、`volume`、`amount`、`wp`、`np`、`date`、`time`。
- 价格笼子：`pricelimit.upper`、`pricelimit.lower`。

**页面要求**

- 卖盘显示卖 5 至卖 1，买盘显示买 1 至买 5；价格和对应数量必须同档显示。
- 实时价、昨收、涨跌额、涨跌幅、开高低、成交量额、行情时间明确展示。
- `topprice/bottomprice` 与 `pricelimit.upper/lower` 必须分开展示，不得未经验证合并为同一种限制价格。
- 行情刷新失败时保留上一次数据但明确标注“行情已过期”，不得让用户误以为是实时数据。
- 用户切换合约时，必须取消或忽略旧合约尚未返回的行情请求，避免旧响应覆盖新合约行情。

### 6.4 最大可交易数量

**接口**

```http
POST /getMaxTradeCount
Content-Type: application/json
```

请求类型：`OptionGetMaxTradeCountReq`

```json
{
  "zqyw": 400,
  "ywxw": 130,
  "jybk": "15",
  "hybm": "10011565",
  "wtjg": 0.0037
}
```

字段：

| 字段 | 含义 |
|---|---|
| `zqyw` | 交易类型编码：400/401/402/403 |
| `ywxw` | 报价方式编码：130/131/132/133/134 |
| `jybk` | 交易所：05 深交所；15 上交所 |
| `hybm` | 合约编码 |
| `wtjg` | 委托价格 |

响应类型：`OptionGetMaxTradeCountResp`，主要字段：

- `Wtsl`
- `Zdwtsl`（源码注释为最大可开仓量）
- `Hybm`、`Jybk`、`Zqdm`、`Wtsl`、`Zdwtsl` 等。

**页面要求**

- 合约、交易类型、报价方式或价格变化后，允许重新查询最大可交易数量。
- 页面显示券商返回的原始数量字段及其字段语义。
- 对“最大允许数量”的确切展示字段，应按对应交易类型及上游返回语义核实；不能把开仓上限无条件套用为平仓可用数量。
- 该接口是下单前的数量查询，并不提交委托。

### 6.5 普通买入/卖出下单

**接口**

```http
POST /bs
Content-Type: application/json
```

请求类型：`OptionTradeBSParam`

```json
{
  "etfCode": "588000",
  "etfName": "科创50ETF华夏",
  "hybh": "10011565",
  "price": 0.0037,
  "amount": 1,
  "optionTradeTypeEnum": "BUY_OPEN",
  "optionPriceTypeEnum": "LMT"
}
```

字段：

| 字段 | 含义 | 必填 |
|---|---|---|
| `etfCode` | 标的 ETF 代码 | 是 |
| `etfName` | 标的 ETF 名称 | 否 |
| `hybh` | 期权合约编码 | 是 |
| `price` | 委托价格，BigDecimal | 是 |
| `amount` | 委托数量，Integer | 是 |
| `optionTradeTypeEnum` | 四类交易类型枚举 | 是 |
| `optionPriceTypeEnum` | 五种报价方式枚举 | 是 |

**交易类型枚举**

| 枚举值 | 券商编码 | 中文名称 |
|---|---:|---|
| `BUY_OPEN` | 400 | 买入开仓 |
| `SELL_CLOSE` | 401 | 卖出平仓 |
| `SELL_OPEN` | 402 | 卖出开仓 |
| `BUY_CLOSE` | 403 | 买入平仓 |

**报价方式枚举**

| 枚举值 | 券商编码 | 中文名称 |
|---|---:|---|
| `LMT` | 130 | 限价 |
| `LOK` | 131 | 限价即全成否则撤 |
| `FAL` | 132 | 市价剩转限价 |
| `FOK` | 133 | 市价即全成否则撤 |
| `FAK` | 134 | 市价即成剩撤 |

**页面要求**

- 四类交易类型均可选择，不能只做买入和卖出两个泛化按钮。
- 五种报价方式均可选择，必须呈现对应全称和执行含义。
- 下单确认摘要必须列出标的 ETF、合约、合约名称、方向、报价方式、价格、数量、估算委托金额。
- 价格显示按期权行情的四位小数精度习惯呈现；实际校验仍以后端及券商返回为准。
- `bs` 返回 `Integer` 委托编号。返回委托编号后，界面状态为“委托已提交/待确认”，不得直接显示为“已成交”。
- 网络超时或浏览器中断时，不得自动重复 POST；先刷新当日委托，查找是否已经产生对应委托。

### 6.6 当日委托列表

**接口**

```http
GET /getTodayOrders
```

响应：`List<OptionGetOrderResp>`。

重要字段：

- 标的：`Bdzqdm`、`Bdzqmc`、`Bdzqlb`。
- 合约：`Hybm`、`Hydm`、`Hyjc`、`Hylx`。
- 委托：`Htxh`、`Jybk`、`Wtjg`、`Wtsl`、`Wtrq`、`Wtsj`、`Wtph`、`Wtzt`。
- 业务：`Zqyw`、`Zqywcode`、`Zqywxw`、`Kp`、`Mm`。
- 成交：`Ycjsl`、`Ycjje`、`Ycdsl`、`Wtdjje`、`Wtjdje`、`Wtje`。
- 自定义：`posPct`、`accountAssets`。

**页面要求**

- 显示委托日期/时间、标的 ETF、期权合约、买卖业务、开平、报价方式、委托价格、委托数量、已成交数量、已撤数量、委托状态、委托金额和仓位占比（若返回有效值）。
- 支持状态筛选、合约筛选、交易方向筛选、时间排序。
- 支持勾选委托，调用批量撤单接口。
- 委托状态必须保留券商原文，状态颜色仅用于辅助辨识，不可改变状态含义。

### 6.7 历史委托列表（含当日）

**接口**

```http
GET /getHisOrders?startDate=2026-11-01&endDate=2026-11-12
```

**参数**

- `startDate`：必填，`YYYY-MM-DD`。
- `endDate`：必填，`YYYY-MM-DD`。

Service 会向历史委托 API 查询历史数据；如果日期区间包含今天，还会调用当日委托列表，并按标的代码、合约代码、合同编号、委托日期、委托时间组合去重后合并。

**页面要求**

- 提供起始日期和结束日期。
- 起止日期不能为空，且起始日期不得晚于结束日期。
- 日期选择按浏览器本地日期录入，提交格式严格为 `YYYY-MM-DD`。
- 历史委托表格字段与当日委托统一，保证切换 Tab 不丢失字段含义。
- 显示查询区间、记录数、加载时间和查询错误。

### 6.8 全部可撤委托列表

**接口**

```http
GET /getRevokeList
```

Service 当前基于当日委托列表，按 `BSOrderStatusEnum.unfilled(wtzt)` 过滤。

已定义委托状态：

- 未成交类：待报、未报、已报、部成。
- 已完成/不可撤类：已撤、已成、废单、部撤。

源码中的 `unfilled` 实现是 `!filled(wtzt)`，因此未知状态也会落入“未完成”分支。页面应原样标注未知状态，并提示用户核查，不能擅自将其解释为确定可撤。

**页面要求**

- 可撤委托区以该接口结果为准，不自行从全部订单用另一套算法推算可撤列表。
- 支持单选、全选、反选和批量撤单。
- 未知委托状态必须显著显示“未知状态/需核验”。

### 6.9 批量撤单

**接口**

```http
POST /revokeOrders
Content-Type: application/json
```

请求体是 `List<OptionTradeRevokeOrdersParam>`，每条包含：

- `Jybk`：交易所编码，05 深交所 / 15 上交所。
- `Htxh`：合同编号。

Service 将撤单列表按每批 5 条拆分后调用券商 API。请求转换为 `wtbhs` 字符串，格式为 `交易所-委托编号,交易所-委托编号`。

响应：`List<OptionRevokeOrderResultDTO>`，主要字段：

- `Htxh`、`Jybk`、`Wtjg`、`Wtph`、`Wtsl`、`Zqyw`、`Zqywxw`。
- `Errcode`、`Errmsg`、`Id`、`Recsum`、`Type`。
- `suc`：Service 转换出的成功标记，源码按 `Errcode == 0` 设置。

**页面要求**

- 请求前弹窗显示选中委托总数及逐笔委托摘要。
- 每条委托按返回的 `suc` 展示成功/失败，并显示 `Errmsg`。
- 批次中部分成功、部分失败时必须呈现逐笔结果，不能只弹“批量撤单成功”。
- 撤单完成后刷新当日委托和可撤委托列表。
- UI 必须明确区分“撤单请求返回成功”和“委托状态已更新为已撤”。

### 6.10 一键清仓

**接口**

```http
GET /quick/clearPosition
```

源码当前实现流程：

1. 调用 `quickCancelOrder()`。
2. 重新读取账户资金和期权持仓。
3. 对持仓列表调用清仓卖出逻辑。
4. 清仓卖出路径使用 `SELL_CLOSE` + `LMT`。
5. 卖出价格策略基于实时价格或偏移后的价格，再乘以 `0.995`，价格精度按四位小数处理。

**页面要求**

- 设置独立、明显的一键清仓操作入口。
- 弹窗明确提示该接口会先执行一键撤单，再对全部当前持仓提交卖出平仓委托。
- 展示清仓前最近一次成功取得的持仓数量、合约列表和可用数量；数据必须标记采样时间。
- 如果前端仅能从已有持仓数据估算影响范围，必须注明“基于最近一次持仓快照，实际以服务端执行时持仓为准”。
- 用户二次确认后只允许提交一次；请求过程中禁用按钮。
- 请求返回后立即刷新账户、持仓、当日委托和可撤委托，并把各阶段状态分开展示。
- 当前接口返回 `Result<Void>`，没有逐笔卖出委托列表；不能将接口 `success=true` 解释成全部持仓已经成交清空。

### 6.11 勾选合约一键等比卖出

**接口**

```http
GET /quick/sellPosition
  ?sellStockCodeList=10011565,10011566
  &sellPosPct=50.0
  &currPricePct=0.0
  &prevPricePct=0.0
```

**参数**

| 参数 | 必选 | 默认值 | 含义 |
|---|---|---:|---|
| `sellStockCodeList` | 是 | 无 | 卖出合约编码列表，逗号分隔 |
| `sellPosPct` | 否 | 100.0 | 卖出持仓比例百分数，100 表示全部可用持仓，50 表示按比例卖出 |
| `currPricePct` | 否 | 0.0 | 当前价格偏移百分比，0 为实时价格 |
| `prevPricePct` | 否 | 0.0 | 昨日收盘价偏移百分比，0 为昨收 |

源码 `sellPriceStrategyPct` 中：当 `prevPricePct != 0` 时使用昨日收盘价偏移；否则使用当前实时价偏移。因此两个偏移字段同时非零时，当前实现以 `prevPricePct` 分支为准。

**页面要求**

- 仅将用户勾选的合约编码传入 `sellStockCodeList`，不得误传标的 ETF 代码。
- `sellPosPct` 输入单位为百分数，不是 0~1 小数。
- 为 `currPricePct` 和 `prevPricePct` 提供独立输入，并明确“若昨收偏移非 0，当前源码优先走昨收价格分支”。
- 确认弹窗显示勾选合约、可用数量、卖出比例、价格偏移参数、预估卖出量（仅前端估算时需标注）。
- 提交后刷新持仓及委托。

### 6.12 账户总仓位一键等比减仓

**接口**

```http
GET /quick/totalAccount/eqRatio/sellPosition?newPositionRate=0.65
```

参数 `newPositionRate`：源码注释说明目标账户总仓位，区间设计为 0~1；例如 0.65 表示目标账户总仓位 65%。

**源码约束**

- Controller 默认值为 `1`。
- Service 使用严格条件 `newPositionRate < 1`。
- Service 另有实际仓位与目标仓位比较及减仓差值大于 5% 的校验。
- 当前 Service 源码的比较断言与后续卖出比例计算方向存在逻辑矛盾，详见第 13 节阻断项。

**页面要求**

- 单独展示“账户总仓位目标比例”，输入单位采用 0~1 比例或 0%~100% 百分比，界面转换必须明确。
- 不允许省略参数依赖 Controller 默认值 1；必须要求用户显式输入，并在前端拦截 100% 及以上的目标。
- 在后端断言逻辑核验完成前，此按钮必须处于“待后端逻辑核验”状态，禁止把潜在反向计算的请求描述为正常减仓功能已验收。
- 核验通过后，弹窗显示当前实际账户总仓位、目标仓位、差值和待执行影响范围。

### 6.13 当前持仓一键等比减仓

**接口**

```http
GET /quick/currPos/eqRatio/sellPosition?newPositionRate=0.50
```

参数：`newPositionRate`，源码注释表示当前持仓（以此刻持仓市值为 100% 基准）减至指定仓位，目标区间设计为 0~1。

源码流程：

1. 读取账户资金和持仓。
2. `sellRate = 1 - newPositionRate`。
3. 对每个持仓按减仓比例计算卖出数量。
4. 以 `SELL_CLOSE` + `LMT` 提交卖出委托。

**页面要求**

- 必须与“账户总仓位等比减仓”分开呈现，不能共用含义模糊的“目标仓位”控件。
- 展示当前持仓基准、目标保留比例、预计减仓比例。
- 默认值不能直接使用接口默认 1；必须让用户明确输入一个小于 1 的目标比例。
- 订单提交后刷新持仓/委托；接口成功仅代表服务端流程返回，不代表减仓成交完成。

### 6.14 一键撤单

**接口**

```http
GET /quick/cancelOrder
```

源码流程：

1. 拉取全部当日委托。
2. 根据委托状态筛选非已完成订单。
3. 转换为撤单参数。
4. 批量撤单。
5. 等待约 1 秒后返回。

**页面要求**

- 提供全局“一键撤单”入口。
- 确认弹窗说明将撤销当前后端判定为未完成的当日委托，不仅限于卖出委托。
- 不能未经用户选择就把“一键撤单”与“一键清仓”合并为一个按钮。
- 操作后刷新当日委托和可撤委托，检查状态是否更新。
- 当前接口返回 `Result<Void>`，没有逐笔结果；页面必须说明实际结果需通过委托列表确认。

### 6.15 快捷分档/梯次下单

**接口**

```http
GET /quick/quickOption
  ?stockCode=588000
  &hybm=10011565
  &priceRangePct=1.0
  &rangeTotal=5
  &amount=1000
  &optionTradeTypeEnum=BUY_OPEN
```

**参数**

| 参数 | 必选 | 含义 |
|---|---|---|
| `stockCode` | 是 | 标的 ETF 代码 |
| `hybm` | 是 | 期权合约编码 |
| `priceRangePct` | 是 | 相邻档位价格间隔百分比 |
| `rangeTotal` | 是 | 总档数 |
| `amount` | 是 | 每档买入/卖出金额 |
| `optionTradeTypeEnum` | 是 | 四类交易类型枚举之一 |

**源码当前实现行为**

- 行情读取 `SHSZQuoteSnapshot(hybm)`。
- 基准价格取 `fivequote.buy1`。
- `SELL_CLOSE` 使用正向价格阶梯；其他三类交易类型当前均走负向价格阶梯。
- 后续每档根据 `priceRangePct` 逐级乘算。
- 每档数量按 `amount / price` 计算后再调用 `StockUtil.quantity(...)`。
- 每档以 `LMT` 方式提交。
- 每档后执行随机等待，源码调用 `SleepUtils.randomSleep(500)`。

**页面要求**

- 展示总档数、间隔百分比、每档金额、合约、交易类型。
- 提交前显示按当前行情估算的分档价格表；必须标记为前端估算，真实服务端价格和数量以实际实现为准。
- 提交后显示“服务端流程已返回”，刷新当日委托确认每一档实际委托数量。
- 在核验 `StockUtil.quantity` 对期权合约数量单位的适配性、以及四种交易类型价格方向之前，不得对该功能标记为最终生产验收通过。

---

## 7. 交易业务与字段规则

### 7.1 交易类型不能合并

下列四类业务在页面中必须保留为独立枚举选择项，不能简化为“买入/卖出”两个选项：

- 买入开仓（BUY_OPEN / 400）
- 卖出平仓（SELL_CLOSE / 401）
- 卖出开仓（SELL_OPEN / 402）
- 买入平仓（BUY_CLOSE / 403）

页面应在订单摘要及委托列表中同时显示中文名称和枚举含义。

### 7.2 报价方式不能删减

所有五种 `OptionPriceTypeEnum` 均必须在普通下单区呈现。若用户选择了非限价类报价方式，页面必须如实显示枚举含义，不能擅自将其替换成 LMT。

### 7.3 交易所编码

- `05`：深交所。
- `15`：上交所。

普通下单请求中的交易所字段由后端 `OptionSubmitTradeReq.getJybk()` 根据标的 ETF 代码映射，不属于 `OptionTradeBSParam` 的前端必填字段。最大可交易数量查询请求则需要传入 `jybk`。前端若要构造该字段，必须使用仓库中同一套 `StockMarketEnum` / `OptionMarketTypeEnum` 映射规则，不允许用“代码前三位猜交易所”的自创规则代替。

### 7.4 价格与金额

- 期权价格在当前服务多个快捷交易逻辑中使用四位小数精度。
- 普通下单金额可在页面显示为 `price × amount` 的估算值。
- 该估算值不等同于实际成交金额、保证金、手续费或账户风险值。
- 当前 Controller 没有独立的保证金试算或费用试算路由，不得在页面上伪造“精确手续费/保证金”。

### 7.5 数量

所有数量必须区分：委托数量、已成交数量、已撤数量、持仓数量、可用持仓数量、券商最大可交易数量。不得将这些字段合并。

### 7.6 委托状态

以券商原始状态字符串为准：待报、未报、已报、已撤、部成、部撤、已成、废单。

页面可以进行视觉分组，但必须保留原始状态。状态解释必须遵循 `BSOrderStatusEnum`，并对未知值单独标记。

---

## 8. 页面详细交互要求

### 8.1 普通下单完整流程

1. 用户选定标的 ETF。
2. 用户选择认购/认沽，或输入合约代码/名称关键词。
3. 页面调用 `searchOptionDict`，用户选中目标合约。
4. 页面调用 `SHSZQuoteSnapshot`，展示行情和五档。
5. 用户选择四种交易类型之一。
6. 用户选择五种报价方式之一。
7. 用户输入价格和数量。
8. 页面调用 `getMaxTradeCount` 查询券商返回的数量信息。
9. 页面进行字段完整性、正数、单位、精度和返回数量的前端校验。
10. 显示订单确认弹窗。
11. 用户明确确认后调用 `POST /bs`。
12. 显示委托编号及提交时间，状态为“已提交/待委托核验”。
13. 自动刷新当日委托；若未立即出现，提供手动刷新和再次查询入口。
14. 按当日委托返回的 `Wtzt` 展示最新券商状态。

### 8.2 批量撤单完整流程

1. 用户打开当日委托或可撤委托。
2. 勾选一笔或多笔委托。
3. 页面按每笔委托提取交易所、合同编号。
4. 显示撤单确认摘要。
5. 用户确认后调用 `POST /revokeOrders`。
6. 对返回数组逐条匹配并展示 `suc`、`Errcode`、`Errmsg`。
7. 刷新当日委托及可撤列表。
8. 显示哪些委托状态已更新、哪些仍待核验或撤单失败。

### 8.3 一键撤单流程

1. 显示当前当日委托列表最近刷新时间。
2. 确认弹窗说明会撤销后端判定为未完成的所有当日委托。
3. 用户确认后调用 `GET /quick/cancelOrder`。
4. 请求完成后刷新当日委托和可撤列表。
5. 页面不得把 `Result<Void>.success` 解释为每一笔委托均已撤销。

### 8.4 一键清仓流程

1. 刷新账户和持仓。
2. 展示全部持仓合约、持仓数量和可用数量。
3. 告知该服务端动作包含先撤单、再卖出平仓。
4. 用户二次确认。
5. 调用 `GET /quick/clearPosition` 一次。
6. 刷新资金、持仓、当日委托、可撤委托。
7. 以实际持仓和委托状态判定是否仍有未完成事项。

### 8.5 等比卖出流程

1. 用户在持仓表勾选要卖出的合约。
2. 页面提取合约编码列表。
3. 用户输入卖出持仓比例。
4. 用户输入当前价偏移/昨收偏移。
5. 确认弹窗显示参数和影响范围。
6. 调用 `GET /quick/sellPosition`。
7. 刷新委托和持仓。

### 8.6 两种等比减仓流程

必须分别设计两个独立表单：

- 账户总仓位目标比例：以账户口径为基准。
- 当前持仓保留比例：以当下持仓市值为 100% 基准。

两个数值虽然均为 0~1 形式，但基准完全不同。UI 必须同时显示当前基准值和目标值，不允许共用没有说明的“目标仓位”输入框。

### 8.7 快捷分档下单流程

1. 选择标的 ETF 和合约。
2. 获取当前五档行情。
3. 输入分档间隔百分比、总档数、每档金额。
4. 选择交易类型。
5. 显示估算分档价格和数量。
6. 用户确认。
7. 调用 `/quick/quickOption`。
8. 刷新当日委托，逐档核验实际提交情况。

---

## 9. 前端技术与工程规范

### 9.1 技术栈硬性要求

- HTML：现代 HTML5 语义化标签。
- CSS：原生 CSS3，CSS Variables、Grid、Flexbox、Media Queries、`position: sticky` 等。
- JavaScript：现代原生 JavaScript，ES Modules、`async/await`、`fetch`、`AbortController`、`URLSearchParams`、`Intl.NumberFormat`。
- 不引入 UI 框架，不引入 jQuery，不使用构建后才可运行的重型 SPA 框架。
- 允许采用原生 SVG 绘制少量图标/状态图形；不得依赖在线图标服务才能正常显示。

### 9.2 推荐项目目录

```text
option-trade-ui/
├── index.html
├── assets/
│   ├── css/
│   │   ├── reset.css
│   │   ├── tokens.css
│   │   ├── layout.css
│   │   ├── components.css
│   │   └── pages.css
│   ├── js/
│   │   ├── app.js
│   │   ├── config.js
│   │   ├── api/
│   │   │   ├── http.js
│   │   │   └── option-trade-api.js
│   │   ├── services/
│   │   │   ├── account-service.js
│   │   │   ├── quote-service.js
│   │   │   ├── order-service.js
│   │   │   └── trade-service.js
│   │   ├── components/
│   │   │   ├── modal.js
│   │   │   ├── toast.js
│   │   │   ├── data-table.js
│   │   │   ├── quote-five-level.js
│   │   │   └── order-confirm.js
│   │   ├── pages/
│   │   │   ├── dashboard-page.js
│   │   │   ├── positions-page.js
│   │   │   ├── trade-page.js
│   │   │   ├── orders-page.js
│   │   │   └── quick-actions-page.js
│   │   └── utils/
│   │       ├── format.js
│   │       ├── validate.js
│   │       ├── date.js
│   │       └── dom.js
│   └── icons/
└── README.md
```

### 9.3 API 客户端要求

统一实现：

- GET 请求：通过 `URLSearchParams` 构造参数，并使用 `cache: 'no-store'`。
- POST 请求：`Content-Type: application/json`，JSON 序列化请求体。
- 统一设置超时/取消机制；超时只取消前端等待，不得认为服务端交易一定未执行。
- 所有响应解析 `success/code/msg/data`。
- 处理 HTTP 500 + JSON Result 的后端异常响应。
- 日志中禁止输出 Cookie、SID、账户凭证和敏感会话头。

### 9.4 状态管理

不需要引入 Redux 等状态管理框架。可以由单一应用状态对象与原生模块维护：

- `accountState`
- `positionsState`
- `selectedContractState`
- `quoteState`
- `todayOrdersState`
- `historyOrdersState`
- `revokeOrdersState`
- `pendingActionsState`
- `operationLogState`

状态变更后只更新受影响的 DOM 区域，避免每次刷新整个页面导致用户正在编辑的表单丢失。

### 9.5 请求防重与重试规范

- 每个交易动作独立维护 `pending` 状态。
- 请求进行中禁用对应按钮，避免重复提交。
- 对查询接口可在用户显式操作下重试。
- 对下单、撤单、清仓、等比卖出、分档下单等产生交易副作用的请求，网络超时后严禁自动重发。
- 若响应不确定，先查询当日委托/持仓，再由用户决定下一步。

---

## 10. 安全、权限与操作审计

### 10.1 会话凭据

`EastMoneyTradeAPI` 中的 SID、Cookie 和请求头由后端维护，并被期权交易 API 复用。前端不得接触或持久化这些值。

### 10.2 浏览器与接口安全

- 优先同源部署。
- 所有用户输入进行 HTML 转义，避免 XSS。
- 使用严格 CSP，按部署需要限制脚本来源。
- 若跨站部署，必须核验 CORS、Cookie SameSite、CSRF 防护和身份验证策略。
- 不在 URL、前端日志、浏览器控制台、埋点中记录账户敏感信息。
- 生产环境不得打开会泄露券商接口完整请求头的调试日志。

### 10.3 交易操作确认

所有真实交易动作均需要显式二次确认。尤其是：

- 普通开仓/平仓。
- 批量撤单。
- 一键撤单。
- 一键清仓。
- 勾选合约等比卖出。
- 账户总仓位等比减仓。
- 当前持仓等比减仓。
- 快捷分档下单。

确认弹窗必须显示关键参数，不允许用“确定执行？”等缺少上下文的模糊提示。

### 10.4 操作审计

本次页面会话内至少记录：

- 操作时间。
- 操作功能。
- 请求路由和 HTTP Method。
- 关键业务参数摘要（不得含敏感身份凭据）。
- 请求是否发出。
- 响应 success/code/msg。
- 委托编号（如有）。
- 后续委托查询得到的状态。

前端操作日志用于本地排查，不取代后端合规审计日志。

---

## 11. 刷新策略与性能要求

### 11.1 刷新方式

- 账户和持仓：默认手动刷新；允许配置低频自动刷新，但必须有开关和刷新间隔。
- 行情：只对当前选中合约刷新；切换合约时取消旧请求。
- 当日委托：支持手动刷新；交易动作结束后触发一次刷新。
- 历史委托：只在用户提交日期查询时请求。
- 可撤委托：在打开该区域或撤单后刷新。

本 Controller 未暴露 WebSocket/SSE 行情订阅接口，因此前端不得宣称具备推送行情能力。

### 11.2 性能

- 首屏优先展示账户摘要和主工作台骨架。
- 表格采用固定表头、横向滚动和分页/虚拟滚动策略，避免大量委托记录阻塞页面。
- 所有查询显示加载状态，避免重复触发同一请求。
- 行情和委托刷新请求采用合适的防抖/节流，但不能延迟用户主动下单确认。
- 所有数值格式化统一使用工具函数，避免同一金额在不同组件出现不同精度。

### 11.3 响应式

- 桌面端：优先采用多栏工作台。
- 平板：行情与下单区可上下排列，委托表保留横向滚动。
- 手机窄屏：以功能分区和卡片形式展示；高风险批量操作的确认信息必须完整，不能因为屏幕窄而省略。

---

## 12. 异常与边界处理

| 场景 | 页面行为 |
|---|---|
| 后端无法连接 | 显示后端连接失败，不提交交易动作 |
| 券商会话失效 | 展示后端返回的错误信息，提示处理后端会话；前端不索取 Cookie/SID |
| 合约搜索为空 | 显示无匹配合约，不自动选取其他合约 |
| 行情为空/超时 | 显示行情不可用或已过期；默认阻止依赖实时价格的快捷操作继续提交 |
| 最大数量查询失败 | 显示查询失败，不伪造最大数量 |
| 普通下单参数缺失 | 前端阻止提交，并标明具体字段 |
| 下单返回错误 | 展示后端 `msg`；保留表单便于检查，不自动重发 |
| 下单超时 | 标记结果未知；先刷新当日委托，不自动重发 |
| 撤单部分成功 | 逐笔显示，不使用单一总成功状态覆盖明细 |
| 一键动作返回成功 | 标记“服务端请求已返回”，随后刷新委托和持仓核验实际结果 |
| 委托状态未知 | 显示原始状态与未知标识，不自动认为已成交或已撤 |
| 日期范围非法 | 前端提示并阻止请求 |
| 空持仓执行清仓 | 显示当前无持仓，仍以后端接口校验为准；不得伪造卖出记录 |
| 可用数量为 0 | 明确显示不可用数量；结合后端真实行为核验，不自行假定接口必然跳过 |

---

## 13. 源码核验与生产上线阻断项

本节非常重要：以下问题来自本次直接阅读到的当前源码。需求文档不擅自修复，但在生产验收前必须由后端负责人确认。前端不能靠隐藏控件掩盖问题。

### 13.1 P0：账户总仓位等比减仓断言方向疑似反向

`totalAccount__eqRatioSellPosition(newPositionRate)` 中：

- `actTotalPosRatio` 表示当前实际总仓位。
- 随后计算 `sellRate = 1 - newPositionRate / actTotalPosRatio`。
- 但之前的断言为 `Assert.isTrue(actTotalPosRatio < newPositionRate, ...)`。

从“将当前总仓位降低至指定目标”的语义看，当前仓位应高于目标仓位；当前断言却要求当前仓位小于目标仓位。紧接着又要求 `rate_diff = actTotalPosRatio - newPositionRate` 大于 0.05。按同一组变量定义，这两个断言无法同时成立：前者要求差值为负，后者要求差值大于正的 0.05。因此当前实现存在明确的互相矛盾校验，且后续卖出比例也可能为负。此处属于生产阻断级逻辑问题。

**要求：** 后端负责人逐行核验并确认正确条件；未确认前页面按钮不得标记为生产可用。不得由前端通过交换参数或取绝对值绕过。

### 13.2 P0：两个等比减仓接口默认值 1 与 Service 严格小于 1 冲突

两个 Controller 路由的 `newPositionRate` 默认值均为 `1`，Service 均执行 `Assert.isTrue(newPositionRate < 1, ...)`。因此省略参数时会触发校验失败。

**要求：** 页面必须显式输入目标值，不依赖默认值；后端是否调整默认值应作为单独修复项。

### 13.3 P0：期权数量单位与 StockUtil 百股取整逻辑疑似不匹配

当前 `quickOption` 传入 `StockUtil.quantity(qty, stockName)`；而已读取的 `StockUtil.quantity(int, String)` 会按传入字符串是否以 `68` 开头判断科创板，并继续按 100 股单位向下取整。传入的 `stockName` 是行情名称，不是标的 ETF 代码。

当前 `equalRatio_sellPosition` 还调用 `StockUtil.quantity(qty, stkavl)`，该重载会进行百位取整后再限制不超过可用数量。

**风险：** 股票数量的百股取整逻辑不一定适用于 ETF 期权合约数量。若期权合约数量单位为“张/手”且最小单位不同，可能造成委托数量为 0 或数量失真。

**要求：** 必须根据实际期权交易单位、上游券商接口要求和线上可验证案例确认；不得由前端擅自按 100 或 1 对数量进行二次修正。

### 13.4 P0：快捷分档下单的价格阶梯方向仅对 SELL_CLOSE 单独处理

`quickOption` 当前逻辑：

- `SELL_CLOSE`：价格正向阶梯。
- `BUY_OPEN`、`SELL_OPEN`、`BUY_CLOSE`：均走负向阶梯。

这意味着四种交易类型没有按买卖方向完全对称地处理。是否符合用户预期需确认。

**要求：** 产品页面必须如实显示当前服务端逻辑；后端负责人确认四种类型正确的阶梯方向后，更新文档和验收用例。

### 13.5 P1：一键清仓会先撤销所有未完成委托

Controller 注释只写一键清仓；Service 实际调用 `quickCancelOrder()`，其实现从全部当日委托中筛选未完成委托后撤单，并非只撤销卖单。

**要求：** 确认弹窗必须说明会先撤销所有符合当前后端“未完成”判定的当日委托，包括可能存在的买入委托。

### 13.6 P1：一键清仓当前没有等待全部撤单最终确认

`quickCancelOrder()` 批量撤单后仅等待约 1 秒；`quickClearPosition()` 随后重新读取持仓并发出卖出委托。虽然类中存在检查/重试方法，但当前清仓主流程没有调用该方法。

**要求：** 清仓结果必须通过之后的当日委托和持仓查询核验。后端是否增加撤单确认、重试和逐笔执行结果，应列为后续可靠性改进。

### 13.7 P1：可撤状态判定对未知状态采取“未完成”分支

`BSOrderStatusEnum.unfilled(wtzt)` 使用 `!filled(wtzt)`。已完成列表为已撤、已成、废单、部撤；未识别的新状态同样会被当作未完成。

**要求：** 页面必须显示未知状态；后端需评估是否改用明确的状态白名单作为可撤依据。

### 13.8 P1：快捷卖出可能提交零数量

`quick__sellPosition` 会按可用数量和卖出百分比计算数量；源码中对可用数量为 0 的提前跳过逻辑处于注释状态。需要确认当前服务是否会继续向 `bs` 发送数量为 0 的委托。

**要求：** 前端显示可用数量；后端需核验是否需要跳过 0 数量项目，并确保结果可追踪。

### 13.9 P1：快捷分档下单的基准价取买一

当前 `quickOption` 的价格基准取 `fivequote.buy1`，而不是当前价、卖一或买卖中间价。

**要求：** 页面提示“服务端基准价为买一价”，预估表必须按该基准计算；后端应确认这符合所有交易类型的预期。

### 13.10 P1：交易限流等待超时返回值未被检查

`EastMoneyOptionTradeAPI.limiter()` 调用 `tryAcquireWithTimeout(stockCode, 60 * 1000)`，但没有检查返回的 boolean 是否成功后再继续请求。

**要求：** 后端核验限流超时后是否仍继续提交上游请求；如确有问题，应单独修复。前端不能声称所有请求都已获得限流许可。

### 13.11 P1：批量撤单分批执行

Service 每 5 笔拆成一组执行撤单。页面需要容忍批次耗时和部分批次失败，并对结果逐条呈现。

### 13.12 P1：GET 路由承载真实交易副作用

`quick/clearPosition`、`quick/sellPosition`、两种等比减仓、`quick/cancelOrder` 和 `quick/quickOption` 均是 GET 路由但有交易副作用。

**要求：** 前端不得预取、自动轮询、自动重试、缓存这些 URL；请求必须只由用户真实点击确认动作触发，并设置 `cache: 'no-store'`。后端后续应评估迁移为 POST 的兼容方案，但本项目第一阶段必须按现有 Controller 方法调用。

---

## 14. API 调用映射清单

前端 API 模块必须至少实现以下 15 个函数，命名可调整，但映射不得缺失：

| 建议函数 | Method | Path |
|---|---|---|
| `getAccountFundsAndPositions()` | GET | `/getAccountFundsAndPositions` |
| `searchOptionDict(params)` | GET | `/searchOptionDict` |
| `getQuoteSnapshot(optionCode)` | GET | `/SHSZQuoteSnapshot` |
| `getMaxTradeCount(req)` | POST | `/getMaxTradeCount` |
| `submitOptionOrder(param)` | POST | `/bs` |
| `getTodayOrders()` | GET | `/getTodayOrders` |
| `getHistoryOrders(startDate, endDate)` | GET | `/getHisOrders` |
| `getRevokeList()` | GET | `/getRevokeList` |
| `revokeOrders(paramList)` | POST | `/revokeOrders` |
| `quickClearPosition()` | GET | `/quick/clearPosition` |
| `quickSellPosition(params)` | GET | `/quick/sellPosition` |
| `quickSellByTotalAccountRate(newPositionRate)` | GET | `/quick/totalAccount/eqRatio/sellPosition` |
| `quickSellByCurrentPositionRate(newPositionRate)` | GET | `/quick/currPos/eqRatio/sellPosition` |
| `quickCancelOrder()` | GET | `/quick/cancelOrder` |
| `quickOption(params)` | GET | `/quick/quickOption` |

---

## 15. 测试与验收方案

### 15.1 功能覆盖验收

| 编号 | 验收项 | 通过标准 |
|---|---|---|
| AT-001 | 账户资金查询 | 能展示接口返回账户字段和刷新时间 |
| AT-002 | 期权持仓 | 持仓数量、可用数量、合约代码、行权价、到期日均正确映射 |
| AT-003 | 合约搜索 | 按 ETF、C/P、编码/名称关键词组合搜索 |
| AT-004 | 五档行情 | 买 1~5、卖 1~5 和数量一一对应 |
| AT-005 | 最大可交易数量 | 请求字段编码正确，响应数量准确展示 |
| AT-006 | 四种交易类型 | 4 种枚举均可选且传递值正确 |
| AT-007 | 五种报价方式 | 5 种枚举均可选且传递值正确 |
| AT-008 | 普通下单 | 参数完整，返回委托编号，状态显示为待核验 |
| AT-009 | 当日委托 | 所有关键订单字段及状态展示准确 |
| AT-010 | 历史委托 | 日期范围传递准确，接口结果完整显示 |
| AT-011 | 可撤委托 | 结果严格来源于 getRevokeList |
| AT-012 | 批量撤单 | 请求参数正确，逐笔成功/失败展示 |
| AT-013 | 一键撤单 | 二次确认，完成后刷新委托状态 |
| AT-014 | 一键清仓 | 二次确认，完成后刷新资金/持仓/委托 |
| AT-015 | 勾选等比卖出 | 合约编码集合、比例和两个价格偏移字段准确 |
| AT-016 | 账户总仓位等比减仓 | 通过源码逻辑核验后，目标仓位参数及结果正确 |
| AT-017 | 当前持仓等比减仓 | 当前持仓基准和保留比例正确 |
| AT-018 | 快捷分档下单 | 档数、间隔、每档金额、合约、类型均准确 |
| AT-019 | 防止重复提交 | 请求期间重复点击不会生成第二个请求 |
| AT-020 | 超时保护 | 交易请求超时后不会自动重发 |
| AT-021 | 统一错误处理 | HTTP 500 + Result 错误结构能显示 msg |
| AT-022 | 会话安全 | 浏览器中不存在券商 SID/Cookie/validatekey |
| AT-023 | GET 副作用保护 | 页面加载、预取和轮询不触发交易 GET 路由 |
| AT-024 | 未知委托状态 | 原始状态可见，且明确显示待核验 |

### 15.2 接口联调测试

- 所有查询接口分别进行成功、空数组、异常、超时测试。
- 普通下单对四类交易类型、五种报价方式做枚举映射测试；测试环境联调不得将测试单误发到生产账户。
- 撤单测试覆盖单笔撤单、多笔撤单、5 条边界、超过 5 条的多批次、部分失败、重复撤单。
- 快捷操作覆盖参数边界、零可用数量、空持仓、部分成交委托和网络超时。
- 等比操作必须先完成第 13 节源代码 P0 问题核验，不能跳过该门槛直接验收。

### 15.3 视觉验收

- 桌面宽屏无横向整体溢出。
- 表格可横向滚动，固定表头不遮挡数据。
- 重要操作不会被折叠到难以发现的位置。
- 所有真实交易动作使用明确的危险操作视觉层级。
- 行情过期、接口失败、未知委托状态、待核验委托有独立样式。
- 不用颜色单独表达买卖方向或成功失败，必须有文本标签。

---

## 16. 开发交付物

开发阶段必须交付：

1. 完整原生 HTML/CSS/JavaScript 源码。
2. 15 个 Controller API 的完整映射实现。
3. 页面导航、工作台、持仓、行情、普通下单、委托、历史委托和快捷交易全套页面。
4. 统一 API 客户端和统一错误处理。
5. 交易确认弹窗、防重复提交、超时未知态和结果回查。
6. 源码核验问题清单及每个问题的最终结论。
7. 逐项测试用例和验收结果。
8. 本地运行与部署说明。
9. 后端 API 兼容性说明及任何必须由后端配合的变更清单。

---

## 17. 最终验收原则

1. `OptionTradeController` 当前 15 个活动路由必须全部有对应 UI 和 API 调用，不得删减。
2. 所有现有交易类型、报价方式、默认参数和字段语义必须由源码决定。
3. 任何接口未实现或被禁用，都必须明确说明是源码缺陷/环境问题/未通过安全核验，不能谎称功能已经交付。
4. 下单提交、成交确认、撤单提交、撤单确认、批量操作部分成功必须使用不同状态。
5. 对源码本身存在的高风险逻辑，不得通过前端偷偷修正；必须先形成明确的后端核验结论。
6. 原生 HTML/CSS/JavaScript 是强制技术边界，不得擅自引入前端框架。
7. 不得以“模拟版”“演示版”替代现有 API 的产品目标。

---

# 附录 A：源码字段和接口速查

## A.1 `OptionTradeBSParam`

- `etfCode: String`
- `etfName: String`
- `hybh: String`
- `price: BigDecimal`
- `amount: Integer`
- `optionTradeTypeEnum: OptionTradeTypeEnum`
- `optionPriceTypeEnum: OptionPriceTypeEnum`

## A.2 `OptionGetMaxTradeCountReq`

- `zqyw: int`
- `ywxw: int`
- `jybk: String`
- `hybm: String`
- `wtjg: double`

## A.3 `OptionTradeRevokeOrdersParam`

- `Jybk: String`
- `Htxh: String`

提示：该 DTO 的 Java 成员名首字母大写，实际 JSON 属性名应以当前 Spring/Jackson 配置及接口实际联调结果为准；开发时应通过 Swagger/OpenAPI 或真实请求验证，不得仅凭字段书写风格猜测。

## A.4 `OptionTradeTypeEnum`

- `BUY_OPEN(400, "买入开仓")`
- `SELL_CLOSE(401, "卖出平仓")`
- `SELL_OPEN(402, "卖出开仓")`
- `BUY_CLOSE(403, "买入平仓")`

## A.5 `OptionPriceTypeEnum`

- `LMT(130, "LMT限价")`
- `LOK(131, "LOK限价即全成否则撤")`
- `FAL(132, "FAL市价剩转限价")`
- `FOK(133, "FOK市价即全成否则撤")`
- `FAK(134, "FAK市价即成剩撤")`

## A.6 `OptionMarketTypeEnum`

- `SZ("05", "深交所")`
- `SH("15", "上交所")`

## A.7 `BSOrderStatusEnum`

- `待报`
- `未报`
- `已报`
- `已撤`
- `部成`
- `部撤`
- `已成`
- `废单`

---

# 附录 B：需求变更控制

后续任何新增、删减或修改必须提交变更记录，至少写明：

- 变更编号和日期。
- 涉及的 Controller 路由。
- 变更前后参数、返回值和业务语义。
- 对现有页面、交易确认、状态机和测试用例的影响。
- 是否涉及真实资金交易行为变化。
- 代码对应 commit SHA。

未经确认，不得删减任何活动 API，不得根据“更简单”原则合并四类交易类型或五种报价方式，也不得将不同口径的仓位比例合并成同一个字段。

