-- ---------------------------------------------------------------------------------------------------------------------
-- 新增参数   控制建表逻辑（默认 1：不存在则新建； 2：删除并新建；）
-- 新增参数   控制建表数量（默认 100）
-- ---------------------------------------------------------------------------------------------------------------------


-- 设置 建表模式：1 - CREATE   /   2 - DROP + CREATE
SET @mode = 1;


-- 设置 分表数量
SET @table_count = 100;



-- ---------------------------------------------------------------------------------------------------------------------
-- 分库（tdx_bt_0 / tdx_bt_1）
-- ---------------------------------------------------------------------------------------------------------------------


CREATE DATABASE IF NOT EXISTS tdx_bt_0 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS tdx_bt_1 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;



USE tdx_bt_0;
-- USE tdx_bt_1;


-- ---------------------------------------------------------------------------------------------------------------------
-- 分表     ->     bt_trade_record
-- ---------------------------------------------------------------------------------------------------------------------


DELIMITER $$
DROP PROCEDURE IF EXISTS create_bt_trade_record_shards$$
CREATE PROCEDURE create_bt_trade_record_shards(IN mode INT, IN table_count INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    WHILE i < table_count
        DO
            IF mode = 2 THEN
                SET @drop_sql = CONCAT('DROP TABLE IF EXISTS bt_trade_record_', i, ';');
                PREPARE stmt FROM @drop_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
            END IF;

            SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS bt_trade_record_', i, ' (
            `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
            `task_id` bigint unsigned NOT NULL COMMENT ''回测任务ID'',
            `trade_date` date NOT NULL COMMENT ''交易日期'',
            `trade_type` tinyint unsigned NOT NULL COMMENT ''交易类型：1-买入；2-卖出；'',
            `trade_signal_type` tinyint unsigned NOT NULL COMMENT ''交易信号-类型'',
            `trade_signal_desc` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ''交易信号-描述'',
            `stock_id` bigint unsigned NOT NULL COMMENT ''股票ID'',
            `stock_code` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ''股票代码'',
            `stock_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ''股票名称'',
            `price` decimal(7,3) unsigned NOT NULL COMMENT ''交易价格'',
            `quantity` mediumint unsigned NOT NULL COMMENT ''交易数量'',
            `amount` decimal(11,2) unsigned NOT NULL COMMENT ''交易金额'',
            `position_pct` decimal(5,2) unsigned NOT NULL COMMENT ''仓位占比（%）'',
            `fee` decimal(8,2) unsigned DEFAULT NULL COMMENT ''交易费用'',
            `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
            `gmt_modify` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
            PRIMARY KEY (`id`),
            KEY `idx__task_id__trade_date` (`task_id`,`trade_date`) USING BTREE
        ) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''回测-BS交易记录'';');

            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
            SET i = i + 1;
        END WHILE;
END$$
DELIMITER ;


CALL create_bt_trade_record_shards(@mode, @table_count);



-- ---------------------------------------------------------------------------------------------------------------------
-- 分表     ->     bt_position_record
-- ---------------------------------------------------------------------------------------------------------------------


DELIMITER $$
DROP PROCEDURE IF EXISTS create_bt_position_record_shards$$
CREATE PROCEDURE create_bt_position_record_shards(IN mode INT, IN table_count INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    WHILE i < table_count
        DO
            IF mode = 2 THEN
                SET @drop_sql = CONCAT('DROP TABLE IF EXISTS bt_position_record_', i, ';');
                PREPARE stmt FROM @drop_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
            END IF;

            SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS bt_position_record_', i, ' (
            `id` bigint unsigned NOT NULL COMMENT ''主键ID'',
            `task_id` bigint unsigned NOT NULL COMMENT ''回测任务ID'',
            `trade_date` date NOT NULL COMMENT ''交易日'',
            `position_type` tinyint unsigned NOT NULL COMMENT ''持仓类型：1-持仓中；2-已清仓；'',
            `stock_id` bigint unsigned NOT NULL COMMENT ''股票ID'',
            `stock_code` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ''股票代码'',
            `stock_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ''股票名称'',
            `avg_cost_price` decimal(7,3) unsigned NOT NULL COMMENT ''加权平均成本价'',
            `close_price` decimal(7,3) unsigned NOT NULL COMMENT ''收盘价'',
            `change_pct` decimal(6,2) NOT NULL COMMENT ''当日涨跌幅（%）'',
            `quantity` mediumint unsigned NOT NULL COMMENT ''持仓数量'',
            `avl_quantity` mediumint unsigned NOT NULL COMMENT ''可用数量'',
            `market_value` decimal(11,2) unsigned NOT NULL COMMENT ''市值'',
            `position_pct` decimal(6,2) unsigned NOT NULL COMMENT ''仓位占比（%）'',
            `cap_today_pnl` decimal(11,2) NOT NULL COMMENT ''当日-浮动盈亏'',
            `cap_today_pnl_pct` decimal(6,2) NOT NULL COMMENT ''当日-盈亏率（%）'',
            `cap_total_pnl` decimal(11,2) NOT NULL COMMENT ''累计-浮动盈亏'',
            `cap_total_pnl_pct` decimal(6,2) NOT NULL COMMENT ''累计-盈亏率（%）'',
            `price_total_return_pct` decimal(6,2) NOT NULL COMMENT ''首次买入价格-累计涨幅（%）'',
            `price_max_return_pct` decimal(6,2) NOT NULL COMMENT ''首次买入价格-最大涨幅（%）'',
            `price_max_drawdown_pct` decimal(6,2) NOT NULL COMMENT ''首次买入价格-最大回撤（%）'',
            `buy_date` date NOT NULL COMMENT ''买入日期'',
            `holding_days` smallint unsigned NOT NULL DEFAULT 0 COMMENT ''持仓天数'',
            `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
            `gmt_modify` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
            PRIMARY KEY (`id`),
            KEY `idx__task_id__date` (`task_id`,`trade_date`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''回测-每日持仓记录'';');

            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
            SET i = i + 1;
        END WHILE;
END$$
DELIMITER ;


CALL create_bt_position_record_shards(@mode, @table_count);