package com.product.mapper;

import com.product.entity.PublishHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PublishHistoryMapper {

    PublishHistory selectById(@Param("id") Long id);

    List<PublishHistory> selectByPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    Integer countTotal();

    List<PublishHistory> selectByPageWithSearch(
            @Param("offset") Integer offset,
            @Param("limit") Integer limit,
            @Param("platformName") String platformName,
            @Param("title") String title);

    Integer countTotalWithSearch(
            @Param("platformName") String platformName,
            @Param("title") String title);

    Integer insert(PublishHistory row);

    Integer update(PublishHistory row);

    Integer deleteById(@Param("id") Long id);
}
