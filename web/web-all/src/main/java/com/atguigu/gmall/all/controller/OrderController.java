package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * @author DuanYang
 * @create 2020-06-27 16:31
 */
@Controller
public class OrderController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("trade.html")
    public String trade(Model model){
        Result<Map<String, Object>> result = orderFeignClient.trade();
        //放置数据到作用域
        model.addAllAttributes(result.getData());
        //URL
        return "order/trade";
    }
}
