package com.converter.utils;

import com.converter.config.CustomizeConfig;
import com.converter.exception.FileException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件工具类
 *
 * @author Evan
 */
@Slf4j
public final class FileUtils {
    private FileUtils() {
    }

    /**
     * 检查源文件是否有问题
     *
     * @param sourceFilePath 源文件路径
     * @return true代表没问题
     */
    public static boolean testSourceFile(final String sourceFilePath) {
        if (StringUtils.isEmpty(sourceFilePath)) {
            return false;
        }
        File sourceFile = new File(sourceFilePath);
        // 判断文件是否存在
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new FileException.FileNotExistsException(sourceFilePath);
        }
        // 判断文件是否已经处理过
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
    public static boolean testSourceDir(final String sourceDirPath,
                                        final boolean isScan) {
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
    private static boolean testAndSetFile(final String path) {
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
    private static boolean testAndSetDir(final String path) {
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
    private static boolean isDir(final String path,
                                 final boolean create) {
        File targetDir = new File(path);
        if (!targetDir.exists()) {
            if (!create || !targetDir.mkdirs()) {
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
    public static String[] listDir(final String path) {
        log.debug("获取文件夹[{}]下所有文件", path);
        String[] empty = new String[0];
        File[] files = new File(path).listFiles();
        if (files == null) {
            return empty;
        }
        List<String> list = new ArrayList<>();
        for (File file : files) {
            // 添加普通文件, 同时排除~$临时文件
            if (file.isFile() && !file.getName().startsWith("~$")) {
                list.add(file.getAbsolutePath());
            }
        }
        return list.toArray(empty);
    }

    /**
     * 处理目录路径
     *
     * @param path 路径
     * @return 处理后的路径
     */
    public static String dealWithDir(final String path) {
        if (StringUtils.isEmpty(path)) {
            throw new FileException.DirNotExistsException(path);
        }
        if (isDir(path, true)) {
            // 可以根据相对路径获取绝对路径
            String absolutePath = new File(path).getAbsolutePath();
            StringBuilder sb = new StringBuilder(absolutePath);
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
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean deleteFile(final String path) {
        boolean delete = false;
        try {
            delete = new File(path).delete();
        } catch (Exception e) {
            log.error("删除文件失败", e);
        }
        return delete;
    }
}
