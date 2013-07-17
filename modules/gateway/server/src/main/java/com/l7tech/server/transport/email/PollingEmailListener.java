package com.l7tech.server.transport.email;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.LifecycleException;

import java.beans.PropertyChangeListener;

/**
 * An interface for objects that can poll an email account to retrieve messages to process.
 */
public interface PollingEmailListener{
    /**
     * Starts the listener thread.
     *
     * @throws com.l7tech.server.LifecycleException when an error is encountered in the thread startup
     */
    public void start() throws LifecycleException;

    /**
     * Tells the listener thread to stop.
     */
    public void stop();

    /**
     * Give the listener thread a set amount of time to shutdown, before it gets interrupted.
     */
    public void ensureStopped();

    /**
     * Returns the listener's EmailListener Goid.
     *
     * @return goid value
     */
    public Goid getEmailListenerGoid();

    // need to add methods to handle updates/deletes to EmailListener.
}
