package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.QaTopBlockDO;
import com.bebopze.tdx.quant.dal.mapper.QaTopBlockMapper;
import com.bebopze.tdx.quant.dal.service.IQaTopBlockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 量化分析 - LV3主线板块（板块-月多2） 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-09-24
 */
@Service
public class QaTopBlockServiceImpl extends ServiceImpl<QaTopBlockMapper, QaTopBlockDO> implements IQaTopBlockService {


    @Override
    public Map<LocalDate, Long> dateIdMap() {

        List<QaTopBlockDO> entityList = baseMapper.listAllSimple();

        return entityList.stream().collect(
                Collectors.toMap(
                        QaTopBlockDO::getDate,
                        QaTopBlockDO::getId
                ));
    }


    @Override
    public QaTopBlockDO getByDate(LocalDate date) {
        return baseMapper.getByDate(date);
    }


    @Override
    public List<QaTopBlockDO> lastN(LocalDate date, int N) {
        return baseMapper.lastN(date, N);
    }

    @Override
    public List<QaTopBlockDO> beforeAfterN(LocalDate date, int N) {
        date = date.plusDays(N / 5 * 7);
        return lastN(date, N);
    }


}