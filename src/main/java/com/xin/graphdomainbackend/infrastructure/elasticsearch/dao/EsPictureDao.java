package com.xin.graphdomainbackend.infrastructure.elasticsearch.dao;

import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsPicture;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsPictureDao
        extends ElasticsearchRepository<EsPicture, Long> {

}
