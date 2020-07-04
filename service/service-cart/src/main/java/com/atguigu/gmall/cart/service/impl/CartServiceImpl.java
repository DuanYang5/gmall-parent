package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartAsyncService;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.base.BaseEntity;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author DuanYang
 * @create 2020-06-23 14:42
 */
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private CartAsyncService cartAsyncService;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 添加购物车功能
     */
    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        String cartKey = getCartKey(userId);
        //如缓存中没有key 则从数据库查询并放入缓存
        if (!redisTemplate.hasKey(cartKey)){
            //调用查询数据库方法
            loadCartCache(userId);
        }

        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        wrapper.eq("sku_id",skuId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(wrapper);
        if (null!=cartInfoExist){
            //购物车有该商品
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            cartAsyncService.updateCartInfo(cartInfoExist);
        }else {
            //购物车没有该商品
            SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setSkuPrice(skuInfo.getPrice());

            cartAsyncService.saveCartInfo(cartInfo);

            cartInfoExist=cartInfo;
        }

        redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);
        setCartKeyExpire(cartKey);
    }

    /**
     * 获取购物车列表
     * 一、未登陆 直接查询临时购物车
     * 二、已登录 将临时购物车与账户购物车合并 再清空临时购物车
     */
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (StringUtils.isNotEmpty(userTempId)){
            if (StringUtils.isEmpty(userId)){
                //未登陆
                cartInfoList = getCartList(userTempId);
            }
            if (StringUtils.isNotEmpty(userId)){
                //已登录
                List<CartInfo> cartTempList = getCartList(userTempId);
                if (CollectionUtils.isNotEmpty(cartTempList)){
                    //调用合并购物车方法
                    cartInfoList = mergeToCartList(cartTempList,userId);
                    //调用清空临时购物车方法
                    deleteCartList(userTempId);
                }
                //临时购物车为空直接查询账户购物车
                if (CollectionUtils.isEmpty(cartTempList)){
                    cartInfoList = getCartList(userId);
                }
            }
        }
        return cartInfoList;
    }

    /**
     * 更新选中状态
     */
    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        //更新数据库
        cartAsyncService.checkCart(userId,isChecked,skuId);
        //更新缓存
        String cartKey = getCartKey(userId);
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(cartKey);
        if (boundHashOps.hasKey(skuId.toString())){
            //hash中存在key 获取value
            CartInfo cartInfo = (CartInfo) boundHashOps.get(skuId.toString());
            cartInfo.setIsChecked(isChecked);
            //放入hash中
            boundHashOps.put(skuId.toString(),cartInfo);
            //设置过期时间
            setCartKeyExpire(cartKey);
        }else {
            loadCartCache(userId);
        }
    }

    /**
     * 删除购物车单件商品
     */
    @Override
    public void deleteCart(Long skuId, String userId) {
        //数据库删除
        cartAsyncService.deleteCartInfo(userId,skuId);
        //缓存删除
        String cartKey = getCartKey(userId);
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(cartKey);
        if (boundHashOps.hasKey(skuId.toString())){
            boundHashOps.delete(skuId.toString());
        }
    }

    /**
     * 根据用户Id 查询用户选中并点击结算的商品列表
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfos = new ArrayList<>();
        String cartKey = getCartKey(userId);
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (CollectionUtils.isNotEmpty(cartInfoList)){
            //不为空循环遍历集合
            for (CartInfo cartInfo : cartInfoList) {
                if (cartInfo.getIsChecked().intValue()==1){
                    cartInfos.add(cartInfo);
                }
            }
        }
        return cartInfos;
    }

    /**
     * 清空购物车
     */
    private void deleteCartList(String userTempId) {
        String cartKey = getCartKey(userTempId);
        //删除缓存
        if (redisTemplate.hasKey(cartKey)){
            redisTemplate.delete(cartKey);
        }
        //删除数据库
        cartAsyncService.deleteCartInfo(userTempId);
    }

    /**
     * 合并购物车方法
     */
    private List<CartInfo> mergeToCartList(List<CartInfo> cartTempList, String userId) {
        List<CartInfo> cartLogList = getCartList(userId);
        //如果登陆购物车为空，直接返回临时购物车
        if (CollectionUtils.isEmpty(cartLogList)){
            return cartTempList;
        }
        //如果临时购物车为空，直接返回登陆购物车
        if (CollectionUtils.isEmpty(cartTempList)){
            return cartLogList;
        }
        //执行到此处的代码 登陆与临时都不为空
        //map key=skuId,value=cartInfo
        Map<Long, CartInfo> cartInfoMap = cartLogList.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        for (CartInfo cartTempInfo : cartTempList) {
            Long skuId = cartTempInfo.getSkuId();
            //map中是否有与 临时id 相同的key
            if (cartInfoMap.containsKey(skuId)){
                //存在相同key
                CartInfo cartLogInfo = cartInfoMap.get(skuId);
                //更新数量
                cartLogInfo.setSkuNum(cartLogInfo.getSkuNum()+cartTempInfo.getSkuNum());
                //如果未登录状态下商品为选中，则合并后状态也为选中
                if (cartTempInfo.getIsChecked().intValue()==1){
                    cartLogInfo.setIsChecked(1);
                }
                cartAsyncService.updateCartInfo(cartLogInfo);
            }else {
                //将临时id更新为用户id
                cartTempInfo.setUserId(userId);
                //不存在相同key
                cartAsyncService.saveCartInfo(cartTempInfo);
            }
        }
        //获取合并后的数据并返回
        return loadCartCache(userId);
    }

    /**
     * 获取购物车列表
     */
    private List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList = null;
        if (StringUtils.isEmpty(userId)){
            return null;
        }
        //先查询缓存 如果缓存没有再查询数据库 并将数据放入缓存
        String cartKey = getCartKey(userId);
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (CollectionUtils.isNotEmpty(cartInfoList)){
            //缓存中有数据
            cartInfoList.sort(Comparator.comparing(BaseEntity::getId));
            return cartInfoList;
        }else {
            //缓存中没有数据
            cartInfoList = loadCartCache(userId);
        }
        return cartInfoList;
    }

    /**
     * 根据userID查询数据库并将数据放入缓存
     */
    public List<CartInfo> loadCartCache(String userId) {
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(new QueryWrapper<CartInfo>().eq("user_id",userId));
        if (CollectionUtils.isNotEmpty(cartInfoList)){
            String cartKey = getCartKey(userId);
            HashMap<String, CartInfo> map = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                //****实时更新价格****
                cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
                //获取filed与value
                map.put(cartInfo.getSkuId().toString(),cartInfo);
            }
            redisTemplate.opsForHash().putAll(cartKey,map);
            //设置过期时间
            setCartKeyExpire(cartKey);
        }
        return cartInfoList;
    }

    /**
     * 设置过期时间
     */
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存对应购物车的key
     */
    private String getCartKey(String userId){
        return RedisConst.USER_KEY_PREFIX + userId +RedisConst.USER_CART_KEY_SUFFIX;
    }
}
