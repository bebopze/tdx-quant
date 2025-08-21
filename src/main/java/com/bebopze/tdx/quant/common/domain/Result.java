package com.bebopze.tdx.quant.common.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.Collection;


/**
 * 统一返回 Result类
 *
 * @author: bebopze
 * @date: 2025/5/7
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = -2361820086956983473L;


    // 数据明细
    private T data;

    private Boolean success;

    private Integer code;

    private String msg;


    /**
     * 为null时,不参与序列化
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalNum;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer pageIndex;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer pageSize;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalPage;


    public static <T> Result<T> of(T data,
                                   boolean success,
                                   BaseExEnum baseExEnum) {
        if (null != baseExEnum) {
            return of(data, success, baseExEnum.getCode(), baseExEnum.getMsg());
        } else {
            return of(data, success, null, null);
        }
    }

    public static <T> Result<T> of(T data,
                                   boolean success,
                                   Integer code,
                                   String msg) {
        Result<T> result = new Result<>();
        result.setData(data);
        result.setSuccess(success);
        result.setCode(code);
        result.setMsg(msg);
        if (data instanceof Collection) {
            result.setTotalNum(((Collection) data).size());
        }
        return result;
    }

    public static <T> Result<T> of(T data,
                                   boolean success,
                                   BaseExEnum baseExEnum,
                                   Integer totalNum,
                                   Integer pageIndex,
                                   Integer pageSize) {
        return of(data, success, baseExEnum, totalNum, pageIndex, pageSize, null);
    }

    public static <T> Result<T> of(T data,
                                   boolean success,
                                   BaseExEnum baseExEnum,
                                   Integer totalNum,
                                   Integer pageIndex,
                                   Integer pageSize,
                                   String msg) {
        Result<T> result = new Result<>();
        result.setData(data);
        result.setSuccess(success);
        if (null != baseExEnum) {
            result.setCode(baseExEnum.getCode());
            result.setMsg(baseExEnum.getMsg());
        }
        if (data instanceof Collection && null == totalNum) {
            result.setTotalNum(((Collection) data).size());
        }
        result.setTotalNum(totalNum);
        result.setPageIndex(pageIndex);
        result.setPageSize(pageSize);
        result.setTotalPage((pageSize == null || pageSize == 0) ? null : (totalNum % pageSize == 0 ? totalNum / pageSize : (totalNum / pageSize + 1)));
        result.setMsg(msg);
        return result;
    }


    public static <T> Result<T> SUC() {
        return SUC(null);
    }

    public static <T> Result<T> SUC(T data) {
        return of(data, true, BaseExEnum.SUC);
    }

    public static <T> Result<T> SUC(T data,
                                    Integer totalNum,
                                    Integer pageIndex,
                                    Integer pageSize,
                                    String msg) {
        return of(data, true, BaseExEnum.SUC, totalNum, pageIndex, pageSize, msg);
    }

    public static <T> Result<T> SUC(T data,
                                    Integer totalNum,
                                    Integer pageIndex,
                                    Integer pageSize) {
        return of(data, true, BaseExEnum.SUC, totalNum, pageIndex, pageSize);
    }

    public static <T> Result<T> SUC(T data,
                                    BaseExEnum baseExEnum) {
        return of(data, true, baseExEnum);
    }

    public static <T> Result<T> SUC(T data,
                                    String msg) {
        return of(data, true, BaseExEnum.SUC.getCode(), msg);
    }

    public static <T> Result<T> ERR(BaseExEnum baseExEnum) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        if (null != baseExEnum) {
            result.setCode(baseExEnum.getCode());
            result.setMsg(baseExEnum.getMsg());
        }
        result.setData(null);
        return result;
    }

    public static <T> Result<T> ERR(String msg) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(BaseExEnum.ERR.getCode());
        result.setMsg(msg);
        result.setData(null);
        return result;
    }


}