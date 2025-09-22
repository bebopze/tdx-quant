package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.dal.entity.CfgAccountDO;
import com.bebopze.tdx.quant.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 配置-持仓账户
 *
 * @author: bebopze
 * @date: 2025/9/23
 */
@RestController
@RequestMapping("/api/cfg/account")
@Tag(name = "配置-持仓账户", description = "配置-持仓账户")
public class CfgAccountController {


    @Autowired
    private AccountService accountService;


    @Operation(summary = "创建", description = "创建")
    @PostMapping("/create")
    public Result<Long> create(@RequestBody CfgAccountDO entity) {
        return Result.SUC(accountService.create(entity));
    }

    @Operation(summary = "修改", description = "修改")
    @PostMapping("/update")
    public Result<Void> update(@RequestBody CfgAccountDO entity) {
        accountService.update(entity);
        return Result.SUC();
    }

    @Operation(summary = "查询", description = "查询")
    @GetMapping("/info")
    public Result<CfgAccountDO> info(@Schema(description = "ID", example = "1")
                                     @RequestParam Long id) {
        return Result.SUC(accountService.info(id));
    }

    @Operation(summary = "删除", description = "删除")
    @GetMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        // 暂不开放
        // accountService.delete(id);
        return Result.ERR("暂不开放【DEL】接口");
    }

}