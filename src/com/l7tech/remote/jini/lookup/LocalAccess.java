package com.l7tech.remote.jini.lookup;

import net.jini.export.ServerContext;
import net.jini.io.context.ClientHost;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.server.ServerNotActiveException;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class used in coonjucntion with invocation dispatcher
 * that accepts calls from the local host onlu.
 * <p/>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
class LocalAccess {
    private static Map cache = new HashMap(3);

    private LocalAccess() {
    }

    public static synchronized void check() {
        ClientHost host = null;
        try {
            host = (ClientHost)
              ServerContext.getServerContextElement(ClientHost.class);
        } catch (ServerNotActiveException e) {
            return;
        }
        if (host == null) return;
        InetAddress addr = host.getClientHost();
        Boolean ok = (Boolean)cache.get(addr);
        if (ok == null) {
            try {
                ok = Boolean.valueOf(NetworkInterface.getByInetAddress(addr) != null);
            } catch (IOException e) {
                ok = Boolean.FALSE;
            }
            cache.put(addr, ok);
        }
        if (!ok.booleanValue()) {
            throw new AccessControlException("origin is non-local host '" + addr + "'");
        }
    }
}
