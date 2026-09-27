package com.yiming.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiming.springbootinit.mapper.ChartMapper;
import com.yiming.springbootinit.model.entity.Chart;
import com.yiming.springbootinit.service.ChartService;
import org.springframework.stereotype.Service;

/**
* @author 29560
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2025-02-09 11:20:07
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService {

}




