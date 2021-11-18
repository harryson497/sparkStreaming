package com.atguigu.gmall.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Author: Felix
 * Date: 2020/9/8
 * Desc:
 */
//@Controller
//标记是一个Spring组件，对象的创建交给Spring容器管理
@RestController
@Slf4j
public class LoggerController {

    //注入KafkaTemplate
    @Autowired
    KafkaTemplate kafkaTemplate;

    //表示接收http://localhost:8080/applog,交给当前方法进行处理
    @RequestMapping("/applog")
    public String applog(@RequestBody String applog){
        //System.out.println(log);
        //将采集到的数据进行落盘操作
        log.info(applog);
        //将采集到的数据进行分流（事件|启动），发送到Kafka对应的主题去
        JSONObject jsonObject = JSON.parseObject(applog);
        if(jsonObject.getJSONObject("start")!=null){
            //启动日志
            kafkaTemplate.send("gmall0421_start",applog);
        }else{
            //事件日志
            kafkaTemplate.send("gmall0421_event",applog);
        }
        return "success";
    }
}
