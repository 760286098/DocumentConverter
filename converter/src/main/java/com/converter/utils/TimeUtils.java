package com.converter.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Time工具类
 *
 * @author Evan
 */
public class TimeUtils {
    /**
     * 将时间戳转换为具体日期
     *
     * @param time 时间戳
     * @return 具体日期
     */
    public static String getReadableDate(Long time) {
        if (StringUtils.isEmpty(time)) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(new Date(time));
    }

    /**
     * 将时间戳之差转换为时长
     *
     * @param time 毫秒数
     * @return 时长
     */
    public static String getReadableTime(Long time) {
        if (StringUtils.isEmpty(time)) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
        return formatter.format(time);
    }
}
