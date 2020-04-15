package com.converter.converter;

import com.converter.config.CustomizeConfig;
import com.converter.constant.CellType;
import com.converter.constant.WordType;
import com.converter.converter.impl.CellConverter;
import com.converter.converter.impl.WordConverter;
import com.converter.exception.ConvertException;
import com.converter.exception.FileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.util.ResourceUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 转换抽象类
 *
 * @author Evan
 */
@Slf4j
@DependsOn("customizeConfig")
public abstract class AbstractConverter {
    static List<String> wordTypes = EnumSet.allOf(WordType.class).parallelStream().map(WordType::getType).distinct().collect(Collectors.toList());
    static List<String> cellTypes = EnumSet.allOf(CellType.class).parallelStream().map(CellType::getType).distinct().collect(Collectors.toList());

    /**
     * 初始化, 载入授权文件, 并设置字体目录
     */
    public static void init() {
        try {
            log.debug("开始初始化AbstractConverter");
            // 载入授权文件
            String path = ResourceUtils.getFile("classpath:static/license/license.lic").getAbsolutePath();
            new com.aspose.words.License().setLicense(path);
            new com.aspose.cells.License().setLicense(path);
            // 设置字体目录
            String fontDir = CustomizeConfig.instance().getFontDir();
            com.aspose.words.FontSettings.getDefaultInstance().setFontsFolder(fontDir, false);
            com.aspose.cells.FontConfigs.setFontFolder(fontDir, false);
            log.debug("成功初始化AbstractConverter");
        } catch (Exception e) {
            if (CustomizeConfig.instance().isAllowWithoutLicense()) {
                log.warn("授权文件未加载, 转换文件会有水印");
            } else {
                throw new ConvertException.LicenseException(e);
            }
        }
    }

    /**
     * 感觉后缀名进行文档转换
     *
     * @param sourceFilePath 源文件路径
     * @param targetFilePath 目的路径
     */
    public static void toConvert(String sourceFilePath, String targetFilePath) {
        AbstractConverter converter;
        String fileExtension = sourceFilePath.substring(sourceFilePath.lastIndexOf(".") + 1);

        if (wordTypes.contains(fileExtension.toUpperCase())) {
            converter = new WordConverter();
        } else if (cellTypes.contains(fileExtension.toUpperCase())) {
            converter = new CellConverter();
        } else {
            throw new FileException.FileTypeException(sourceFilePath);
        }
        converter.convert(sourceFilePath, targetFilePath);
    }

    /**
     * 抽象方法, 由子类实现
     *
     * @param sourceFilePath 源文件路径
     * @param targetFilePath 目的路径
     */
    protected abstract void convert(String sourceFilePath, String targetFilePath);
}
