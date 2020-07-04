package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @author DuanYang
 * @create 2020-06-23 14:41
 */
public interface CartService {
    // 添加购物车 用户Id，商品Id，商品数量。
    void addToCart(Long skuId, String userId, Integer skuNum);

    /**
     * 通过用户Id 查询购物车列表
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 更新选中状态
     *
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, Integer isChecked, Long skuId);

    /**
     * 删除购物车单件商品
     * @param userId
     * @param skuId
     */
    void deleteCart(Long skuId, String userId);

    /**
     * 根据用户Id 查询用户选中并点击结算的商品列表
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据userID查询数据库并将数据放入缓存
     */
    List<CartInfo> loadCartCache(String userId);
}
