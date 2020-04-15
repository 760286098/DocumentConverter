package com.converter.core;

import com.converter.config.CustomizeConfig;
import com.converter.constant.ConvertStatus;
import com.converter.exception.ConvertException;
import com.converter.exception.FileException;
import com.converter.pojo.ConvertInfo;
import com.converter.utils.FileUtils;
import com.converter.utils.RedisUtils;
import com.converter.utils.TimeUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 任务类, 负责具体文档转换
 *
 * @author Evan
 */
@Slf4j
@Getter
public class ConvertMission {
    /**
     * 错误分隔符
     */
    private final static String SEPARATOR = "\r\n";
    /**
     * 任务id
     */
    private Integer missionId;
    /**
     * 任务信息
     */
    private ConvertInfo convertInfo;
    /**
     * 任务对应的runnable对象
     */
    private ConvertRunnable runnable;
    /**
     * 任务执行线程池, 用于执行任务
     */
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    /**
     * 任务调度线程池, 用于给任务设置timeout
     */
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;
    /**
     * 任务列表, 当任务成功或失败时从列表中删除（此时存入数据库）
     */
    private ConcurrentHashMap<Integer, ConvertMission> missions;
    /**
     * 任务线所属任务组
     */
    private ConcurrentHashMap<Integer, ListenableFuture<?>> futures;
    /**
     * 用于取消超时
     */
    private ScheduledFuture<?> timer;

    public ConvertMission(Integer missionId, ConvertInfo convertInfo) {
        this.missionId = missionId;
        this.convertInfo = convertInfo;
        this.runnable = new ConvertRunnable(this);
        this.threadPoolTaskExecutor = ConvertManager.getThreadPoolTaskExecutor();
        this.threadPoolTaskScheduler = ConvertManager.getThreadPoolTaskScheduler();
        this.missions = ConvertManager.getMissions();
        this.futures = ConvertManager.getFutures();
    }

    /**
     * 设置任务状态
     *
     * @param status 新状态
     */
    public void setStatus(ConvertStatus status) {
        convertInfo.setStatus(status);
    }

    /**
     * 任务重试
     *
     * @return true代表重试成功（只是修改状态, 并没有实际运行）
     */
    private boolean retry() {
        int retry = convertInfo.getRetry();
        if (retry < CustomizeConfig.instance().getMaxRetries()) {
            futures.remove(missionId);
            convertInfo.setStatus(ConvertStatus.RETRY);
            convertInfo.setRetry(retry + 1);
            log.debug("任务重试[{}]", convertInfo.getSourceFilePath());
            return true;
        }
        return false;
    }

    /**
     * 启动计时器
     */
    public void startTimer() {
        String sourceFilePath = convertInfo.getSourceFilePath();
        log.info("启动计时器[{}]", sourceFilePath);
        this.timer = threadPoolTaskScheduler.getScheduledExecutor().schedule(() -> {
            ListenableFuture<?> future = futures.get(missionId);
            if (future != null && !future.isDone()) {
                future.cancel(true);
            } else {
                log.error("timer未知错误[id={}][{}]", missionId, sourceFilePath);
            }
        }, CustomizeConfig.instance().getMissionTimeout(), TimeUnit.SECONDS);
    }

    /**
     * 取消计时器
     */
    private void stopTimer() {
        log.info("结束计时器{}", convertInfo.getSourceFilePath());
        if (timer != null && !timer.isCancelled()) {
            timer.cancel(true);
        }
    }

    /**
     * 启动任务
     */
    public void startMission() {
        ListenableFuture<?> future = threadPoolTaskExecutor.submitListenable(runnable);
        if (convertInfo.getStatus() != ConvertStatus.RETRY) {
            convertInfo.setStatus(ConvertStatus.WAIT_IN_POOL);
        }
        String sourceFilePath = convertInfo.getSourceFilePath();
        future.addCallback(
                // 任务成功的回调函数
                data -> {
                    stopTimer();
                    convertInfo.setStatus(ConvertStatus.FINISH);
                    convertInfo.setEndTime(System.currentTimeMillis());
                    missions.remove(missionId);
                    futures.remove(missionId);
                    // 写入数据库
                    save();
                    log.info("任务转换完成, 耗时:{}[{}]", TimeUtils.getReadableTime(convertInfo.getEndTime() - convertInfo.getStartTime()), sourceFilePath);
                },
                // 任务失败的回调函数
                ex -> {
                    stopTimer();
                    // 记录之前任务状态
                    ConvertStatus status = convertInfo.getStatus();
                    // 修改任务状态
                    convertInfo.setStatus(ConvertStatus.ERROR);
                    // 删除临时文件
                    FileUtils.deleteFile(convertInfo.getTargetFilePath());
                    // 错误信息
                    String error = ex.getMessage();
                    if (ex instanceof CancellationException) {
                        if (status == ConvertStatus.CANCEL) {
                            convertInfo.setStatus(status);
                            error = "任务取消";
                        } else {
                            error = "任务超时";
                        }
                    }
                    Integer retry = convertInfo.getRetry();
                    // 写入错误信息
                    if (retry == 0) {
                        convertInfo.setExceptions(error);
                    } else {
                        String exceptions = convertInfo.getExceptions();
                        // 如果是新型错误才添加
                        if (!Arrays.asList(exceptions.split(SEPARATOR)).contains(error)) {
                            convertInfo.setExceptions(exceptions + SEPARATOR + error);
                        }
                    }
                    if (ex instanceof CancellationException) {
                        // 如果是取消任务, 则直接结束
                        if (status == ConvertStatus.CANCEL) {
                            log.info("取消任务成功[{}]", sourceFilePath);
                        } else if (retry()) {
                            log.error("任务超时, 进行重试, 重试次数:{}[{}]", retry + 1, sourceFilePath);
                            startMission();
                            return;
                        } else {
                            log.error("任务超时, 重试超过最大次数, 停止执行[{}]", sourceFilePath);
                        }
                    } else if (ex instanceof FileException.FileTypeException) {
                        log.error("不支持的文件类型:[{}]", sourceFilePath);
                    } else if (ex instanceof ConvertException.WordConvertException ||
                            ex instanceof ConvertException.CellConvertException) {
                        if (retry()) {
                            log.error("任务转换出错, 进行重试, 重试次数:{}, 错误信息:[{}][{}]", retry + 1, ex.getMessage(), sourceFilePath);
                            startMission();
                            return;
                        } else {
                            log.error("任务转换出错, 重试超过最大次数, 停止执行, 错误信息:[{}][{}]", ex.getMessage(), sourceFilePath);
                        }
                    } else {
                        ex.printStackTrace();
                        log.error("任务出现未知错误, 异常信息:[{}][{}]", error, sourceFilePath);
                    }
                    missions.remove(missionId);
                    futures.remove(missionId);
                    // 写入数据库
                    save();
                }
        );
        futures.put(missionId, future);
    }

    /**
     * 取消任务
     */
    public void cancelMission() {
        convertInfo.setStatus(ConvertStatus.CANCEL);
        ListenableFuture<?> future = futures.get(missionId);
        // 如果是正在运行或在线程池等待的任务, 利用future.cancel触发异常, 从而取消任务; 否则手动取消
        if (future != null && !future.isDone()) {
            future.cancel(true);
        } else {
            missions.remove(missionId);
            futures.remove(missionId);
            save();
        }
    }

    /**
     * 任务结束时写入数据库
     */
    private void save() {
        // 写入redis
        RedisUtils.sSet(CustomizeConfig.instance().getRedisInfoKey(), convertInfo.toString());
        // 写入数据库
        ConvertManager.getConvertInfoMapper().insert(convertInfo);
        log.debug("写入数据库成功{}", convertInfo);
    }

    @Override
    public String toString() {
        return String.format("[id=%s]" +
                        "%s",
                missionId,
                convertInfo);
    }
}
