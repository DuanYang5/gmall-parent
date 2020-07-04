package com.atguigu.gmall.list.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.stereotype.Component;

/**
 * @author DuanYang
 * @create 2020-06-19 15:38
 */
@Component
public class ListDegradeFeignClient implements ListFeignClient {
    @Override
    public Result list(SearchParam listParam) {
        return Result.fail();
    }

    @Override
    public Result upperGoods(Long skuId) {
        return Result.fail();
    }

    @Override
    public Result lowerGoods(Long skuId) {
        return Result.fail();
    }

    @Override
    public Result incrHotScore(Long skuId) {
        return Result.fail();
    }
}
