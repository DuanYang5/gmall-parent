package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author DuanYang
 * @create 2020-06-12 11:31
 */
@RestController
@RequestMapping("admin/product")
public class SkuManagerController {

    @Autowired
    private ManagerService managerService;
    /**
     * 根据spuId查询spu图片列表
     */
    //http://api.gmall.com/admin/product/spuImageList/5
    @GetMapping("spuImageList/{spuId}")
    public Result spuImageList(@PathVariable Long spuId){
        List<SpuImage> spuImageList = managerService.spuImageList(spuId);
        return Result.ok(spuImageList);
    }
    /**
     * 销售属性与销售属性值 回显
     */
    //http://api.gmall.com/admin/product/spuSaleAttrList/4
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId){
        List<SpuSaleAttr> spuSaleAttrList = managerService.getSpuSaleAttrListBySpuId(spuId);
        return Result.ok(spuSaleAttrList);
    }
    /**
     * 大保存sku
     */
    //http://api.gmall.com/admin/product/saveSkuInfo
    @PostMapping("saveSkuInfo")
    private Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        managerService.saveSkuInfo(skuInfo);
        return Result.ok();
    }
    /**
     * 分页查询 重载
     */
    //http://api.gmall.com/admin/product/list/1/10
    @GetMapping("list/{page}/{limit}")
    public Result getList(@PathVariable Long page,
                          @PathVariable Long limit){
        Page<SkuInfo> skuInfoPage = new Page<>(page,limit);
        IPage<SkuInfo> skuInfoIPage = managerService.selectPage(skuInfoPage);
        return Result.ok(skuInfoIPage);
    }

    /**
     * 上架
     */
    //http://api.gmall.com/admin/product/onSale/18
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        managerService.onSale(skuId);
        return Result.ok();
    }

    /**
     * 下架
     */
    //http://api.gmall.com/admin/product/cancelSale/18
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        managerService.cancelSale(skuId);
        return Result.ok();
    }
}
