package com.l7tech.spring.remoting;

import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Remoting utility methods.
 *
 * <p>You would not usually create instances of this class.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class RemoteUtils {

    //- PUBLIC

    /**
     * Set the remote host for the current thread.
     *
     * @param host the remote ip
     */
    public static void setClientHost(String host) {
        clientHost.set(host);
    }

    /**
     * Get the remote host for the current thread.
     *
     * @return the remote ip (or null)
     */
    public static String getClientHost() throws ServerNotActiveException {
        String host = (String) clientHost.get();

        if (host == null) {
            host = UnicastRemoteObject.getClientHost();
        }

        return host;
    }

    //- PRIVATE

    private static ThreadLocal clientHost = new ThreadLocal();
}
