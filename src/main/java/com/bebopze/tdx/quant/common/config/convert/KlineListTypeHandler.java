package com.bebopze.tdx.quant.common.config.convert;

import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
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
 * 自定义 TypeHandler，用于处理数据库中存储的 Kline 字符串数组到 List<KlineDTO> 的转换。
 *
 * @author: bebopze
 * @date: 2025/11/24
 */
@Slf4j
public class KlineListTypeHandler extends BaseTypeHandler<List<KlineDTO>> {


    @SneakyThrows
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<KlineDTO> param, JdbcType jdbcType) {
        // List<KlineDTO>   ->   字符串（json数组）
        String jsonStr = ConvertStockKline.dtoList2JsonStr(param);
        // 序列化为 JSON字符串 存储到数据库
        ps.setString(i, jsonStr);
    }

    @Override
    public List<KlineDTO> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<KlineDTO> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<KlineDTO> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }


    private List<KlineDTO> parseJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return Lists.newArrayList();
        }

        // 字符串（json数组）   ->   List<KlineDTO>
        return ConvertStockKline.str2DTOList(jsonStr);
    }


}