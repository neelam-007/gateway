package com.l7tech.common.transport.jms;

/**
 * The exception is used for reporting the SSG don't support Topic in JMS.
 * @auther: ghuang
 */
public class JmsNotSupportTopicException extends Exception {
    public JmsNotSupportTopicException(String message) {
        super( message );
    }
}
