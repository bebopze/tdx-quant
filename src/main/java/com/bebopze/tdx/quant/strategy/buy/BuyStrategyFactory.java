package com.bebopze.tdx.quant.strategy.buy;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * B策略 - 工厂
 *
 * @author: bebopze
 * @date: 2025/7/17
 */
@Data
@Slf4j
@Component
public class BuyStrategyFactory {


    /**
     * key   -> strategy.key()
     *
     * value -> 对应的 Spring Bean
     */
    private final Map<String, BuyStrategy> strategyMap;


    public BuyStrategyFactory(List<BuyStrategy> strategies) {
        strategyMap = strategies.stream().collect(Collectors.toMap(BuyStrategy::key, Function.identity()));
    }


    public BuyStrategy get(String key) {
        BuyStrategy s = strategyMap.get(key);
        if (s == null) {
            throw new IllegalArgumentException("unknown strategy:" + key);
        }
        return s;
    }

}