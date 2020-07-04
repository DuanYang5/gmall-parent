package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author DuanYang
 * @create 2020-06-09 11:30
 */
@Service
public class ManagerServiceImpl implements ManagerService {
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    @Autowired
    private RabbitService rabbitService;
    /**
     * 封装 平台属性信息
     */
    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long skuId) {
        return baseAttrInfoMapper.selectAttrInfoList(skuId);
    }

    /**
     * 封装 品牌信息
     */
    @Override
    public BaseTrademark getBaseTrademark(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }

    /**
     * 获取所有分类信息
     */
    @Override
    @GmallCache(prefix = "index:")
    public List<JSONObject> getBaseCategoryList() {
        ArrayList<JSONObject> list = new ArrayList<>();
        //查询所有分类信息
        List<BaseCategoryView> categoryViewList = baseCategoryViewMapper.selectList(null);
        //按照1级分类id分组，并获取1级分类数据
        int index = 1;
        Map<Long, List<BaseCategoryView>> category1Map = categoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));

        for (Map.Entry<Long, List<BaseCategoryView>> entry : category1Map.entrySet()) {
            //获取map的key
            Long category1Id = entry.getKey();
            //获取二级分类下所有数据
            List<BaseCategoryView> category2List = entry.getValue();
            JSONObject category1 = new JSONObject();
            category1.put("index",index++);
            category1.put("categoryId",category1Id);
            //获取一级分类名称
            String categoryName = category2List.get(0).getCategory1Name();
            category1.put("categoryName",categoryName);

//            index++;
            //按照2级分类id分组，并获取2级分类数据
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            ArrayList<JSONObject> category2Child = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> category2Entry : category2Map.entrySet()) {
                //获取map的key
                Long category2Id = category2Entry.getKey();
                //获取三级分类下所有数据
                List<BaseCategoryView> category3List = category2Entry.getValue();
                JSONObject category2 = new JSONObject();
                category2.put("categoryId",category2Id);
                category2.put("categoryName",category3List.get(0).getCategory2Name());
                category2Child.add(category2);
                //添加三级分类信息
                ArrayList<JSONObject> category3Child = new ArrayList<>();
                category3List.stream().forEach(category3View ->{
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId",category3View.getCategory3Id());
                    category3.put("categoryName",category3View.getCategory3Name());
                    category3Child.add(category3);
                });
                //todo 放置三级分类信息
                category2.put("categoryChild",category3Child);
            }
            //todo 放置二级分类信息
            category1.put("categoryChild",category2Child);
            list.add(category1);
        }
        return list;
    }

    /**
     * 查询所有一级分类
     */
    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }
    /**
     * 根据一级分类id查询二级分类
     */
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        QueryWrapper<BaseCategory2> wrapper = new QueryWrapper<>();
        wrapper.eq("category1_id",category1Id);
        return baseCategory2Mapper.selectList(wrapper);
    }
    /**
     * 根据二级分类id查询三级分类
     */
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        QueryWrapper<BaseCategory3> wrapper = new QueryWrapper<>();
        wrapper.eq("category2_id",category2Id);
        return baseCategory3Mapper.selectList(wrapper);
    }
    /**
     * 根据分类id查询分类集合
     */
    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id,category2Id,category3Id);
    }
    /**
     * 保存平台属性和平台属性值
     */
    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        if (baseAttrInfo.getId()!=null){
            //修改功能
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else{
            //插入数据
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        //先将要修改的数据删除
        QueryWrapper<BaseAttrValue> wrapper = new QueryWrapper<>();
        wrapper.eq("attr_id",baseAttrInfo.getId());
        baseAttrValueMapper.delete(wrapper);

        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList!=null && attrValueList.size()>0){
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //attrId没有赋值，需要手动赋值
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }
    /**
     * 根据属性值id获取平台属性信息
     */
    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        QueryWrapper<BaseAttrValue> wrapper = new QueryWrapper<>();
        wrapper.eq("attr_id",attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.selectList(wrapper);

        baseAttrInfo.setAttrValueList(baseAttrValueList);
        return baseAttrInfo;

    }
    /**
     * 带有条件的 分页查询
     */
    @Override
    public IPage<SpuInfo> selectPage(Page<SpuInfo> spuInfoPageParam, SpuInfo spuInfo) {
        QueryWrapper<SpuInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id",spuInfo.getCategory3Id());
        wrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(spuInfoPageParam,wrapper);
    }
    /**
     * 分页查询 重载
     */
    @Override
    public IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoPage) {
        QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoPage,wrapper);
    }
    /**
     * 查询基础销售信息
     */
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    /**
     * spu信息大保存
     */
    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
    //spuInfo 保存数据
        spuInfoMapper.insert(spuInfo);
    //spuImage 保存 没有spuId,从spuInfo获取
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (null!=spuImageList && spuImageList.size()>0){
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }
    //spuSaleAttr 保存 没有spuId,从spuInfo获取
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (null!=spuSaleAttrList && spuSaleAttrList.size()>0){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);
            //spuSaleAttrValue 保存 没有spuId,从spuInfo获取
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (null!=spuSaleAttrValueList && spuSaleAttrValueList.size()>0){
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                    //没有SaleAttrName 从对应循环的SaleAttr获取
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
    }
    /**
     * 根据spuId查询spu图片列表
     */
    @Override
    public List<SpuImage> spuImageList(Long spuId) {
        QueryWrapper<SpuImage> wrapper = new QueryWrapper<>();
        wrapper.eq("spu_id",spuId);
        return spuImageMapper.selectList(wrapper);
    }
    /**
     * 销售属性与销售属性值 回显
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListBySpuId(Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrListBySpuId(spuId);
    }
    /**
     * 大保存sku
     */
    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
    //
        skuInfoMapper.insert(skuInfo);

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (CollectionUtils.isNotEmpty(skuSaleAttrValueList)){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());

                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }
        //平台属性
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (CollectionUtils.isNotEmpty(skuAttrValueList)){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }

        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (CollectionUtils.isNotEmpty(skuImageList)){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
        //发送商品上架消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuInfo.getId());
    }

    @Override
    public void onSale(Long skuId) {
        //上架
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(1);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);
        //发送商品上架消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);
    }

    @Override
    public void cancelSale(Long skuId) {
        //下架
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(0);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);
        //发送商品下架消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);
    }

    /**
     * 通过skuId获取基本信息与图片数据redisson
     */
    @Override
    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX)
    public SkuInfo getSkuInfo(Long skuId) {
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoRedisson(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //先查询缓存，如果缓存有数据，则查询，没有则查询数据库并放入缓存
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            if (skuInfo==null){
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(lockKey);
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX2, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                if (res){
                    try {
                        skuInfo = getSkuInfoDB(skuId);
                        if (skuInfo==null){
                            //防止缓存穿透
                            SkuInfo skuInfo1 = new SkuInfo();
                            redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }else{
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }else{
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoRedis(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //先查询缓存，如果缓存有数据，则查询，没有则查询数据库并放入缓存
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            if (skuInfo==null){
                //应从数据库中获取数据之前，添加分布式锁 防止缓存击穿
                //定义分布式锁的key lockKey = sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                String uuid = UUID.randomUUID().toString();
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                //设置分布式锁成功
                if (isExist){
                    //从数据库获取数据
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo==null){
                        //防止缓存穿透
                        SkuInfo skuInfo1 = new SkuInfo();
                        redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo1;
                    }
                    redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    //lua脚本
                    String script="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    //指定返回数据类型
                    redisScript.setResultType(Long.class);
                    redisScript.setScriptText(script);
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey),uuid);
                    return skuInfo;
                }else{
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }else{
                if (skuInfo.getId()==null){
                    return null;
                }
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return getSkuInfoDB(skuId);
    }


    private SkuInfo getSkuInfoDB(Long skuId) {
        //获取sku基本信息
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        if (null!=skuInfo) {
            QueryWrapper<SkuImage> wrapper = new QueryWrapper<>();
            wrapper.eq("sku_id",skuId);
            List<SkuImage> skuImageList = skuImageMapper.selectList(wrapper);
            //获取skuImage 信息
            skuInfo.setSkuImageList(skuImageList);
        }

        return skuInfo;
    }

    /**
     * 通过视图 获取分类信息
     */
    @Override
    @GmallCache(prefix = "categoryViewByCategory3Id:")
    public BaseCategoryView getBaseCategoryViewByCategory3Id(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }
    /**
     * 单独获取价格信息
     */
    @GmallCache(prefix = "price:")
    @Override
    public BigDecimal getSkuPriceBySkuId(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (null!=skuInfo){
            return skuInfo.getPrice();
        }
        return null;
    }

    /**
     * 根据 skuId,spuId 获取选定销售属性值
     */
    @GmallCache(prefix = "spuSaleAttrListCheckBySku:")
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }
    /**
     * 根据 spuId 获取封装之后的数据，格式：map.put(value_ids,sku_id)
     */
    @Override
    @GmallCache(prefix = "skuValueIdsMap:")
    public Map getSkuValueIdsMap(Long spuId) {
        Map<Object,Object> map = new HashMap<>();
        List<Map> mapList =skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
        if (CollectionUtils.isNotEmpty(mapList)){
            for (Map skuMaps : mapList) {
                map.put(skuMaps.get("value_ids"),skuMaps.get("sku_id"));
            }
        }
        return map;
    }


}
