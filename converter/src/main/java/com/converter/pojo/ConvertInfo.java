package com.converter.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import com.converter.constant.ConvertStatus;
import com.converter.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 转换信息
 *
 * @author Evan
 */
@Data
@AllArgsConstructor
public class ConvertInfo {
    /**
     * 源文件路径
     */
    @JSONField(name = "source", ordinal = 1)
    private String sourceFilePath;
    /**
     * 目的路径
     */
    @JSONField(name = "target", ordinal = 2)
    private String targetFilePath;
    /**
     * 文件大小
     */
    @JSONField(name = "size", ordinal = 3)
    private String fileSize;
    /**
     * 任务加入队列时间
     */
    @JSONField(name = "join", ordinal = 4)
    private Long joinTime;
    /**
     * 任务开始时间
     */
    @JSONField(name = "start", ordinal = 5)
    private Long startTime;
    /**
     * 任务结束时间
     */
    @JSONField(name = "end", ordinal = 6)
    private Long endTime;
    /**
     * 任务状态
     */
    @JSONField(name = "status", ordinal = 7)
    private ConvertStatus status;
    /**
     * 任务重试次数
     */
    @JSONField(name = "retry", ordinal = 8)
    private Integer retry;
    /**
     * 重试过程中产生的错误
     */
    @JSONField(name = "exceptions", ordinal = 9)
    private String exceptions;

    public ConvertInfo(Long joinTime, String sourceFilePath, String targetFilePath, String fileSize) {
        this.joinTime = joinTime;
        this.startTime = 0L;
        this.endTime = 0L;
        this.sourceFilePath = sourceFilePath;
        this.targetFilePath = targetFilePath;
        this.fileSize = fileSize;
        this.status = ConvertStatus.WAIT_OUTSIDE;
        this.retry = 0;
        this.exceptions = "";
    }

    /**
     * 获取json格式的字符串
     */
    @Override
    public String toString() {
        return StringUtils.toJsonString(this);
    }
}
