package com.converter.mapper;

import com.converter.constant.ConvertStatus;
import com.converter.pojo.ConvertInfo;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Evan
 */
@Mapper
@Repository
public interface ConvertInfoMapper {
    /**
     * 将转换信息写入数据库
     *
     * @param convertInfo 转换信息
     */
    @Insert("INSERT INTO `convert_info` ( source_path, target_path, file_size, join_time, start_time, end_time, convert_status, retry, exceptions )" +
            "VALUES ( " +
            "#{sourceFilePath, jdbcType=VARCHAR}, " +
            "#{targetFilePath, jdbcType=VARCHAR}, " +
            "#{fileSize, jdbcType=VARCHAR}, " +
            "#{joinTime, jdbcType=BIGINT}, " +
            "#{startTime, jdbcType=BIGINT}, " +
            "#{endTime, jdbcType=BIGINT}, " +
            "#{status, jdbcType=VARCHAR}, " +
            "#{retry, jdbcType=INTEGER}, " +
            "#{exceptions, jdbcType=CLOB})")
    void insert(ConvertInfo convertInfo);


    /**
     * 读取数据库中所有转换信息
     *
     * @return 信息列表
     */
    @Select("SELECT source_path, target_path, file_size, join_time, start_time, end_time, convert_status, retry, exceptions FROM `convert_info`")
    @Results({
            @Result(property = "sourceFilePath", column = "source_path", javaType = String.class),
            @Result(property = "targetFilePath", column = "target_path", javaType = String.class),
            @Result(property = "fileSize", column = "file_size", javaType = String.class),
            @Result(property = "joinTime", column = "join_time", javaType = Long.class),
            @Result(property = "startTime", column = "start_time", javaType = Long.class),
            @Result(property = "endTime", column = "end_time", javaType = Long.class),
            @Result(property = "status", column = "convert_status", javaType = ConvertStatus.class),
            @Result(property = "retry", column = "retry", javaType = Integer.class),
            @Result(property = "exceptions", column = "exceptions", javaType = String.class)
    })
    List<ConvertInfo> getAll();
}
