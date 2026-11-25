package com.bebopze.tdx.quant.optionbs;

import com.bebopze.tdx.quant.dal.mapper.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public OptionBsRepository optionBsRepository(ObtTaskMapper taskMapper,
                                                 ObtDailyEquityMapper equityMapper,
                                                 ObtDailyPositionMapper positionMapper,
                                                 ObtTradeMapper tradeMapper,
                                                 ObtCloseRecordMapper closeMapper,
                                                 ObtSignalEventMapper signalMapper,
                                                 ObtTaskLogMapper logMapper,
                                                 ObtMetaMapper metaMapper) {
        return new OptionBsRepository(taskMapper, equityMapper, positionMapper, tradeMapper,
                                      closeMapper, signalMapper, logMapper, metaMapper);
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
