package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartAsyncService;
import com.atguigu.gmall.model.cart.CartInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author DuanYang
 * @create 2020-06-23 15:40
 */
@Service
public class CartAsyncServiceImpl implements CartAsyncService {
    @Autowired
    private CartInfoMapper cartInfoMapper;

    /**
     * 修改购物车
     * @param cartInfo
     */
    @Override
    @Async
    public void updateCartInfo(CartInfo cartInfo) {
        cartInfoMapper.updateById(cartInfo);
    }

    /**
     * 保存购物车
     * @param cartInfo
     */
    @Override
    @Async
    public void saveCartInfo(CartInfo cartInfo) {
        cartInfoMapper.insert(cartInfo);
    }

    /**
     * 清空购物车
     * @param userId
     */
    @Override
    @Async
    public void deleteCartInfo(String userId) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        cartInfoMapper.delete(wrapper);
    }

    /**
     * 选中状态变更
     * @param userId
     * @param isChecked
     * @param skuId
     */
    @Override
    @Async
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        //update cartInfo set is_checked = isChecked where user_id=userId and sku_id=skuId;
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId).eq("sku_id",skuId);
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        cartInfoMapper.update(cartInfo,wrapper);
    }

    /**
     * 删除购物车单件商品
     * @param userId
     * @param skuId
     */
    @Override
    @Async
    public void deleteCartInfo(String userId, Long skuId) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId).eq("sku_id",skuId);
        cartInfoMapper.delete(wrapper);
    }
}
