package com.bebopze.tdx.quant.optionbs;

import com.bebopze.tdx.quant.common.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;


/**
 * 期权 BS 回测 API
 *
 * @author bebopze
 * @date 2026/11/26
 */
@RestController
@RequestMapping("/api/option/backtest")
@Tag(name = "期权BS回测", description = "中期顶底信号 · ETF/指数期权 BS 回测")
public class OptionBsBacktestController {

    @Autowired
    private OptionBsBacktestService service;

    @Autowired
    private OptionBsRepository repo;


    @Operation(summary = "创建并异步执行回测")
    @PostMapping("/run")
    public Result<Map<String, Object>> run(@RequestBody OptionBsParams params) {
        return Result.SUC(service.submit(params));
    }

    @Operation(summary = "回测记录分页")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "100") int size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) String signalMode,
                                            @RequestParam(required = false) String underlyingCode,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate,
                                            @RequestParam(defaultValue = "totalReturn") String sortBy,
                                            @RequestParam(defaultValue = "desc") String sortDir) {

        return Result.SUC(service.page(page, size, keyword, status, signalMode, underlyingCode, startDate, endDate, sortBy, sortDir));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable long id) {
        Map<String, Object> m = service.detail(id);
        return m == null ? Result.ERR("任务不存在") : Result.SUC(m);
    }

    @Operation(summary = "统计 / 风险 / 月度 / 归因")
    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable long id,
                                             @RequestParam(required = false) String from,
                                             @RequestParam(required = false) String to) {

        return Result.SUC(service.stats(id, from, to));
    }

    @Operation(summary = "净值曲线")
    @GetMapping("/{id}/equity")
    public Result<List<Map<String, Object>>> equity(@PathVariable long id,
                                                    @RequestParam(required = false) String from,
                                                    @RequestParam(required = false) String to) {
        return Result.SUC(service.equity(id, from, to));
    }

    @Operation(summary = "每日持仓")
    @GetMapping("/{id}/positions")
    public Result<Map<String, Object>> positions(@PathVariable long id,
                                                 @RequestParam(required = false) String date,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "100") int size) {

        return Result.SUC(service.positions(id, date, page, size));
    }

    @Operation(summary = "每日交易")
    @GetMapping("/{id}/trades")
    public Result<Map<String, Object>> trades(@PathVariable long id,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "100") int size,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) String reason,
                                              @RequestParam(required = false) String from,
                                              @RequestParam(required = false) String to) {

        return Result.SUC(service.trades(id, page, size, type, reason, from, to));
    }

    @Operation(summary = "每日清仓")
    @GetMapping("/{id}/closes")
    public Result<Map<String, Object>> closes(@PathVariable long id,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "100") int size,
                                              @RequestParam(required = false) String reason,
                                              @RequestParam(required = false) String from,
                                              @RequestParam(required = false) String to) {

        return Result.SUC(service.closes(id, page, size, reason, from, to));
    }

    @Operation(summary = "信号事件")
    @GetMapping("/{id}/signals")
    public Result<List<Map<String, Object>>> signals(@PathVariable long id) {
        return Result.SUC(service.signals(id));
    }

    @Operation(summary = "运行日志")
    @GetMapping("/{id}/logs")
    public Result<List<Map<String, Object>>> logs(@PathVariable long id,
                                                  @RequestParam(defaultValue = "200") int limit) {

        return Result.SUC(service.logs(id, limit));
    }

    @Operation(summary = "标的元数据（创建页）")
    @GetMapping("/underlyings")
    public Result<List<Map<String, Object>>> underlyings() {
        return Result.SUC(service.underlyings());
    }

    @Operation(summary = "取消任务")
    @PostMapping("/{id}/cancel")
    public Result<String> cancel(@PathVariable long id) {
        service.cancel(id);
        return Result.SUC("CANCELED");
    }

    @Operation(summary = "收藏 / 取消收藏")
    @PostMapping("/{id}/favorite")
    public Result<String> favorite(@PathVariable long id, @RequestParam int flag) {
        repo.favorite(id, flag);
        return Result.SUC("OK");
    }

    @Operation(summary = "逻辑删除")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable long id) {
        repo.logicDelete(id);
        return Result.SUC("OK");
    }

    @Operation(summary = "导出 CSV")
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable long id,
                                         @RequestParam(defaultValue = "equity") String type) {

        String csv = service.exportCsv(id, type);
        String name = "option-bs-" + id + "-" + type + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";

        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                             .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                             .body(csv.getBytes(StandardCharsets.UTF_8));
    }


}