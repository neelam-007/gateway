package com.l7tech.server.transport.jms2;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.LifecycleException;

import java.beans.PropertyChangeListener;

/**
 * Interface definition specifying the operations a process that listens to a JmsEndpoint (destination) must support.
 *
 * @author: vchan
 */
public interface JmsEndpointListener extends PropertyChangeListener {
    
    /**
     * Starts the listener thread.
     *
     * @throws LifecycleException when an error is encountered in the thread startup
     */
    void start() throws LifecycleException;

    /**
     * Tells the listener thread to stop.
     */
    void stop();

    /**
     * Give the listener thread a set amount of time to shutdown, before it gets interrupted.
     */
    void ensureStopped();

    /**
     * Returns the listener's JmsConnection Oid.
     *
     * @return oid value
     */
    Goid getJmsConnectionGoid();

    /**
     * Returns the listener's JmsEndpoint Oid.
     *
     * @return oid value
     */
    Goid getJmsEndpointGoid();

}
