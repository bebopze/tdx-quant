package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.util.MemorySize;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.impl.InitDataServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 性能监控
 *
 * @author: bebopze
 * @date: 2025/9/1
 */
@RestController
@RequestMapping("/api/monitor")
@Tag(name = "性能监控", description = "性能监控")
public class MonitorController {


    @Autowired
    private InitDataService initDataService;


    /**
     * memorySize
     *
     * @return
     */
    @Operation(summary = "memorySize - backtestCache", description = "memorySize - backtestCache")
    @GetMapping(value = "/memorySize/backtestCache")
    public Result<String> memorySize() {

        // double sizeMB = MemorySize.sizeMB(InitDataServiceImpl.data);
        // double sizeMB2 = MemorySize.sizeMB(BacktestCache.stockFunCache);

        String size = MemorySize.format(InitDataServiceImpl.data);
        return Result.SUC(size);
    }


}
