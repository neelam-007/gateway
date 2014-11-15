package com.l7tech.server.transport.jms;

import javax.jms.JMSException;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 11/14/14
 */
public class JmsMessageExpiredException extends JMSException{

    public JmsMessageExpiredException(String reason, String errorCode) {
        super(reason, errorCode);
    }

    public JmsMessageExpiredException(String reason) {
        super(reason);
    }

    public JmsMessageExpiredException() {
        this("JMS message expired");
    }
}
