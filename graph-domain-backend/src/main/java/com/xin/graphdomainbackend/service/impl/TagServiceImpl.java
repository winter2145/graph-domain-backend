package com.xin.graphdomainbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.dto.PageRequest;
import com.xin.graphdomainbackend.model.entity.Tag;
import com.xin.graphdomainbackend.model.vo.TagVO;
import com.xin.graphdomainbackend.service.TagService;
import com.xin.graphdomainbackend.mapper.TagMapper;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import net.bytebuddy.implementation.bytecode.Throw;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2025-06-20 21:47:50
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

    @Override
    public List<String> listTag() {
        return this.baseMapper.listTag();
    }

    @Override
    public Page<TagVO> getTagByPage(PageRequest pageRequest) {
        ThrowUtils.throwIf(pageRequest == null, ErrorCode.PARAMS_ERROR);
        long current = pageRequest.getCurrent();
        long pageSize = pageRequest.getPageSize();
        Page<Tag> pageTag = this.page(new Page<>(current, pageSize));
        Page<TagVO> pageTagVO = new Page<>(current, pageSize, pageTag.getTotal());

        List<Tag> records = pageTag.getRecords();
        // tag -> tagVO
        List<TagVO> tagVOList = records.stream().map(this::getTagVo).collect(Collectors.toList());
        pageTagVO.setRecords(tagVOList);

        return pageTagVO;
    }


    @Override
    public Boolean addTag(String tagName) {
        Tag tag = new Tag();
        if (StringUtils.isNotBlank(tagName)) {
            tag.setTagName(tagName);
            return this.save(tag);
        }

        return null;
    }

    @Override
    public Boolean deleteTag(Tag tag) {
        long id = Optional.ofNullable(tag)
                .map(Tag::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "标签Id不能为空"));
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "标签ID必须为正数");

        return removeById(id);
    }

    @Override
    public List<TagVO> searchByName(String name) {
        ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR);
        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("tagName", name);
        // 从数据库中查询符合条件的Tag实体列表
        List<Tag> tagList = baseMapper.selectList(queryWrapper);
        return this.getTagByList(tagList);
    }

    @Override
    public List<TagVO> getTagByList(List<Tag> list) {
        if (!CollectionUtils.isEmpty(list)) {
            return list.stream().map(this::getTagVo).collect(Collectors.toList());
        }
        return null;
    }


    @Override
    public TagVO getTagVo(Tag tag) {
        if (tag == null) {
            return null;
        }
        TagVO tagVO = new TagVO();
        BeanUtils.copyProperties(tag, tagVO);
        return tagVO;
    }
}




