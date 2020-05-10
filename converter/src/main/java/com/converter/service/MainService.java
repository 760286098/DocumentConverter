package com.converter.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.converter.config.CustomizeConfig;
import com.converter.config.ThreadPoolConfig;
import com.converter.core.ConvertManager;
import com.converter.core.ConvertMission;
import com.converter.utils.RedisUtils;
import com.converter.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
     * 添加单个文件任务, 使用默认目的路径
     *
     * @param sourceFilePath 源文件路径
     */
    public void addMission(final String sourceFilePath) {
        log.debug("添加文件[{}], 使用默认目的路径", sourceFilePath);
        ConvertManager.addMission(sourceFilePath);
    }

    /**
     * 添加单个文件任务, 使用自定义目的路径
     *
     * @param sourceFilePath 源文件路径
     * @param targetDirPath  目的路径
     */
    public void addMission(final String sourceFilePath,
                           final String targetDirPath) {
        log.debug("添加文件[{}], 使用自定义目的路径[{}]", sourceFilePath, targetDirPath);
        ConvertManager.addMission(sourceFilePath, targetDirPath);
    }

    /**
     * 添加文件夹任务, 使用默认目的路径
     *
     * @param sourceDirPath 源文件夹路径
     */
    public void addMissions(final String sourceDirPath) {
        log.debug("添加文件夹[{}], 使用默认目的路径", sourceDirPath);
        ConvertManager.addMissions(sourceDirPath, false);
    }

    /**
     * 添加文件夹任务, 使用自定义目的路径
     *
     * @param sourceDirPath 源文件夹路径
     * @param targetDirPath 目的路径
     */
    public void addMissions(final String sourceDirPath,
                            final String targetDirPath) {
        log.debug("添加文件夹[{}], 使用自定义目的路径[{}]", sourceDirPath, targetDirPath);
        ConvertManager.addMissions(sourceDirPath, targetDirPath, false);
    }

    /**
     * 根据id取消任务
     *
     * @param id 任务id
     */
    public void cancelMissionById(final Integer id) {
        ConvertMission mission = ConvertManager.getMission(id);
        if (mission == null) {
            throw new RuntimeException("任务已完成");
        }
        log.debug("正在取消任务[{}]", mission.getConvertInfo().getSourceFilePath());
        mission.cancelMission();
    }

    /**
     * 获取所有任务的集合, 若是已完成任务, value就为-1, 否则为任务id, 可用于取消任务(返回json格式字符串)
     *
     * @param cache true代表允许缓存, false代表禁止缓存
     * @return 任务集合的json格式字符串
     */
    public String getAllConvertInfoOfJson(final boolean cache) {
        log.debug("获取所有任务json格式字符串");
        return ConvertManager.getAllConvertInfoOfJson(cache);
    }

    /**
     * 获取所有自定义配置
     *
     * @return 自定义配置
     */
    public Map<String, String> getConfig() {
        log.debug("获取自定义配置");
        Map<String, String> map = new HashMap<>(32);

        // 获取自定义配置
        CustomizeConfig config = CustomizeConfig.instance();
        map.put("license", String.valueOf(config.isAllowWithoutLicense()));
        map.put("fileKey", config.getRedisFileKey());
        map.put("dirKey", config.getRedisDirKey());
        map.put("target", new File(config.getTargetDirPath()).getAbsolutePath());
        map.put("upload", new File(config.getUploadPath()).getAbsolutePath());
        map.put("maxRetry", String.valueOf(config.getMaxRetries()));
        map.put("timeout", String.valueOf(config.getMissionTimeout()));
        map.put("enableSlides", String.valueOf(config.isEnableSlides()));

        // 获取线程池配置
        ThreadPoolTaskExecutor pool = ConvertManager.getThreadPoolTaskExecutor();
        map.put("corePool", String.valueOf(pool.getCorePoolSize()));
        map.put("maxPool", String.valueOf(pool.getMaxPoolSize()));
        map.put("queueCapacity", String.valueOf(ThreadPoolConfig.instance().getQueueCapacity()));
        map.put("prefix", pool.getThreadNamePrefix());
        map.put("alive", String.valueOf(pool.getKeepAliveSeconds()));

        return map;
    }

    /**
     * 修改自定义配置
     *
     * @param map 配置信息
     */
    public void setConfig(final Map<String, String> map) {
        log.debug("修改自定义配置{}", map.toString());
        // 修改自定义配置
        CustomizeConfig config = CustomizeConfig.instance();
        config.setTargetDirPath(map.get("target"));
        config.setUploadPath(map.get("upload"));
        config.setMaxRetries(Integer.valueOf(map.get("maxRetry")));
        config.setMissionTimeout(Integer.valueOf(map.get("timeout")));
        config.setEnableSlides("true".equals(map.get("enableSlides")));
        // 修改线程池配置
        ThreadPoolTaskExecutor pool = ConvertManager.getThreadPoolTaskExecutor();
        pool.setCorePoolSize(Integer.parseInt(map.get("corePool")));
        pool.setMaxPoolSize(Integer.parseInt(map.get("maxPool")));
        pool.setThreadNamePrefix(map.get("prefix"));
        pool.setKeepAliveSeconds(Integer.parseInt(map.get("alive")));
        ConvertManager.getThreadPoolTaskScheduler()
                .setPoolSize(Integer.parseInt(map.get("maxPool"))
                        + Integer.parseInt(map.get("queueCapacity")) + 5);
        // 修改线程池Config中配置
        ThreadPoolConfig poolConfig = ThreadPoolConfig.instance();
        poolConfig.setCorePoolSize(Integer.parseInt(map.get("corePool")));
        poolConfig.setMaxPoolSize(Integer.parseInt(map.get("maxPool")));
        poolConfig.setPrefix(map.get("prefix"));
        poolConfig.setKeepAliveSeconds(Integer.parseInt(map.get("alive")));
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
        JSONArray result = new JSONArray();
        if (dirs != null) {
            for (Object dir : dirs) {
                JSONObject item = new JSONObject();
                item.put("path", dir);
                item.put("type", "dir");
                result.add(item);
            }
        }
        if (files != null) {
            for (Object file : files) {
                JSONObject item = new JSONObject();
                item.put("path", file);
                item.put("type", "file");
                result.add(item);
            }
        }
        return StringUtils.toJsonString(result);
    }

    /**
     * 获取线程池信息
     *
     * @return 线程池信息
     */
    public String getThreadsInfo() {
        JSONArray result = new JSONArray();
        // 转换任务线程组
        ThreadGroup converter = ConvertManager.getThreadPoolTaskExecutor().getThreadGroup();
        dealWithThreadGroup(result, converter, ThreadPoolConfig.instance().getPrefix());
        // 流程控制线程组
        ThreadGroup scheduler = ConvertManager.getThreadPoolTaskScheduler().getThreadGroup();
        dealWithThreadGroup(result, scheduler, "scheduler");
        // json格式字符串
        return StringUtils.toJsonString(result);
    }

    /**
     * 将threadGroup中所有线程信息加入result
     *
     * @param result      结果
     * @param threadGroup group
     * @param prefix      线程前缀
     */
    private void dealWithThreadGroup(final JSONArray result,
                                     final ThreadGroup threadGroup,
                                     final String prefix) {
        if (threadGroup != null) {
            int count = threadGroup.activeCount();
            Thread[] threads = new Thread[count];
            threadGroup.enumerate(threads);

            for (int i = 0; i < count; i++) {
                String name = threads[i].getName();
                if (name.startsWith(prefix)) {
                    JSONObject item = new JSONObject();
                    StackTraceElement[] stackTrace = threads[i].getStackTrace();
                    item.put("id", threads[i].getId());
                    item.put("name", name);
                    item.put("state", threads[i].getState());
                    item.put("stack", stackTrace.length == 0 ? "" : stackTrace[0].toString());
                    result.add(item);
                }
            }
        }
    }
}
