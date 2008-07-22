package com.l7tech.server.config.exceptions;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 23, 2005
 * Time: 11:13:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseConfigException extends Exception {
    public DatabaseConfigException() {
        super();
    }

    public DatabaseConfigException(String message) {
        super(message);
    }

    public DatabaseConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
