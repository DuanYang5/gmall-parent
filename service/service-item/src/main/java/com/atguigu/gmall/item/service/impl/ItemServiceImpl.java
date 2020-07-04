package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author DuanYang
 * @create 2020-06-13 11:32
 */
@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ListFeignClient listFeignClient;
    /**
     * 通过skuId放入map中
     */
    @Override
    public Map<String, Object> getBySkuId(Long skuId) {
        Map<String, Object> result = new HashMap<>();
/*
runAsync 没有返回值
supplyAsync 有返回值
*/

        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //获取基本信息与图片信息
            SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
            result.put("skuInfo", skuInfo);
            return skuInfo;
        });

        CompletableFuture<Void> spuSaleAttrListCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //获取选定数据
            Long spuId = skuInfo.getSpuId();
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, spuId);
            result.put("spuSaleAttrList", spuSaleAttrList);
        });

        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //获取分类视图数据
            Long category3Id = skuInfo.getCategory3Id();
            BaseCategoryView categoryView = productFeignClient.getCategoryView(category3Id);
            result.put("categoryView", categoryView);
        });

        CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
            //获取单独价格数据
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            result.put("price", skuPrice);
        });

        CompletableFuture<Void> valuesSkuJsonCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //根据 spuId 获取封装之后的数据，格式：map.put(value_ids,sku_id)
            Long spuId = skuInfo.getSpuId();
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(spuId);
            String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
            result.put("valuesSkuJson", valuesSkuJson);
        });
        //更新热点信息
        CompletableFuture<Void> incrHotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        });

        CompletableFuture.allOf(skuInfoCompletableFuture,
                spuSaleAttrListCompletableFuture,
                categoryViewCompletableFuture,
                priceCompletableFuture,
                valuesSkuJsonCompletableFuture,
                incrHotScoreCompletableFuture).join();

        return result;
    }
}
