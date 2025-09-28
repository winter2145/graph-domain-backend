package com.xin.graphdomainbackend.infrastructure.elasticsearch.dao;

import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsUser;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EsUserDao
        extends ElasticsearchRepository<EsUser, Long> {

    /**
     * 根据用户账号查询（精准）
     */
    Optional<EsUser> findByUserAccount(String userAccount);

    /**
     * 根据用户账号查询(模糊)
     */
    List<EsUser> findByUserAccountContaining(String userAccount);

}