package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 12/01/12
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SessionHandler {
    public void addDataFragment(byte[] bytes);
    
    public void sessionTerminated();
}
