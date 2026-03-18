package com.product.mapper;

import com.product.entity.PolishTask;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PolishMapper {

    @Insert("INSERT INTO ai_polish (original_content, polish_type, target_style, platform, model, status, created_at) " +
            "VALUES (#{originalContent}, #{polishType}, #{targetStyle}, #{platform}, #{model}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PolishTask task);

    @Update("UPDATE ai_polish SET " +
            "result_content = #{resultContent}, changes = #{changes}, status = #{status}, completed_at = #{completedAt}, " +
            "platform = #{platform} " +
            "WHERE id = #{id}")
    int updateResult(PolishTask task);

    @Select("SELECT * FROM ai_polish WHERE id = #{id}")
    PolishTask selectById(Long id);

    @Select("SELECT * FROM ai_polish ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<PolishTask> selectList(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM ai_polish WHERE polish_type = #{type} ORDER BY created_at DESC")
    List<PolishTask> selectByType(String type);

    @Delete("DELETE FROM ai_polish WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM ai_polish")
    Long count();

    @Select("SELECT COUNT(*) FROM ai_polish WHERE DATE(created_at) = CURDATE()")
    Long countToday();

    @Select("SELECT COALESCE(SUM(CHAR_LENGTH(result_content)), 0) FROM ai_polish")
    Long sumWords();

    @Select("SELECT * FROM ai_polish ORDER BY created_at DESC")
    List<PolishTask> selectAll();
}