package com.product.mapper;

import com.product.entity.MarkdownCopy;
import org.apache.ibatis.annotations.*;

import java.util.List;

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

    @Select("SELECT * FROM ai_markdown_copy ORDER BY updated_at DESC")
    List<MarkdownCopy> selectAll();

    @Delete("DELETE FROM ai_markdown_copy WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM ai_markdown_copy")
    Long count();

    @Select("SELECT COUNT(*) FROM ai_markdown_copy WHERE DATE(updated_at) = CURDATE()")
    Long countToday();

    @Select("SELECT COALESCE(SUM(CHAR_LENGTH(content)), 0) FROM ai_markdown_copy")
    Long sumWords();
}
