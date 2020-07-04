package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * @author DuanYang
 * @create 2020-06-28 9:10
 */
public interface OrderService extends IService<OrderInfo> {

    /**
     * 根据订单Id 查询订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);
    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 保存订单
     * @return 订单号
     */
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 获取流水号 ,并放入缓存
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     */
    boolean checkTradeNo(String tradeNo,String userId);

    /**
     * 删除流水号
     */
    void deleteTradeNo(String userId);

    /**
     *关闭过期订单
     * @param orderId
     */
    void execExpiredOrder(Long orderId);

    /**
     * 更新orderInfo 状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 发送消息给库存！
     * @param orderId
     */
    void sendOrderStatus(Long orderId);

    /**
     * 将orderInfo转为map
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单方法
     */
    List<OrderInfo> orderSplit(Long orderId, String wareSkuMap);

    /**
     * 关闭过期订单
     * @param orderId
     * @param flag
     */
    void execExpiredOrder(Long orderId, String flag);
}
