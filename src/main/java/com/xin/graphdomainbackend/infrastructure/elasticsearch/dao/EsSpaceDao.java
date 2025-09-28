package com.xin.graphdomainbackend.infrastructure.elasticsearch.dao;

import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsSpace;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsSpaceDao
        extends ElasticsearchRepository<EsSpace, Long> {
}
