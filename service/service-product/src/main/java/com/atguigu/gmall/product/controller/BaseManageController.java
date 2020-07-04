package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author DuanYang
 * @create 2020-06-09 14:39
 */
@Api("后台测试")
@RestController
@RequestMapping("admin/product")
//@CrossOrigin
public class BaseManageController {
    @Autowired
    private ManagerService managerService;
    /**
     * 查询所有一级分类
     */
    @GetMapping("getCategory1")
    public Result<List> getCategory1(){
        List<BaseCategory1> category1List = managerService.getCategory1();
        return Result.ok(category1List);
    }
    /**
     * 根据一级分类id查询二级分类
     */
//    http://api.gmall.com/admin/product/getCategory2/{category1Id}
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id){
        List<BaseCategory2> category2List = managerService.getCategory2((category1Id));
        return Result.ok(category2List);
    }
    /**
     * 根据二级分类id查询三级分类
     */
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id){
        List<BaseCategory3> category3List = managerService.getCategory3((category2Id));
        return Result.ok(category3List);
    }
    /**
     * 根据分类id查询分类集合
     */
    //http://api.gmall.com/admin/product
    // /attrInfoList/{category1Id}/{category2Id}/{category3Id}
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result getAttrInfoList(@PathVariable Long category1Id,
                      @PathVariable Long category2Id,
                      @PathVariable Long category3Id){
        List<BaseAttrInfo> attrInfoList = managerService.getAttrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(attrInfoList);
    }
    /**
     * 保存平台属性和平台属性值
     */
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        managerService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    /**
     * 根据平台属性id查询平台属性数据
     * @param attrId
     * @return
     */
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId){
//        List<BaseAttrValue> baseAttrValueList= managerService.getAttrValueList(attrId);
        BaseAttrInfo baseAttrInfo = managerService.getAttrInfo(attrId);
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(attrValueList);
    }
    /**
     * 带有条件的 分页查询
     */
//    http://api.gmall.com/admin/product/ {page}/{limit}?category3Id=61
    @GetMapping("{page}/{limit}")
    public Result getPageList(@PathVariable Long page,
                              @PathVariable Long limit,
                              SpuInfo spuInfo){
        Page<SpuInfo> spuInfoPage = new Page<>(page,limit);
        IPage<SpuInfo> spuInfoIPage = managerService.selectPage(spuInfoPage, spuInfo);
        return Result.ok(spuInfoIPage);
    }

}
