package com.l7tech.spring.remoting;

import javax.servlet.http.HttpServletRequest;
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
    public static void runWithConnectionInfo(String host, HttpServletRequest request, Runnable runnable) {
        clientHost.set(host);
        servletRequest.set(request);
        try {
            runnable.run();
        }
        finally {
            clientHost.set(null);
            servletRequest.set(null);
        }
    }

    /**
     * Get the HttpServletRequest associated with the current thread.
     *
     * @return the HttpServletRequest set for this thread, or null if one could not be found.
     */
    public static HttpServletRequest getHttpServletRequest() {
        return servletRequest.get();
    }

    /**
     * Get the remote host for the current thread.
     *
     * @return the remote ip (or null)
     * @throws java.rmi.server.ServerNotActiveException if no thread-local host is set, and we are unable to find
     *                                                  one by invoking {@link UnicastRemoteObject#getClientHost}.
     */
    public static String getClientHost() throws ServerNotActiveException {
        String host = clientHost.get();

        if (host == null) {
            host = UnicastRemoteObject.getClientHost();
        }

        return host;
    }

    //- PRIVATE

    private static ThreadLocal<String> clientHost = new ThreadLocal<String>();
    private static ThreadLocal<HttpServletRequest> servletRequest = new ThreadLocal<HttpServletRequest>();
}
