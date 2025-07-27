SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;



-- ----------------------------
-- Table structure for base_block
-- ----------------------------
DROP TABLE IF EXISTS `base_block`;
CREATE TABLE `base_block`
(
    `id`           bigint unsigned                                              NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`         varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '板块代码',
    `name`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '板块名称',
    `code_path`    varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '板块code-路径',
    `name_path`    varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '板块name-路径',
    `parent_id`    bigint unsigned                                              NOT NULL DEFAULT '0' COMMENT '父-ID（行业板块）',
    `type`         tinyint unsigned                                             NOT NULL COMMENT 'tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；',
    `level`        tinyint unsigned                                             NOT NULL DEFAULT '0' COMMENT '行业级别：1-1级行业；2-2级行业；3-3级行业（细分行业）；',
    `end_level`    tinyint unsigned                                             NOT NULL DEFAULT '0' COMMENT '是否最后一级：0-否；1-是；（行业板块）',
    `trade_date`   date                                                                  DEFAULT NULL COMMENT '交易日期',
    `open`         decimal(10, 3)                                                        DEFAULT NULL COMMENT '开盘价',
    `high`         decimal(10, 3)                                                        DEFAULT NULL COMMENT '最高价',
    `low`          decimal(10, 3)                                                        DEFAULT NULL COMMENT '最低价',
    `close`        decimal(10, 3)                                                        DEFAULT NULL COMMENT '收盘价',
    `volume`       bigint unsigned                                                       DEFAULT NULL COMMENT '成交量',
    `amount`       decimal(20, 2) unsigned                                               DEFAULT NULL COMMENT '成交额',
    `change_pct`   decimal(10, 5)                                                        DEFAULT NULL COMMENT '涨跌幅',
    `range_pct`    decimal(10, 5)                                                        DEFAULT NULL COMMENT '振幅',
    `turnover_pct` decimal(10, 5)                                                        DEFAULT NULL COMMENT '换手率',
    `kline_his`    longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '历史行情-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）',
    `ext_data_his` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '扩展数据-JSON（[日期,RPS5,RPS10,RPS15,RPS20,RPS50]）',
    `gmt_create`   datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify`   datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`) USING BTREE COMMENT '板块code'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='板块/指数-实时行情（以 tdx 为准）';

-- ----------------------------
-- Table structure for base_block_new
-- ----------------------------
DROP TABLE IF EXISTS `base_block_new`;
CREATE TABLE `base_block_new`
(
    `id`         bigint unsigned                                              NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '自定义板块-代码',
    `name`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '自定义板块-名称',
    `gmt_create` datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify` datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_code` (`code`) USING BTREE COMMENT '自定义板块code'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='tdx - 自定义板块';

-- ----------------------------
-- Table structure for base_block_new_rela_stock
-- ----------------------------
DROP TABLE IF EXISTS `base_block_new_rela_stock`;
CREATE TABLE `base_block_new_rela_stock`
(
    `id`           bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `block_new_id` bigint unsigned  NOT NULL COMMENT '自定义板块ID',
    `stock_id`     bigint unsigned  NOT NULL COMMENT '关联ID：股票ID/板块ID/指数ID',
    `type`         tinyint unsigned NOT NULL COMMENT '关联ID类型：1-个股；2-板块；3-指数；',
    `gmt_create`   datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify`   datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx__block_new_id__type` (`block_new_id`, `type`) USING BTREE COMMENT '自定义板块ID',
    KEY `idx__stock_id__type` (`stock_id`, `type`) USING BTREE COMMENT '关联ID'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='自定义板块 - 股票/板块/指数 关联';

-- ----------------------------
-- Table structure for base_block_rela_stock
-- ----------------------------
DROP TABLE IF EXISTS `base_block_rela_stock`;
CREATE TABLE `base_block_rela_stock`
(
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `block_id`   bigint unsigned NOT NULL COMMENT '板块ID（3级行业 + 概念板块 => end_level=1）',
    `stock_id`   bigint unsigned NOT NULL COMMENT '股票ID',
    `gmt_create` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_block_id` (`block_id`) USING BTREE COMMENT '板块ID',
    KEY `idx_stock_id` (`stock_id`) USING BTREE COMMENT '股票ID'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='板块-股票 关联';

-- ----------------------------
-- Table structure for base_stock
-- ----------------------------
DROP TABLE IF EXISTS `base_stock`;
CREATE TABLE `base_stock`
(
    `id`              bigint unsigned                                             NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`            varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '股票代码',
    `name`            varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '股票名称',
    `type`            tinyint unsigned                                            NOT NULL COMMENT '股票类型：1-A股；2-ETF；',
    `tdx_market_type` tinyint unsigned                                            NOT NULL COMMENT '通达信-市场类型：0-深交所；1-上交所；2-北交所；',
    `trade_date`      date                                                                 DEFAULT NULL COMMENT '交易日期',
    `open`            decimal(10, 3)                                                       DEFAULT NULL COMMENT '开盘价',
    `high`            decimal(10, 3)                                                       DEFAULT NULL COMMENT '最高价',
    `low`             decimal(10, 3)                                                       DEFAULT NULL COMMENT '最低价',
    `close`           decimal(10, 3)                                                       DEFAULT NULL COMMENT '收盘价',
    `volume`          bigint unsigned                                                      DEFAULT NULL COMMENT '成交量',
    `amount`          decimal(20, 2) unsigned                                              DEFAULT NULL COMMENT '成交额',
    `change_pct`      decimal(10, 5)                                                       DEFAULT NULL COMMENT '涨跌幅',
    `range_pct`       decimal(10, 5)                                                       DEFAULT NULL COMMENT '振幅',
    `turnover_pct`    decimal(10, 5)                                                       DEFAULT NULL COMMENT '换手率',
    `kline_his`       longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '历史行情-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）',
    `ext_data_his`    longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）',
    `gmt_create`      datetime                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify`      datetime                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`) USING BTREE COMMENT '股票code'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='股票-实时行情';

-- ----------------------------
-- Table structure for bt_daily_return
-- ----------------------------
DROP TABLE IF EXISTS `bt_daily_return`;
CREATE TABLE `bt_daily_return`
(
    `id`                 bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id`            bigint unsigned NOT NULL COMMENT '回测任务ID',
    `trade_date`         date            NOT NULL COMMENT '交易日期',
    `daily_return`       decimal(10, 4)  NOT NULL COMMENT '当日收益率',
    `profit_loss_amount` decimal(20, 2)  NOT NULL COMMENT '当日盈亏额',
    `nav`                decimal(20, 4)  NOT NULL COMMENT '净值（初始为1.0000）',
    `capital`            decimal(20, 2)  NOT NULL COMMENT '总资金',
    `market_value`       decimal(20, 2)  NOT NULL COMMENT '持仓市值',
    `avl_capital`        decimal(20, 2)  NOT NULL COMMENT '可用资金',
    `buy_capital`        decimal(20, 2)  NOT NULL COMMENT '买入金额',
    `sell_capital`       decimal(20, 2)  NOT NULL COMMENT '卖出金额',
    `benchmark_return`   decimal(10, 4)           DEFAULT NULL COMMENT '基准收益（可选）',
    `gmt_create`         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify`         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `task_id` (`task_id`, `trade_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='回测-每日收益率';

-- ----------------------------
-- Table structure for bt_position_record
-- ----------------------------
DROP TABLE IF EXISTS `bt_position_record`;
CREATE TABLE `bt_position_record`
(
    `id`                 bigint unsigned                                              NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id`            bigint unsigned                                              NOT NULL COMMENT '回测任务ID',
    `trade_date`         date                                                         NOT NULL COMMENT '交易日',
    `stock_id`           bigint unsigned                                              NOT NULL COMMENT '股票ID',
    `stock_code`         varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '股票代码',
    `stock_name`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '股票名称',
    `avg_cost_price`     decimal(10, 3)                                               NOT NULL COMMENT '加权平均成本价',
    `close_price`        decimal(10, 3) unsigned                                      NOT NULL COMMENT '收盘价',
    `quantity`           int unsigned                                                 NOT NULL COMMENT '持仓数量',
    `avl_quantity`       int unsigned                                                 NOT NULL COMMENT '可用数量',
    `market_value`       decimal(20, 2)                                               NOT NULL COMMENT '市值',
    `unrealized_pnl`     decimal(20, 2)                                               NOT NULL COMMENT '浮动盈亏',
    `unrealized_pnl_pct` decimal(10, 2)                                               NOT NULL COMMENT '盈亏率（%）',
    `buy_date`           date                                                         NOT NULL COMMENT '买入日期',
    `holding_days`       int unsigned                                                 NOT NULL DEFAULT '0' COMMENT '持仓天数',
    `gmt_create`         datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify`         datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `task_id` (`task_id`, `trade_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='回测-每日持仓记录';

-- ----------------------------
-- Table structure for bt_task
-- ----------------------------
DROP TABLE IF EXISTS `bt_task`;
CREATE TABLE `bt_task`
(
    `id`                bigint unsigned                                              NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `buy_strategy`      varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'B策略',
    `sell_strategy`     varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'S策略',
    `start_date`        date                                                         NOT NULL COMMENT '回测-起始日期',
    `end_date`          date                                                         NOT NULL COMMENT '回测-结束日期',
    `initial_capital`   decimal(20, 2) unsigned                                      NOT NULL COMMENT '初始资金',
    `final_capital`     decimal(20, 2) unsigned                                               DEFAULT NULL COMMENT '结束资金',
    `initial_nav`       decimal(10, 4) unsigned                                      NOT NULL DEFAULT '1.0000' COMMENT '初始净值',
    `final_nav`         decimal(10, 4) unsigned                                               DEFAULT NULL COMMENT '结束净值',
    `total_day`         smallint unsigned                                                     DEFAULT NULL COMMENT '总天数',
    `total_return_pct`  decimal(10, 2)                                                        DEFAULT NULL COMMENT '总收益率',
    `annual_return_pct` decimal(10, 2)                                                        DEFAULT NULL COMMENT '年化收益率',
    `sharpe_ratio`      decimal(10, 2)                                                        DEFAULT NULL COMMENT '夏普比率',
    `max_drawdown_pct`  decimal(10, 2)                                                        DEFAULT NULL COMMENT '最大回撤',
    `win_rate`          decimal(10, 2)                                                        DEFAULT NULL COMMENT '胜率',
    `profit_factor`     decimal(10, 2)                                                        DEFAULT NULL COMMENT '盈亏比',
    `gmt_create`        datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify`        datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='回测-任务';

-- ----------------------------
-- Table structure for bt_trade_record
-- ----------------------------
DROP TABLE IF EXISTS `bt_trade_record`;
CREATE TABLE `bt_trade_record`
(
    `id`           bigint unsigned                                               NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id`      bigint unsigned                                               NOT NULL COMMENT '回测任务ID',
    `trade_date`   date                                                          NOT NULL COMMENT '交易日期',
    `trade_type`   tinyint unsigned                                              NOT NULL COMMENT '交易类型：1-买入；2-卖出；',
    `trade_signal` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '交易信号',
    `stock_id`     bigint unsigned                                               NOT NULL COMMENT '股票ID',
    `stock_code`   varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NOT NULL COMMENT '股票代码',
    `stock_name`   varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '股票名称',
    `price`        decimal(10, 3) unsigned                                       NOT NULL COMMENT '交易价格',
    `quantity`     int unsigned                                                  NOT NULL COMMENT '交易数量',
    `amount`       decimal(20, 2) unsigned                                       NOT NULL COMMENT '交易金额',
    `fee`          decimal(10, 2) unsigned                                       NOT NULL DEFAULT '0.00' COMMENT '交易费用',
    `gmt_create`   datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify`   datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx__task_id__trade_date__trade_type` (`task_id`, `trade_date`, `trade_type`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='回测-BS交易记录';

-- ----------------------------
-- Table structure for qa_block_new_rela_stock_his
-- ----------------------------
DROP TABLE IF EXISTS `qa_block_new_rela_stock_his`;
CREATE TABLE `qa_block_new_rela_stock_his`
(
    `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `block_new_id`    tinyint unsigned NOT NULL COMMENT '自定义板块ID：1-百日新高；2-涨幅榜；3-RPS三线红（一线95/双线90/三线85）；4-二阶段；5-均线大多头；',
    `date`            date             NOT NULL COMMENT '日期',
    `stock_id_list`   text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '关联ID列表：股票ID/板块ID/指数ID（逗号分隔）',
    `result`          longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '汇总-分析结果JSON（二级普通行业 + 概念板块）',
    `gn_result`       longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '概念板块-分析结果JSON',
    `pthy_lv1_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '一级普通行业-分析结果JSON',
    `pthy_lv2_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '二级普通行业-分析结果JSON',
    `pthy_lv3_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '三级普通行业-分析结果JSON',
    `yjhy_lv1_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '一级研究行业-分析结果JSON',
    `yjhy_lv2_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '二级研究行业-分析结果JSON',
    `yjhy_lv3_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '三级研究行业-分析结果JSON',
    `gmt_create`      datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify`      datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx__block_new_id__date` (`block_new_id`, `date`) USING BTREE,
    KEY `idx_date` (`date`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='量化分析 - 主线板块';

-- ----------------------------
-- Table structure for qa_market_mid_cycle
-- ----------------------------
DROP TABLE IF EXISTS `qa_market_mid_cycle`;
CREATE TABLE `qa_market_mid_cycle`
(
    `id`                      bigint unsigned        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `date`                    date                   NOT NULL COMMENT '日期',
    `market_bull_bear_status` tinyint unsigned       NOT NULL DEFAULT '0' COMMENT '大盘-牛熊：1-牛市；2-熊市；',
    `market_mid_status`       tinyint unsigned       NOT NULL DEFAULT '0' COMMENT '大盘-中期顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；',
    `market_low_day`          int unsigned           NOT NULL DEFAULT '0' COMMENT '底_DAY',
    `market_high_day`         int unsigned           NOT NULL DEFAULT '0' COMMENT '顶_DAY',
    `ma50_pct`                decimal(5, 2) unsigned NOT NULL COMMENT 'MA50占比（%）',
    `position_pct`            decimal(5, 2) unsigned NOT NULL COMMENT '仓位占比（%）',
    `stock_month_bull_pct`    decimal(5, 2) unsigned NOT NULL COMMENT '个股月多-占比（%）',
    `block_month_bull_pct`    decimal(5, 2) unsigned NOT NULL COMMENT '板块月多-占比（%）',
    `high_num`                int unsigned           NOT NULL COMMENT '新高数量',
    `low_num`                 int unsigned           NOT NULL COMMENT '新低数量',
    `all_stock_num`           int unsigned           NOT NULL COMMENT '全A数量',
    `high_low_diff`           int                    NOT NULL COMMENT '差值',
    `bs1_pct`                 decimal(5, 2) unsigned NOT NULL COMMENT '左侧试仓-占比（%）',
    `bs2_pct`                 decimal(5, 2) unsigned NOT NULL COMMENT '左侧买-占比（%）',
    `bs3_pct`                 decimal(5, 2) unsigned NOT NULL COMMENT '右侧买-占比（%）',
    `bs4_pct`                 decimal(5, 2) unsigned NOT NULL COMMENT '强势卖出-占比（%）',
    `bs5_pct`                 decimal(5, 2) unsigned NOT NULL COMMENT '左侧卖-占比（%）',
    `bs6_pct`                 decimal(5, 2) unsigned NOT NULL COMMENT '右侧卖-占比（%）',
    `right_buy_pct`           decimal(5, 2) unsigned NOT NULL COMMENT '右侧B-占比（%）',
    `right_sell_pct`          decimal(5, 2) unsigned NOT NULL COMMENT '右侧S-占比（%）',
    `gmt_create`              datetime               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify`              datetime               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date` (`date`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='量化分析 - 大盘中期顶底';



SET FOREIGN_KEY_CHECKS = 1;