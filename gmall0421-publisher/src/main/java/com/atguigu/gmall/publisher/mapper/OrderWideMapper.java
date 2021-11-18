package com.atguigu.gmall.publisher.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Author: Felix
 * Date: 2020/9/23
 * Desc:
 */
public interface OrderWideMapper {
    //查询当日交易额总数
    public BigDecimal selectOrderAmountTotal(String date);

    //查询当日交易额分时值
    public List<Map> selectOrderAmountHourMap(String date);
}
