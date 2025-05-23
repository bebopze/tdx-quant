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
    `type`         tinyint unsigned                                             NOT NULL COMMENT 'tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；',
    `parent_id`    bigint unsigned                                              NOT NULL DEFAULT '0' COMMENT '父-ID（行业板块）',
    `level`        tinyint unsigned                                             NOT NULL DEFAULT '0' COMMENT '行业级别：1-1级行业；2-2级行业；3-3级行业（细分行业）；',
    `end_level`    tinyint unsigned                                             NOT NULL DEFAULT '0' COMMENT '是否最后一级：0-否；1-是；（行业板块）',
    `trade_date`   date                                                                  DEFAULT NULL COMMENT '交易日期',
    `open`         decimal(10, 3)                                                        DEFAULT NULL COMMENT '开盘价',
    `high`         decimal(10, 3)                                                        DEFAULT NULL COMMENT '最高价',
    `low`          decimal(10, 3)                                                        DEFAULT NULL COMMENT '最低价',
    `close`        decimal(10, 3)                                                        DEFAULT NULL COMMENT '收盘价',
    `adj_close`    decimal(10, 3)                                                        DEFAULT NULL COMMENT '复权后收盘价（可选）',
    `volume`       bigint unsigned                                                       DEFAULT NULL COMMENT '成交量',
    `amount`       decimal(20, 2) unsigned                                               DEFAULT NULL COMMENT '成交额',
    `change_pct`   decimal(10, 5)                                                        DEFAULT NULL COMMENT '涨跌幅',
    `range_pct`    decimal(10, 5)                                                        DEFAULT NULL COMMENT '振幅',
    `turnover_pct` decimal(10, 5)                                                        DEFAULT NULL COMMENT '换手率',
    `kline_his`    longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '历史行情-JSON（[日期,O,C,H,L,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）',
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
-- Table structure for base_stock
-- ----------------------------
DROP TABLE IF EXISTS `base_stock`;

CREATE TABLE `base_stock`
(
    `id`              bigint unsigned                                             NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`            varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '股票代码',
    `name`            varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '股票名称',
    `tdx_market_type` tinyint unsigned                                            NOT NULL COMMENT '通达信-市场类型：0-深交所；1-上交所；2-北交所；',
    `trade_date`      date                                                                 DEFAULT NULL COMMENT '交易日期',
    `open`            decimal(10, 3)                                                       DEFAULT NULL COMMENT '开盘价',
    `high`            decimal(10, 3)                                                       DEFAULT NULL COMMENT '最高价',
    `low`             decimal(10, 3)                                                       DEFAULT NULL COMMENT '最低价',
    `close`           decimal(10, 3)                                                       DEFAULT NULL COMMENT '收盘价',
    `adj_close`       decimal(10, 3)                                                       DEFAULT NULL COMMENT '复权后收盘价（可选）',
    `volume`          bigint unsigned                                                      DEFAULT NULL COMMENT '成交量',
    `amount`          decimal(20, 2) unsigned                                              DEFAULT NULL COMMENT '成交额',
    `change_pct`      decimal(10, 5)                                                       DEFAULT NULL COMMENT '涨跌幅',
    `range_pct`       decimal(10, 5)                                                       DEFAULT NULL COMMENT '振幅',
    `turnover_pct`    decimal(10, 5)                                                       DEFAULT NULL COMMENT '换手率',
    `kline_his`       longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '历史行情-JSON（[日期,O,C,H,L,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）',
    `gmt_create`      datetime                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify`      datetime                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`) USING BTREE COMMENT '股票code'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='股票-实时行情';

-- ----------------------------
-- Table structure for base_block_rela_stock
-- ----------------------------
DROP TABLE IF EXISTS `base_block_rela_stock`;

CREATE TABLE `base_block_rela_stock`
(
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `block_id`   bigint unsigned NOT NULL COMMENT '板块ID',
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
    KEY `idx__stock_id__type` (`stock_id`, `type`) USING BTREE COMMENT '股票ID'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='自定义板块 - 股票/板块/指数 关联';



SET FOREIGN_KEY_CHECKS = 1;
