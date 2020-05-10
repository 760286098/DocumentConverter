package com.converter.converter;

import com.converter.config.CustomizeConfig;
import com.converter.constant.CellType;
import com.converter.constant.WordType;
import com.converter.converter.impl.CellConverter;
import com.converter.converter.impl.WordConverter;
import com.converter.exception.ConvertException;
import com.converter.exception.FileException;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 转换抽象类
 *
 * @author Evan
 */
@Slf4j
public abstract class AbstractConverter {
    private static com.aspose.cells.LoadOptions cellLoadOptions
            = new com.aspose.cells.LoadOptions();

    private static com.aspose.words.PdfSaveOptions wordToPdfOptions
            = new com.aspose.words.PdfSaveOptions();
    private static com.aspose.cells.PdfSaveOptions cellToPdfOptions
            = new com.aspose.cells.PdfSaveOptions();

    private static List<String> wordTypes
            = EnumSet.allOf(WordType.class)
            .parallelStream()
            .map(WordType::getType)
            .distinct()
            .collect(Collectors.toList());
    private static List<String> cellTypes
            = EnumSet.allOf(CellType.class)
            .parallelStream()
            .map(CellType::getType)
            .distinct()
            .collect(Collectors.toList());

    static {
        // =============================Word============================
        // 设置启用超链
        wordToPdfOptions.setCreateNoteHyperlinks(true);
        // 减小内存消耗, 不过会增加处理时间
        wordToPdfOptions.setMemoryOptimization(true);
        // 优化输出, 不过可能影响结果准确性
        wordToPdfOptions.setOptimizeOutput(true);

        // =============================Cell============================
        // 优化内存
        cellLoadOptions.setMemorySetting(com.aspose.cells.MemorySetting.MEMORY_PREFERENCE);
        // 将所有的列放入一页
        cellToPdfOptions.setAllColumnsInOnePagePerSheet(true);
    }

    /**
     * 初始化, 载入授权文件, 并设置字体目录
     */
    public static void init() {
        try {
            log.debug("开始初始化AbstractConverter");
            InputStream inputStream;
            // 载入授权文件
            inputStream = AbstractConverter.class.getResourceAsStream("/static/license/license.lic");
            new com.aspose.words.License().setLicense(inputStream);
            // input流不能重复读取, 所以需要重新获取
            inputStream = AbstractConverter.class.getResourceAsStream("/static/license/license.lic");
            new com.aspose.cells.License().setLicense(inputStream);
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
     * 根据文件后缀名获取对应的转换器
     *
     * @param sourceFilePath 源文件路径
     */
    public static AbstractConverter getConverter(final String sourceFilePath) {
        AbstractConverter converter;
        String fileExtension = sourceFilePath.substring(sourceFilePath.lastIndexOf(".") + 1);

        if (wordTypes.contains(fileExtension.toUpperCase())) {
            converter = new WordConverter();
        } else if (cellTypes.contains(fileExtension.toUpperCase())) {
            converter = new CellConverter();
        } else {
            throw new FileException.FileTypeException(fileExtension);
        }
        return converter;
    }

    /**
     * Getter
     */
    public static com.aspose.cells.LoadOptions getCellLoadOptions() {
        return cellLoadOptions;
    }

    /**
     * Getter
     */
    public static com.aspose.words.PdfSaveOptions getWordToPdfOptions() {
        return wordToPdfOptions;
    }

    /**
     * Getter
     */
    public static com.aspose.cells.PdfSaveOptions getCellToPdfOptions() {
        return cellToPdfOptions;
    }

    /**
     * 抽象方法, 用于执行转换任务, 由子类实现
     *
     * @param sourceFilePath 源文件路径
     * @param targetFilePath 目的路径
     */
    public abstract void convert(final String sourceFilePath,
                                 final String targetFilePath);

    /**
     * 抽象方法, 用于中断任务, 由子类实现
     */
    public abstract void interrupt();
}
