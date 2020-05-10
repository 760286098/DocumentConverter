package com.converter.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Cell类型
 *
 * @author Evan
 */

@SuppressWarnings({"ALL", "AlibabaEnumConstantsMustHaveComment"})
@Getter
@AllArgsConstructor
public enum CellType {
    XLS("XLS"),
    XLSX("XLSX"),
    XLSB("XLSB"),
    XLSM("XLSM"),
    XLT("XLT"),
    XLTX("XLTX"),
    XLTM("XLTM"),
    CSV("CSV"),
    TSV("TSV"),
    ODS("ODS"),
    SXC("SXC"),
    FODS("FODS");

    /**
     * 文件类型
     */
    private String type;
}
