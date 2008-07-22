/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.skunkworks;

import com.ibm.mq.jms.MQQueueConnectionFactory;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * @author alex
 */
public class JmsPing {
    public static void main(String[] args) throws Exception {
        QueueConnection qcon = null;
        QueueSession sess = null;
        Context ctx = null;
        try {
            String icfClassname = args[0];
            String jndiUrl = args[1];
            String qcfUrl = args[2];
            String queueName = args[3];

            Hashtable<String, String> hash = new Hashtable<String, String>();
            hash.put(Context.INITIAL_CONTEXT_FACTORY, icfClassname);
            hash.put(Context.PROVIDER_URL, jndiUrl);
//            hash.put(Context.SECURITY_AUTHENTICATION, "simple");
//            hash.put(Context.SECURITY_PRINCIPAL, "");
//            hash.put(Context.SECURITY_CREDENTIALS, "");

            ctx = new InitialDirContext(hash);

            Object o = ctx.lookup(qcfUrl);
            QueueConnectionFactory qcf;
            if (o instanceof QueueConnectionFactory) {
                qcf = (QueueConnectionFactory) o;
            } else throw new RuntimeException(o.getClass().getName());

            if (qcf instanceof MQQueueConnectionFactory) {
                MQQueueConnectionFactory mqcf = (MQQueueConnectionFactory) qcf;
//                mqcf.setSSLCipherSuite("SSL_RSA_WITH_3DES_EDE_CBC_SHA");
                //            SslClientSocketFactory.setTrustManager(new PermissiveX509TrustManager());
                //            SSLSocketFactory sf = SslClientSocketFactory.getDefault();
                //            mqcf.setSSLSocketFactory(sf);
            }

            qcon = qcf.createQueueConnection("", "");
            sess = qcon.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            Queue q = sess.createQueue(queueName);
        } finally {
            if (sess != null) sess.close();
            if (qcon != null) qcon.close();
            if (ctx != null) ctx.close();
        }
    }
}
