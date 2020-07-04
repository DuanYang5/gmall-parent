package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author DuanYang
 * @create 2020-06-19 11:48
 */
@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 搜索功能开发
     */
    @Override
    public SearchResponseVo search(SearchParam searchParam) throws Exception {
        //构建dsl语句
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //执行查询dsl语句
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //获取查询后的结果集
        SearchResponseVo responseVo =parseSearchResult(searchResponse);
        //设置属性
        Integer pageSize = searchParam.getPageSize();
        responseVo.setPageSize(pageSize);
        responseVo.setPageNo(searchParam.getPageNo());
        //计算分页总页数
        Long totalPages = (responseVo.getTotal() + pageSize-1)/pageSize;
        responseVo.setTotalPages(totalPages);

        return responseVo;
    }

    /**
     * 获取查询后结果集
     */
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();

        //private List<SearchResponseTmVo> trademarkList;
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        //获取品牌信息
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        //流式编程 获取 buckets 返回SearchResponseTmVo集合
        List<SearchResponseTmVo> responseTmVoList = tmIdAgg.getBuckets().stream().map(buckets -> {
            String tmId = ((Terms.Bucket) buckets).getKeyAsString();
            //给前台品牌赋值
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            searchResponseTmVo.setTmId(Long.parseLong(tmId));

            //获取 桶中Aggregations
            Map<String, Aggregation> tmIdAggregationMap = ((Terms.Bucket) buckets).getAggregations().asMap();

            ParsedStringTerms tmNameAgg = (ParsedStringTerms) tmIdAggregationMap.get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms) tmIdAggregationMap.get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);

            return searchResponseTmVo;
        }).collect(Collectors.toList());

        searchResponseVo.setTrademarkList(responseTmVoList);

        //private List<Goods> goodsList = new ArrayList<>();
        //hits hits _source 下为json字符串
        SearchHits hits = searchResponse.getHits();
        SearchHit[] subHits = hits.getHits();
        List<Goods> goodsArrayList = new ArrayList<>();
        if (null!=subHits&& subHits.length>0){
            for (SearchHit subHit : subHits) {
                //获取 _source
                String sourceAsString = subHit.getSourceAsString();
                //获取高亮标题
                HighlightField title = subHit.getHighlightFields().get("title");
                Goods goods = JSON.parseObject(sourceAsString, Goods.class);
                if (null!=title){
                    goods.setTitle(title.getFragments()[0].toString());
                }
                //转为json对象
                goodsArrayList.add(goods);
            }
        }
        searchResponseVo.setGoodsList(goodsArrayList);

        //private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
        ParsedNested attrsAgg = (ParsedNested) aggregationMap.get("attrsAgg");
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if (CollectionUtils.isNotEmpty(attrIdAggBuckets)) {
            List<SearchResponseAttrVo> attrsList = attrIdAggBuckets.stream().map(buckets -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                String attrId = ((Terms.Bucket) buckets).getKeyAsString();
                searchResponseAttrVo.setAttrId(Long.parseLong(attrId));

                ParsedStringTerms attrNameAgg = ((Terms.Bucket) buckets).getAggregations().get("attrNameAgg");
                String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
                searchResponseAttrVo.setAttrName(attrName);

                ParsedStringTerms attrValueAgg = ((Terms.Bucket) buckets).getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> attrValueAggBucketsList = attrValueAgg.getBuckets();
                List<String> attrValueList = attrValueAggBucketsList.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());

                searchResponseAttrVo.setAttrValueList(attrValueList);
                return searchResponseAttrVo;
            }).collect(Collectors.toList());

            searchResponseVo.setAttrsList(attrsList);
        }
        //private Long total;//总记录数
        searchResponseVo.setTotal(hits.getTotalHits());
        return searchResponseVo;
    }

    /**
     * 构建dsl语句
     * GET /goods/info/_search
     * {
     *   "query": {
     *     "bool": {
     *       "filter": [
     *       {"term": {"category1Id": "2"}},
     *       {"term": {"category2Id": "13"}},
     *       {"term": {"category3Id": "61"}},
     *       {"bool":{
     *           "must":[
     *             {"nested":{
     *                "path": "attrs",
     *                "query": {
     *                 "bool": {
     *                   "must": [
     *                     { "term": { "attrs.attrValue": "2800-4499" }}
     *                   ]
     *                 }
     *               }
     *             }}
     *           ]
     *         }}
     *       ],"must": [
     *         {"match": {
     *           "title": "小米手机"
     *         }}
     *       ]
     *     }
     *   },"sort": [
     *     {
     *       "hotScore": {
     *         "order": "desc"
     *       }
     *     }
     *   ],"highlight": {
     *     "fields": {
     *       "title": {}
     *     },"post_tags": [
     *       "</span>"
     *     ],"pre_tags": [
     *       "<span style=color:red>"
     *     ]
     *   },"from": 0
     *   ,"size": 2
     *   ,"aggs": {
     *     "tmIdAgg": {
     *       "terms": {
     *         "field": "tmId"
     *       },"aggs": {
     *         "tmNameAgg": {
     *           "terms": {
     *             "field": "tmName"
     *           },"aggs": {
     *             "tmLogoUrlAgg": {
     *               "terms": {
     *                 "field": "tmLogoUrl"
     *               }
     *             }
     *           }
     *         }
     *       }
     *     },
     *     "attrsAgg":{
     *       "nested": {
     *         "path": "attrs"
     *       },"aggs": {
     *         "attrIdAgg": {
     *           "terms": {
     *             "field": "attrs.attrId"
     *           },"aggs": {
     *             "attrNameAgg": {
     *               "terms": {
     *                 "field": "attrs.attrName"
     *               },"aggs": {
     *                 "attrValueAgg": {
     *                   "terms": {
     *                     "field": "attrs.attrValue"
     *                   }
     *                 }
     *               }
     *             }
     *           }
     *         }
     *       }
     *     }
     *   }
     * }
     */
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //定义查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (StringUtils.isNotEmpty(searchParam.getKeyword())){
            //match
            //Operator.AND 表示查询拆分词语 在title中同时存在
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            //must todo match
            boolQueryBuilder.must(title);
        }
        if (null!=searchParam.getCategory1Id()){
            //term1
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            //bool filter todo term1
            boolQueryBuilder.filter(category1Id);
        }
        if (null!=searchParam.getCategory2Id()){
            //term2
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            //bool filter todo term2
            boolQueryBuilder.filter(category2Id);
        }
        if (null!=searchParam.getCategory3Id()){
            //term3
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            //bool filter todo term3
            boolQueryBuilder.filter(category3Id);
        }
        //查询品牌
        String trademark = searchParam.getTrademark();
        if (StringUtils.isNotEmpty(trademark)){
            //数据格式 trademark=2:华为
            String[] split = trademark.split(":");
            if (null!=split&&split.length==2){
                //term4
                TermQueryBuilder tmId = QueryBuilders.termQuery("tmId", split[0]);
                //bool filter todo term4
                boolQueryBuilder.filter(tmId);
            }
        }
        //根据 平台属性 进行查询 props=23:4G:运行内存
        String[] props = searchParam.getProps();
        if (null!=props&&props.length>0){
            for (String prop : props) {
                String[] split = prop.split(":");
                if (null != split && split.length==3){
                    //bool bool
                    BoolQueryBuilder boolQuery1 = QueryBuilders.boolQuery();
                    //bool bool... bool
                    BoolQueryBuilder boolQuery2 = QueryBuilders.boolQuery();

                    //bool bool... bool must term 格式"attrs.attrId": 1
                    boolQuery2.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                    boolQuery2.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                    boolQuery2.must(QueryBuilders.termQuery("attrs.attrName",split[2]));

                    //bool bool nested
                    boolQuery1.must(QueryBuilders.nestedQuery("attrs",boolQuery2, ScoreMode.None));

                    //
                    boolQueryBuilder.filter(boolQuery1);
                }
            }
        }

        //query todo bool
        searchSourceBuilder.query(boolQueryBuilder);

        //分页编写 from
        //每页开始的起始条数
        int from =(searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        //每页显示条数 size
        searchSourceBuilder.size(searchParam.getPageSize());

        //设置高亮 highlight
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);

        //设置排序 sort
        // 排序规则
        // 1:hotScore 2:price 3:
        String order = searchParam.getOrder();
        if (StringUtils.isNotEmpty(order)){
            String[] split = order.split(":");
            if (null!=split&&split.length==2){
                String field = null;
                switch (split[0]){
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                searchSourceBuilder.sort(field,"asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
            }else {
                // 没有传值的时候给默认值
                searchSourceBuilder.sort("hotScore",SortOrder.DESC);
            }
        }

        //聚合 品牌
        TermsAggregationBuilder termsAggregation =
                AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));
        //将聚合放入查询器
        searchSourceBuilder.aggregation(termsAggregation);

        //聚合 平台属性
        NestedAggregationBuilder nestedAggregationBuilder = AggregationBuilders.nested("attrsAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")));
        //将聚合放入查询器
        searchSourceBuilder.aggregation(nestedAggregationBuilder);

        //过滤结果集
        searchSourceBuilder.fetchSource(new String[]{"id","defaultImg","title","price"},null);
        //指定 index 与 type GET /goods/info/_search
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);

        System.out.println("dsl : " + searchSourceBuilder.toString());
        return searchRequest;
    }

    /**
     * 更新热点
     */
    @Override
    public void incrHotScore(Long skuId) {
        String key = "hotScore:";
        //自增一
        Double hotScore = redisTemplate.opsForZSet().incrementScore(key, "skuId:" + skuId, 1);
        if (hotScore%10==0){
            //从es中获取goods属性
            Optional<Goods> optional = goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(Math.round(hotScore));
            goodsRepository.save(goods);
        }
    }

    @Override
    public void upperGoods(Long skuId) {
        Goods goods = new Goods();
        //获取商品基本信息
        SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
        if (null!=skuInfo){
            goods.setId(skuId);
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setTitle(skuInfo.getSkuName());
            goods.setCreateTime(new Date());
            //获取商品分类信息
            Long category3Id = skuInfo.getCategory3Id();
            BaseCategoryView categoryView = productFeignClient.getCategoryView(category3Id);
            if (null!=categoryView){
                goods.setCategory1Id(categoryView.getCategory1Id());
                goods.setCategory2Id(categoryView.getCategory2Id());
                goods.setCategory3Id(categoryView.getCategory3Id());
                goods.setCategory1Name(categoryView.getCategory1Name());
                goods.setCategory2Name(categoryView.getCategory2Name());
                goods.setCategory3Name(categoryView.getCategory3Name());
            }
            //品牌信息
            Long tmId = skuInfo.getTmId();
            BaseTrademark trademark = productFeignClient.getTrademark(tmId);
            if (null!=trademark){
                goods.setTmId(trademark.getId());
                goods.setTmName(trademark.getTmName());
                goods.setTmLogoUrl(trademark.getLogoUrl());
            }

        }
        //平台属性信息
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        if (null!=attrList){
            List<SearchAttr> searchAttrList = attrList.stream().map((baseAttrInfo) -> {
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                //获取平台属性值集合
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                searchAttr.setAttrValue(attrValueList.get(0).getValueName());
                return searchAttr;
            }).collect(Collectors.toList());
            goods.setAttrs(searchAttrList);
        }

        //将数据保存到es
        goodsRepository.save(goods);
    }

    @Override
    public void lowerGoods(Long skuId) {
        goodsRepository.deleteById(skuId);
    }
}
