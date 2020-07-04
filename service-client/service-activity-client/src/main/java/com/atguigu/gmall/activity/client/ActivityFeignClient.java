package com.atguigu.gmall.activity.client;

import com.atguigu.gmall.activity.client.impl.ActivityDegradeFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @author DuanYang
 * @create 2020-07-03 9:52
 */
@FeignClient(value = "service-activity", fallback = ActivityDegradeFeignClient.class)
public interface ActivityFeignClient {
    /**
     * 查询所有秒杀商品列表
     */
    @GetMapping("/api/activity/seckill/findAll")
    Result findAll();

    /**
     * 根据商品ID查询秒杀商品详情
     */
    @GetMapping("/api/activity/seckill/getSeckillGoods/{skuId}")
    Result getSeckillGoods(@PathVariable("skuId") Long skuId);

    /**
     * 秒杀确认订单
     */
    @GetMapping("/api/activity/seckill/auth/trade")
    Result<Map<String,Object>> trade();
}
