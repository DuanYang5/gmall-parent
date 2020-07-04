package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author DuanYang
 * @create 2020-06-27 16:02
 */
@RestController
@RequestMapping("api/order")
public class OrderApiController {
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 提交订单
     */
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        String tradeNo = request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeNo(tradeNo, userId);
//        if (flag){
//            //缓存有数据 代表第一次提交订单
//        }else {
//            //缓存没有数据 重复提交
//        }
        if (!flag) {
            return Result.fail().message("不能无刷新页面重复提交订单");
        }
        //声明集合存储异步编排对象
        List<CompletableFuture> futureList = new ArrayList<>();
        //创建集合存储异常信息
        List<String> errorList = new ArrayList<>();

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
		if (CollectionUtils.isNotEmpty(orderDetailList)) {
        	for (OrderDetail orderDetail : orderDetailList) {
                CompletableFuture<Void> stockCompletableFuture = CompletableFuture.runAsync(() -> {
                    //检查库存
                    boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                    if (!result) {
                        errorList.add(orderDetail.getSkuName() + "库存不足");
                    }
                }, threadPoolExecutor);
                CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
                    BigDecimal price = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                    //比较价格
                    if (orderDetail.getOrderPrice().compareTo(price) != 0) {
                        cartFeignClient.loadCartCache(userId);
                        errorList.add(orderDetail.getSkuName() + "价格发生变化，请重新下单");
                    }
                }, threadPoolExecutor);
                //添加线程到集合中
                futureList.add(stockCompletableFuture);
                futureList.add(priceCompletableFuture);
            }
        }
        //合并线程 allOf anyOf
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
        //返回页面提示信息
        if (errorList.size()>0){
            return Result.fail().message(StringUtils.join(errorList,","));
        }

//        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
//        if (CollectionUtils.isNotEmpty(orderDetailList)){
//            for (OrderDetail orderDetail : orderDetailList) {
//                BigDecimal price = productFeignClient.getSkuPrice(orderDetail.getSkuId());
//                //比较价格
//                if (orderDetail.getOrderPrice().compareTo(price)!=0){
//                    cartFeignClient.loadCartCache(userId);
//                    return Result.fail().message(orderDetail.getSkuName() + "价格发生变化，请重新下单");
//                }
//                //检查库存
//                boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
//                if (!result){
//                    return Result.fail().message(orderDetail.getSkuName() + "库存不足");
//                }
//            }
//        }


        orderService.deleteTradeNo(userId);
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return Result.ok(orderId);
    }

    /**
     * 确认订单
     */
    @GetMapping("auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        int totalNum = 0;
        //一定是登录的
        String userId = AuthContextHolder.getUserId(request);
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();

        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);

        if (CollectionUtils.isNotEmpty(cartCheckedList)) {
            for (CartInfo cartInfo : cartCheckedList) {
                OrderDetail orderDetail = new OrderDetail();

                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(cartInfo.getSkuPrice());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                totalNum += cartInfo.getSkuNum();
                orderDetail.setSkuId(cartInfo.getSkuId());
                //添加到集合中
                detailArrayList.add(orderDetail);
            }
        }
        HashMap<String, Object> map = new HashMap<>();
        //放置用户地址
        map.put("userAddressList", userAddressList);
        //放置商品详情
        map.put("detailArrayList", detailArrayList);
        //放置商品总数
        map.put("totalNum", totalNum);

        //orderInfo 中有计算总金额的方法
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        //调用方法
        orderInfo.sumTotalAmount();
        BigDecimal totalAmount = orderInfo.getTotalAmount();
        map.put("totalAmount", totalAmount);

        //流水号
        String tradeNo = orderService.getTradeNo(userId);
        map.put("tradeNo", tradeNo);

        return Result.ok(map);
    }

    /**
     * 内部调用获取订单
     * @param orderId
     * @return
     */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable("orderId") Long orderId){
        return orderService.getOrderInfo(orderId);
    }

    /**
     * 拆单
     * @param request
     * @return
     */
    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");

        List<Map> mapList = new ArrayList<>();
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId),wareSkuMap);
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map map = orderService.initWareOrder(orderInfo);
            //将一个个map放进集合中
            mapList.add(map);
        }
        //将集合转为json
        return JSON.toJSONString(mapList);
    }

    /**
     * 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
     * @param orderInfo
     * @return
     */
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo){
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return orderId;
    }
}
