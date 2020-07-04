package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author DuanYang
 * @create 2020-07-03 14:15
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 查询所有秒杀商品列表
     */
    @Override
    public List<SeckillGoods> findAll() {
        List<SeckillGoods> list = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
        return list;
    }

    /**
     * 根据商品ID查询秒杀商品详情
     */
    @Override
    public SeckillGoods getSeckillGoodsBySkuId(Long skuId) {
        return (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId.toString());
    }

    /**
     * 根据用户和商品ID实现秒杀预下单
     */
    @Override
    public void seckillOrder(String userId, Long skuId) {
        String skuIdStr = (String) CacheHelper.get("skuIdStr");
        if ("0".equals(skuIdStr)){
            return ;
        }
        //判定是否重复下单
        //key = seckill:user:userId value = skuId
        Boolean isExist = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER+userId,skuId,RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        if (!isExist){
            return ;
        }
        //将商品数量从右侧 吐出
        String goodsId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(goodsId)){
            //商品为空 已售销
            redisTemplate.convertAndSend("seckillpush", skuId + ":0");
            return ;
        }
        //保存秒杀订单信息
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setUserId(userId);
        orderRecode.setSeckillGoods(this.getSeckillGoodsBySkuId(skuId));
        orderRecode.setNum(1);
        //设置下单码
        orderRecode.setOrderStr(MD5.encrypt(userId));

        //预下单存入缓存
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(orderRecode.getUserId(),orderRecode);
        //更新库存
        this.updateStockCount(skuId);
    }

    /***
     * 根据商品id与用户ID查看订单信息
     */
    @Override
    public Result checkOrder(Long skuId, String userId) {
        //用户订单在缓存中是否存在
        Boolean isExist = redisTemplate.hasKey(RedisConst.SECKILL_USER+ userId);
        if (isExist){
            //用户在缓存中是否有预订单
            Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if(flag){
                //从缓存中获得预订单信息
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }
        //是否有下单成功，不是预下单
        Boolean res = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        if (res){
            //有下过订单 去查看返回的订单Id
            String orderId = (String) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            return Result.build(orderId,ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        //防止高并发 获取状态位
        String skuIdStr = (String) CacheHelper.get("skuIdStr");
        if ("0".equals(skuIdStr)){
            //抢单失败
            return Result.build(null,ResultCodeEnum.SECKILL_FAIL);
        }
        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }

    /**
     * 更新库存
     */
    private void updateStockCount(Long skuId) {

        Long count = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        if (count %2 == 0){
            //更新数据库库存
            SeckillGoods seckillGoods = getSeckillGoodsBySkuId(skuId);
            seckillGoods.setStockCount(count.intValue());
            seckillGoodsMapper.updateById(seckillGoods);
            //更新缓存库存
            redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(skuId,seckillGoods);
        }
    }
}
