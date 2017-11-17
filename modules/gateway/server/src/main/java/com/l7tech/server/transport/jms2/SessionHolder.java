package com.l7tech.server.transport.jms2;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.util.concurrent.atomic.AtomicLong;

public interface SessionHolder {
    String getName();

    int getEndpointVersion();

    int getConnectionVersion();

    void touch();

    /**
     * Borrow a JmsBag from the Cached Connection. Expect to return the JmsBag using returnJmsBag.
     * Fail to return the JmsBag will leave the JMS Session open, the caller is responsible to
     * close the jms session.
     *
     * @return A JmsBag with JmsSession
     * @throws JmsRuntimeException If error occur when getting the JmsBag
     */
    JmsBag borrowJmsBag() throws JmsRuntimeException, NamingException;

    /**
     * Return a JmsBag to the Cached Connection, the JmsBag can be reuse by other Thread
     * @param jmsBag The bag return to the pool
     */
    void returnJmsBag(JmsBag jmsBag);

    void doWithJmsResources(JmsResourceManager.JmsResourceCallback callback) throws JMSException, JmsRuntimeException, NamingException;

    boolean ref();

    void unRef();

    void close();

    int refCount();

    long getCreatedTime();

    AtomicLong getLastAccessTime();
}
