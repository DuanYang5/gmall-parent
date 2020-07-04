package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author DuanYang
 * @create 2020-06-28 9:10
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper,OrderInfo> implements OrderService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitService rabbitService;

    @Value("${ware.url}")
    private String wareUrl;

    /**
     * 根据订单Id 查询订单信息
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        //查询订单明细
        QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(wrapper);

        orderInfo.setOrderDetailList(orderDetails);
        return orderInfo;
    }

    /**
     * 验证库存
     * http://localhost:9001/hasStock?skuId=10221&num=2
     */
    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        //0：无库存   1：有库存
        return "1".equals(result);
    }

    /**
     * 保存订单
     * @return 订单号
     */
    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {
        //orderInfo 缺少字段
        orderInfo.sumTotalAmount();//总金额
        //订单交易编号
        String outTraderNo = "ATGUIGU:" + UUID.randomUUID().toString();
        orderInfo.setOutTradeNo(outTraderNo);
        // 根据订单明细的中的商品名称进行拼接
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuffer sb = new StringBuffer();
        for (OrderDetail orderDetail : orderDetailList) {
            sb.append(orderDetail.getSkuName()+" ");
        }
        if (sb.toString().length()>100){
            orderInfo.setTradeBody(sb.toString().substring(0,100));
        }else {
            orderInfo.setTradeBody(sb.toString());
        }
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());

        //创建时间
        orderInfo.setCreateTime(new Date());
        //过期时间 调用日期类 1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        orderInfoMapper.insert(orderInfo);
        if (CollectionUtils.isNotEmpty(orderDetailList)){
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insert(orderDetail);
            }
        }
        //发送消息
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,MqConst.ROUTING_ORDER_CANCEL,orderInfo.getId(),MqConst.DELAY_TIME);
        return orderInfo.getId();
    }

    /**
     * 获取流水号 ,并放入缓存
     */
    @Override
    public String getTradeNo(String userId) {
        String tradeKey = getTradeKey(userId);
        String tradeNo = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(tradeKey,tradeNo);
        return tradeNo;
    }

    private String getTradeKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + ":tradeNo";
    }

    /**
     *  比较流水号
     * @param tradeNo
     * @param userId
     * @return
     */
    @Override
    public boolean checkTradeNo(String tradeNo, String userId) {
        String tradeKey = getTradeKey(userId);
        String tradeNoRedis = (String) redisTemplate.opsForValue().get(tradeKey);

        return tradeNo.equals(tradeNoRedis);
    }

    /**
     * 删除流水号
     */
    @Override
    public void deleteTradeNo(String userId) {
        String tradeKey = getTradeKey(userId);
        redisTemplate.delete(tradeKey);
    }

    /**
     *关闭过期订单
     * @param orderId
     */
    @Override
    public void execExpiredOrder(Long orderId) {
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
        //关闭支付宝交易
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
    }

    /**
     * 更新orderInfo 状态
     * @param orderId
     * @param processStatus
     */
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        orderInfoMapper.updateById(orderInfo);
    }

    /**
     * 发送消息给库存！
     * @param orderId
     */
    @Override
    public void sendOrderStatus(Long orderId) {
        //更新状态 通知仓储
        updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        String wareJson = initWareOrder(orderId);
        //json 发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK,wareJson);
    }

    private String initWareOrder(Long orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }

    /**
     * 将orderInfo转为map
     * @param orderInfo
     * @return
     */
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！


        //details:[{skuId:101,skuNum:1,skuName:’小米手64G’},
        //         {skuId:201,skuNum:1,skuName:’索尼耳机’}]
        //根据格式创建一个list 存储map
        ArrayList<Map> mapArrayList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> orderMap = new HashMap<>();
            orderMap.put("skuId",orderDetail.getSkuId());
            orderMap.put("skuNum",orderDetail.getSkuNum());
            orderMap.put("skuName",orderDetail.getSkuName());

            mapArrayList.add(orderMap);
        }
        //
        map.put("details", mapArrayList);
        return map;
    }

    /**
     * 拆单方法 获取子订单集合
     */
    @Override
    public List<OrderInfo> orderSplit(Long orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList= new ArrayList<>();

        //原始订单
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        //转为Java对象
        // [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        for (Map map : mapList) {
            String wareId = (String) map.get("wareId");
            List<String> skuIdList = (List<String>) map.get("skuIds");
            OrderInfo subOrderInfo = new OrderInfo();
            //给子订单赋值
            BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderId);
            subOrderInfo.setWareId(wareId);
            List<OrderDetail> orderDetails = new ArrayList<>();
            //获得原始订单详情
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            if (CollectionUtils.isNotEmpty(orderDetailList)){
                //遍历原始订单详情
                for (OrderDetail orderDetail : orderDetailList) {
                    //遍历map 中获得的skuId
                    for (String skuId : skuIdList) {
                        if (Long.parseLong(skuId)==orderDetail.getSkuId()){
                            //相等则添加到集合中
                            orderDetails.add(orderDetail);
                        }
                    }
                }
            }
            //将集合设置进去
            subOrderInfo.setOrderDetailList(orderDetails);
            //子订单金额
            subOrderInfo.sumTotalAmount();
            //保存子订单
            saveOrderInfo(subOrderInfo);
            subOrderInfoList.add(subOrderInfo);
        }
        //更新主订单状态
        updateOrderStatus(orderId,ProcessStatus.SPLIT);

        return subOrderInfoList;
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        if ("2".equals(flag)){
            //关闭支付宝交易
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }
    }
}
