package com.converter.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.converter.pojo.LoggerMessage;
import com.converter.utils.TimeUtils;
import org.springframework.stereotype.Service;

/**
 * 日志过滤器
 *
 * @author Evan
 */
@Service
public class LogFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(final ILoggingEvent event) {
        LoggerMessage loggerMessage = new LoggerMessage(
                TimeUtils.getReadableDate(event.getTimeStamp()),
                event.getLevel().levelStr,
                event.getThreadName(),
                event.getLoggerName(),
                event.getFormattedMessage()
        );
        LogQueue.getInstance().push(loggerMessage);
        return FilterReply.ACCEPT;
    }
}
