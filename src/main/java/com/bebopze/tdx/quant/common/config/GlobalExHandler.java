package com.bebopze.tdx.quant.common.config;

import com.bebopze.tdx.quant.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.sql.SQLException;


/**
 * 全局统一 异常拦截
 *
 * @author: bebopze
 * @date: 2025/5/11
 */
@Slf4j
@ControllerAdvice
public class GlobalExHandler {


    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Result handleMethodArgumentNotValidException(Exception e) {

        log.error("GlobalExHandler     >>>     {}", e.getMessage(), e);


        if (e instanceof BizException) {
            Integer code = ((BizException) e).getCode();
            String msg = ((BizException) e).getMsg();
            if (null == code) {
                return Result.ERR(msg);
            } else {
                return Result.of(null, false, code, msg);
            }
        } else if (e instanceof MissingServletRequestParameterException) {
            return Result.ERR("必入参数未填写");
        } else if (e instanceof MethodArgumentNotValidException) {
            return Result.ERR("必入参数未填写");
        } else if (e instanceof IllegalArgumentException) {
            return Result.ERR(e.getMessage());
        } else if (e instanceof NullPointerException) {
            return Result.ERR(e.getMessage());
        } else if (e instanceof BadSqlGrammarException) {
            return Result.ERR("服务器异常,请联系管理员!");
        } else if (e instanceof SQLException) {
            return Result.ERR("服务器异常,请联系管理员!");
        } else if (e instanceof RuntimeException) {
            return Result.ERR("服务器异常,请联系管理员!");
        } else {
            String errorMsg = e.getMessage();
            return Result.ERR(errorMsg == null || "".equals(errorMsg) ? "未知错误" : errorMsg);
        }
    }

}
