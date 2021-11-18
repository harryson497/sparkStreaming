package com.atguigu.gmall.publisher.service.impl;

import com.atguigu.gmall.publisher.mapper.TrademarkMapper;
import com.atguigu.gmall.publisher.service.MySQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Author: Felix
 * Date: 2020/9/24
 * Desc:
 */
@Service
public class MySQLServiceImpl implements MySQLService {

    @Autowired
    TrademarkMapper trademarkMapper;

    @Override
    public List<Map> getTrademarkSum(String startDate, String endDate, int topN) {
        return trademarkMapper.selectTrademarkSum(startDate,endDate,topN);
    }
}
