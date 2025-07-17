package com.bebopze.tdx.quant.strategy.sell;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * S策略 - 工厂
 *
 * @author: bebopze
 * @date: 2025/7/17
 */
@Data
@Slf4j
@Component
public class SellStrategyFactory {


    /**
     * key   -> strategy.key()
     *
     * value -> 对应的 Spring Bean
     */
    private final Map<String, SellStrategy> strategyMap;


    public SellStrategyFactory(List<SellStrategy> strategies) {
        strategyMap = strategies.stream().collect(Collectors.toMap(SellStrategy::key, Function.identity()));
    }


    public SellStrategy get(String key) {
        SellStrategy s = strategyMap.get(key);
        if (s == null) {
            throw new IllegalArgumentException("unknown strategy:" + key);
        }
        return s;
    }

}