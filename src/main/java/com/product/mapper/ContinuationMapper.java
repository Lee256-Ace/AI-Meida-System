package com.product.mapper;

import com.product.entity.ContinuationTask;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ContinuationMapper {

    @Insert("INSERT INTO ai_continuation (original_content, context_hint, platform, model, status, created_at) " +
            "VALUES (#{originalContent}, #{contextHint}, #{platform}, #{model}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ContinuationTask task);

    @Update("UPDATE ai_continuation SET " +
            "continuation = #{continuation}, total_length = #{totalLength}, status = #{status}, completed_at = #{completedAt}, " +
            "platform = #{platform} " +
            "WHERE id = #{id}")
    int updateResult(ContinuationTask task);

    @Select("SELECT * FROM ai_continuation WHERE id = #{id}")
    ContinuationTask selectById(Long id);

    @Select("SELECT * FROM ai_continuation ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ContinuationTask> selectList(@Param("limit") int limit, @Param("offset") int offset);

    @Delete("DELETE FROM ai_continuation WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM ai_continuation")
    Long count();

    @Select("SELECT COUNT(*) FROM ai_continuation WHERE DATE(created_at) = CURDATE()")
    Long countToday();

    @Select("SELECT COALESCE(SUM(total_length), 0) FROM ai_continuation")
    Long sumWords();

    @Select("SELECT * FROM ai_continuation ORDER BY created_at DESC")
    List<ContinuationTask> selectAll();
}