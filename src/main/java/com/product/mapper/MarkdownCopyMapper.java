package com.product.mapper;

import com.product.entity.MarkdownCopy;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MarkdownCopyMapper {

    @Insert("INSERT INTO ai_markdown_copy (title, content, created_at, updated_at) " +
            "VALUES (#{title}, #{content}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MarkdownCopy row);

    @Update("UPDATE ai_markdown_copy SET title = #{title}, content = #{content}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(MarkdownCopy row);

    @Select("SELECT * FROM ai_markdown_copy WHERE id = #{id}")
    MarkdownCopy selectById(Long id);
}
