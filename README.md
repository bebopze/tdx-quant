# TDX Quant

基于 通达信 本地数据、行情数据 和 交易接口构建的 Java 量化交易系统，覆盖数据解析、行情分析、主线板块筛选、策略回测、持仓管理、委托查询、自动化任务以及大模型多模态调用。

项目当前基于 Spring Boot 4、Java 25、MyBatis Plus 和 ShardingSphere JDBC，默认监听 `7001` 端口。

> [!CAUTION]
> 本项目包含真实下单、撤单、一键卖出、融资操作和定时任务代码。首次运行前必须检查启用的 Spring Profile、交易账户、数据库和任务配置。不要在真实账户环境中直接执行未经验证的接口或测试。

> [!WARNING]
> 当前 `application.yml` 默认启用 `prod` Profile，并开放全部 Actuator 端点及 shutdown。仅应在可信内网使用；公开部署前必须增加认证、限制管理端点并关闭不需要的交易入口。

## 功能概览

- 通达信板块、个股、日线及扩展数据解析。
- 股票、ETF、板块行情与技术指标计算。
- 主线板块、主线个股和市场周期分析。
- 策略回测、交易记录、每日收益及持仓快照。
- ShardingSphere 分库分表存储大规模回测数据。
- 信用账户持仓、历史委托、撤单及快捷交易。
- 定时任务、异步任务、分布式锁和执行进度查询。
- Springdoc OpenAPI、Actuator 和 Spring Boot Admin 集成。
- 基于 Spring AI 的 DeepSeek、Qwen、豆包、MiMo、OpenAI 通用客户端。
- 验证码图片识别和通用图片分析。

## 技术栈

| 组件 | 版本/说明 |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Spring Cloud | 2025.1.3 |
| Spring AI | 2.0.1 |
| Spring Boot Admin | 4.1.2，服务端和客户端开关默认关闭 |
| MyBatis Plus | 3.5.17 |
| PageHelper | 4.1.1 |
| ShardingSphere JDBC | 5.5.3 |
| Druid | 1.2.28，Spring Boot 4 Starter |
| springdoc-openapi | 3.1.0 |
| MySQL | 建议使用 8.x |
| 前端 | 原生 HTML、CSS、JavaScript、Bootstrap |

完整依赖版本以 [`pom.xml`](pom.xml) 为准。

## 项目结构

```text
tdx-quant/
├── docs/
│   ├── DB/                         # 主库和回测分片建表脚本
│   └── llm-client.md               # 通用大模型客户端文档
├── src/main/java/com/bebopze/tdx/quant/
│   ├── ai/                         # Spring AI 通用客户端
│   ├── client/                     # 行情、交易及第三方 API 客户端
│   ├── common/                     # 配置、缓存、DTO、异常和工具类
│   ├── dal/                        # Mapper、Entity、Service
│   ├── indicator/                  # 技术指标
│   ├── parser/                     # 通达信数据解析
│   ├── service/                    # 业务服务
│   ├── strategy/                   # 买卖策略与回测策略
│   ├── task/                       # 定时任务和后台任务
│   └── web/                        # HTTP Controller
├── src/main/resources/
│   ├── static/                     # 管理页面
│   ├── application.yml             # 公共配置
│   ├── application-*.yml           # Profile 配置
│   ├── shardingsphere-dev.yml      # 开发环境数据源与分片规则
│   └── shardingsphere-prod.yml     # 生产环境数据源与分片规则
└── src/test/                       # 单元测试和上下文测试
```

## 环境要求

- JDK 25。
- Maven 3.9+。
- MySQL 8.x。
- 需要解析通达信本地文件时，准备可访问的通达信数据目录。
- 需要使用交易功能时，准备独立测试账户及有效会话。
- 需要使用大模型时，准备相应供应商的 API Key。

确认本机版本：

```bash
java -version
mvn -version
mysql --version
```

## 快速开始

### 1. 初始化数据库

创建公共数据库并导入基础表：

```sql
CREATE DATABASE IF NOT EXISTS tdx
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
```

```bash
mysql -u root -p tdx < docs/DB/tdx.sql
```

回测记录默认使用两个数据库：

- `tdx_bt_0`
- `tdx_bt_1`

[`docs/DB/tdx_bt_shard.sql`](docs/DB/tdx_bt_shard.sql) 默认创建两个数据库，并在 `tdx_bt_0` 中创建 `bt_trade_record_0..99` 和 `bt_position_record_0..99`。执行一次后，将脚本中的 `USE tdx_bt_0` 改为 `USE tdx_bt_1`，再执行一次以初始化第二个分库。

脚本的 `@mode` 默认是 `1`，表示只创建不存在的表。将其改为 `2` 会先删除分表再重建，会丢失数据，请谨慎使用。

### 2. 配置数据源

项目根据 `spring.profiles.active` 加载：

```text
classpath:shardingsphere-{profile}.yml
```

开发环境对应 [`shardingsphere-dev.yml`](src/main/resources/shardingsphere-dev.yml)，生产环境对应 [`shardingsphere-prod.yml`](src/main/resources/shardingsphere-prod.yml)。启动前至少检查：

- `ds_common` 是否指向 `tdx`。
- `ds_0`、`ds_1` 是否指向两个回测分库。
- MySQL 用户名、密码和网络地址是否正确。
- 分片表数量是否与 SQL 脚本一致。

不要提交真实数据库密码、交易 Cookie、验证码、API Key 或其他账户凭证。

### 3. 配置通达信目录

公共配置中的 `tdx-path` 默认是 Windows 示例路径。可以在启动时覆盖：

```bash
java -jar target/tdx-quant-1.0-SNAPSHOT.jar \
  --spring.profiles.active=dev \
  --tdx-path=/absolute/path/to/new_tdx
```

如果当前任务不需要解析本地通达信文件，也应确认相关定时任务不会在启动后自动访问该目录。

### 4. 构建

```bash
mvn clean package -DskipTests
```

构建产物：

```text
target/tdx-quant-1.0-SNAPSHOT.jar
```

### 5. 启动

开发环境建议显式指定 `dev`，避免使用公共配置中的默认 `prod`：

```bash
java -jar target/tdx-quant-1.0-SNAPSHOT.jar \
  --spring.profiles.active=dev
```

也可以使用 Maven：

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

启动完成后访问：

- 功能菜单：<http://localhost:7001/>
- Swagger UI：<http://localhost:7001/swagger-ui.html>
- OpenAPI JSON：<http://localhost:7001/v3/api-docs>
- Actuator：<http://localhost:7001/actuator>

## 页面入口

| 功能 | 地址 |
|---|---|
| 持仓与当日委托 | <http://localhost:7001/trade/position-list.html> |
| 数据分析 | <http://localhost:7001/data-analysis/topList.html> |
| 回测任务 | <http://localhost:7001/backtest/backtest-list.html> |
| 主线板块 | <http://localhost:7001/topblock/topblock.html> |
| 定时任务 | <http://localhost:7001/task/task.html> |
| 股票 K 线 | <http://localhost:7001/stock/stock-kline.html> |

页面由 Spring Boot 直接提供静态资源。重新打包并重启服务后，如果浏览器仍显示旧页面，请执行强制刷新：

- macOS：`Cmd + Shift + R`
- Windows/Linux：`Ctrl + F5`

## API 概览

| 前缀 | 功能 |
|---|---|
| `/api/trade` | 持仓、委托、撤单及快捷交易 |
| `/api/backtest` | 回测执行、任务、分析和交易记录 |
| `/api/parser/tdxdata` | 通达信数据导入和 K 线填充 |
| `/api/topBlock` | 主线板块和主线个股计算 |
| `/api/data/analysis` | 数据分析与榜单 |
| `/api/task` | 数据刷新、后台任务和执行进度 |
| `/api/strategy` | 买卖策略执行与信号列表 |
| `/api/stock` | 股票详情和板块信息 |
| `/api/block`、`/api/blockNew` | 板块及其成分股 |
| `/api/market` | 市场周期数据 |
| `/api/monitor` | 缓存及运行状态监控 |

> [!IMPORTANT]
> `/api/trade`、`/api/strategy` 和部分 `/api/task` 接口会改变交易或业务状态。不要把整个 `/api/**` 暴露到公网，也不要通过浏览器、监控探针或爬虫无差别调用。

## 大模型客户端

项目提供统一的 [`OpenAiCompatibleLlmClient`](src/main/java/com/bebopze/tdx/quant/ai/OpenAiCompatibleLlmClient.java)，支持：

- Qwen
- 豆包
- OpenAI GPT
- MiMo
- DeepSeek
- 其他兼容 OpenAI Chat Completions 的服务

复制 [`application-llm-example.yml`](src/main/resources/application-llm-example.yml) 中需要的配置，并通过环境变量提供 API Key：

```bash
export DASHSCOPE_API_KEY="..."
export ARK_API_KEY="..."
export OPENAI_API_KEY="..."
export MIMO_API_KEY="..."
export DEEPSEEK_API_KEY="..."
```

切换默认供应商：

```yaml
llm:
  default-provider: qwen
```

验证码识别示例：

```java
String captcha = llmClient.recognizeCaptcha(imageFile);
String doubaoResult = llmClient.recognizeCaptcha("doubao", imageFile);
```

图片会发送到所选择的第三方模型服务。不要上传未获授权的交易截图、账户信息或其他敏感资料。

更多说明见 [`docs/llm-client.md`](docs/llm-client.md)。

## 测试

纯单元测试示例：

```bash
mvn -Dtest=OpenAiCompatibleLlmClientTest,GetOrdersDataRespJsonTest test
```

完整测试：

```bash
mvn test
```

> [!WARNING]
> 当前 `TdxQuantAppTests` 会加载完整 Spring 上下文。由于默认 Profile 是 `prod`，它可能连接 MySQL、初始化交易会话并访问第三方接口。完整测试只能在已隔离的开发环境执行，不要使用真实交易账户。

常用检查命令：

```bash
# 编译
mvn -DskipTests compile

# 打包
mvn -DskipTests package

# 检查可升级的显式版本属性
mvn versions:display-property-updates -DallowSnapshots=false
```

## 配置与安全建议

- 显式指定启动 Profile，不依赖 `application.yml` 中的默认值。
- 将数据库密码、大模型 Key 和交易会话迁移到安全配置中心或环境变量。
- 日志中不得输出 Cookie、API Key、完整账户信息或订单认证参数。
- 生产环境关闭 Swagger，或仅允许内网访问。
- 将 `management.endpoints.web.exposure.include` 改为最小端点集合。
- 禁止公网访问 `/actuator/shutdown`。
- 为交易接口增加认证、授权、审计、幂等和二次确认。
- 在模拟账户完成回归测试后，再逐项开放真实交易能力。
- 定期备份 `tdx`、`tdx_bt_0` 和 `tdx_bt_1`。

## 已知约束

- 公共配置当前默认启用 `prod` Profile。
- Spring Cloud 兼容性检查当前被显式关闭；升级 Spring Boot 或 Spring Cloud 后必须运行完整回归测试。
- 数据源 YAML 由 `MybatisConfig` 直接读取，配置文件名必须与 Profile 一致。
- 通达信目录结构依赖本地客户端版本，升级客户端后应验证解析器。
- 部分页面和接口直接依赖第三方行情、交易服务的字段及会话状态。
- Spring Boot Admin 依赖已集成，但服务端注解和客户端开关默认关闭。

## 端口转发

需要通过 SSH 访问远程服务时，可将本机 `7001` 转发至远程 `localhost:7001`：

```bash
ssh -p 2222 -L 7001:localhost:7001 user@example-host
```

然后访问本机 <http://localhost:7001/>。不要将管理端口直接暴露到公网。

## License

本项目采用 [Apache License 2.0](LICENSE)。

## 免责声明

本项目仅用于技术研究、数据分析和自动化实验，不构成投资建议。实盘交易可能造成资金损失，使用者应自行评估并承担全部风险。
