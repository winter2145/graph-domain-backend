package com.xin.graphdomainbackend.service;

import com.xin.graphdomainbackend.model.entity.HotSearch;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Date;
import java.util.List;

/**
* @author Administrator
* @description 针对表【hot_search(热门搜索记录表)】的数据库操作Service
* @createDate 2025-09-06 09:30:40
*/
public interface HotSearchService extends IService<HotSearch> {

    /**
     * 获取热门搜索词 列表
     */
    List<String> getHotSearchList(String type, int size);

    /**
     * 保存搜索关键词
     */
    void recordSearchKeyword(String searchText, String type);

}
