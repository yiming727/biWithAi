package com.yiming.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiming.springbootinit.model.entity.Chart;

import java.util.List;
import java.util.Map;

/**
* @author 29560
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2025-02-09 11:20:07
* @Entity generator.domain.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {

    List<Map<String, Object>> queryChartData(String querySql);
}




