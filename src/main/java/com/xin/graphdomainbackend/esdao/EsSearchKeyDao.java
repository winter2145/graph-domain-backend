package com.xin.graphdomainbackend.esdao;

import com.xin.graphdomainbackend.model.entity.es.EsSearchKeyword;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface EsSearchKeyDao
        extends ElasticsearchRepository<EsSearchKeyword, Long>{

    /**
     * 根据类型和关键词查询
     */
    List<EsSearchKeyword> findByTypeAndKeyword(String type, String keyword);
}
