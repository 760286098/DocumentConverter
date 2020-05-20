package com.converter.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.converter.config.CustomizeConfig;
import com.converter.config.ThreadPoolConfig;
import com.converter.constant.ConvertStatus;
import com.converter.converter.AbstractConverter;
import com.converter.mapper.ConvertInfoMapper;
import com.converter.pojo.ConvertInfo;
import com.converter.utils.FileUtils;
import com.converter.utils.RedisUtils;
import com.converter.utils.StringUtils;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.Weighers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
     * 标记上传文件
     */
    public static final String UPLOAD = "_UPLOAD_";
    /**
     * 存放已完成的转换信息(状态为FINISH、ERROR或CANCEL)
     */
    private static final List<ConvertInfo> FINISHED_INFO = Collections.synchronizedList(new LinkedList<>());
    /**
     * 任务执行线程池, 用于执行转换任务
     */
    private static ThreadPoolTaskExecutor threadPoolTaskExecutor;
    /**
     * 任务调度线程池, 用于控制任务流程
     */
    private static ThreadPoolTaskScheduler threadPoolTaskScheduler;
    /**
     * mapper, 用于读写mysql数据库
     */
    private static ConvertInfoMapper convertInfoMapper;
    /**
     * 递增的任务id
     */
    private static AtomicInteger id;
    /**
     * 存放所有准备运行或正在运行任务, 所有新创建的任务加入并等待调度
     */
    private static ConcurrentLinkedHashMap<Integer, ConvertMission> missions;
    /**
     * 存放任务执行线程池中所有任务的结果, 可用于取消任务
     */
    private static ConcurrentHashMap<Integer, Future<?>> futures;
    /**
     * 判断任务状态是否改变
     */
    private static boolean modify;
    /**
     * 记录当前线程池任务数
     */
    private static AtomicInteger threadCount;

    private ConvertManager() {
    }

    /**
     * 清理Redis缓存, 使用MySQL数据库中数据代替, 之后程序主要读取Redis缓存
     */
    private static void deepInit() {
        try {
            log.debug("开始加载mysql, 准备写入redis");
            // 文件路径key
            String fileKey = CustomizeConfig.instance().getRedisFileKey();
            // 文件夹路径key
            String dirKey = CustomizeConfig.instance().getRedisDirKey();
            // 删除redis缓存
            RedisUtils.del(fileKey, dirKey);
            // 从数据库中读取所有转换信息（只有状态为FINISH、CANCEL或ERROR的才写入数据库）
            List<ConvertInfo> convertInfos = convertInfoMapper.getAll();
            // 将数据库中数据添加到redis缓存中
            if (convertInfos.size() > 0) {
                RedisUtils.sSet(fileKey, convertInfos.parallelStream().map(ConvertInfo::getSourceFilePath).distinct().toArray());
            }
            // 写入FINISHED_INFO
            synchronized (FINISHED_INFO) {
                FINISHED_INFO.addAll(convertInfos);
            }
            log.debug("mysql加载成功, 已成功写入redis缓存");
            // 启动扫描
            startScan();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 设置定时任务, 每60秒扫描一次文件夹, 每5秒扫描一次任务列表
     */
    private static void startScan() {
        try {
            // 扫描redis缓存中所有源文件夹, 并加入到任务队列missions, 每60s扫描一次
            threadPoolTaskScheduler.getScheduledExecutor().scheduleWithFixedDelay(() -> {
                log.info("开始扫描redis中所有文件夹");
                Set<Object> dirs = RedisUtils.sGet(CustomizeConfig.instance().getRedisDirKey());
                if (dirs != null) {
                    for (Object dir : dirs) {
                        // 防止某个文件夹错误而影响其他文件夹
                        try {
                            addMissions((String) dir, true);
                        } catch (Exception e) {
                            log.error("文件夹[{}]扫描出错, 错误信息: {}", dir, e.getMessage());
                        }
                    }
                }
            }, 60, 60, TimeUnit.SECONDS);

            ThreadPoolExecutor threadPoolExecutor = threadPoolTaskExecutor.getThreadPoolExecutor();
            // 扫描任务列表, 如果线程池空闲就启动新任务, 每5秒扫描一次
            threadPoolTaskScheduler.getScheduledExecutor().scheduleWithFixedDelay(() -> {
                int activeCount = threadPoolExecutor.getActiveCount();
                int queueSize = threadPoolExecutor.getQueue().size();
                // 输出线程池状态
                log.debug("ActiveCount: {}, QueueSize: {}", activeCount, queueSize);
                // 用真实的线程池任务数代替threadCount
                int realCount = activeCount + queueSize;
                threadCount.lazySet(realCount);
                if (realCount < ThreadPoolConfig.getCapacity() && futures.size() < missions.size()) {
                    startMissions();
                }
            }, 5, 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("扫描出错{}", e.getMessage(), e);
        }
    }

    /**
     * 添加单个文件任务, 使用默认目的路径
     *
     * @param sourceFilePath 源文件路径
     */
    public static void addMission(final String sourceFilePath) {
        addMission(sourceFilePath, CustomizeConfig.instance().getTargetDirPath());
    }

    /**
     * 添加单个文件任务, 使用自定义目的路径
     *
     * @param sourceFilePath 源文件路径
     * @param targetDirPath  目的路径
     */
    public static void addMission(final String sourceFilePath,
                                  final String targetDirPath) {
        if (FileUtils.testSourceFile(sourceFilePath)) {
            String targetDirPathWithSeparator = FileUtils.dealWithDir(targetDirPath);
            String fileName = sourceFilePath.substring(sourceFilePath.lastIndexOf(File.separatorChar) + 1);
            int index;
            if ((index = fileName.indexOf(UPLOAD)) != -1) {
                fileName = fileName.substring(index + UPLOAD.length());
            }
            String targetFilePath = targetDirPathWithSeparator + fileName + ".pdf";

            Integer missionId = id.incrementAndGet();
            ConvertMission mission = new ConvertMission(missionId, new ConvertInfo(sourceFilePath, targetFilePath));
            missions.put(missionId, mission);
            log.info("文件添加成功[{}]", sourceFilePath);
        }
    }

    /**
     * 添加文件夹任务, 使用默认目的路径
     *
     * @param sourceDirPath 源文件夹路径
     * @param isScan        true代表是扫描, 不用检测目录是否在redis缓存中
     */
    public static void addMissions(final String sourceDirPath,
                                   final boolean isScan) {
        addMissions(sourceDirPath, CustomizeConfig.instance().getTargetDirPath(), isScan);
    }

    /**
     * 添加文件夹任务, 使用自定义目的路径
     *
     * @param sourceDirPath 源文件夹路径
     * @param targetDirPath 目的路径
     * @param isScan        true代表是扫描, 不用检测目录是否在redis缓存中
     */
    public static void addMissions(final String sourceDirPath,
                                   final String targetDirPath,
                                   final boolean isScan) {
        if (FileUtils.testSourceDir(sourceDirPath, isScan)) {
            String targetDirPathWithSeparator = FileUtils.dealWithDir(targetDirPath);
            String[] filePaths = FileUtils.listDir(sourceDirPath);
            for (String filePath : filePaths) {
                // 使用线程池并行添加文件夹内文件
                threadPoolTaskScheduler.execute(() -> {
                    // 防止文件夹内某个文件错误而影响文件夹内其他文件
                    try {
                        addMission(filePath, targetDirPathWithSeparator);
                    } catch (Exception e) {
                        log.error("添加文件[{}]失败, 异常信息: {}", filePath, e.getMessage());
                    }
                });
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
    public static ConvertMission getMission(final Integer id) {
        return missions.get(id);
    }

    /**
     * 开始任务列表中所有任务
     */
    private static void startMissions() {
        Collection<ConvertMission> convertMissions = missions.ascendingMap().values();
        for (ConvertMission mission : convertMissions) {
            // 限制任务数
            if (threadCount.get() >= ThreadPoolConfig.getCapacity()) {
                log.info("队列已满, 等待下一轮扫描");
                return;
            }
            // 只有状态为WAIT_OUTSIDE或RETRY的任务才能执行
            ConvertStatus status = mission.getConvertInfo().getStatus();
            if (status == ConvertStatus.WAIT_OUTSIDE || status == ConvertStatus.RETRY) {
                // threadCount+1
                threadCount.incrementAndGet();
                // 使用线程池启动任务
                threadPoolTaskScheduler.execute(() -> {
                    try {
                        mission.startMission();
                    } catch (Exception e) {
                        log.error("任务执行出错", e);
                    } finally {
                        // threadCount-1
                        threadCount.decrementAndGet();
                    }
                });
            }
        }
    }

    /**
     * 获取所有任务的集合, 若是已完成任务, value就为-1, 否则为任务id, 可用于取消任务(返回json格式字符串)
     *
     * @param cache true代表允许缓存, false代表禁止缓存
     * @return 任务集合的json格式字符串
     */
    public static String getAllConvertInfoOfJson(final boolean cache) {
        // 如果所有任务状态没有改变, 直接返回Not Modified, 注意最后一批任务状态的刷新
        if (cache && !modify) {
            return "[\"Not Modified\"]";
        } else {
            modify = false;
        }
        // 从missions中获取等待或正在运行的任务
        Set<Map.Entry<Integer, ConvertMission>> entries = missions.ascendingMap().entrySet();
        // 返回所有任务
        JSONArray result = new JSONArray();
        // 未完成任务
        for (Map.Entry<Integer, ConvertMission> entry : entries) {
            JSONObject json = (JSONObject) JSONObject.toJSON(entry.getValue().getConvertInfo());
            json.put("id", entry.getKey());
            result.add(json);
        }
        // SynchronizedList对iterator没有上锁, 可能会报错ConcurrentModificationException
        synchronized (FINISHED_INFO) {
            // 已完成任务
            for (ConvertInfo info : FINISHED_INFO) {
                JSONObject json = (JSONObject) JSONObject.toJSON(info);
                json.put("id", -1);
                result.add(json);
            }
        }
        return StringUtils.toJsonString(result);
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
    public static ConcurrentLinkedHashMap<Integer, ConvertMission> getMissions() {
        return missions;
    }

    /**
     * Getter
     *
     * @return futures
     */
    public static ConcurrentHashMap<Integer, Future<?>> getFutures() {
        return futures;
    }

    /**
     * Getter
     *
     * @return finishedInfo
     */
    public static List<ConvertInfo> getFinishedInfo() {
        return FINISHED_INFO;
    }

    /**
     * 设置修改状态
     */
    public static void modify() {
        modify = true;
    }

    /**
     * 初始化, 自动注入需要的Bean
     *
     * @param threadPoolTaskExecutor  任务转换线程池
     * @param threadPoolTaskScheduler 任务调度线程池
     * @param convertInfoMapper       mapper, 用于读写mysql数据库
     */
    @Autowired
    private void init(final @Qualifier("threadPoolTaskExecutor") ThreadPoolTaskExecutor threadPoolTaskExecutor,
                      final @Qualifier("threadPoolTaskScheduler") ThreadPoolTaskScheduler threadPoolTaskScheduler,
                      final @Qualifier("convertInfoMapper") ConvertInfoMapper convertInfoMapper) {
        log.debug("开始初始化ConvertManager");
        // 初始化AbstractConverter, 载入授权文件
        AbstractConverter.init();
        // 获取合适capacity, 尽量避免map扩容（其中futures最大值为max-pool-size + queue-capacity, missions可能超过这个值）
        int capacity = (int) ((ThreadPoolConfig.getCapacity()) / 0.75) + 1;
        ConvertManager.threadPoolTaskExecutor = threadPoolTaskExecutor;
        ConvertManager.threadPoolTaskScheduler = threadPoolTaskScheduler;
        ConvertManager.convertInfoMapper = convertInfoMapper;
        id = new AtomicInteger(0);
        missions = new ConcurrentLinkedHashMap
                .Builder<Integer, ConvertMission>()
                .weigher(Weighers.singleton())
                .initialCapacity(capacity)
                .maximumWeightedCapacity(Long.MAX_VALUE)
                .build();
        futures = new ConcurrentHashMap<>(capacity);
        threadCount = new AtomicInteger(0);
        modify = false;
        log.debug("成功初始化ConvertManager");
        // 进一步初始化, 主要是处理缓存数据
        deepInit();
    }
}
