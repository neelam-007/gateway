/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms.prov;

import com.ibm.mq.jms.MQConnectionFactory;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.l7tech.server.transport.jms.ConnectionFactoryCustomizer;
import com.l7tech.server.transport.jms.JmsConfigException;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.net.ssl.SSLSocketFactory;
import java.security.GeneralSecurityException;

/**
 * @author alex
 */
public class MQSeriesCustomizer implements ConnectionFactoryCustomizer {
    public void configureConnectionFactory(final JmsConnection jmsConnection,
                                           final ConnectionFactory connectionFactory,
                                           final Context jndiContext)
            throws JmsConfigException
    {
        if (!(connectionFactory instanceof MQConnectionFactory)) throw new JmsConfigException("The provided ConnectionFactory does not appear to be from WebsphereMQ");

        final MQConnectionFactory mqcf = (MQConnectionFactory) connectionFactory;

        // TODO Nasty hack for backwards compatibility--the lack of the "use client auth" flag indicates that client
        // authentication should be used!
        final String sclientAuth = (String)jmsConnection.properties().get(JmsConnection.PROP_QUEUE_USE_CLIENT_AUTH);
        final boolean clientAuth = sclientAuth == null || "true".equals(sclientAuth);
        final String alias = (String)jmsConnection.properties().get(JmsConnection.PROP_QUEUE_SSG_KEY_ALIAS);
        final String skid = (String)jmsConnection.properties().get(JmsConnection.PROP_QUEUE_SSG_KEYSTORE_ID);
        final SSLSocketFactory socketFactory;

        if (alias != null && skid != null) {
            try {
                socketFactory = SslClientSocketFactory.getInstance(Long.valueOf(skid), alias);
            } catch (NumberFormatException e) {
                throw new JmsConfigException("Bad keystore ID: " + skid, e);
            } catch (FindException e) {
                throw new JmsConfigException("Unable to find SSG Keystore Entry", e);
            } catch (GeneralSecurityException e) {
                throw new JmsConfigException("Unable to initialize SSLSocketFactory", e);
            }
        } else if (clientAuth) {
            socketFactory = SslClientSocketFactory.getDefault();
        } else {
            socketFactory = SslClientSocketFactory.getAnonymous();
        }
        mqcf.setSSLSocketFactory(socketFactory);
    }
}
