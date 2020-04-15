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
    protected void convert(String sourceFilePath, String targetFilePath) {
        try {
            com.aspose.words.Document document = new com.aspose.words.Document(sourceFilePath);
            document.save(targetFilePath, com.aspose.words.SaveFormat.PDF);
        } catch (Exception e) {
            throw new ConvertException.WordConvertException(e);
        }
    }
}
