package com.xin.graphdomainbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xin.graphdomainbackend.model.entity.Comments;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;


/**
* @author Administrator
* @description 针对表【comments】的数据库操作Mapper
* @createDate 2025-07-26 14:10:43
* @Entity generator.domain.Comments
*/
public interface CommentsMapper extends BaseMapper<Comments> {

    @Select("WITH RECURSIVE comment_tree AS (" +
            "SELECT commentId FROM comments WHERE parentCommentId = #{parentId} " +
            "AND isDelete = 0 " +
            "UNION ALL " +
            "SELECT c.commentId FROM comments c" +
            "  JOIN comment_tree ct ON c.parentCommentId = ct.commentId " +
            "  WHERE c.isDelete = 0 )" +
            " SELECT commentId FROM comment_tree")
    List<Long> selectAllChildCommentIds(@Param("parentId") Long parentId);

    @Select({
            "WITH RECURSIVE comment_tree AS (",
            "  SELECT * FROM comments WHERE parentCommentId = #{parentId} AND isDelete = 0",
            "  UNION ALL",
            "  SELECT c.* FROM comments c",
            "  JOIN comment_tree ct ON c.parentCommentId = ct.commentId",
            "  WHERE c.isDelete = 0",
            ")",
            "SELECT * FROM comment_tree"
    })
    List<Comments> selectAllChildCommentsWithDetails(@Param("parentId") Long parentId);

    @Update("<script>" +
            "UPDATE comments SET isDelete = 1 WHERE commentId IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int batchSoftDelete(@Param("ids") List<Long> ids);
}




