package com.converter.exception;

/**
 * 文档转换过程中的异常
 *
 * @author Evan
 */
public class ConvertException {
    public static class LicenseException extends RuntimeException {
        /**
         * 关于授权文件的异常
         *
         * @param e 异常
         */
        public LicenseException(final Exception e) {
            super(String.format("载入授权文件失败:[%s]", e.getMessage()));
        }
    }

    public static class WordConvertException extends RuntimeException {
        /**
         * Word类型文件转换出现的异常
         *
         * @param e 异常
         */
        public WordConvertException(final Exception e) {
            super(String.format("Word文件转换出错:[%s]", e.getMessage()));
        }
    }

    public static class CellConvertException extends RuntimeException {
        /**
         * Cell类型文件转换出现的异常
         *
         * @param e 异常
         */
        public CellConvertException(final Exception e) {
            super(String.format("Cell文件转换出错:[%s]", e.getMessage()));
        }
    }

    public static class SlideConvertException extends RuntimeException {
        /**
         * Cell类型文件转换出现的异常
         *
         * @param e 异常
         */
        public SlideConvertException(final Exception e) {
            super(String.format("Slide文件转换出错:[%s]", e.getMessage()));
        }
    }
}
