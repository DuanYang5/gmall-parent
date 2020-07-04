package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.DateUtil;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * @author DuanYang
 * @create 2020-07-03 10:29
 */
@Component
public class SeckillReceiver {
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsService seckillGoodsService;
    /**
     * 将数据库中的秒杀商品数据放入缓存
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importItemToRedis(Message message , Channel channel){
        //从数据库查询秒杀商品
        QueryWrapper<SeckillGoods> wrapper = new QueryWrapper<>();
        //审核状态为1 通过
        wrapper.eq("status",1);
        //剩余库存数大于0
        wrapper.gt("stock_count",0);
        //秒杀时间 今天的
        wrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> list = seckillGoodsMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(list)){
            for (SeckillGoods seckillGoods : list) {
                //放入缓存
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                if (!flag){
                    //不存在相同key则放入缓存
                    redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(),seckillGoods);

                    //控制库存超卖 使用Redis -list lpush，pop
                    for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                        // key = seckill:stock:skuId
                        redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
                    }
                    //信息发布订阅 channel 表示发送频道 message 表示发送内容
                    //publish seckillpush skuId:1
                    redisTemplate.convertAndSend("seckillpush", seckillGoods.getSkuId()+":1");

                }
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }


    /**
     * 定时清空
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void clearRedisData(Message message , Channel channel){
        //获取活动结束商品
        QueryWrapper<SeckillGoods> wrapper = new QueryWrapper<>();
        wrapper.eq("status","1");
        wrapper.le("end_time",new Date());
        List<SeckillGoods> seckillGoodList = seckillGoodsMapper.selectList(wrapper);

        //清空
        for (SeckillGoods seckillGoods : seckillGoodList) {
            redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId());
        }
        redisTemplate.delete(RedisConst.SECKILL_GOODS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setStatus("2");
        seckillGoodsMapper.update(seckillGoods,wrapper);
        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 秒杀预下单
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckill(UserRecode userRecode,Message message , Channel channel){
        if (null!=userRecode){
            //预下单
            seckillGoodsService.seckillOrder(userRecode.getUserId(),userRecode.getSkuId());
            //手动发送消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }
}
