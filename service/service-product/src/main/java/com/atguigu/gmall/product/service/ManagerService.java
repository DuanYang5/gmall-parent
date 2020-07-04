package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author DuanYang
 * @create 2020-06-09 11:23
 */
public interface ManagerService {
    /**
     * 封装 平台属性信息
     */
    List<BaseAttrInfo> getAttrInfoList(Long skuId);

    /**
     * 封装 品牌信息
     */
    BaseTrademark getBaseTrademark(Long tmId);

    /**
     * 获取所有分类信息
     */
    List<JSONObject> getBaseCategoryList();
    /**
     * 查询所有一级分类
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类id查询二级分类
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据二级分类id查询三级分类
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类id查询分类集合
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id,Long category2Id,Long category3Id);

    /**
     * 保存平台属性和平台属性值
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据属性值id获取平台属性信息
     */
    BaseAttrInfo getAttrInfo(Long attrId);

    /**
     * 带有条件的 分页查询
     */
    IPage<SpuInfo> selectPage(Page<SpuInfo> spuInfoPageParam, SpuInfo spuInfo);

    /**
     * 分页查询 重载
     */
    IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoPage);

    /**
     * 查询基础销售信息
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * spu信息大保存
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId查询spu图片列表
     */
    List<SpuImage> spuImageList(Long spuId);

    /**
     * 销售属性与销售属性值 回显
     */
    List<SpuSaleAttr> getSpuSaleAttrListBySpuId(Long spuId);

    /**
     * 大保存sku
     */
    void saveSkuInfo(SkuInfo skuInfo);

    void onSale(Long skuId);

    void cancelSale(Long skuId);

    /**
     * 通过skuId获取基本信息与图片数据
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 通过视图 获取分类信息
     */
    BaseCategoryView getBaseCategoryViewByCategory3Id(Long category3Id);

    /**
     * 单独获取价格信息
     */
    BigDecimal getSkuPriceBySkuId(Long skuId);

    /**
     * 根据 skuId,spuId 获取选定销售属性值
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId,Long spuId);

    /**
     * 根据 spuId 获取封装之后的数据，格式：map.put(value_ids,sku_id)
     */
    Map getSkuValueIdsMap(Long spuId);
}
