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
     * Workbook文档对象
     */
    private com.aspose.cells.Workbook workbook = null;
    /**
     * 中断监视器
     */
    private com.aspose.cells.InterruptMonitor monitor = null;

    /**
     * 转换Cell类型的文件
     *
     * @param sourceFilePath 源文件路径
     * @param targetFilePath 目的路径
     */
    @Override
    public void convert(final String sourceFilePath,
                        final String targetFilePath) {
        try {
            // 创建中断监视器
            monitor = new com.aspose.cells.InterruptMonitor();
            // 创建Workbook文档对象
            workbook = new com.aspose.cells.Workbook(sourceFilePath, getCellLoadOptions());
            // 设置中断
            workbook.setInterruptMonitor(monitor);
            // 开始文档转换
            workbook.save(targetFilePath, getCellToPdfOptions());
        } catch (Exception e) {
            throw new ConvertException.CellConvertException(e);
        } finally {
            // 释放资源
            if (workbook != null) {
                workbook.dispose();
                workbook = null;
            }
        }
    }

    /**
     * 中断任务
     */
    @Override
    public void interrupt() {
        if (monitor != null) {
            monitor.interrupt();
            monitor = null;
        }
    }
}