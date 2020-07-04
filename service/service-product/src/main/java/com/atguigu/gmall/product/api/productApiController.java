package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 为service-item微服务调用提供
 * @author DuanYang
 * @create 2020-06-13 11:49
 */
@RestController
@RequestMapping("api/product")
public class productApiController {
    @Autowired
    private ManagerService managerService;

    /**
     * 获取所有分类信息
     */
    @GetMapping("getBaseCategoryList")
    public Result getBaseCategoryList(){
        List<JSONObject> baseCategoryList = managerService.getBaseCategoryList();

        return Result.ok(baseCategoryList);
    }
    /**
     * 通过skuId获取基本信息与图片数据
     */
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfoById(@PathVariable("skuId") Long skuId){
        return managerService.getSkuInfo(skuId);
    }

    /**
     * 通过视图 获取分类信息
     */
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable("category3Id") Long category3Id){
        return managerService.getBaseCategoryViewByCategory3Id(category3Id);
    }

    /**
     * 单独获取价格信息
     */
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable("skuId")Long skuId){
        return managerService.getSkuPriceBySkuId(skuId);
    }

    /**
     * 获取选定状态的销售属性
     */
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId,
                                                          @PathVariable("spuId") Long spuId){
        return managerService.getSpuSaleAttrListCheckBySku(skuId, spuId);
    }
    /**
     * 根据 spuId 获取封装之后的数据，格式：map.put(value_ids,sku_id)
     */
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable("spuId")Long spuId){
        return managerService.getSkuValueIdsMap(spuId);
    }
    /**
     * 封装 品牌信息
     */
    @GetMapping("inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable Long tmId){
        return managerService.getBaseTrademark(tmId);
    }
    /**
     * 封装 平台属性信息
     */
     @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){
         return managerService.getAttrInfoList(skuId);
     }
}
