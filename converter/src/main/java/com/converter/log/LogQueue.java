package com.converter.log;

import com.converter.pojo.LoggerMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 日志队列
 *
 * @author Evan
 */
@Slf4j
public class LogQueue {
    /**
     * 队列大小
     */
    public static final int QUEUE_MAX_SIZE = 10000;
    private static LogQueue instance = new LogQueue();
    /**
     * 阻塞队列
     */
    private BlockingQueue<LoggerMessage> blockingQueue = new LinkedBlockingQueue<>(QUEUE_MAX_SIZE);

    private LogQueue() {
    }

    public static LogQueue getInstance() {
        return instance;
    }

    /**
     * 消息入队
     */
    public boolean push(final LoggerMessage log) {
        return blockingQueue.offer(log);
    }

    /**
     * 消息出队
     */
    public LoggerMessage poll() {
        LoggerMessage result = null;
        try {
            // 队列为空时阻塞
            result = blockingQueue.take();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        return result;
    }
}