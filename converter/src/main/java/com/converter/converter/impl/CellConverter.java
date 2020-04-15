package com.converter.converter.impl;

import com.converter.converter.AbstractConverter;
import com.converter.exception.ConvertException;

/**
 * Cell转换类
 *
 * @author Evan
 */
public class CellConverter extends AbstractConverter {
    /**
     * 转换Cell类型的文件
     *
     * @param sourceFilePath 源文件路径
     * @param targetFilePath 目的路径
     */
    @Override
    protected void convert(String sourceFilePath, String targetFilePath) {
        try {
            com.aspose.cells.Workbook workbook = new com.aspose.cells.Workbook(sourceFilePath);
            workbook.save(targetFilePath, com.aspose.cells.SaveFormat.PDF);
        } catch (Exception e) {
            throw new ConvertException.CellConvertException(e);
        }
    }
}
