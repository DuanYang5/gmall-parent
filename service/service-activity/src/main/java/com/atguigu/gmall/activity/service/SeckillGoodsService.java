package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

/**
 * @author DuanYang
 * @create 2020-07-03 14:12
 */
public interface SeckillGoodsService {
    /**
     * 查询所有秒杀商品列表
     */
    List<SeckillGoods> findAll();

    /**
     * 根据商品ID查询秒杀商品详情
     */
    SeckillGoods getSeckillGoodsBySkuId(Long skuId);

    /**
     * 根据用户和商品ID实现秒杀下单
     */
    void seckillOrder(String userId, Long skuId);

    /***
     * 根据商品id与用户ID查看订单信息
     */
    Result checkOrder(Long skuId, String userId);
}
