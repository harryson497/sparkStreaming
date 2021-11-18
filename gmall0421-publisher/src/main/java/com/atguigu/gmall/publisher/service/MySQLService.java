package com.atguigu.gmall.publisher.service;

import java.util.List;
import java.util.Map;

/**
 * Author: Felix
 * Date: 2020/9/24
 * Desc: 品牌统计service
 */
public interface MySQLService {

    public List<Map> getTrademarkSum(String startDate,String endDate,int topN);
}
