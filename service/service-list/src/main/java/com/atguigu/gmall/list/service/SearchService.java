package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

/**
 * @author DuanYang
 * @create 2020-06-19 11:46
 */
public interface SearchService {
    /**
     * 搜索功能开发
     */
    SearchResponseVo search(SearchParam searchParam) throws Exception;

    /**
     * 更新热点
     */
    void incrHotScore(Long skuId);

    /**
     * 商品上架
     */
    void upperGoods(Long skuId);

    /**
     * 商品下架
     */
    void lowerGoods(Long skuId);
}
