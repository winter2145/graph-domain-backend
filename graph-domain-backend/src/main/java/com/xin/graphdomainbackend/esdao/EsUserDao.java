package com.xin.graphdomainbackend.esdao;

import com.xin.graphdomainbackend.model.entity.es.EsUser;
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

    /**
     * 根据用户名查询(模糊)
     */
    List<EsUser> findByUserNameContaining(String username);

    /**
     * 根据用户简介查询(模糊)
     */
    List<EsUser> findByUserProfileContaining(String userProfile);

    /**
     * 根据用户角色查询(精准)
     */
    List<EsUser> findByUserRole(String userRole);

    /**
     * 根据用户名或简介查询(模糊)
     */
    List<EsUser> findByUserNameContainingOrUserProfileContaining(String userName, String userProfile);

}
