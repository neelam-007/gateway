package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.gateway.common.admin.TimeoutRuntimeException;
import com.l7tech.gateway.common.spring.remoting.ssl.SSLTrustFailureHandler;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.remoting.httpinvoker.AbstractHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Client side HTTP request executor.
 * <p/>
 * <p>This sets the host/port info and adds the session identifier to urls.</p>
 *
 * @author Steve Jones, $Author: steve $
 * @version $Revision: 27392 $
 */
public class SecureHttpComponentsHttpInvokerRequestExecutor extends AbstractHttpInvokerRequestExecutor implements ConfigurableHttpInvokerRequestExecutor {

    private HttpClient httpClient;

    //- PUBLIC

    public SecureHttpComponentsHttpInvokerRequestExecutor() {
        super();
        this.userAgent = null;
        this.hostSubstitutionPattern = Pattern.compile(HOST_REGEX);
        this.sessionInfoHolder = new SessionSupport();
    }

    public SecureHttpComponentsHttpInvokerRequestExecutor(HttpClient httpClient) {
        this(httpClient, null);
    }

    public SecureHttpComponentsHttpInvokerRequestExecutor(HttpClient httpClient, String userAgent) {
        this.httpClient = httpClient;
        this.userAgent = userAgent;
        this.hostSubstitutionPattern = Pattern.compile(HOST_REGEX);
        this.sessionInfoHolder = new SessionSupport();
    }

    @Override
    protected RemoteInvocationResult doExecuteRequest(HttpInvokerClientConfiguration config, ByteArrayOutputStream baos) throws Exception {
        HttpPost postMethod = createPostMethod(config);
        setRequestBody(postMethod, baos);
        HttpResponse response = executePostMethod(postMethod);
        validateResponse(response);
        InputStream responseBody = getResponseBody(response);
        return readRemoteInvocationResult(responseBody, config.getCodebaseUrl());
    }

    protected void setRequestBody(HttpPost httpPost, ByteArrayOutputStream baos) throws IOException {

        ByteArrayEntity entity = new ByteArrayEntity(baos.toByteArray());
        entity.setContentType(getContentType());
        httpPost.setEntity(entity);
    }


    @Override
    public <R, E extends Throwable> R doWithSession(String host, int port, String sessionId, Functions.NullaryThrows<R, E> block) throws E {
        SessionSupport.SessionInfo info = sessionInfoHolder.getSessionInfo();
        final String oldHost = info.host;
        final int oldPort = info.port;
        final String oldSessionId = info.sessionId;

        try {
            info.host = host;
            info.port = port;
            info.sessionId = sessionId;

            return block.call();
        } finally {
            info.host = oldHost;
            info.port = oldPort;
            info.sessionId = oldSessionId;
        }
    }

    public void setDefaultTrustFailureHandler(SSLTrustFailureHandler failureHandler) {
        synchronized (lock) {
            this.trustFailureHandler = failureHandler;
            if (httpClient instanceof SecureHttpComponentsClient) {
                //reset connections
                ((SecureHttpComponentsClient) httpClient).resetConnection();
            }
        }
    }

    @Override
    public <R, E extends Throwable> R doWithTrustFailureHandler(SSLTrustFailureHandler failureHandler, Functions.NullaryThrows<R, E> block) throws E {
        final SSLTrustFailureHandler oldFailureHandler;
        synchronized (lock) {
            oldFailureHandler = this.trustFailureHandler;
            this.trustFailureHandler = failureHandler;
            if (httpClient instanceof SecureHttpComponentsClient) {
                //reset connections
                ((SecureHttpComponentsClient) httpClient).resetConnection();
            }
        }

        try {
            return block.call();
        } finally {
            synchronized (lock) {
                this.trustFailureHandler = oldFailureHandler;
                if (httpClient instanceof SecureHttpComponentsClient) {
                    //reset connections
                    ((SecureHttpComponentsClient) httpClient).resetConnection();
                }
            }
        }
    }

    protected InputStream getResponseBody(HttpResponse response) throws IOException {

        InputStream inputStream = response.getEntity().getContent();

        Header contentEncodingHeader = response.getFirstHeader(HttpConstants.HEADER_CONTENT_ENCODING);
        if (contentEncodingHeader != null) {
            String encoding = contentEncodingHeader.getValue();
            if (encoding != null) {
                if (ENCODING_GZIP.equals(encoding)) {
                    logger.finest("Using gzip compression.");
                    inputStream = new GZIPInputStream(inputStream);
                } else {
                    logger.fine("Unknown content encoding '" + encoding + "'.");
                }
            }
        }

        return inputStream;
    }

    protected HttpPost createPostMethod(HttpInvokerClientConfiguration config) throws IOException {
        config = new HttpInvokerClientConfigurationImpl(config);
        HttpPost postMethod = new HttpPost(config.getServiceUrl());
        LocaleContext locale = LocaleContextHolder.getLocaleContext();
        if (locale != null) {
            postMethod.addHeader(HTTP_HEADER_ACCEPT_LANGUAGE, StringUtils.toLanguageTag(locale.getLocale()));
        }

        if (userAgent != null)
            postMethod.addHeader(HttpConstants.HEADER_USER_AGENT, userAgent);

        postMethod.addHeader(HttpConstants.HEADER_ACCEPT_ENCODING, ENCODING_GZIP);

        return postMethod;
    }

    protected void validateResponse(HttpResponse response) throws IOException {
        try {
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() >= 300) {
                throw new NoHttpResponseException (
                        "Did not receive successful HTTP response: status code = " + status.getStatusCode() +
                                ", status message = [" + status.getReasonPhrase() + "]");
            }
        } catch (IOException ioe) {
            // HACK: detect ssg shutdown 503 error code using
            // the message text and create a dummy throwable instead
            if (ioe instanceof NoHttpResponseException) {
                if (ioe.getMessage() != null &&
                        (ioe.getMessage().contains("Did not receive successful HTTP response: status code = 503, status message = [") ||
                                ioe.getMessage().contains("Did not receive successful HTTP response: status code = 404, status message = ["))) {
                    logger.log(Level.WARNING, "Replacing HttpException with (dummy) SocketException: " + ExceptionUtils.getMessage(ioe), ExceptionUtils.getDebugException(ioe));
                    throw new SocketException("Dummy cause since HttpClient doesn't nest exceptions.");
                } else throw ioe;
            } else throw ioe;
        }
    }

    protected HttpResponse executePostMethod(HttpPost postMethod) throws IOException {
        SSLTrustFailureHandler trustFailureHandler;
        synchronized (lock) {
            trustFailureHandler = this.trustFailureHandler;
        }

        SessionSupport.SessionInfo info = sessionInfoHolder.getSessionInfo();
        if (info.host == null) {
            throw new CausedIOException("Not logged in");
        }

        SecureHttpComponentsClient.setTrustFailureHandler(trustFailureHandler);
        HttpHost hostConfiguration = new HttpHost(info.host, info.port, "https");
        postMethod.addHeader("X-Layer7-SessionId", info.sessionId);
        try {
            return httpClient.execute(hostConfiguration, postMethod);
        } catch (SocketTimeoutException ex) {
            throw new TimeoutRuntimeException(ExceptionUtils.getMessage(ex));
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SecureHttpComponentsHttpInvokerRequestExecutor.class.getName());

    /**
     * Regex for replacing the host name in the url with the host[:port] from the login dialog.
     */
    private static final String HOST_REGEX = "^http[s]?\\://[a-zA-Z_\\-0-9\\.\\:]{1,1024}";
    private static final String ENCODING_GZIP = "gzip";

    private final Object lock = new Object();
    private final String userAgent;
    private SSLTrustFailureHandler trustFailureHandler;
    private final SessionSupport sessionInfoHolder;
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
            return matcher.replaceFirst("");
        }
    }
}
