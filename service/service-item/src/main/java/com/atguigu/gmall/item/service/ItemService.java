package com.atguigu.gmall.item.service;

import java.util.Map;

/**
 * @author DuanYang
 * @create 2020-06-13 11:21
 */
public interface ItemService {
    /**
     * 通过skuId获取基本信息与图片数据
     */
    Map<String,Object> getBySkuId(Long skuId);
}
