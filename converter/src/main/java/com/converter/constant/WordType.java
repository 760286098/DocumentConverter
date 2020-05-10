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
    DOT("DOT"),
    DOCX("DOCX"),
    DOCM("DOCM"),
    DOTX("DOTX"),
    DOTM("DOTM"),
    RTF("RTF"),
    HTML("HTML"),
    MHT("MHT"),
    MHTML("MHTML"),
    MOBI("MOBI"),
    ODT("ODT"),
    OTT("OTT"),
    TXT("TXT"),
    MD("MD");

    /**
     * 文件类型
     */
    private String type;
}
