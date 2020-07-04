package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author DuanYang
 * @create 2020-07-03 14:28
 */
@Controller
public class SeckillController {
    @Autowired
    private ActivityFeignClient activityFeignClient;

    /**
     * 跳转到秒杀商品列表
     */
    @GetMapping("seckill.html")
    public String index(Model model){
        Result result = activityFeignClient.findAll();
        model.addAttribute("list",result.getData());
        return "seckill/index";
    }

    /**
     * 跳转到秒杀商品详情
     */
    @GetMapping("seckill/{skuId}.html")
    public String seckillItem(@PathVariable Long skuId,Model model){
        Result result = activityFeignClient.getSeckillGoods(skuId);
        model.addAttribute("item",result.getData());
        return "seckill/item";
    }

    /**
     * 跳转到排队页面
     */
    @GetMapping("seckill/queue.html")
    public String queue(@RequestParam("skuId")Long skuId, HttpServletRequest request){
        String skuIdStr = request.getParameter("skuIdStr");
        request.setAttribute("skuId",skuId);
        request.setAttribute("skuIdStr",skuIdStr);
        return "seckill/queue";
    }

    @GetMapping("seckill/trade.html")
    public String trade(Model model){
        Result<Map<String, Object>> result = activityFeignClient.trade();
        if (result.isOk()){
            model.addAllAttributes(result.getData());
            return "seckill/trade";
        }else {
            model.addAttribute("message",result.getMessage());
            return "seckill/fail";
        }
    }
}
