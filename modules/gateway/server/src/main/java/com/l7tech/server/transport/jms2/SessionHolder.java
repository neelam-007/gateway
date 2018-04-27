package com.l7tech.server.transport.jms2;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * interface SessionHolder
 */
public interface SessionHolder {
    /**
     * @return SessionHolder name
     */
    String getName();

    /**
     * @return JMS Endpoint Verion
     */
    int getEndpointVersion();

    /**
     * @return JMS Connection version
     */
    int getConnectionVersion();

    /**
     *
     */
    void touch();

    /**
     * Borrow a JmsBag from the SessionHolder. Expect to return the JmsBag using returnJmsBag.
     * Fail to return the JmsBag will leave the JMS Session open, the caller is responsible to
     * close the jms session.
     *
     * @return A JmsBag with JmsSession
     * @throws JmsRuntimeException If error occur when getting the JmsBag
     */
    JmsBag borrowJmsBag() throws JmsRuntimeException, NamingException;

    /**
     * Return a JmsBag to the SessionHolder, the JmsBag can be reuse by other Thread
     * @param jmsBag The bag return to the pool
     */
    void returnJmsBag(JmsBag jmsBag);

    /**
     * Runs the task with specified resources
     * @param callback The callback task to run
     * @throws JMSException If the given task throws a JMSException
     * @throws JmsRuntimeException If an error occurs creating the resources
     */
    void doWithJmsResources(JmsResourceManager.JmsResourceCallback callback) throws JMSException, JmsRuntimeException, NamingException;

    /**
     * references the SessionHolder object
     * @return true if successful
     */
    boolean ref();

    /**
     * removes reference to the SessionHolder object
     */
    void unRef();

    /**
     * closes the object and releases the resources
     */
    void close();

    /**
     * @return int number of references
     */
    int refCount();

    /**
     * @return long time when the object was created
     */
    long getCreatedTime();

    /**
     * @return time when the object was last accessed
     */
    AtomicLong getLastAccessTime();
}
