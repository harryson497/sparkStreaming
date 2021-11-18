package com.atguigu.gmall.publisher;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.atguigu.gmall.publisher.mapper")
public class Gmall0421PublisherApplication {

    public static void main(String[] args) {
        SpringApplication.run(Gmall0421PublisherApplication.class, args);
    }

}
