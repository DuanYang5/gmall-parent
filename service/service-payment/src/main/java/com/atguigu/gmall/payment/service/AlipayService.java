package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

/**
 * @author DuanYang
 * @create 2020-06-29 16:40
 */
public interface AlipayService {
    /**
     * 支付功能
     * @param orderId
     * @return
     * @throws AlipayApiException
     */
    String alipay(Long orderId) throws AlipayApiException;

    /**
     * 根据orderID进行退款
     * @param orderId
     * @return
     */
    boolean refund(Long orderId);

    /***
     * 关闭交易
     * @param orderId
     * @return
     */
    Boolean closePay(Long orderId);

    /**
     * 根据订单查询是否支付成功！
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);
}
