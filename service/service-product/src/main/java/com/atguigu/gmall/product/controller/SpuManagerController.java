package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManagerService;
import org.apache.catalina.Manager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author DuanYang
 * @create 2020-06-12 9:04
 */
@RestController
@RequestMapping("admin/product")
public class SpuManagerController {
    @Autowired
    private ManagerService managerService;
    /**
     * 查询基础销售信息
     */
    @GetMapping("baseSaleAttrList")
    public Result baseSaleAttrList(){
        List<BaseSaleAttr> baseSaleAttrList =managerService.getBaseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    /**
     * spu信息大保存
     */
    //http://api.gmall.com/admin/product/saveSpuInfo
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        if (null != spuInfo){
            managerService.saveSpuInfo(spuInfo);
            return Result.ok();
        }else{
            return Result.fail();
        }
    }

}
