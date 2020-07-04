package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

/**
 * @author DuanYang
 * @create 2020-06-23 15:39
 */
public interface CartAsyncService {
    /**
     * 修改购物车
     * @param cartInfo
     */
    void updateCartInfo(CartInfo cartInfo);

    /**
     * 保存购物车
     * @param cartInfo
     */
    void saveCartInfo(CartInfo cartInfo);

    /**
     * 清空购物车
     * @param userId
     */
    void deleteCartInfo(String userId);

    /**
     * 选中状态变更
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
    void deleteCartInfo(String userId, Long skuId);
}
