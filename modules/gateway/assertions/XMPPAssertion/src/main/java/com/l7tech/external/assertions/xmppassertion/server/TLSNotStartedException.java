package com.l7tech.external.assertions.xmppassertion.server;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 23/03/12
 * Time: 1:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class TLSNotStartedException extends Exception {
    public TLSNotStartedException(long sessionId) {
        super("XMPP session (" + sessionId + ") has not started TLS communication yet.");
    }
}
