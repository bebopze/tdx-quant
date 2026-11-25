package com.bebopze.tdx.quant.optionbs;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 期权 BS 回测 - Spring 装配
 *
 * @author bebopze
 * @date 2026/11/26
 */
@Configuration
public class OptionBsConfig {

    @Bean
    public OptionBsDb optionBsDb(@Qualifier("dataSource") DataSource dataSource) {
        return dataSource::getConnection;
    }

    @Bean
    public OptionBsRepository optionBsRepository(OptionBsDb db) {
        return new OptionBsRepository(db);
    }

    @Bean(name = "optionBsExecutor", destroyMethod = "shutdown")
    public ExecutorService optionBsExecutor() {
        AtomicInteger seq = new AtomicInteger();
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "option-bs-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        return Executors.newFixedThreadPool(2, tf);
    }
}
