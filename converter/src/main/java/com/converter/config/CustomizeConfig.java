package com.converter.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 获取用户自定义配置
 *
 * @author Evan
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "customize")
public class CustomizeConfig {
    private static CustomizeConfig customizeConfig;
    /**
     * 允许没有授权文件
     */
    private boolean allowWithoutLicense = false;
    /**
     * 字体目录
     */
    private String fontDir = "C:\\Windows\\Fonts";
    /**
     * 用于存放文件路径的Key
     */
    private String redisFileKey = "REDIS_FILE";
    /**
     * 用于存放目录路径的Key
     */
    private String redisDirKey = "REDIS_DIR";
    /**
     * 默认目标目录路径
     */
    private String targetDirPath = "result";
    /**
     * 文件上传目录
     */
    private String uploadPath = "upload";
    /**
     * 最大重试次数
     */
    private Integer maxRetries = 5;
    /**
     * 任务超时时间（单位秒）
     */
    private Integer missionTimeout = 300;
    /**
     * 是否允许转换slides
     */
    private boolean enableSlides = false;

    /**
     * 允许获取自定义配置
     */
    public static CustomizeConfig instance() {
        return customizeConfig;
    }

    @Autowired()
    public void init(final @Qualifier("customizeConfig") CustomizeConfig customizeConfig) {
        log.debug("开始初始化CustomizeConfig");
        CustomizeConfig.customizeConfig = customizeConfig;
        log.debug("成功初始化CustomizeConfig");
    }
}
