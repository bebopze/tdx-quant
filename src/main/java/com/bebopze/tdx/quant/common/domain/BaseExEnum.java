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


    SUC(200, "success"),


    ERR(500, "系统异常"),


    NOT_LOGIN(301, "操作会话已失效，请重新登录！"),

    NOT_AUTH(302, "您无该权限"),


    API_404(404, "该接口不存在");


    @Getter
    @Setter
    private Integer code;

    @Getter
    @Setter
    private String msg;

}
