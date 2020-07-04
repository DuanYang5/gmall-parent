package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * @author DuanYang
 * @create 2020-06-29 16:19
 */
public interface PaymentService {
    /**
     * 保存交易记录
     * @param orderInfo
     * @param paymentType 支付类型（1：微信 2：支付宝）
     */
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    /**
     * 获取交易记录信息
     * @param outTradeNo
     * @param name
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String name);

    /**
     * 支付成功更新交易记录
     * @param outTradeNo
     * @param name
     * @param paramsMap
     */
    void paySuccess(String outTradeNo, String name, Map<String, String> paramsMap);

    /**
     * 更新交易记录
     * 退款
     * @param outTradeNo
     * @param paymentInfo
     */
    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo);

    /**
     * 关闭支付宝交易
     * @param orderId
     */
    void closePayment(Long orderId);
}
