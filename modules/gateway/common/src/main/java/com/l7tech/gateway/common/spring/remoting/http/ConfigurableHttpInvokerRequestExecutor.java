package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.gateway.common.spring.remoting.ssl.SSLTrustFailureHandler;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

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
     * @param sessionId The session identifier to use, or null if not set yet
     * @param block code to run with this session information in place.
     */
    public <R,E extends Throwable> R doWithSession(String host, int port, @Nullable String sessionId, Functions.NullaryThrows<R, E> block) throws E;

    /**
     * Run the specified callable with the trust failure handler set to the specified value.
     *
     * @param failureHandler The handler to use for trust failures.
     * @param block code to run with this failure handler in place.
     * @return the result of the callable.
     */
    public <R,E extends Throwable> R doWithTrustFailureHandler(SSLTrustFailureHandler failureHandler, Functions.NullaryThrows<R, E> block) throws E;

}
