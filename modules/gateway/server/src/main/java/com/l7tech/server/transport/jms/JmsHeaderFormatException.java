package com.l7tech.server.transport.jms;

import javax.jms.JMSException;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 11/19/14
 */
public class JmsHeaderFormatException extends JMSException {
    public JmsHeaderFormatException(String reason, String errorCode) {
        super(reason, errorCode);
    }

    public JmsHeaderFormatException(String reason) {
        super(reason);
    }
}
