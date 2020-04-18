package com.converter.core;

import com.converter.config.CustomizeConfig;
import com.converter.config.ThreadPoolConfig;
import com.converter.constant.ConvertStatus;
import com.converter.converter.AbstractConverter;
import com.converter.mapper.ConvertInfoMapper;
import com.converter.pojo.ConvertInfo;
import com.converter.utils.FileUtils;
import com.converter.utils.RedisUtils;
import com.converter.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主要管理类
 * 调用addMission/addMissions来添加文件/文件夹, 调用getAllConvertInfo来监控所有任务
 *
 * @author Evan
 */
@Slf4j
@Component
@EnableScheduling
@DependsOn({"redisUtils", "customizeConfig"})
public class ConvertManager {
    /**
     * 任务执行线程池, 用于执行转换任务
     */
    private static ThreadPoolTaskExecutor threadPoolTaskExecutor;
    /**
     * 任务调度线程池, 用于给转换任务设置timeout
     */
    private static ThreadPoolTaskScheduler threadPoolTaskScheduler;
    /**
     * mapper, 用于读写mysql数据库
     */
    private static ConvertInfoMapper convertInfoMapper;
    /**
     * 递增的任务id
     */
    private static Integer id;
    /**
     * 存放所有准备运行或正在运行任务, 所有新创建的任务加入并等待调度
     */
    private static ConcurrentHashMap<Integer, ConvertMission> missions;
    /**
     * 存放任务执行线程池中所有任务的结果, 可用于取消任务
     */
    private static ConcurrentHashMap<Integer, ListenableFuture<?>> futures;

    private ConvertManager() {
    }

    /**
     * 清理redis文件路径缓存, 使用MySQL数据库中数据代替, 之后程序主要读取Redis缓存
     */
    private static void init() {
        try {
            log.debug("开始加载mysql, 准备写入redis");
            // 文件路径key
            String fileKey = CustomizeConfig.instance().getRedisFileKey();
            // 删除redis缓存
            RedisUtils.del(fileKey);
            // 从数据库中读取所有转换信息（只有状态为FINISH、CANCEL或ERROR的才写入数据库）
            List<ConvertInfo> convertInfos = ConvertManager.convertInfoMapper.getAll();
            // 将数据库中数据添加到redis缓存中
            if (convertInfos.size() > 0) {
                Object[] filePaths = convertInfos.parallelStream().map(ConvertInfo::getSourceFilePath).distinct().toArray();
                RedisUtils.sSet(fileKey, filePaths);
            }
            log.debug("mysql加载成功, 已成功写入redis缓存");
            // 开始扫描源文件夹
            scanDir();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 扫描redis缓存中所有源文件夹, 并加入到任务队列missions, 每60s扫描一次
     */
    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 60 * 1000)
    private static void scanDir() {
        log.info("开始扫描redis中所有文件夹");
        Set<Object> dirs = RedisUtils.sGet(CustomizeConfig.instance().getRedisDirKey());
        if (dirs != null) {
            for (Object dir : dirs) {
                // 防止某个文件夹错误而影响其他文件夹
                try {
                    addMissions((String) dir, true);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
            // 扫描完成后开始任务
            startMissions();
        }
    }

    /**
     * 添加单个文件任务, 使用默认目的路径
     *
     * @param sourceFilePath 源文件路径
     */
    public static void addMission(String sourceFilePath) {
        addMission(sourceFilePath, CustomizeConfig.instance().getTargetDirPath());
    }

    /**
     * 添加单个文件任务, 使用自定义目的路径
     *
     * @param sourceFilePath 源文件路径
     * @param targetDirPath  目的路径
     */
    public static void addMission(String sourceFilePath, String targetDirPath) {
        if (FileUtils.testSourceFile(sourceFilePath)) {
            String fileSize = FileUtils.getReadableByteCountBin(new File(sourceFilePath).length());
            targetDirPath = FileUtils.dealWithDir(targetDirPath);
            String fileName = sourceFilePath.substring(sourceFilePath.lastIndexOf(File.separatorChar) + 1);
//            int index = fileName.lastIndexOf(".");
//            if (index == -1) {
//                throw new FileException.FileTypeException("file name[" + sourceFilePath + "]");
//            }
//            String fileNameWithoutExtension = fileName.substring(0, index);
//            String fileExtension = sourceFilePath.substring(sourceFilePath.lastIndexOf(".") + 1);
//            String targetFilePath = targetDirPath + fileNameWithoutExtension + "[" + fileExtension + "].pdf";
            String targetFilePath = targetDirPath + fileName + ".pdf";

            ConvertInfo convertInfo = new ConvertInfo(System.currentTimeMillis(), sourceFilePath, targetFilePath, fileSize);
            ConvertMission mission = new ConvertMission(++id, convertInfo);
            missions.put(id, mission);
            log.info("文件添加成功[{}]", sourceFilePath);
        }
    }

    /**
     * 添加文件夹任务, 使用默认目的路径
     *
     * @param sourceDirPath 源文件夹路径
     * @param isScan        true代表是扫描, 不用检测目录是否在redis缓存中
     */
    public static void addMissions(String sourceDirPath, boolean isScan) {
        addMissions(sourceDirPath, CustomizeConfig.instance().getTargetDirPath(), isScan);
    }

    /**
     * 添加文件夹任务, 使用自定义目的路径
     *
     * @param sourceDirPath 源文件夹路径
     * @param targetDirPath 目的路径
     * @param isScan        true代表是扫描, 不用检测目录是否在redis缓存中
     */
    public static void addMissions(String sourceDirPath, String targetDirPath, boolean isScan) {
        if (FileUtils.testSourceDir(sourceDirPath, isScan)) {
            targetDirPath = FileUtils.dealWithDir(targetDirPath);
            String[] filePaths = FileUtils.listDir(sourceDirPath);
            for (String filePath : filePaths) {
                // 防止文件夹内某个文件错误而影响文件夹内其他文件
                try {
                    addMission(filePath, targetDirPath);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
            // 如果不是扫描任务, 则手动开始任务
            if (!isScan) {
                log.info("目录添加成功[{}]", sourceDirPath);
                startMissions();
            }
        }
    }

    /**
     * 根据任务id获取任务
     *
     * @param id 任务id
     * @return id对应任务
     */
    public static ConvertMission getMission(Integer id) {
        return missions.get(id);
    }

    /**
     * 开始任务列表中所有任务
     */
    private static void startMissions() {
        // 线程池最大容量为: 等待队列长度+最大线程数
        int capacity = ThreadPoolConfig.instance().getQueueCapacity() + ThreadPoolConfig.instance().getMaxPoolSize();
        for (ConvertMission mission : missions.values()) {
            // 限制任务数
            if (futures.size() >= capacity) {
                log.info("队列已满, 等待下一轮扫描");
                return;
            }
            // 只有状态为WAIT_OUTSIDE的任务才能执行
            ConvertStatus status = mission.getConvertInfo().getStatus();
            if (status == ConvertStatus.WAIT_OUTSIDE) {
                mission.startMission();
            }
        }
    }

    /**
     * 取消所有已提交任务
     */
    public static void cancelAllMissions() {
        for (Map.Entry<Integer, ListenableFuture<?>> next : futures.entrySet()) {
            missions.get(next.getKey()).cancelMission();
        }
        futures.clear();
    }

    /**
     * 获取所有任务的集合, 若是已完成任务, value就为-1, 否则为任务id, 可用于取消任务
     *
     * @return 任务集合
     */
    public static HashMap<ConvertInfo, Integer> getAllConvertInfo() {
        // 从redis缓存中读取已经完成的任务
        Set<Object> objects = RedisUtils.sGet(CustomizeConfig.instance().getRedisInfoKey());
        // 从missions中获取等待或正在运行的任务
        Set<Map.Entry<Integer, ConvertMission>> entries = ConvertManager.missions.entrySet();
        // 返回所有任务
        HashMap<ConvertInfo, Integer> result;
        if (objects != null) {
            result = new HashMap<>((int) ((objects.size() + entries.size()) / 0.75) + 1);
            for (Object object : objects) {
                result.put(StringUtils.parseJsonString((String) object, ConvertInfo.class), -1);
            }
        } else {
            result = new HashMap<>((int) (entries.size() / 0.75) + 1);
        }
        for (Map.Entry<Integer, ConvertMission> entry : entries) {
            result.put(entry.getValue().getConvertInfo(), entry.getKey());
        }
        return result;
    }

    /**
     * 获取所有任务的集合, 若是已完成任务, value就为-1, 否则为任务id, 可用于取消任务(返回json格式字符串)
     *
     * @return 任务集合的json格式字符串
     */
    public static String getAllConvertInfoOfJson() {
        // 从redis缓存中读取已经完成的任务
        Set<Object> objects = RedisUtils.sGet(CustomizeConfig.instance().getRedisInfoKey());
        // 从missions中获取等待或正在运行的任务
        Set<Map.Entry<Integer, ConvertMission>> entries = ConvertManager.missions.entrySet();
        // 返回所有任务
        StringBuilder result = new StringBuilder("[");
        if (objects != null) {
            for (Object object : objects) {
                result.append(addIdToJson((String) object, -1)).append(",");
            }
        }
        for (Map.Entry<Integer, ConvertMission> entry : entries) {
            result.append(addIdToJson(entry.getValue().getConvertInfo().toString(), entry.getKey())).append(",");
        }
        return result.deleteCharAt(result.length() - 1).append("]").toString();
    }

    /**
     * 处理json格式字符串, 添加id,no到字符串中
     *
     * @param json 待处理的json
     * @param id   任务id
     * @return 处理后的json
     */
    private static String addIdToJson(String json, Integer id) {
        return json.substring(0, json.length() - 1) + ",\"id\":" + id + "}";
    }

    /**
     * Getter
     *
     * @return threadPoolTaskExecutor
     */
    public static ThreadPoolTaskExecutor getThreadPoolTaskExecutor() {
        return threadPoolTaskExecutor;
    }

    /**
     * Getter
     *
     * @return threadPoolTaskScheduler
     */
    public static ThreadPoolTaskScheduler getThreadPoolTaskScheduler() {
        return threadPoolTaskScheduler;
    }

    /**
     * Getter
     *
     * @return convertInfoMapper
     */
    public static ConvertInfoMapper getConvertInfoMapper() {
        return convertInfoMapper;
    }

    /**
     * Getter
     *
     * @return missions
     */
    public static ConcurrentHashMap<Integer, ConvertMission> getMissions() {
        return missions;
    }

    /**
     * Getter
     *
     * @return futures
     */
    public static ConcurrentHashMap<Integer, ListenableFuture<?>> getFutures() {
        return futures;
    }

    /**
     * 初始化, 自动注入需要的Bean
     *
     * @param threadPoolTaskExecutor  任务转换线程池
     * @param threadPoolTaskScheduler 任务调度线程池（主要是设置timeout）
     * @param convertInfoMapper       mapper, 用于读写mysql数据库
     */
    @Autowired
    private void init(ThreadPoolTaskExecutor threadPoolTaskExecutor, ThreadPoolTaskScheduler threadPoolTaskScheduler, ConvertInfoMapper convertInfoMapper) {
        log.debug("开始初始化ConvertManager");
        // 初始化AbstractConverter, 载入授权文件
        AbstractConverter.init();
        // 获取合适capacity, 尽量避免map扩容（其中futures最大值为max-pool-size + queue-capacity, missions可能超过这个值）
        int capacity = (int) ((ThreadPoolConfig.instance().getQueueCapacity() + ThreadPoolConfig.instance().getMaxPoolSize()) / 0.75) + 1;
        ConvertManager.threadPoolTaskExecutor = threadPoolTaskExecutor;
        ConvertManager.threadPoolTaskScheduler = threadPoolTaskScheduler;
        ConvertManager.convertInfoMapper = convertInfoMapper;
        id = 0;
        missions = new ConcurrentHashMap<>(capacity);
        futures = new ConcurrentHashMap<>(capacity);
        log.debug("成功初始化ConvertManager");
        // 进一步初始化, 主要是处理缓存数据
        init();
    }
}
