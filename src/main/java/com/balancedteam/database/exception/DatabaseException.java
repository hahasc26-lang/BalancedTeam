package com.balancedteam.database.exception;

/**
 * 数据库操作异常
 * 封装底层的 SQLException，使上层业务逻辑能够精确捕获并响应持久化错误
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(Throwable cause) {
        super(cause);
    }
}
