package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author DuanYang
 * @create 2020-06-22 9:34
 */
@Controller
@RequestMapping
public class ListController {
    @Autowired
    private ListFeignClient listFeignClient;

    @GetMapping("list.html")
    public String search(SearchParam searchParam, Model model){
        Result<Map> result = listFeignClient.list(searchParam);
        //拼接检索URL
        String urlParam = makeUrlParam(searchParam);
        model.addAttribute("urlParam",urlParam);
        model.addAttribute("searchParam",searchParam);

        //添加品牌属性面包屑
        String tradeMark = makeTradeMark(searchParam.getTrademark());
        model.addAttribute("trademarkParam",tradeMark);

        //添加平台属性面包屑
        List<Map<String, String>> props = makeProps(searchParam.getProps());
        model.addAttribute("propsParamList",props);

        //商品排序功能
        Map<String, Object> order = order(searchParam.getOrder());
        model.addAttribute("orderMap",order);

        model.addAllAttributes(result.getData());
        return "list/index";
    }

    /**
     * 商品排序功能
     */
    private Map<String,Object> order(String order){
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(order)){
            String[] split = order.split(":");
            if (null!=split&&split.length==2){
                map.put("type",split[0]);
                map.put("sort",split[1]);
            }
        }else{
            //默认排序
            map.put("type","1");
            map.put("sort","asc");
        }
        return map;
    }

    /**
     * 品牌面包屑
     */
    private String makeTradeMark(String trademark){
        if (StringUtils.isNotEmpty(trademark)){
            String[] split = trademark.split(":");
            if (null!=split && split.length==2){
                return "品牌：" + split[1];
            }
        }
        return null;
    }

    /**
     * 平台属性面包屑
     */
    private List<Map<String,String>> makeProps(String[] props){
        List<Map<String, String>> list = new ArrayList<>();
        //props=23:4G:运行内存
        if (null!=props && props.length>0){
            for (String prop : props) {
                String[] split = prop.split(":");
                if (null!=split && split.length==3){
                    HashMap<String, String> map = new HashMap<>();
                    map.put("attrId",split[0]);
                    map.put("attrValue",split[1]);
                    map.put("attrName",split[2]);

                    list.add(map);
                }
            }
        }
        return list;
    }

    /**
     * 拼接URL字符串
     */
    private String makeUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        //根据关键字检索
        if (StringUtils.isNotEmpty(searchParam.getKeyword())){
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        //根据分类id检索
        if (null!=searchParam.getCategory3Id()){
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        if (null!=searchParam.getCategory2Id()){
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if (null!=searchParam.getCategory1Id()){
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        //根据品牌检索
        if (StringUtils.isNotEmpty(searchParam.getTrademark())){
            if (urlParam.length()>0){
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        //拼接平台属性
        if (null != searchParam.getProps()){
            for (String prop : searchParam.getProps()) {
                if (urlParam.length()>0){
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        //返回拼接URL
        return "list.html?"+urlParam.toString();
    }
}
