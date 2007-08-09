package com.l7tech.spring.remoting.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import org.springframework.remoting.httpinvoker.CommonsHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.protocol.Protocol;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.spring.remoting.rmi.ssl.SSLTrustFailureHandler;

/**
 * Client side HTTP request executor.
 *
 * <p>This sets the host/port info and adds the session identifier to urls.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SecureHttpInvokerRequestExecutor extends CommonsHttpInvokerRequestExecutor implements ConfigurableHttpInvokerRequestExecutor {

    //- PUBLIC

    public SecureHttpInvokerRequestExecutor() {
        super();
        this.userAgent = null;
        this.hostSubstitutionPattern = Pattern.compile(HOST_REGEX);
    }

    public SecureHttpInvokerRequestExecutor(HttpClient httpClient) {
        this(httpClient, null);
    }

    public SecureHttpInvokerRequestExecutor(HttpClient httpClient, String userAgent) {
        super(httpClient);
        this.userAgent = userAgent;
        this.hostSubstitutionPattern = Pattern.compile(HOST_REGEX);
    }

    public void setSession(String host, int port, String sessionId) {
        synchronized (lock) {
            this.host = host;
            this.port = port;
            this.sessionId = sessionId;
        }
    }

    public void setTrustFailureHandler(SSLTrustFailureHandler failureHandler) {
        synchronized (lock) {
            this.trustFailureHandler = failureHandler;
        }
    }

    public void clearSessionIfMatches(String sessionId) {
        synchronized (lock) {
            if (sessionId != null && sessionId.equals(this.sessionId)) {
                this.sessionId = null;
                this.host = null;
                this.port = 0;
            }
        }
    }

    //- PROTECTED

    protected InputStream getResponseBody(HttpInvokerClientConfiguration config, PostMethod postMethod) throws IOException {
        InputStream inputStream = super.getResponseBody(config, postMethod);

        Header contentEncodingHeader = postMethod.getResponseHeader(HttpConstants.HEADER_CONTENT_ENCODING);
        if (contentEncodingHeader != null) {
            String encoding = contentEncodingHeader.getValue();
            if (encoding != null) {
                if (ENCODING_GZIP.equals(encoding)) {
                    logger.finest("Using gzip compression.");
                    inputStream = new GZIPInputStream(inputStream);
                }
                else {
                    logger.fine("Unknown content encoding '"+encoding+"'.");
                }
            }
        }

        return inputStream;
    }

    protected PostMethod createPostMethod(HttpInvokerClientConfiguration config) throws IOException {
        PostMethod postMethod = super.createPostMethod(new HttpInvokerClientConfigurationImpl(config));

        if (userAgent != null)
            postMethod.addRequestHeader(HttpConstants.HEADER_USER_AGENT, userAgent);

        postMethod.addRequestHeader(HttpConstants.HEADER_ACCEPT_ENCODING, ENCODING_GZIP);

        return postMethod;
    }

    protected void validateResponse(HttpInvokerClientConfiguration httpInvokerClientConfiguration, PostMethod postMethod) throws IOException {
        try {
            super.validateResponse(httpInvokerClientConfiguration, postMethod);
        }
        catch (IOException ioe) {
            // HACK: detect ssg shutdown 503 error code using
            // the message text and create a dummy throwable instead
            if (ioe instanceof HttpException) {
                if (ioe.getMessage() != null &&
                    ioe.getMessage().contains("Did not receive successful HTTP response: status code = 503, status message = [This application is not currently available]")) {
                    logger.log(Level.WARNING, "Replacing HttpException with (dummy) SocketException.", ioe);
                    throw new SocketException("Dummy cause since HttpClient doesn't nest exceptions.");
                }
                else throw ioe;
            }
            else throw ioe;
        }
    }

    protected void executePostMethod(HttpInvokerClientConfiguration config, HttpClient httpClient, PostMethod postMethod) throws IOException {
        SSLTrustFailureHandler trustFailureHandler;
        String host;
        int port;
        String sessionId;
        synchronized (lock) {
            trustFailureHandler = this.trustFailureHandler;
            host = this.host;
            port = this.port;
            sessionId = this.sessionId;
        }

        if (host == null) {
            throw new CausedIOException("Not logged in");
        }

        SecureHttpClient.setTrustFailureHandler(trustFailureHandler);
        Protocol protocol = httpClient.getHostConfiguration().getProtocol();
        HostConfiguration hostConfiguration = new HostConfiguration();
        hostConfiguration.setHost(host, port, protocol);
        postMethod.addRequestHeader("X-Layer7-SessionId", sessionId);
        httpClient.executeMethod(hostConfiguration, postMethod);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SecureHttpInvokerRequestExecutor.class.getName());

    /**
     * Regex for replacing the host name in the url with the host[:port] from the login dialog.
     */
    private static final String HOST_REGEX = "^http[s]?\\://[a-zA-Z_\\-0-9\\.\\:]{1,1024}";
    private static final String ENCODING_GZIP = "gzip";

    private final Object lock = new Object();
    private final String userAgent;
    private SSLTrustFailureHandler trustFailureHandler;
    private String host;
    private int port;
    private String sessionId;
    private Pattern hostSubstitutionPattern;

    private class HttpInvokerClientConfigurationImpl implements HttpInvokerClientConfiguration {
        private final HttpInvokerClientConfiguration delegate;

        private HttpInvokerClientConfigurationImpl(final HttpInvokerClientConfiguration config) {
            delegate = config;
        }

        public String getCodebaseUrl() {
            return decorate(delegate.getCodebaseUrl());
        }

        public String getServiceUrl() {
            return decorate(delegate.getServiceUrl());
        }

        /**
         * Turn the URL into a relative URL and add the session id if there is one.
         */
        private String decorate(final String url) {
            // switch in the correct host/port
            Matcher matcher = hostSubstitutionPattern.matcher(url);
            String decoratedUrl = matcher.replaceFirst("");

            return decoratedUrl;
        }
    }
}
