package com.atguigu.gmall.publisher.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Author: Felix
 * Date: 2020/9/24
 * Desc: 品牌统计mapper接口
 */
public interface TrademarkMapper {
    /**
     * 查询某一个时间范围  品牌交易额的topN
     * @param startDate
     * @param endDate
     * @param topN
     * @return
     */
    public List<Map> selectTrademarkSum(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("topN") int topN);
}
