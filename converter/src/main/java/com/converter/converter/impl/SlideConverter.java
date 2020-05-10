package com.converter.converter.impl;

import com.converter.converter.AbstractConverter;
import com.converter.exception.ConvertException;

/**
 * Slide转换类
 *
 * @author Evan
 */
public class SlideConverter extends AbstractConverter {
    /**
     * Presentation文档对象
     */
    private com.aspose.slides.Presentation presentation = null;
    /**
     * 中断监视器
     */
    private com.aspose.slides.InterruptionTokenSource monitor = null;

    /**
     * 转换Slide类型的文件
     *
     * @param sourceFilePath 源文件路径
     * @param targetFilePath 目的路径
     */
    @Override
    public void convert(final String sourceFilePath,
                        final String targetFilePath) {
        try {
            // 创建中断监视器
            monitor = new com.aspose.slides.InterruptionTokenSource();
            // 启动设置
            com.aspose.slides.LoadOptions loadOptions = new com.aspose.slides.LoadOptions();
            // 设置中断
            loadOptions.setInterruptionToken(monitor.getToken());
            // 创建Presentation文档对象
            presentation = new com.aspose.slides.Presentation(sourceFilePath, loadOptions);
            // 开始文档转换
            presentation.save(targetFilePath, com.aspose.slides.SaveFormat.Pdf, getSlideToPdfOptions());
        } catch (Exception e) {
            throw new ConvertException.SlideConvertException(e);
        } finally {
            // 释放资源
            if (presentation != null) {
                presentation.dispose();
                presentation = null;
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