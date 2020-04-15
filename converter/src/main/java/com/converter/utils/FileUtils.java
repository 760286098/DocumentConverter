package com.converter.utils;

import com.converter.config.CustomizeConfig;
import com.converter.exception.FileException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件工具类
 *
 * @author Evan
 */
@Slf4j
public final class FileUtils {
    /**
     * 检查源文件是否有问题
     *
     * @param sourceFilePath 源文件路径
     * @return true代表没问题
     */
    public static boolean testSourceFile(String sourceFilePath) {
        if (StringUtils.isEmpty(sourceFilePath)) {
            return false;
        }
        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new FileException.FileNotExistsException(sourceFilePath);
        }
        if (testAndSetFile(sourceFilePath)) {
            log.debug("源文件已在redis缓存中: [" + sourceFilePath + "]");
            return false;
        }
        return true;
    }

    /**
     * 检查源目录是否有问题
     *
     * @param sourceDirPath 源目录路径
     * @param isScan        true代表是扫描, 不用检测目录是否在redis缓存中
     * @return true代表没问题
     */
    public static boolean testSourceDir(String sourceDirPath, boolean isScan) {
        if (StringUtils.isEmpty(sourceDirPath)) {
            return false;
        }
        if (isDir(sourceDirPath, false)) {
            if (isScan) {
                return true;
            } else if (testAndSetDir(sourceDirPath)) {
                log.debug("源目录已在redis缓存中: [" + sourceDirPath + "]");
                return false;
            }
        }
        return true;
    }

    /**
     * 检查文件是否在redis缓存中
     *
     * @param path 文件路径
     * @return true代表已存在, false代表不存在, 并将其加入redis
     */
    private static boolean testAndSetFile(String path) {
        String key = CustomizeConfig.instance().getRedisFileKey();
        if (!RedisUtils.sHasKey(key, path)) {
            RedisUtils.sSet(key, path);
            return false;
        }
        return true;
    }

    /**
     * 检查目录是否在redis缓存中
     *
     * @param path 目录路径
     * @return true代表已存在, false代表不存在, 并将其加入redis
     */
    private static boolean testAndSetDir(String path) {
        String key = CustomizeConfig.instance().getRedisDirKey();
        if (!RedisUtils.sHasKey(key, path)) {
            RedisUtils.sSet(key, path);
            return false;
        }
        return true;
    }

    /**
     * 检查是否是正确目录
     *
     * @param path   目录路径
     * @param create true代表没有就新建
     * @return true代表没问题
     */
    private static boolean isDir(String path, boolean create) {
        File targetDir = new File(path);
        if (!targetDir.exists()) {
            if (create) {
                targetDir.mkdirs();
            } else {
                throw new FileException.DirNotExistsException(path);
            }
        }
        if (!targetDir.isDirectory()) {
            throw new FileException.DirNotExistsException(path);
        }
        return true;
    }

    /**
     * 遍历获取目录下文件
     *
     * @param path 目录路径
     * @return 文件路径集合
     */
    public static String[] listDir(String path) {
        log.debug("获取文件夹[{}]下所有文件", path);
        File[] files = new File(path).listFiles();
        if (files == null) {
            return new String[0];
        }
        List<String> list = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                list.add(file.getAbsolutePath());
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * 将文件长度转为可读大小, 1k=1000
     *
     * @param bytes 文件长度
     * @return 可读大小
     */
    public static String getReadableByteCountSi(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }

    /**
     * 将文件长度转为可读大小, 1K=1024
     *
     * @param bytes 文件长度
     * @return 可读大小
     */
    public static String getReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %cB", value / 1024.0, ci.current());
    }

    /**
     * 处理目录路径
     *
     * @param path 路径
     * @return 处理后的路径
     */
    public static String dealWithDir(String path) {
        if (StringUtils.isEmpty(path)) {
            throw new FileException.DirNotExistsException(path);
        }
        if (isDir(path, true)) {
            StringBuilder sb = new StringBuilder(path);
            if (sb.charAt(sb.length() - 1) != File.separatorChar) {
                sb.append(File.separatorChar);
            }
            return sb.toString();
        }
        return "";
    }

    /**
     * 删除文件
     *
     * @param path 文件路径
     * @return 成功与否
     */
    public static boolean deleteFile(String path) {
        return new File(path).delete();
    }
}
