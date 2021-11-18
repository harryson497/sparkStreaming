package com.atguigu.gmall.realtime.app

import java.lang
import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.gmall.realtime.bean.DauInfo
import com.atguigu.gmall.realtime.utils.{MyESUtil, MyKafkaUtil, MyRedisUtil, OffsetManagerUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer

/**
  * Author: Felix
  * Date: 2020/9/12
  * Desc: 日活统计业务类
  */
object DauApp {
  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setAppName("DauApp").setMaster("local[4]")
    val ssc: StreamingContext = new StreamingContext(conf,Seconds(5))
    var topic = "gmall0421_start"
    var groupId = "dau_group"
    //==============功能1.从Kafka中读取数据================
    //从Redis中获取偏移量
    val offsetMap: Map[TopicPartition, Long] = OffsetManagerUtil.getOffset(topic,groupId)

    //从kafka读取数据封装为DS
    var recordDStream: InputDStream[ConsumerRecord[String, String]] = null
    if(offsetMap != null &&offsetMap.size >0){
      //在Redis中已经维护了偏移量
      recordDStream = MyKafkaUtil.getKafkaStream(topic,ssc,offsetMap,groupId)
    }else{
      //在Redis中还没有维护偏移量
      recordDStream = MyKafkaUtil.getKafkaStream(topic,ssc,groupId)
    }

    //从读取到的kafka数据中获取到对应的偏移量
    var offsetRanges: Array[OffsetRange] = Array.empty[OffsetRange]
    val offsetDStream: DStream[ConsumerRecord[String, String]] = recordDStream.transform {
      rdd => {
        offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        rdd
      }
    }


    //将DS中数据进行结构的转换，只保留记录的value部分，将字符串value转换为json对象，向json对象中添加dt，hr
    val jsonObjDStream: DStream[JSONObject] = offsetDStream.map {
      record => {
        //获取启动日志的json格式字符串
        val jsonStr: String = record.value()
        //将json格式字符串转换为Json对象
        val jsonObj: JSONObject = JSON.parseObject(jsonStr)
        //获取当前时间戳
        val ts: lang.Long = jsonObj.getLong("ts")
        //将时间戳转换为日期和小时的形式
        val dtHourStr: String = new SimpleDateFormat("yyyy-MM-dd HH").format(new Date(ts))
        //对日期和小时按空格进行切割
        val dtHourArr: Array[String] = dtHourStr.split(" ")
        jsonObj.put("dt", dtHourArr(0))
        jsonObj.put("hr", dtHourArr(1))
        jsonObj
      }
    }
    //jsonObjDStream.print(100)

    //==============功能2.通过Redis对数据进行去重================
    /*
    //方式1:思路最简单  使用filter算子     Redis:  Type->Set   Key->dau:2020-09-12   value->mid
    //弊端：获取连接的次数过于频繁
    val filteredDStream = jsonObjDStream.filter {
      jsonObj => {
        //获取日志当前时间
        val dt: String = jsonObj.getString("dt")
        //获取设备id
        val mid: String = jsonObj.getJSONObject("common").getString("mid")
        //拼接向Redis保存数据的key
        var dauKey = "dau:" + dt
        //获取Jedis
        val jedis: Jedis = MyRedisUtil.getJedisClient()
        //向Redis中保存信息   根据返回的结果，判断是否已经存在   返回1，不存在，返回0，说明已经存在了
        val isNew: lang.Long = jedis.sadd(dauKey,mid)
        //设置key的失效时间
        jedis.expire(dauKey,3600*24)
        //释放资源
        jedis.close()
        if (isNew == 1L) {
          true
        } else {
          false
        }
      }
    }
    filteredDStream.count().print()
    */
    //方式2：以分区为单位对数据进行过滤
    val filteredDStream: DStream[JSONObject] = jsonObjDStream.mapPartitions {
      jsonObjItr => {
        //获取Jedis
        val jedis: Jedis = MyRedisUtil.getJedisClient()
        //定义一个集合。用于存放首次登录的json对象
        val filteredList = new ListBuffer[JSONObject]()
        //对当前分区中的所有json对象进行遍历
        for (jsonObj <- jsonObjItr) {
          //获取日志当前时间
          val dt: String = jsonObj.getString("dt")
          //获取设备id
          val mid: String = jsonObj.getJSONObject("common").getString("mid")
          //拼接向Redis保存数据的key
          var dauKey = "dau:" + dt
          //向Redis中保存信息   根据返回的结果，判断是否已经存在   返回1，不存在，返回0，说明已经存在了
          val isNew: lang.Long = jedis.sadd(dauKey, mid)
          //设置key的失效时间
          jedis.expire(dauKey, 3600 * 24)
          if (isNew == 1L) {
            filteredList.append(jsonObj)
          }
        }
        //释放资源
        jedis.close()
        filteredList.toIterator
      }
    }
    //filteredDStream.count().print

    //==============功能3.向ES中保存日活信息================
    filteredDStream.foreachRDD{
      rdd=>{
        rdd.foreachPartition{
          //以分区为单位，对RDD中的元素进行批量的保存
          jsonObjItr=>{
            val dauList: List[(String,DauInfo)] = jsonObjItr.map {
              jsonObj => {
                //获取commonJsonObj
                val commonJsonObj: JSONObject = jsonObj.getJSONObject("common")
                val dauInfo = DauInfo(
                  commonJsonObj.getString("mid"),
                  commonJsonObj.getString("uid"),
                  commonJsonObj.getString("ar"),
                  commonJsonObj.getString("ch"),
                  commonJsonObj.getString("vc"),
                  jsonObj.getString("dt"),
                  jsonObj.getString("hr"),
                  "00",
                  jsonObj.getLong("ts")
                )
                (dauInfo.mid,dauInfo)
              }
            }.toList
            //调用ES工具类，将dauList中的数据批量保存到ES中
            val dt: String = new SimpleDateFormat("yyyy-MM-dd").format(new Date)
            //向ES中批量插入数据
            MyESUtil.bulkInsert(dauList,"gmall0421_dau_info_" + dt)
          }
        }
        //将偏移量提交到Redis中去
        OffsetManagerUtil.saveOffset(topic,groupId,offsetRanges)
      }
    }

    //启动采集
    ssc.start()
    ssc.awaitTermination()
  }
}
