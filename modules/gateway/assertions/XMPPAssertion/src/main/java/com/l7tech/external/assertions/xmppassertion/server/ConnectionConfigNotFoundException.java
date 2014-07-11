package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.objectmodel.Goid;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 12/03/12
 * Time: 12:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConnectionConfigNotFoundException extends Exception {
    public ConnectionConfigNotFoundException(Goid goid) {
        super("XMPP connection configuration not found (" + goid.toString() + ").");
    }
}
