package com.bebopze.tdx.quant.common.config;

import lombok.Data;


/**
 * 自定义 业务异常
 *
 * @author: bebopze
 * @date: 2025/5/11
 */
@Data
public class BizException extends RuntimeException {

    private Integer code;

    private String msg;


    public BizException() {
        super();
    }


    public BizException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public BizException(Integer code,
                        String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

}