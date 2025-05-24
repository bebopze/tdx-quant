package com.bebopze.tdx.quant.common.util;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.dal.service.impl.BaseStockServiceImpl;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;


/**
 * 静态调用   Mybatis-Plus
 *
 * @author: bebopze
 * @date: 2025/5/18
 */
public class MybatisPlusUtil {


    private static final SqlSessionFactory SQL_SESSION_FACTORY;


    static {

        // 1. 数据源
        HikariConfig hk = new HikariConfig();
        hk.setJdbcUrl(PropsUtil.getProperty("spring.datasource.url"));
        hk.setUsername(PropsUtil.getProperty("spring.datasource.username"));
        hk.setPassword(PropsUtil.getProperty("spring.datasource.password"));
        hk.setDriverClassName(PropsUtil.getProperty("spring.datasource.driver-class-name"));

        DataSource ds = new HikariDataSource(hk);


        // 2. 事务工厂（可换成 SpringManagedTransactionFactory）
        TransactionFactory txFactory = new JdbcTransactionFactory();


        // 3. Environment
        Environment env = new Environment("prod", txFactory, ds);  // 名称任意


        // 4. MyBatis-Plus Configuration
        MybatisConfiguration config = new MybatisConfiguration(env);
        config.setMapUnderscoreToCamelCase(true);
        config.setJdbcTypeForNull(null);
        config.setEnvironment(env);
        // 手动注册所有 Mapper 接口
        config.addMapper(BaseStockMapper.class);


        // 5. 构建 SqlSessionFactory
        SQL_SESSION_FACTORY = new MybatisSqlSessionFactoryBuilder().build(config);
    }


    public static SqlSessionFactory getSqlSessionFactory() {
        return SQL_SESSION_FACTORY;
    }

    // 获取 Mapper 实例
    public static <T> T getMapper(Class<T> mapperClass) {
        SqlSession session = SQL_SESSION_FACTORY.openSession();
        return session.getMapper(mapperClass);
    }

    public static IBaseStockService getBaseStockService() {
        BaseStockMapper mapper = getMapper(BaseStockMapper.class);

        BaseStockServiceImpl service = new BaseStockServiceImpl();
        service.injectMapper(mapper);

        return service;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {

        SqlSession session = MybatisPlusUtil.getSqlSessionFactory().openSession();


        String stockCode = "000001";


        // BaseStockMapper mapper = getMapper(BaseStockMapper.class);
        BaseStockMapper mapper = session.getMapper(BaseStockMapper.class);
        BaseStockDO baseStockDO = mapper.getByCode(stockCode);

        System.out.println(baseStockDO);


        // 插入数据
//        BaseStockDO entity = new BaseStockDO();
//        entity.setCode(stockCode);
//        entity.setName("平安银行");
//        entity.setId(1L);
//
//        mapper.insertOrUpdate(entity);


        // session.commit();
    }
}
