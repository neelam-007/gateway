package com.l7tech.spring.remoting.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.net.HttpURLConnection;
import java.io.IOException;

import org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;

import com.l7tech.spring.remoting.rmi.ssl.SSLTrustFailureHandler;

/**
 * Extension of the Spring SimpleHttpInvokerRequestExecutor for setting host/session.
 *
 * @author Steve Jones
 */
public class SimpleBrowserHttpInvokerRequestExecutor extends SimpleHttpInvokerRequestExecutor implements ConfigurableHttpInvokerRequestExecutor {

    //- PUBLIC

    public SimpleBrowserHttpInvokerRequestExecutor() {
        this.hostSubstitutionPattern = Pattern.compile(HOST_REGEX);

    }

    public void setSession(String host, int port, String sessionId) {
        synchronized (this) {
            this.host = host;
            this.port = port;
            this.sessionId = sessionId;
        }
    }

    public void setTrustFailureHandler(SSLTrustFailureHandler failureHandler) {
    }

    public void clearSessionIfMatches(String sessionId) {
        synchronized (this) {
            if (sessionId != null && sessionId.equals(this.sessionId)) {
                this.sessionId = null;
                this.host = null;
                this.port = 0;
            }
        }
    }

    //- PROTECTED
        
    protected HttpURLConnection openConnection(HttpInvokerClientConfiguration config) throws IOException {
        HttpURLConnection connection = super.openConnection(new HttpInvokerClientConfigurationImpl(config));
        synchronized (this) {
            connection.setRequestProperty("X-Layer7-SessionId", sessionId);
        }

        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        return connection;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SimpleBrowserHttpInvokerRequestExecutor.class.getName());
    private static final String HOST_REGEX = "(?<=^http[s]?\\://)[a-zA-Z_\\-0-9\\.\\:]{1,1024}";

    private Pattern hostSubstitutionPattern;
    private String host;
    private int port;
    private String sessionId;

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
            String decoratedUrl = matcher.replaceFirst(host + ":" + port);

            return decoratedUrl;
        }
    }
}
