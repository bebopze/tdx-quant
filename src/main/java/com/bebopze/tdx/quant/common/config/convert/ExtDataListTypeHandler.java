package com.bebopze.tdx.quant.common.config.convert;

import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


/**
 * 自定义 TypeHandler，用于处理数据库中存储的 ExtData 字符串数组到 List<ExtDataDTO> 的转换。
 *
 * @author: bebopze
 * @date: 2025/11/24
 */
@Slf4j
public class ExtDataListTypeHandler extends BaseTypeHandler<List<ExtDataDTO>> {


    @SneakyThrows
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<ExtDataDTO> parameter, JdbcType jdbcType) {
        // List<ExtDataDTO>   ->   字符串（json数组）
        String jsonStr = ConvertStockExtData.dtoList2JsonStr(parameter);
        // 序列化为 JSON字符串 存储到数据库
        ps.setString(i, jsonStr);
    }

    @Override
    public List<ExtDataDTO> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String jsonStr = rs.getString(columnName);
        return parseJsonStringToList(jsonStr);
    }

    @Override
    public List<ExtDataDTO> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String jsonStr = rs.getString(columnIndex);
        return parseJsonStringToList(jsonStr);
    }

    @Override
    public List<ExtDataDTO> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String jsonStr = cs.getString(columnIndex);
        return parseJsonStringToList(jsonStr);
    }

    private List<ExtDataDTO> parseJsonStringToList(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return Lists.newArrayList();
        }

        // 字符串（json数组）   ->   List<ExtDataDTO>
        return ConvertStockExtData.extDataHis2DTOList(jsonStr);
    }


}