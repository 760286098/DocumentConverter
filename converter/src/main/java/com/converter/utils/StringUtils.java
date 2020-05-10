package com.converter.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * String工具类
 *
 * @author Evan
 */
public final class StringUtils {
    private StringUtils() {
    }

    /**
     * 判断对象是否为空
     *
     * @param obj 对象
     * @return true为空
     */
    public static boolean isEmpty(final Object obj) {
        return obj == null || "".equals(obj);
    }

    /**
     * 根据对象生成Json格式字符串（字符串类型的为null时写入"", 数字型为null时写入0）
     *
     * @param object 待处理对象
     * @return 对象对应json字符串
     */
    public static String toJsonString(final Object object) {
        if (isEmpty(object)) {
            return "";
        }
        return JSON.toJSONString(object, SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullNumberAsZero);
    }
}
