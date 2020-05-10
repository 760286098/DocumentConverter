package com.converter.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 日志消息实体
 *
 * @author Evan
 */
@Data
@AllArgsConstructor
public class LoggerMessage {
    /**
     * 时间戳
     */
    private String timestamp;
    /**
     * 日志级别
     */
    private String level;
    /**
     * 对应线程名
     */
    private String threadName;
    /**
     * 对应类名
     */
    private String className;
    /**
     * 日志主体信息
     */
    private String body;
}