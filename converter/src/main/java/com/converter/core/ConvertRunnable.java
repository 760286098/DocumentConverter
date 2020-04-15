package com.converter.core;

import com.converter.constant.ConvertStatus;
import com.converter.converter.AbstractConverter;
import com.converter.pojo.ConvertInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 转换任务具体执行者
 *
 * @author Evan
 */
@Slf4j
@Getter
public class ConvertRunnable implements Runnable {
    /**
     * 调用者, 用于传递参数
     */
    private ConvertMission monitor;

    public ConvertRunnable(ConvertMission monitor) {
        this.monitor = monitor;
    }

    @Override
    public void run() {
        // 启动计时器
        monitor.startTimer();
        ConvertInfo convertInfo = monitor.getConvertInfo();
        String sourceFilePath = convertInfo.getSourceFilePath();
        // 设置任务开始时间（重试任务时重置开始时间）
        convertInfo.setStartTime(System.currentTimeMillis());
        monitor.setStatus(ConvertStatus.RUN);
        Integer retry = convertInfo.getRetry();
        if (retry == 0) {
            log.info("任务开始运行[{}]", sourceFilePath);
        } else {
            log.info("任务开始重试, 重试次数:{}[{}]", retry, sourceFilePath);
        }
        AbstractConverter.toConvert(sourceFilePath, convertInfo.getTargetFilePath());
    }
}
