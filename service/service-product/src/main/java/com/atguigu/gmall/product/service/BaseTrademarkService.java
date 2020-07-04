package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author DuanYang
 * @create 2020-06-11 18:41
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {
    /**
     * 后台管理获取品牌商标图片
     */
    IPage<BaseTrademark> selectPage(Page<BaseTrademark> baseTrademarkPage);
}
