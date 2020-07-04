package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sun.net.www.protocol.http.AuthenticationHeader;

import javax.servlet.http.HttpServletRequest;

/**
 * @author DuanYang
 * @create 2020-06-26 17:21
 */
@Controller
public class CartController {
    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;
    /**
     * 添加购物车
     */
    @RequestMapping("addCart.html")
    public String addCart(@RequestParam("skuId") Long skuId,
                          @RequestParam("skuNum") Integer skuNum,
                          HttpServletRequest request){
        cartFeignClient.addToCart(skuId,skuNum);
        //存储前台需要的数据
        SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "cart/addCart";
    }

    /**
     * 查询购物车列表
     */
    @GetMapping("cart.html")
    public String index(){
        return "cart/index";
    }
}
