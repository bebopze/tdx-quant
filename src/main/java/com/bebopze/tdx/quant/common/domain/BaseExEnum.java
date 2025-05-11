package com.bebopze.tdx.quant.common.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


/**
 * 异常
 *
 * @author: bebopze
 * @date: 2025/5/6
 */
@AllArgsConstructor
public enum BaseExEnum {


    // ----------------------------------------------- 公告异常 ---------------------------------------------------------


    SUC(200, "success"),


    ERR(500, "系统异常"),


    NOT_LOGIN(401, "操作会话已失效，请重新登录！"),
    NOT_AUTH(402, "您无该权限"),
    API_404(404, "该接口不存在"),


    // ----------------------------------------------- 业务异常 ---------------------------------------------------------


    TREAD_EM_COOKIE_EXPIRED(1001, "东方财富 - cookie过期，请重新登录！"),


    ;


    @Getter
    @Setter
    private Integer code;

    @Getter
    @Setter
    private String msg;

}