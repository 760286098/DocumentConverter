package com.converter.service;

import com.converter.config.CustomizeConfig;
import com.converter.config.ThreadPoolConfig;
import com.converter.core.ConvertManager;
import com.converter.core.ConvertMission;
import com.converter.pojo.ConvertInfo;
import com.converter.utils.RedisUtils;
import com.converter.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 主service类, 屏蔽细节, 对ConvertManager进一步封装
 * 实际上ConvertManager就可以作为Service, 不过代码冗杂, 所以将其中API单独提取出来
 *
 * @author Evan
 */
@Slf4j
@Service
public class MainService {
    /**
     * 配置分隔符
     */
    private final static String SEPARATOR = ",";

    /**
     * 添加单个文件任务, 使用默认目的路径
     *
     * @param sourceFilePath 源文件路径
     */
    public void addMission(String sourceFilePath) {
        log.debug("添加文件[{}], 使用默认目的路径", sourceFilePath);
        ConvertManager.addMission(sourceFilePath);
    }

    /**
     * 添加单个文件任务, 使用自定义目的路径
     *
     * @param sourceFilePath 源文件路径
     * @param targetDirPath  目的路径
     */
    public void addMission(String sourceFilePath, String targetDirPath) {
        log.debug("添加文件[{}], 使用自定义目的路径[{}]", sourceFilePath, targetDirPath);
        ConvertManager.addMission(sourceFilePath, targetDirPath);
    }

    /**
     * 添加文件夹任务, 使用默认目的路径
     *
     * @param sourceDirPath 源文件夹路径
     */
    public void addMissions(String sourceDirPath) {
        log.debug("添加文件夹[{}], 使用默认目的路径", sourceDirPath);
        ConvertManager.addMissions(sourceDirPath, false);
    }

    /**
     * 添加文件夹任务, 使用自定义目的路径
     *
     * @param sourceDirPath 源文件夹路径
     * @param targetDirPath 目的路径
     */
    public void addMissions(String sourceDirPath, String targetDirPath) {
        log.debug("添加文件夹[{}], 使用自定义目的路径[{}]", sourceDirPath, targetDirPath);
        ConvertManager.addMissions(sourceDirPath, targetDirPath, false);
    }

    /**
     * 根据任务id获取任务
     *
     * @param id 任务id
     * @return id对应任务
     */
    public ConvertMission getMission(Integer id) {
        log.debug("获取任务[id={}]", id);
        return ConvertManager.getMission(id);
    }

    /**
     * 根据id取消任务
     *
     * @param id 任务id
     */
    public void cancelMissionById(Integer id) {
        ConvertMission mission = ConvertManager.getMission(id);
        log.debug("正在取消任务[{}]", mission.getConvertInfo().getSourceFilePath());
        mission.cancelMission();
    }

    /**
     * 取消所有已提交任务
     */
    public void cancelAllMissions() {
        log.debug("取消所有任务");
        ConvertManager.cancelAllMissions();
        log.debug("成功取消所有任务");
    }

    /**
     * 获取所有任务的集合, 若是已完成任务, value就为-1, 否则为任务id, 可用于取消任务
     *
     * @return 任务集合
     */
    public HashMap<ConvertInfo, Integer> getAllConvertInfo() {
        log.debug("获取所有任务map集合");
        return ConvertManager.getAllConvertInfo();
    }

    /**
     * 获取所有任务的集合, 若是已完成任务, value就为-1, 否则为任务id, 可用于取消任务(返回json格式字符串)
     *
     * @return 任务集合的json格式字符串
     */
    public String getAllConvertInfoOfJson() {
        log.debug("获取所有任务json格式字符串");
        return ConvertManager.getAllConvertInfoOfJson();
    }

    /**
     * 获取所有自定义配置
     *
     * @return 自定义配置
     */
    public String getConfig() {
        log.debug("获取自定义配置");
        StringBuilder configs = new StringBuilder();
        // 获取自定义配置
        CustomizeConfig config = CustomizeConfig.instance();
        configs.append(config.isAllowWithoutLicense()).append(SEPARATOR)
                .append(config.getRedisFileKey()).append(SEPARATOR)
                .append(config.getRedisDirKey()).append(SEPARATOR)
                .append(config.getRedisInfoKey()).append(SEPARATOR)
                .append(config.getTargetDirPath()).append(SEPARATOR)
                .append(config.getMaxRetries()).append(SEPARATOR)
                .append(config.getMissionTimeout()).append(SEPARATOR);
        // 获取线程池配置
        ThreadPoolConfig poolConfig = ThreadPoolConfig.instance();
        configs.append(poolConfig.getCorePoolSize()).append(SEPARATOR)
                .append(poolConfig.getMaxPoolSize()).append(SEPARATOR)
                .append(poolConfig.getQueueCapacity()).append(SEPARATOR)
                .append(poolConfig.getPrefix()).append(SEPARATOR)
                .append(poolConfig.getKeepAliveSeconds());
        return configs.toString();
    }

    /**
     * 修改自定义配置
     *
     * @param configString 自定义配置
     */
    public void setConfig(String configString) {
        log.debug("修改自定义配置{}", configString);
        // 使用分隔符[,]分割配置字符串
        String[] configs = configString.split(SEPARATOR);
        // 修改自定义配置
        CustomizeConfig config = CustomizeConfig.instance();
        config.setAllowWithoutLicense("true".equals(configs[0]));
        config.setRedisFileKey(configs[1]);
        config.setRedisDirKey(configs[2]);
        config.setRedisInfoKey(configs[3]);
        config.setTargetDirPath(configs[4]);
        config.setMaxRetries(Integer.valueOf(configs[5]));
        config.setMissionTimeout(Integer.valueOf(configs[6]));
        // 修改线程池配置
        ThreadPoolTaskExecutor pool = ConvertManager.getThreadPoolTaskExecutor();
        pool.setCorePoolSize(Integer.parseInt(configs[7]));
        pool.setMaxPoolSize(Integer.parseInt(configs[8]));
        pool.setQueueCapacity(Integer.parseInt(configs[9]));
        pool.setThreadNamePrefix(configs[10]);
        pool.setKeepAliveSeconds(Integer.parseInt(configs[11]));
        ConvertManager.getThreadPoolTaskScheduler().setPoolSize(Integer.parseInt(configs[7]));
        // 写入自定义线程池配置
        ThreadPoolConfig poolConfig = ThreadPoolConfig.instance();
        poolConfig.setCorePoolSize(Integer.parseInt(configs[7]));
        poolConfig.setMaxPoolSize(Integer.parseInt(configs[8]));
        poolConfig.setQueueCapacity(Integer.parseInt(configs[9]));
        poolConfig.setPrefix(configs[10]);
        poolConfig.setKeepAliveSeconds(Integer.parseInt(configs[11]));
    }

    /**
     * 从Redis缓存中获取文件和文件夹
     *
     * @return json格式字符串
     */
    public String getWatchedFiles() {
        log.debug("获取Redis缓存中的文件和文件夹");
        Set<Object> files = RedisUtils.sGet(CustomizeConfig.instance().getRedisFileKey());
        Set<Object> dirs = RedisUtils.sGet(CustomizeConfig.instance().getRedisDirKey());
        List<Map<String, String>> list = new ArrayList<>();
        if (files != null) {
            for (Object file : files) {
                Map<String, String> map = new HashMap<>(4);
                map.put("path", (String) file);
                map.put("type", "file");
                list.add(map);
            }
        }
        if (dirs != null) {
            for (Object dir : dirs) {
                Map<String, String> map = new HashMap<>(4);
                map.put("path", (String) dir);
                map.put("type", "dir");
                list.add(map);
            }
        }
        return StringUtils.toJsonString(list);
    }
}
