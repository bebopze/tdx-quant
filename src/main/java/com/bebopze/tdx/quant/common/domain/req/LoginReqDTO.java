package com.bebopze.tdx.quant.common.domain.req;

import lombok.Data;


/**
 * @author: bebopze
 * @date: 2025/4/29
 */
@Data
public class LoginReqDTO {


    // userId: 500000123456
    // password: P2WAHwQxmcNl0Ab76D5RsLcmy1zMFsS9gpMRnfVmR9MkxxEkwkJEg9pOpF7IjPgMwATiXHcxvlWckkechHev71iEdGTp3Jc0kamz0fN4b2aVuSBLEpo06tdCWX90PJscUzl2d2T2co1KiKC5q0x1ajjj6M0jKV2GeF9K9PNUL+w=
    // randNumber: 0.8746451723316416
    // identifyCode: 2816
    // duration: 1800
    // authCode:
    // type: Z
    // secInfo:


    private String userId;

    private String password;

    private String randNumber;

    private String identifyCode;

    private String duration;

    private String authCode;

    private String type;

    private String secInfo;
}