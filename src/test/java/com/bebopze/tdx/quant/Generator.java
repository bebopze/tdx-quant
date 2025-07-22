package com.bebopze.tdx.quant;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.bebopze.tdx.quant.common.util.PropsUtil;

import java.nio.file.Paths;
import java.sql.Types;


/**
 * mybatis-plus 代码生成器
 *
 * @author: bebopze
 * @date: 2025-05-09
 */
public class Generator {


    public static void main(String[] args) {


        String url = PropsUtil.getProperty("spring.datasource.url");
        String username = PropsUtil.getProperty("spring.datasource.username");
        String password = PropsUtil.getProperty("spring.datasource.password");


        FastAutoGenerator.create(url, username, password)

                         .globalConfig(builder -> builder
                                 .author("bebopze")
                                 .enableSpringdoc()
                                 .outputDir(Paths.get(System.getProperty("user.dir")) + "/src/main/java")
                                 .commentDate("yyyy-MM-dd")
                         )

                         .dataSourceConfig(builder ->
                                                   builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                                                       int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                                                       if (typeCode == Types.TINYINT) {
                                                           // 自定义类型转换
                                                           return DbColumnType.INTEGER;
                                                       }
                                                       return typeRegistry.getColumnType(metaInfo);
                                                   })
                         )

                         .packageConfig(builder -> builder
                                 .parent("com.bebopze.tdx.quant.dal2")
                                 .entity("entity")
                                 .mapper("mapper")
                                 .service("service")
                                 .serviceImpl("service.impl")
                                 .xml("mapper.xml")
                         )

                         .strategyConfig(builder -> {
                             builder
                                     // 设置需要生成的表名
                                     // .addInclude("base_stock", "base_block", "base_block_rela_stock")
                                     // .addInclude("base_block_new", "base_block_new_rela_stock")

                                     // .addInclude("bt_task", "bt_trade_record", "bt_position_record", "bt_daily_return")

                                     .addInclude(/*"qa_block_new_rela_stock_his",*/ "qa_market_mid_cycle")

                                     .entityBuilder()
                                     .formatFileName("%sDO")
                                     // 启用 Lombok
                                     .enableLombok()
                                     // 启用字段注解
                                     .enableTableFieldAnnotation()
                                     .enableFileOverride()


                                     .mapperBuilder().enableFileOverride()
                                     .serviceBuilder().enableFileOverride()


                                     // 不生成 Controller
                                     .controllerBuilder().disable();
                         })

                         // 使用 Freemarker引擎模板，默认的是 Velocity引擎模板
                         .templateEngine(new FreemarkerTemplateEngine())
                         .execute();
    }

}