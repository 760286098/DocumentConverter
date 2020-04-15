package com.converter.exception;

/**
 * 文件处理过程中的异常
 *
 * @author Evan
 */
public class FileException {
    public static class FileTypeException extends RuntimeException {
        /**
         * 文件格式异常
         *
         * @param msg 异常信息
         */
        public FileTypeException(String msg) {
            super(String.format("不支持的文件类型:[%s]", msg));
        }
    }

    public static class FileNotExistsException extends RuntimeException {
        /**
         * 文件不存在异常
         *
         * @param msg 异常信息
         */
        public FileNotExistsException(String msg) {
            super(String.format("文件不存在:[%s]", msg));
        }
    }

    public static class DirNotExistsException extends RuntimeException {
        /**
         * 文件夹不存在异常
         *
         * @param msg 异常信息
         */
        public DirNotExistsException(String msg) {
            super(String.format("文件夹不存在:[%s]", msg));
        }
    }
}
