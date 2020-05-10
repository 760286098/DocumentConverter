package com.converter.converter.impl;

import com.converter.converter.AbstractConverter;
import com.converter.exception.ConvertException;

/**
 * Word转换类
 *
 * @author Evan
 */
public class WordConverter extends AbstractConverter {
    /**
     * 转换Word类型的文件
     *
     * @param sourceFilePath 源文件路径
     * @param targetFilePath 目的路径
     */
    @Override
    public void convert(final String sourceFilePath,
                        final String targetFilePath) {
        try {
            // 创建Document文档对象
            com.aspose.words.Document document = new com.aspose.words.Document(sourceFilePath);
            // 开始文档转换
            document.save(targetFilePath, getWordToPdfOptions());
        } catch (Exception e) {
            throw new ConvertException.WordConvertException(e);
        }
    }

    /**
     * 中断任务
     */
    @Override
    public void interrupt() {
        // Aspose.Words暂时没有提供中断方法
    }
}