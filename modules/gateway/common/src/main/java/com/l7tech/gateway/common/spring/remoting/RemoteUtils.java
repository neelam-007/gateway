package com.l7tech.gateway.common.spring.remoting;

import javax.servlet.http.HttpServletRequest;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Callable;
import java.security.cert.X509Certificate;

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
     * Run a runnable with the given host (and, optionally, HttpServletRequest) in the context.
     *
     * @param host the remote ip
     * @param request an HttpServletRequest to make available or null
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
     * Call a callable with the given host (and, optionally, HttpServletRequest) in the context.
     *
     * @param host the remote ip.  May be null if request is provided.
     * @param request an HttpServletRequest to make available or null
     * @param callable the Callable to invoke
     * @return the value returned by the callable
     * @throws Exception if the callable threw an exception
     */
    public static <OUT> OUT callWithConnectionInfo(String host, HttpServletRequest request, Callable<OUT> callable) throws Exception {
        if (host == null && request != null) host = request.getRemoteAddr();
        String oldHost = clientHost.get();
        HttpServletRequest oldRequest = servletRequest.get();
        clientHost.set(host);
        servletRequest.set(request);
        try {
            return callable.call();
        } finally {
            clientHost.set(oldHost);
            servletRequest.set(oldRequest);
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
     * Get the X509Certificate for the HttpServletRequest associated with the current thread.
     *
     * @return the X509Certificate, or null if one could not be found.
     */
    public static X509Certificate getClientCertificate() {
        X509Certificate cert = null;

        HttpServletRequest request = servletRequest.get();
        if ( request != null ) {
            Object certObj = request.getAttribute("javax.servlet.request.X509Certificate");
            if ( certObj instanceof X509Certificate ) {
                cert = (X509Certificate) certObj;
            } else if ( certObj instanceof X509Certificate[] ) {
                X509Certificate[] certs = ( X509Certificate[]) certObj;
                if ( certs.length > 0 ) {
                    cert = certs[0];
                }
            }
        }

        return cert;
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

    /**
     * Set the facility that is associated with this thread.
     *
     * @param facility The facility.
     */
    public static void setFacility( final String facility ) {
        inboundFacility.set(facility);
    }

    /**
     * Get the facility in which this request originated.
     *
     * @return The facility or null
     */
    public static String getFacility() {
        return inboundFacility.get();
    }

    //- PRIVATE

    private static ThreadLocal<String> inboundFacility = new ThreadLocal<String>();
    private static ThreadLocal<String> clientHost = new ThreadLocal<String>();
    private static ThreadLocal<HttpServletRequest> servletRequest = new ThreadLocal<HttpServletRequest>();
}
