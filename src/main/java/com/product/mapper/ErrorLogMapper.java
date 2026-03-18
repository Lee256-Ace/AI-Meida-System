package com.product.mapper;

import com.product.entity.ErrorLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ErrorLogMapper {

    @Insert("INSERT INTO ai_error_log (module, phase, error_message, error_detail, request_info, created_at) " +
            "VALUES (#{module}, #{phase}, #{errorMessage}, #{errorDetail}, #{requestInfo}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ErrorLog log);

    @Select("SELECT * FROM ai_error_log WHERE id = #{id}")
    ErrorLog selectById(Long id);

    @Select("SELECT * FROM ai_error_log WHERE module = #{module} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ErrorLog> selectByModule(@Param("module") String module, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM ai_error_log ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ErrorLog> selectList(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM ai_error_log")
    Long count();

    @Select("SELECT COUNT(*) FROM ai_error_log WHERE module = #{module}")
    Long countByModule(String module);

    @Delete("DELETE FROM ai_error_log WHERE id = #{id}")
    int deleteById(Long id);
}
