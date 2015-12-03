package com.l7tech.external.assertions.remotecacheassertion.server;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 15/11/11
 * Time: 3:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCacheConnectionException extends Exception {
    public RemoteCacheConnectionException(final String message) {
        super(message);
    }

    public RemoteCacheConnectionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
