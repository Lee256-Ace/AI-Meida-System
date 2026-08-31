package com.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 后台管理系统启动类
 */
@SpringBootApplication
@MapperScan("com.product.mapper")
@EnableAsync
@EnableScheduling
public class ProductApplication {

    // 测试
    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
        System.out.println("后台管理系统启动成功！");
        System.out.println("访问地址: http://localhost:8000/api");
        System.out.println("Druid监控: http://localhost:8080/api/druid");
    }
}