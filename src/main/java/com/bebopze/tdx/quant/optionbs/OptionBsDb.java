package com.bebopze.tdx.quant.optionbs;

import java.sql.Connection;
import java.sql.SQLException;


/**
 * 回测数据源抽象：Spring DataSource / CLI DriverManager 共用
 *
 * @author bebopze
 * @date 2026/11/26
 */
@FunctionalInterface
public interface OptionBsDb {
    Connection getConnection() throws SQLException;
}
