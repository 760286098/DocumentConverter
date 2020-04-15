package com.converter.constant;

/**
 * 转换状态
 *
 * @author Evan
 */

@SuppressWarnings({"AlibabaEnumConstantsMustHaveComment"})
public enum ConvertStatus {
    WAIT_OUTSIDE,
    WAIT_IN_POOL,
    RUN,
    FINISH,
    RETRY,
    ERROR,
    CANCEL
}
