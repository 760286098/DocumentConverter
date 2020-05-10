package com.converter.core;

import com.converter.config.CustomizeConfig;
import com.converter.constant.ConvertStatus;
import com.converter.exception.ConvertException;
import com.converter.exception.FileException;
import com.converter.pojo.ConvertInfo;
import com.converter.utils.FileUtils;
import com.converter.utils.StringUtils;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
    private static final String SEPARATOR = "\r\n";
    /**
     * 任务id
     */
    private Integer missionId;
    /**
     * 任务信息
     */
    private ConvertInfo convertInfo;
    /**
     * 任务对应的future
     */
    private Future<?> future;

    public ConvertMission(final Integer missionId,
                          final ConvertInfo convertInfo) {
        this.missionId = missionId;
        this.convertInfo = convertInfo;
        this.future = null;
    }

    /**
     * 启动任务
     */
    public void startMission() {
        ConvertRunnable runnable = new ConvertRunnable(this);
        future = ConvertManager.getThreadPoolTaskExecutor().submit(runnable);
        ConvertManager.getFutures().put(missionId, future);
        convertInfo.setStatus(ConvertStatus.WAIT_IN_POOL);
        try {
            // 阻塞等待
            future.get();
            // 任务成功
            success();
        } catch (Exception e) {
            // 向runnable发送中断请求
            runnable.interrupt();
            // 取消任务
            future.cancel(true);
            // 任务失败
            fail(e);
        } finally {
            // 从futures中移除
            ConvertManager.getFutures().remove(missionId);
        }
    }

    /**
     * 任务成功后操作
     */
    private void success() {
        convertInfo.setStatus(ConvertStatus.FINISH);
        convertInfo.setEndTime(System.currentTimeMillis());
        // 写入数据库
        save();
        log.info("任务转换完成, 耗时:{}秒[{}]", (convertInfo.getEndTime() - convertInfo.getStartTime()) / 1000.0, convertInfo.getSourceFilePath());
    }

    /**
     * 任务失败后操作
     *
     * @param e 异常
     */
    private void fail(final Exception e) {
        // 源文件路径
        String sourceFilePath = convertInfo.getSourceFilePath();
        // 记录任务状态
        ConvertStatus status = convertInfo.getStatus();
        // 错误信息
        String error = e.getMessage();
        // 是否重试
        boolean retry = false;
        // 是否删除文件
        boolean delete = true;
        // 判断错误类型
        if (e instanceof CancellationException) {
            if (status == ConvertStatus.CANCEL) {
                error = "任务取消";
                log.info("取消任务成功[{}]", sourceFilePath);
            } else if (status == ConvertStatus.RUN) {
                error = "任务超时";
                if (retry()) {
                    retry = true;
                    log.error("任务超时, 进行重试, 重试次数:{}[{}]", convertInfo.getRetry(), sourceFilePath);
                } else {
                    log.error("任务超时, 重试超过最大次数, 停止执行[{}]", sourceFilePath);
                }
            } else {
                // 任务队列已满, 等待下一轮扫描
                convertInfo.setStatus(ConvertStatus.WAIT_OUTSIDE);
                return;
            }
        } else if (e instanceof ExecutionException) {
            Throwable cause = e.getCause();
            error = cause == null ? "未知错误" : cause.getMessage();
            delete = false;
            if (cause instanceof FileException.FileTypeException) {
                log.error(error);
            } else if (cause instanceof ConvertException.WordConvertException
                    || cause instanceof ConvertException.CellConvertException
                    || cause instanceof ConvertException.SlideConvertException) {
                /*if (retry()) {
                    retry = true;
                    log.error("任务转换出错, 进行重试, 重试次数:{}, 错误信息:[{}][{}]", convertInfo.getRetry(), error, sourceFilePath);
                } else {
                    log.error("任务转换出错, 重试超过最大次数, 停止执行, 错误信息:[{}][{}]", error, sourceFilePath);
                }*/
                log.error("任务转换出错, 错误信息:[{}][{}]", error, sourceFilePath);
            } else {
                log.error("任务出现未知错误[{}]", sourceFilePath, e);
            }
        } else if (e instanceof InterruptedException) {
            error = "任务中断";
            log.error("任务中断[{}]", sourceFilePath);
        } else {
            log.error("任务出现未知错误[{}]", sourceFilePath, e);
        }
        // 修改任务状态
        if (!retry && status != ConvertStatus.CANCEL) {
            convertInfo.setStatus(ConvertStatus.ERROR);
        }
        // 写入错误信息
        String exceptions = convertInfo.getExceptions();
        // 如果是新型错误才添加
        if (StringUtils.isEmpty(exceptions)) {
            convertInfo.setExceptions(error);
        } else if (!Arrays.asList(exceptions.split(SEPARATOR)).contains(error)) {
            convertInfo.setExceptions(exceptions + SEPARATOR + error);
        }
        // 如果重试, 将任务移除并添加到队尾
        if (retry) {
            ConcurrentLinkedHashMap<Integer, ConvertMission> missions = ConvertManager.getMissions();
            missions.put(missionId, missions.remove(missionId));
        }
        // 如果无法重试, 则移除并写入数据库
        else {
            // 删除临时文件, 如果失败则重试一次
            String targetFilePath = convertInfo.getTargetFilePath();
            if (delete && !FileUtils.deleteFile(targetFilePath)) {
                if (!FileUtils.deleteFile(targetFilePath)) {
                    log.error("文件[{}]删除失败, 请手动删除", targetFilePath);
                }
            }
            // 写入数据库
            save();
        }
    }

    /**
     * 任务重试
     *
     * @return true代表重试成功（只是修改状态, 并没有实际运行）
     */
    private boolean retry() {
        int retry = convertInfo.getRetry();
        if (retry < CustomizeConfig.instance().getMaxRetries()) {
            convertInfo.setStatus(ConvertStatus.RETRY);
            convertInfo.setRetry(retry + 1);
            log.debug("任务重试[{}]", convertInfo.getSourceFilePath());
            return true;
        }
        return false;
    }

    /**
     * 取消任务
     */
    public void cancelMission() {
        try {
            // 如果是正在运行或在线程池等待的任务, 利用future.cancel触发异常, 从而取消任务
            if (future != null && !future.isDone()) {
                convertInfo.setStatus(ConvertStatus.CANCEL);
                future.cancel(true);
            }
        } catch (Exception e) {
            log.error("取消任务失败{}", convertInfo, e);
        }
    }

    /**
     * 任务结束时写入数据库
     */
    private void save() {
        try {
            // 将已完成任务从列表移除, 并将转换信息写入finish队列
            if (!ConvertManager.getMissions().remove(missionId, this)) {
                log.error("任务移除失败[{}]", missionId);
            }
            ConvertManager.getFinishedInfo().add(convertInfo);
            // 写入数据库
            ConvertManager.getConvertInfoMapper().insert(convertInfo);
            log.debug("写入数据库成功[{}]", convertInfo);
        } catch (Exception e) {
            log.error("写入数据库失败, 错误信息:{}[{}]", e.getMessage(), convertInfo);
        }
    }

    @Override
    public String toString() {
        return String.format("[id=%s]"
                        + "%s",
                missionId,
                convertInfo);
    }
}
