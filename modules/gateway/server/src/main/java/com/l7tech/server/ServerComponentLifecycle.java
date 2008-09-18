package com.l7tech.server;

/**
 * @author alex
 * @version $Revision$
 */
public interface ServerComponentLifecycle {

    void start() throws LifecycleException;

    void stop() throws LifecycleException;

    void close() throws LifecycleException;
}
