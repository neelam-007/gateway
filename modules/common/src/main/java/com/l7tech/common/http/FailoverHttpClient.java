/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.http;

import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.util.IOUtils;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link GenericHttpClient} that uses a {@link FailoverStrategy} to implement failover, over top of an
 * existing GenericHttpClient. 
 */
public class FailoverHttpClient implements GenericHttpClient {
    private final GenericHttpClient client;
    private final FailoverStrategy<String> failoverStrategy;
    private final int maxFailoverAttempts;
    private final Logger logger;

    /**
     * Create a wrapped HTTP client that fails over on unsuccessful requests.  A request that gets an answer
     * with a status code of 500 is <b>NOT</b> considered unsuccessful.
     * <p>
     * If possible, callers are strongly advised to set an {@link RerunnableHttpRequest.InputStreamFactory} on the created requests, even
     * if this requires type checking and downcasting to FailoverHttpRequest.  Without an InputStreamFactory,
     * all POST request bodies will be buffered in RAM in case they need to retransmitted for failover.
     *
     * @param client   HTTP client to enhance.  Required.
     * @param failoverStrategy   strategy which must be managing servers identified as Strings containing hostnames or IP addresses.
     * @param maxFailoverAttempts maximum number of times to invoke the failover strategy for a single request.
     * @param logger   Logger to which failure messages should be recorded, or null to do no logging of failed attempts
     */ 
    public FailoverHttpClient(GenericHttpClient client, FailoverStrategy<String> failoverStrategy, int maxFailoverAttempts, Logger logger) {
        if (client == null || failoverStrategy == null) throw new NullPointerException();
        if (maxFailoverAttempts < 1) throw new IllegalArgumentException("maxFailoverAttempts must be positive");
        this.client = client;
        this.failoverStrategy = failoverStrategy;
        this.maxFailoverAttempts = maxFailoverAttempts;
        this.logger = logger;
    }

    public GenericHttpRequest createRequest(final HttpMethod method, final GenericHttpRequestParams params)
            throws GenericHttpException
    {
        return new FailoverHttpRequest(client, failoverStrategy, maxFailoverAttempts, logger, method, params);
    }

    /**
     * Identical to {@link #createRequest} except saves the caller the trouble of downcasting to
     * {@link FailoverHttpRequest} if they will be doing so anyway to set an {@link RerunnableHttpRequest.InputStreamFactory} on the
     * request.  Without an InputStreamFactory, a failover request must read and buffer the entire request body
     * in memory before making the first attempt, in caes it fails and needs to be retried.
     *
     * @param method the request method to use.  May be one of {@link HttpMethod#GET} or {@link HttpMethod#POST}.
     * @param params the request params.  Must not be null.
     * @return the HTTP request object, ready to proceed.  Never null.
     * @throws GenericHttpException if there is a configuration, network, or HTTP problem.
     */
    public FailoverHttpRequest createFailoverRequest(final HttpMethod method, final GenericHttpRequestParams params)
            throws GenericHttpException
    {
        return new FailoverHttpRequest(client, failoverStrategy, maxFailoverAttempts, logger, method, params);
    }

    /**
     * An HTTP request that is prepared to do failover.  For best results, check for instances of this and call
     * {@link #setInputStreamFactory} to prevent buffering of the entire request for failover.
     */
    public static class FailoverHttpRequest implements RerunnableHttpRequest {
        private final GenericHttpClient client;
        private final FailoverStrategy<String> failoverStrategy;
        private final int maxFailoverAttempts;
        private final Logger logger;
        private final HttpMethod method;
        private final GenericHttpRequestParams origParams;

        private RerunnableHttpRequest.InputStreamFactory inputStreamFactory = null;
        private InputStream inputStream = null;
        private String host = null;
        private List<String[]> paramList = new ArrayList<String[]>();

        private FailoverHttpRequest(GenericHttpClient client,
                            FailoverStrategy<String> failoverStrategy,
                            int maxFailoverAttempts,
                            Logger logger,
                            HttpMethod method,
                            GenericHttpRequestParams params)
        {
            this.client = client;
            this.failoverStrategy = failoverStrategy;
            this.maxFailoverAttempts = maxFailoverAttempts;
            this.logger = logger;
            this.method = method;
            this.origParams = params;
        }

        /** @param inputStreamFactory source for replacement input stream if a request needs to be resent due to failover. */
        public void setInputStreamFactory(RerunnableHttpRequest.InputStreamFactory inputStreamFactory) {
            this.inputStreamFactory = inputStreamFactory;
        }
        
        public void setInputStream(InputStream bodyInputStream) {
            this.inputStream = bodyInputStream;
        }

        @SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
        public GenericHttpResponse getResponse() throws GenericHttpException {
            URL u = this.origParams.getTargetUrl();
            GenericHttpRequestParams params = origParams;
            Throwable lastFailure = null;

            byte[] bodyBytes = null;

            for (int i = 0; i < maxFailoverAttempts; i++) {
                GenericHttpRequest request = null;
                GenericHttpResponse response = null;
                try {
                    host = failoverStrategy.selectService();
                    params.setTargetUrl(new URL(u.getProtocol(), host, u.getPort(), u.getFile()));
                    request = client.createRequest(method, params);

                    if (params.needsRequestBody(method)) {
                        if (paramList != null && paramList.size() > 0) {
                            request.addParameters(paramList);
                        } else {
                            final InputStreamFactory factory; // must find one or fail

                            if (bodyBytes != null) {
                                final byte[] body = bodyBytes;
                                factory = new InputStreamFactory() {
                                    public InputStream getInputStream() {
                                        return new ByteArrayInputStream(body);
                                    }
                                };
                            } else if (inputStreamFactory != null) {
                                factory = inputStreamFactory;
                            } else if (inputStream != null) {
                                // Need to buffer the input stream ourselves before using it up
                                PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
                                if (logger != null)
                                    logger.finer("Buffering request body");
                                try {
                                    IOUtils.copyStream(inputStream, baos);
                                    inputStream = null;
                                    bodyBytes = baos.toByteArray();
                                    final byte[] body = bodyBytes;
                                    factory = new InputStreamFactory() {
                                        public InputStream getInputStream() {
                                            return new ByteArrayInputStream(body);
                                        }
                                    };
                                } catch (IOException e) {
                                    final String msg = "Unable to read request InputStream -- failover terminated";
                                    if (logger != null)
                                        logger.log(Level.WARNING, msg, e);
                                    lastFailure = new GenericHttpException(msg);
                                    break;
                                } finally {
                                    baos.close();
                                }
                            } else {
                                // We don't have one at all.  Create an empty body.
                                if (logger != null)
                                    logger.warning("No request InputStream provided -- transmitting empty body request");
                                params.setContentLength((long) 0);
                                factory = new InputStreamFactory() {
                                    public InputStream getInputStream() {
                                        return new EmptyInputStream();
                                    }
                                };
                            }

                            if (request instanceof RerunnableHttpRequest) {
                                ((RerunnableHttpRequest)request).setInputStreamFactory(factory);
                            } else {
                                request.setInputStream(factory.getInputStream());
                            }
                        }
                    }

                    if (logger != null)
                        logger.finer("Attempting HTTP request to " + host);
                    response = request.getResponse();
                    
                    // Success!  Report success and hand off the response to our caller
                    failoverStrategy.reportSuccess(host);
                    GenericHttpResponse ret = response;
                    response = null; // avoid closing it: we are handing it off to our caller
                    return ret;
                } catch (GenericHttpException e) {
                    if (logger != null)
                        logger.log(Level.INFO,
                                   "HTTP request to " + host + " failed: " + e.getMessage(), ExceptionUtils.getDebugException(e));
                    failoverStrategy.reportFailure(host);
                    lastFailure = e;
                    /* FALLTHROUGH and try again */
                } catch (MalformedURLException e) {
                    if (logger != null)
                        logger.log(Level.INFO,
                                   "HTTP request to " + host + " failed: " + e.getMessage(), ExceptionUtils.getDebugException(e));
                    failoverStrategy.reportFailure(host);
                    lastFailure = e;
                    /* FALLTHROUGH and try again */
                } finally {
                    ResourceUtils.closeQuietly(request, response);
                }
            }

            if (lastFailure != null)
                throw new GenericHttpException("Too many failures; giving up.  Last failure: " + lastFailure.getMessage(), lastFailure);
            throw new GenericHttpException("Too many failures; giving up.");
        }

        public void addParameters(List<String[]> parameters) throws IllegalArgumentException, IllegalStateException {
            paramList.addAll(parameters);
        }

        public void close() {
        }
    }
}
