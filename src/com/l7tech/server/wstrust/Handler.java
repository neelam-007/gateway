package com.l7tech.server.wstrust;

/**
 * @author emil
 * @version 12-Aug-2004
 */
public interface Handler {
    /**
     * Message chain handler for message invocation.
     * @param i The invocation.
     */
    void invoke(MessageInvocation i) throws Throwable;
}
