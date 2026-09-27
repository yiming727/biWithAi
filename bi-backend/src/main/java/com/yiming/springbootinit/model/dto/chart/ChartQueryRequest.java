package com.yiming.springbootinit.model.dto.chart;

import com.yiming.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *

 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 用户id
     */
    private Long userId;

    @Override
    public String toString() {
        return "ChartQueryRequest{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", goal='" + goal + '\'' +
                ", chartType='" + chartType + '\'' +
                ", userId=" + userId +
                '}';
    }

    private static final long serialVersionUID = 1L;
}