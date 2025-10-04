package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.JsonFileWriterAndReader;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * <p>
 * 板块/指数-实时行情（以 tdx 为准） 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
@Slf4j
@Service
public class BaseBlockServiceImpl extends ServiceImpl<BaseBlockMapper, BaseBlockDO> implements IBaseBlockService {


    @Override
    public Long getIdByCode(String code) {
        return baseMapper.getIdByCode(code);
    }

    @Override
    public BaseBlockDO getByCode(String code) {
        return baseMapper.getByCode(code);
    }

    @Override
    public BaseBlockDO getSimpleByCode(String code) {
        return baseMapper.getSimpleByCode(code);
    }

    @Override
    public BaseBlockDO getSimpleById(Long id) {
        return baseMapper.getSimpleById(id);
    }


    @Override
    public Map<String, Long> codeIdMap() {

        List<BaseBlockDO> entityList = baseMapper.listAllSimple();


        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseBlockDO::getCode,
                                                          BaseBlockDO::getId,

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return code_id_map;
    }


    @Override
    public Map<String, Long> codeIdMap(Collection<String> blockCodeList) {

        List<BaseBlockDO> entityList = listSimpleByCodeList(blockCodeList);


        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseBlockDO::getCode,  // 键：从对象中提取 code 字段
                                                          BaseBlockDO::getId,    // 值：从对象中提取 id 字段

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return code_id_map;
    }


    @Override
    public List<BaseBlockDO> listSimpleByCodeList(Collection<String> blockCodeList) {
        if (CollectionUtils.isEmpty(blockCodeList)) {
            return Collections.emptyList();
        }

        return baseMapper.listSimpleByCodeList(blockCodeList);
    }


    @Override
    public List<BaseBlockDO> listAllSimple() {
        return baseMapper.listAllSimple();
    }

    @Override
    public List<BaseBlockDO> listAllKline() {
        return listAllKline(false);
    }

    @TotalTime
    @Override
    public List<BaseBlockDO> listAllKline(boolean refresh) {
        log.info("listAllKline     >>>     refresh : {}", refresh);


        // listAllFromDiskCache >>> totalTime :6.9 s
        return listAllFromDiskCache(refresh);


        // return baseMapper.listAllKline();
    }


    @Override
    public List<BaseBlockDO> listAllRpsKline() {
        return baseMapper.listAllRpsKline();
    }


    // -----------------------------------------------------------------------------------------------------------------


    private List<BaseBlockDO> listAllFromDiskCache(boolean refresh) {
        long start = System.currentTimeMillis();


        // read Cache
        List<BaseBlockDO> list = JsonFileWriterAndReader.readLargeListFromFile___block_listAllKline();

        if (CollectionUtils.isEmpty(list) || refresh) {
            list = baseMapper.listAllKline();


            // write Cache
            JsonFileWriterAndReader.writeLargeListToFile___block_listAllKline(list);
        }


        //  listAllFromDiskCache     >>>     totalTime : 6.9s
        log.info("listAllFromDiskCache     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start));
        return list;
    }

}
