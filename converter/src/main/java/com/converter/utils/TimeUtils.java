package com.converter.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Time工具类
 *
 * @author Evan
 */
public final class TimeUtils {
    private TimeUtils() {
    }

    /**
     * 将时间戳转换为具体日期
     *
     * @param time 时间戳
     * @return 具体日期
     */
    public static String getReadableDate(final Long time) {
        if (time == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return formatter.format(new Date(time));
    }
}
