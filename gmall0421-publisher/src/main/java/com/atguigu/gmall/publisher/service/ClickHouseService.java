package com.atguigu.gmall.publisher.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Author: Felix
 * Date: 2020/9/23
 * Desc:
 */
public interface ClickHouseService {
    //获取当日交易额总和
    public BigDecimal getOrderAmountTotal(String date);

    //获取当日交易额分时值
    public Map<String,BigDecimal> getOrderAmountHourMap(String date);
}
