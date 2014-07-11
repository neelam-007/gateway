package com.l7tech.external.assertions.xmppassertion.server;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 12/03/12
 * Time: 4:09 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SessionStartedCallback {
    public void sessionStarted(Object session);
    
    public long getSessionId();

    public void waitForSessionId();
}
