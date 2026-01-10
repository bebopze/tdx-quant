package com.bebopze.tdx.quant.common.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.InputStream;


/**
 * Mybatis 配置
 *
 * @author bebopze
 * @date 2026/1/10
 */
@Slf4j
@Configuration
@MapperScan(basePackages = "com.bebopze.tdx.quant.dal.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
public class MybatisConfig {


    @Value("${spring.profiles.active:dev}")
    private String profile;


    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource() throws Exception {
        log.info("使用 [{}] 配置文件初始化 ShardingSphere 数据源...", profile);


        ClassPathResource resource = new ClassPathResource("shardingsphere-" + profile + ".yml");
        if (!resource.exists()) {
            throw new RuntimeException("shardingsphere-prod.yml 文件不存在！请检查 src/main/resources/shardingsphere" + profile + ".yml");
        }

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] yamlBytes = inputStream.readAllBytes();
            DataSource dataSource = YamlShardingSphereDataSourceFactory.createDataSource(yamlBytes);
            log.info("ShardingSphere 数据源创建成功: {}", dataSource.getClass().getName());
            return dataSource;
        }
    }


    @Bean(name = "sqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        log.info("初始化 SqlSessionFactory...");


        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // XML 文件目录
        Resource[] resources = resolver.getResources("classpath*:com/bebopze/tdx/quant/dal/mapper/*.xml");
        if (resources.length > 0) {
            factory.setMapperLocations(resources);
            log.info("找到 {} 个 Mapper XML 文件", resources.length);
        } else {
            log.warn("未找到 Mapper XML 文件！路径可能有问题");
        }


        factory.setTypeAliasesPackage("com.bebopze.tdx.quant.dal.entity");

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.log4j2.Log4j2Impl.class);
        factory.setConfiguration(configuration);

        return factory.getObject();
    }


    @Bean(name = "sqlSessionTemplate")
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        log.info("初始化 SqlSessionTemplate...");
        return new SqlSessionTemplate(sqlSessionFactory);
    }


    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        log.info("初始化事务管理器...");
        return new DataSourceTransactionManager(dataSource);
    }


}