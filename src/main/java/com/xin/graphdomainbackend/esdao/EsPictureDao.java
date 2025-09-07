package com.xin.graphdomainbackend.esdao;

import com.xin.graphdomainbackend.model.entity.es.EsPicture;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface EsPictureDao
        extends ElasticsearchRepository<EsPicture, Long> {

}
