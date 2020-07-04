package com.atguigu.gmall.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author DuanYang
 * @create 2020-06-09 14:53
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan("com.atguigu.gmall") // 最主要的是扫描mapper
public class ServiceProductApplicaion {
    public static void main(String[] args) {
        SpringApplication.run(ServiceProductApplicaion.class,args);
    }
}
