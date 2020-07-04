package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.activity.util.DateUtil;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author DuanYang
 * @create 2020-07-03 14:19
 */
@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsController {
    @Autowired
    private SeckillGoodsService seckillGoodsService;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("findAll")
    public Result findAll(){
        List<SeckillGoods> list = seckillGoodsService.findAll();
        return Result.ok(list);
    }

    @GetMapping("getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable("skuId")Long skuId){
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoodsBySkuId(skuId);
        return Result.ok(seckillGoods);
    }

    /**
     * 获取下单码
     * @param skuId
     * @return
     */
    //http://api.gmall.com/api/activity/seckill/auth/getSeckillSkuIdStr/16
    @GetMapping("auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable("skuId")Long skuId, HttpServletRequest request){
        //下单码 使用用户ID进行MD5加密 获得的字符串即为下单码
        //获取用户ID
        String userId = AuthContextHolder.getUserId(request);
        //检查商品是否正在秒杀
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoodsBySkuId(skuId);
        if (null!=seckillGoods){
            //当前时间是否在秒杀时间内
            Date curTime = new Date();
            if (DateUtil.dateCompare(seckillGoods.getStartTime(),curTime) &&
                    DateUtil.dateCompare(curTime,seckillGoods.getEndTime())){
                //在秒杀时间范围内
                if (StringUtils.isNotEmpty(userId)){
                    String encrypt = MD5.encrypt(userId);
                    return Result.ok(encrypt);
                }
            }
        }
        return Result.fail().message("非法秒杀");
    }

    /**
     * 根据用户和商品ID实现秒杀预下单
     */
    @PostMapping("auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //检查下单码
        String skuIdStr = request.getParameter("skuIdStr");
        if (!MD5.encrypt(userId).equals(skuIdStr)){
            return Result.build(null,ResultCodeEnum.SECKILL_ILLEGAL);
        }
        //从内存中获取状态码 1 可以秒杀，0秒杀结束
        String state = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(state)){
            return Result.build(null , ResultCodeEnum.SECKILL_ILLEGAL);
        }
        if ("1".equals(state)){
            UserRecode userRecode = new UserRecode();
            userRecode.setUserId(userId);
            userRecode.setSkuId(skuId);
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER,MqConst.ROUTING_SECKILL_USER,userRecode);
        }else {
            return Result.build(null,ResultCodeEnum.SECKILL_FINISH);
        }
        return Result.ok();
    }

    /**
     * 查询秒杀状态
     * @return
     */
    @GetMapping(value = "auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable Long skuId,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        return seckillGoodsService.checkOrder(skuId,userId);
    }

    /**
     * 秒杀确认订单
     */
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //获取用户地址
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        //获取商品订单
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null==orderRecode){
            return Result.fail().message("非法操作");
        }
        //获取秒杀商品订单
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();

        List<OrderDetail> orderDetailList = new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        //从秒杀订单里获得数量
        orderDetail.setSkuNum(orderRecode.getNum());
        //获取秒杀价格
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        orderDetailList.add(orderDetail);

        //订单总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();

        HashMap<String, Object> map = new HashMap<>();
        //放置用户地址
        map.put("userAddressList", userAddressList);
        //放置商品详情
        map.put("detailArrayList", orderDetailList);

        BigDecimal totalAmount = orderInfo.getTotalAmount();
        map.put("totalAmount", totalAmount);
        return Result.ok(map);
    }


    /**
     * 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
     * @param orderInfo
     * @return
     */
    @PostMapping("inner/seckill/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        //确认是否有预订单
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null==orderRecode){
            return Result.fail().message("非法操作");
        }
        Long orderId = orderFeignClient.submitOrder(orderInfo);
        if (null==orderId) {
            return Result.fail().message("抢购失败");
        }
        //删除预订单
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);
        //将下的订单保存在缓存
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId,orderId.toString());
        return Result.ok(orderId);

    }
}
