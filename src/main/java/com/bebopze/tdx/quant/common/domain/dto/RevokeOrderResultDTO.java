package com.bebopze.tdx.quant.common.domain.dto;

import lombok.Data;

import java.io.Serializable;


/**
 * 撤单结果
 *
 * @author: bebopze
 * @date: 2025/5/11
 */
@Data
public class RevokeOrderResultDTO implements Serializable {


    // 委托日期_委托编号
    private String revoke;

    // 撤单结果-描述
    private String resultDesc;


    // 撤单成功
    private boolean suc;
}
