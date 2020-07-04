package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * @author DuanYang
 * @create 2020-06-29 11:58
 */
@Component
public class OrderReceiver {
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentFeignClient paymentFeignClient;
    @Autowired
    private RabbitService rabbitService;

    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel){
        if (null!= orderId){
            OrderInfo orderInfo = orderService.getById(orderId);
            if (null!=orderInfo && ProcessStatus.UNPAID.getOrderStatus().name().equals(orderInfo.getOrderStatus())){
                //关闭过期订单
                //orderService.execExpiredOrder(orderId);
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                if (null!=paymentInfo && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())){
                    //用户已经生成订单，验证是否扫了二维码
                    Boolean aBoolean = paymentFeignClient.checkPayment(orderId);
                    if(aBoolean){
                        //有交易记录 关闭支付宝
                        Boolean flag = paymentFeignClient.closePay(orderId);
                        if (flag){
                            //没有支付 关闭订单 关闭交易记录
                            orderService.execExpiredOrder(orderId,"2");
                        }else {
                            //支付成功不能关闭 发送消息改变订单状态
                            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,orderId);
                        }
                    }else {
                        //没有交易记录 没有扫二维码 在电商中有交易记录，在支付宝中没有交易记录
                        orderService.execExpiredOrder(orderId,"2");
                    }
                }else {
                    //paymentInfo为空 没有交易记录
                    orderService.execExpiredOrder(orderId,"1");
                }
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 接收消息 订单支付，更改订单状态与通知扣减库存
     * @param orderId
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void updOrder(Long orderId,Message message,Channel channel){
        if (null!=orderId){
            OrderInfo orderInfo = orderService.getById(orderId);
            if (null!= orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
                //更新状态
                orderService.updateOrderStatus(orderId,ProcessStatus.PAID);
                //更新库存
                orderService.sendOrderStatus(orderId);
            }
        }
        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrderStatus(String msgJson,Message message,Channel channel){
        if (StringUtils.isNotEmpty(msgJson)){
            Map map = JSON.parseObject(msgJson, Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");
            //判断
            if ("DEDUCTED".equals(status)){
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);
            }else {
                // ‘OUT_OF_STOCK’  (库存超卖)
                // 库存超卖了，那么如何处理？
                // 第一种：调用其他仓库货物进行补货。 想办法补货，补库存。
                // 发送消息重新更新一下减库存的结果！
                // 第二种：人工客服介入，给你退款
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
