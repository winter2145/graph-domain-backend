package com.xin.graphdomainbackend.esdao;

import com.xin.graphdomainbackend.model.entity.es.EsSpace;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsSpaceDao
        extends ElasticsearchRepository<EsSpace, Long> {
}
