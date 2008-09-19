package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.gateway.common.spring.remoting.ssl.SSLTrustFailureHandler;

/**
 * Common HttpInvokerRequestExecutor methods.
 *
 * @author Steve Jones
 */
public interface ConfigurableHttpInvokerRequestExecutor {

    /**
     * Configure the host and session information.
     *
     * @param host The remote host.
     * @param port The remote port.
     * @param sessionId The session identifier to use
     */
    public void setSession(String host, int port, String sessionId);

    /**
     * Configure the trust failure handler.
     *
     * @param failureHandler The handler to use for trust failures.
     */
    public void setTrustFailureHandler(SSLTrustFailureHandler failureHandler);

}
