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
     * Run a runnable with the given host in the context.
     *
     * @param host the remote ip
     * @param runnable the runnable to run
     */
    public static void runWithClientHost(String host, Runnable runnable) {
        clientHost.set(host);
        try {
            runnable.run();
        }
        finally {
            clientHost.set(null);
        }
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
