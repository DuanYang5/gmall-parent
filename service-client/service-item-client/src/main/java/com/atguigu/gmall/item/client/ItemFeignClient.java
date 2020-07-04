package com.atguigu.gmall.item.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.impl.ItemDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author DuanYang
 * @create 2020-06-15 10:21
 */
@FeignClient(name = "service-item",fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {
    /**
     * 根据skuId 获取商品详情
     */
    @GetMapping("api/item/{skuId}")
    Result getItem(@PathVariable Long skuId);
}
