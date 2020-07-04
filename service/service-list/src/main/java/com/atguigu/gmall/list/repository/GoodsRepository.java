package com.atguigu.gmall.list.repository;


import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author DuanYang
 * @create 2020-06-19 11:52
 */
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
