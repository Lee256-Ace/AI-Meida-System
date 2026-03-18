package com.product.mapper;

import com.product.entity.GenerationTask;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface GenerationMapper {

    @Insert("INSERT INTO ai_generation (prompt, model, status, style, theme, created_at) " +
            "VALUES (#{prompt}, #{model}, #{status}, #{style}, #{theme}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GenerationTask task);

    @Update("UPDATE ai_generation SET " +
            "reasoning = #{reasoning}, content = #{content}, " +
            "douyin_copy = #{douyinCopy}, xiaohongshu_copy = #{xiaohongshuCopy}, " +
            "video_account_copy = #{videoAccountCopy}, toutiao_copy = #{toutiaoCopy}, " +
            "status = #{status}, word_count = #{wordCount}, completed_at = #{completedAt} " +
            "WHERE id = #{id}")
    int updateResult(GenerationTask task);

    /** 仅更新基础字段（无平台列时使用，确保至少 content 能入库） */
    @Update("UPDATE ai_generation SET reasoning = #{reasoning}, content = #{content}, " +
            "status = #{status}, word_count = #{wordCount}, completed_at = #{completedAt} WHERE id = #{id}")
    int updateResultBasic(GenerationTask task);

    @Select("SELECT * FROM ai_generation WHERE id = #{id}")
    GenerationTask selectById(Long id);

    @Select("SELECT * FROM ai_generation ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<GenerationTask> selectList(@Param("limit") int limit, @Param("offset") int offset);

    @Delete("DELETE FROM ai_generation WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM ai_generation")
    Long count();

    @Select("SELECT COUNT(*) FROM ai_generation WHERE DATE(created_at) = CURDATE()")
    Long countToday();

    @Select("SELECT COALESCE(SUM(word_count), 0) FROM ai_generation")
    Long sumWords();

    @Select("SELECT * FROM ai_generation ORDER BY created_at DESC")
    List<GenerationTask> selectAll();
}