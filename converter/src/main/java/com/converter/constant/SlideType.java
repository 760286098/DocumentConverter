package com.converter.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Slide类型
 *
 * @author Evan
 */

@SuppressWarnings({"ALL", "AlibabaEnumConstantsMustHaveComment"})
@Getter
@AllArgsConstructor
public enum SlideType {
    PPT("PPT"),
    POT("POT"),
    PPS("PPS"),
    PPTX("PPTX"),
    POTX("POTX"),
    PPSX("PPSX"),
    PPTM("PPTM"),
    PPSM("PPSM"),
    POTM("POTM"),
    OTP("OTP"),
    ODP("ODP");

    /**
     * 文件类型
     */
    private String type;
}
