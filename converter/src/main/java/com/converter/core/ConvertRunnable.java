package com.converter.core;

import com.converter.config.CustomizeConfig;
import com.converter.constant.ConvertStatus;
import com.converter.converter.AbstractConverter;
import com.converter.pojo.ConvertInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 转换任务具体执行者
 *
 * @author Evan
 */
@Slf4j
public class ConvertRunnable implements Runnable {
    /**
     * 调用者, 用于传递参数
     */
    private ConvertMission caller;
    /**
     * 文档转换器
     */
    private AbstractConverter converter;
    /**
     * 计时器
     */
    private ScheduledFuture<?> timer;

    public ConvertRunnable(final ConvertMission caller) {
        this.caller = caller;
        this.converter = null;
        this.timer = null;
    }

    @Override
    public void run() {
        // 获取转换信息
        ConvertInfo convertInfo = caller.getConvertInfo();
        // 获取源文件路径
        String sourceFilePath = convertInfo.getSourceFilePath();
        // 获取文档转换器
        converter = AbstractConverter.getConverter(sourceFilePath);
        // 具体转换操作
        proceed(convertInfo, sourceFilePath);
    }

    /**
     * 具体转换操作
     */
    private void proceed(final ConvertInfo convertInfo,
                         final String sourceFilePath) {
        try {
            // 开始计时器
            startTimer();
            // 设置任务开始时间（重试任务时重置开始时间）
            convertInfo.setStartTime(System.currentTimeMillis());
            // 修改任务状态为RUN
            convertInfo.setStatus(ConvertStatus.RUN);
            // 获取重试次数
            Integer retry = convertInfo.getRetry();
            if (retry == 0) {
                log.info("任务开始运行[{}]", sourceFilePath);
            } else {
                log.info("任务开始重试, 重试次数:{}[{}]", retry, sourceFilePath);
            }
            // 执行转换任务
            converter.convert(sourceFilePath, convertInfo.getTargetFilePath());
        } finally {
            // 结束计时器
            stopTimer();
        }
    }

    /**
     * 启动计时器
     */
    private void startTimer() {
        Future<?> future = caller.getFuture();
        int timeout = CustomizeConfig.instance().getMissionTimeout() * (caller.getConvertInfo().getRetry() + 1);
        this.timer = ConvertManager.getThreadPoolTaskScheduler().getScheduledExecutor().schedule(() -> {
            if (future != null) {
                future.cancel(true);
            }
        }, timeout, TimeUnit.SECONDS);
    }

    /**
     * 取消计时器
     */
    private void stopTimer() {
        if (timer != null) {
            timer.cancel(true);
        }
    }

    /**
     * 中断任务
     */
    public void interrupt() {
        if (converter != null) {
            converter.interrupt();
        }
    }
}
