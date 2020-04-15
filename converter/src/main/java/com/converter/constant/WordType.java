package com.converter.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Word类型
 *
 * @author Evan
 */

@SuppressWarnings({"ALL", "AlibabaEnumConstantsMustHaveComment"})
@Getter
@AllArgsConstructor
public enum WordType {
    DOC("DOC"),
    DOCX("DOCX"),
    HTML("HTML"),
    MHT("MHT"),
    MHTML("MHTML"),
    DOT("DOT"),
    DOCM("DOCM"),
    DOTX("DOTX"),
    DOTM("DOTM"),
    RTF("RTF"),
    MOBI("MOBI"),
    ODT("ODT"),
    OTT("OTT"),
    TXT("TXT");

    /**
     * 文件类型
     */
    private String type;
}
