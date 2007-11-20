/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms.prov;

import com.ibm.mq.jms.MQConnectionFactory;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.l7tech.server.transport.jms.ConnectionFactoryCustomizer;
import com.l7tech.server.transport.jms.JmsConfigException;
import org.springframework.context.ApplicationContext;

import javax.jms.ConnectionFactory;
import javax.naming.Context;

/**
 * @author alex
 */
public class MQSeriesCustomizer implements ConnectionFactoryCustomizer {
    public void configureConnectionFactory(ConnectionFactory connectionFactory, Context context, ApplicationContext spring)
            throws JmsConfigException
    {
        if (!(connectionFactory instanceof MQConnectionFactory)) throw new JmsConfigException("The provided ConnectionFactory does not appear to be from WebsphereMQ");

        MQConnectionFactory mqcf = (MQConnectionFactory) connectionFactory;
        // TODO customize this SocketFactory according to configuration
        mqcf.setSSLSocketFactory(SslClientSocketFactory.getDefault());
    }
}
