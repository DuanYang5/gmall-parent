package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * @author DuanYang
 * @create 2020-06-29 16:20
 */
@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private RabbitService rabbitService;
    /**
     * 保存交易记录
     * @param orderInfo
     * @param paymentType 支付类型（1：微信 2：支付宝）
     */
    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        PaymentInfo paymentInfo = new PaymentInfo();
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",orderInfo.getId());
        wrapper.eq("payment_type",paymentType);
        Integer count = paymentInfoMapper.selectCount(wrapper);
        if (count > 0){
            return;
        }

        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setCreateTime(new Date());

        paymentInfoMapper.insert(paymentInfo);
    }

    /**
     * 获取交易记录信息
     * @param outTradeNo
     * @param name
     * @return
     */
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String name) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",outTradeNo).eq("payment_type",name);
        return paymentInfoMapper.selectOne(wrapper);
    }

    /**
     * 支付成功更新交易记录
     * @param outTradeNo
     * @param name
     * @param paramsMap
     */
    @Override
    public void paySuccess(String outTradeNo, String name, Map<String, String> paramsMap) {
        PaymentInfo paymentInfo = this.getPaymentInfo(outTradeNo, name);
        if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name())
                || paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())){
            return;
        }

        PaymentInfo paymentInfoUPD = new PaymentInfo();
        //设置状态为已支付
        paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfoUPD.setCallbackTime(new Date());
        paymentInfoUPD.setCallbackContent(paramsMap.toString());
        paymentInfoUPD.setTradeNo(paramsMap.get("trade_no"));

        //调用更新交易记录方法
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",outTradeNo);
        wrapper.eq("payment_type",name);
        paymentInfoMapper.update(paymentInfoUPD,wrapper);

        //发送消息 更新订单状态 通过商品Id
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfo.getOrderId());
    }

    /**
     * 更新交易记录
     * 退款
     * @param outTradeNo
     * @param paymentInfo
     */
    @Override
    public void updatePaymentInfo(String outTradeNo,PaymentInfo paymentInfo) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",outTradeNo);
        paymentInfoMapper.update(paymentInfo,wrapper);
    }

    /**
     * 关闭支付宝交易
     * @param orderId
     */
    @Override
    public void closePayment(Long orderId) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",orderId);
        Integer count = paymentInfoMapper.selectCount(wrapper);
        if (null==count || count.intValue()==0){
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
        //修改状态为关闭
        paymentInfoMapper.update(paymentInfo,wrapper);
    }
}
