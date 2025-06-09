package com.bebopze.tdx.quant.common.tdxfun;

import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;

import java.util.List;

/**
 * @author: bebopze
 * @date: 2025/6/10
 */
public class Tools {


    public static void extData(String extDataHis) {

        List<ExtDataDTO> extDataDTOList = ConvertStockExtData.extDataHis2DTOList(extDataHis);

    }
}
