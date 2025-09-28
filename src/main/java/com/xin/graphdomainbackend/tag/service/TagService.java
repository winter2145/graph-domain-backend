package com.xin.graphdomainbackend.tag.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.common.PageRequest;
import com.xin.graphdomainbackend.tag.api.dto.vo.TagVO;
import com.xin.graphdomainbackend.tag.dao.entity.Tag;

import java.util.List;

/**
* @author Administrator
* @description 针对表【tag(标签)】的数据库操作Service
* @createDate 2025-06-20 21:47:50
*/
public interface TagService extends IService<Tag> {

    /**
     * 获取所有标签名称列表
     */
    List<String> listTag();

    /**
     * 分页获取 标签
     * @param pageRequest 分页请求
     */
    Page<TagVO> getTagByPage(PageRequest pageRequest);

    /**
     * 添加新标签
     * @param tagName 标签名称
     */
    Boolean addTag(String tagName);

    /**
     * 删除标签
     */
    Boolean deleteTag(Tag tag);

    /**
     * 根据名字，查找标签
     * @param name
     */
    List<TagVO> searchByName(String name);

    /**
     * 标签列表脱敏
     * @param list 标签列表
     */
    List<TagVO> getTagByList(List<Tag> list);

    /**
     * 标签脱敏
     * @param tag 标签
     */
    TagVO getTagVo(Tag tag);
}
