/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.util.HexUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link GenericHttpClient} that uses a {@link FailoverStrategy} to implement failover, over top of an
 * existing GenericHttpClient. 
 */
public class FailoverHttpClient implements GenericHttpClient {
    private final GenericHttpClient client;
    private final FailoverStrategy failoverStrategy;
    private final int maxFailoverAttempts;
    private final Logger logger;

    /**
     * Create a wrapped HTTP client that fails over on unsuccessful requests.  A request that gets an answer
     * with a status code of 500 is <b>NOT</b> considered unsuccessful.
     * <p>
     * If possible, callers are strongly advised to set an {@link InputStreamFactory} on the created requests, even
     * if this requires type checking and downcasting to FailoverHttpRequest.  Without an InputStreamFactory,
     * all POST request bodies will be buffered in RAM in case they need to retransmitted for failover.
     *
     * @param client
     * @param failoverStrategy   strategy which must be managing servers identified as Strings containing hostnames or IP addresses.
     * @param maxFailoverAttempts
     * @param logger   Logger to which failure messages should be recorded, or null to do no logging of failed attempts
     */ 
    public FailoverHttpClient(GenericHttpClient client, FailoverStrategy failoverStrategy, int maxFailoverAttempts, Logger logger) {
        if (client == null || failoverStrategy == null) throw new NullPointerException();
        if (maxFailoverAttempts < 1) throw new IllegalArgumentException("maxFailoverAttempts must be positive");
        this.client = client;
        this.failoverStrategy = failoverStrategy;
        this.maxFailoverAttempts = maxFailoverAttempts;
        this.logger = logger;
    }

    public GenericHttpRequest createRequest(final GenericHttpMethod method, final GenericHttpRequestParams params)
            throws GenericHttpException
    {
        return new FailoverHttpRequest(client, failoverStrategy, maxFailoverAttempts, logger, method, params);
    }

    /**
     * Identical to {@link #createRequest} except saves the caller the trouble of downcasting to
     * {@link FailoverHttpRequest} if they will be doing so anyway to set an {@link InputStreamFactory} on the
     * request.  Without an InputStreamFactory, a failover request must read and buffer the entire request body
     * in memory before making the first attempt, in caes it fails and needs to be retried.
     *
     * @param method
     * @param params
     * @return
     * @throws GenericHttpException
     */
    public FailoverHttpRequest createFailoverRequest(final GenericHttpMethod method, final GenericHttpRequestParams params)
            throws GenericHttpException
    {
        return new FailoverHttpRequest(client, failoverStrategy, maxFailoverAttempts, logger, method, params);
    }

    /** Produces an InputStream.  Provide one of these to {@link FailoverHttpRequest} to avoid request buffering. */
    public static interface InputStreamFactory {
        /** @return an InputStream ready to play back the request body for a POST request. */
        InputStream getInputStream();
    }

    /**
     * An HTTP request that is prepared to do failover.  For best results, check for instances of this and call
     * {@link #setInputStreamFactory} to prevent buffering of the entire request for failover.
     */
    public static class FailoverHttpRequest implements GenericHttpRequest {
        private final GenericHttpClient client;
        private final FailoverStrategy failoverStrategy;
        private final int maxFailoverAttempts;
        private final Logger logger;
        private final GenericHttpMethod method;
        private final GenericHttpRequestParams origParams;

        private InputStreamFactory inputStreamFactory = null;
        private InputStream inputStream = null;
        private String host = null;

        private FailoverHttpRequest(GenericHttpClient client,
                            FailoverStrategy failoverStrategy,
                            int maxFailoverAttempts,
                            Logger logger,
                            GenericHttpMethod method,
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
        public void setInputStreamFactory(InputStreamFactory inputStreamFactory) {
            this.inputStreamFactory = inputStreamFactory;
        }
        
        public void setInputStream(InputStream bodyInputStream) {
            this.inputStream = bodyInputStream;
        }

        public GenericHttpResponse getResponse() throws GenericHttpException {
            URL u = this.origParams.getTargetUrl();
            GenericHttpRequestParamsImpl params = new GenericHttpRequestParamsImpl(origParams);
            Throwable lastFailure = null;

            byte[] bodyBytes = null;

            for (int i = 0; i < maxFailoverAttempts; i++) {
                GenericHttpRequest request = null;
                GenericHttpResponse response = null;
                try {
                    host = (String)failoverStrategy.selectService();
                    params.setTargetUrl(new URL(u.getProtocol(), host, u.getPort(), u.getFile()));
                    request = client.createRequest(method, params);

                    if (method.needsRequestBody()) {
                        final InputStream bis; // must find one or fail

                        if (bodyBytes != null) {
                            bis = new ByteArrayInputStream(bodyBytes);
                        } else {
                            if (inputStream == null && inputStreamFactory == null) {
                                // We don't have one at all.  Create an empty body.
                                logger.warning("No request InputStream provided -- transmitting empty body request");
                                params.setContentLength(new Long(0));
                                bis = new EmptyInputStream();
                            } else if (inputStreamFactory == null) {
                                // inputStreamFactory == null, inputStream != null

                                // Need to buffer the input stream ourselves before using it up
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                logger.finer("Buffering request body");
                                try {
                                    HexUtils.copyStream(inputStream, baos);
                                } catch (IOException e) {
                                    final String msg = "Unable to read request InputStream -- failover terminated";
                                    logger.log(Level.WARNING, msg, e);
                                    lastFailure = new GenericHttpException(msg);
                                    break;
                                }
                                bodyBytes = baos.toByteArray();
                                bis = new ByteArrayInputStream(bodyBytes);
                                inputStream = null;
                             } else if (inputStream == null) {
                                // inputStream == null, inputStreamFactory != null

                                // Can just get a new input stream for this attempt
                                bis = inputStreamFactory.getInputStream();
                            } else {
                                // inputStream != null, inputStreamFactory != null

                                // Use up the existing InputStream; we'll get a new one from the factory if needed
                                bis = inputStream;
                                inputStream = null;
                            }
                        }

                        request.setInputStream(bis);
                    }

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
                                   "HTTP request to " + host + " failed: " + e.getMessage(), e);
                    failoverStrategy.reportFailure(host);
                    lastFailure = e;
                    /* FALLTHROUGH and try again */
                } catch (MalformedURLException e) {
                    if (logger != null)
                        logger.log(Level.INFO,
                                   "HTTP request to " + host + " failed: " + e.getMessage(), e);
                    failoverStrategy.reportFailure(host);
                    lastFailure = e;
                    /* FALLTHROUGH and try again */
                } finally {
                    if (request != null) try { request.close(); } catch (Throwable t) {}
                    if (response != null) try { response.close(); } catch (Throwable t) {}
                }
            }

            if (lastFailure != null)
                throw new GenericHttpException("Too many failures; giving up.  Last failure: " + lastFailure.getMessage(), lastFailure);
            throw new GenericHttpException("Too many failures; giving up.");
        }

        public void close() {
        }
    }
}
