package com.l7tech.server.util;

import org.springframework.aop.ThrowsAdvice;

/**
 * @author: ghuang
 */
public class ThrowsAdviceSupport implements ThrowsAdvice {

    private static final String SYS_PROP_INCLUDE_STACK_FOR_CLIENT = "com.l7tech.spring.remoting.http.sendStack";
    private static final String DEFAULT_INCLUDE_STACK = Boolean.FALSE.toString();
    private static final boolean sendStackToClient = Boolean.valueOf(System.getProperty(SYS_PROP_INCLUDE_STACK_FOR_CLIENT, DEFAULT_INCLUDE_STACK));

    /**
     * Are we configured to send stack traces to the client?
     *
     * @return true to send stack information.
     */
    protected boolean isSendStackToClient() {
        return sendStackToClient;
    }
}
