package com.converter.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * String工具类
 *
 * @author Evan
 */
public class StringUtils {
    /**
     * 判断对象是否为空
     *
     * @param obj 对象
     * @return true为空
     */
    public static boolean isEmpty(Object obj) {
        return obj == null || "".equals(obj);
    }

    /**
     * 处理转义字符
     *
     * @param keyword 待处理字符串
     * @return 对转义字符进行转义处理
     */
    public static String escapeExprSpecialWord(String keyword) {
        if (isEmpty(keyword)) {
            return keyword;
        }
        String[] fbsArr = {"\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|"};
        for (String key : fbsArr) {
            if (keyword.contains(key)) {
                keyword = keyword.replace(key, "\\" + key);
            }
        }
        return keyword;
    }

    /**
     * 根据对象生成Json格式字符串（字符串类型的为null时写入"", 数字型为null时写入0）
     *
     * @param object 待处理对象
     * @return 对象对应json字符串
     */
    public static String toJsonString(Object object) {
        if (isEmpty(object)) {
            return "";
        }
        return JSON.toJSONString(object, SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullNumberAsZero);
    }

    /**
     * 根据Json字符串返回对应的对象
     *
     * @param json  json字符串
     * @param clazz the class of T
     * @return 转换后对象
     */
    public static <T> T parseJsonString(String json, Class<T> clazz) {
        if (isEmpty(json)) {
            return null;
        }
        return JSON.parseObject(json, clazz);
    }
}
