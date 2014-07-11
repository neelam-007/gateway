package com.l7tech.external.assertions.xmppassertion.server;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 12/03/12
 * Time: 12:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class InboundSessionNotFoundException extends Exception {
    public InboundSessionNotFoundException(long sessionId) {
        super("XMPP connection not found (" + sessionId + ").");
    }
}
