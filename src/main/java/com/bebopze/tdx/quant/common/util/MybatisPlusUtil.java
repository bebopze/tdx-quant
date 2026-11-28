package com.bebopze.tdx.quant.common.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.*;
import com.bebopze.tdx.quant.optionbs.OptionBsRepository;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.dal.service.impl.BaseBlockServiceImpl;
import com.bebopze.tdx.quant.dal.service.impl.BaseStockServiceImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.JdbcType;

import static com.bebopze.tdx.quant.common.util.PropsUtil.*;


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
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(DATA_SOURCE_URL);
        ds.setUsername(DATA_SOURCE_USERNAME);
        ds.setPassword(DATA_SOURCE_PASSWORD);
        ds.setDriverClassName(PropsUtil.getProperty("dataSources.ds_common.driverClassName"));


        // 2. 事务工厂（可换成 SpringManagedTransactionFactory）
        TransactionFactory txFactory = new JdbcTransactionFactory();


        // 3. Environment
        Environment env = new Environment("prod", txFactory, ds);  // 名称任意


        // 4. MyBatis-Plus Configuration
        MybatisConfiguration config = new MybatisConfiguration(env);
        config.setMapUnderscoreToCamelCase(true);
        config.setJdbcTypeForNull(JdbcType.NULL);
        config.setEnvironment(env);

        // 手动注册所有 Mapper 接口
        config.addMapper(BaseStockMapper.class);
        config.addMapper(BaseBlockMapper.class);
        config.addMapper(BaseOptionMapper.class);
        config.addMapper(BaseOptionUnderlyingMapper.class);
        config.addMapper(QaMarketMidCycleMapper.class);

        // 期权 BS 回测
        config.addMapper(ObtTaskMapper.class);
        config.addMapper(ObtDailyEquityMapper.class);
        config.addMapper(ObtDailyPositionMapper.class);
        config.addMapper(ObtTradeMapper.class);
        config.addMapper(ObtCloseRecordMapper.class);
        config.addMapper(ObtSignalEventMapper.class);
        config.addMapper(ObtTaskLogMapper.class);
        config.addMapper(ObtTemplateMapper.class);
        config.addMapper(ObtMetaMapper.class);


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


    /**
     * 期权 BS 回测 - 非 Spring 环境（CLI）仓储
     */
    public static OptionBsRepository getOptionBsRepository() {
        SqlSession session = SQL_SESSION_FACTORY.openSession(true);
        return new OptionBsRepository(
                session.getMapper(ObtTaskMapper.class),
                session.getMapper(ObtDailyEquityMapper.class),
                session.getMapper(ObtDailyPositionMapper.class),
                session.getMapper(ObtTradeMapper.class),
                session.getMapper(ObtCloseRecordMapper.class),
                session.getMapper(ObtSignalEventMapper.class),
                session.getMapper(ObtTaskLogMapper.class),
                session.getMapper(ObtMetaMapper.class));
    }


    public static IBaseBlockService getBaseBlockService() {
        BaseBlockMapper mapper = getMapper(BaseBlockMapper.class);

        BaseBlockServiceImpl service = new BaseBlockServiceImpl();
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