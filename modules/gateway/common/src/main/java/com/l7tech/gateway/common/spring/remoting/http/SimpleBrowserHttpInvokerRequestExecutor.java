package com.l7tech.gateway.common.spring.remoting.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

import org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.support.RemoteInvocationResult;

import com.l7tech.gateway.common.spring.remoting.ssl.SSLTrustFailureHandler;

/**
 * Extension of the Spring SimpleHttpInvokerRequestExecutor for setting host/session.
 *
 * @author Steve Jones
 */
public class SimpleBrowserHttpInvokerRequestExecutor extends SimpleHttpInvokerRequestExecutor implements ConfigurableHttpInvokerRequestExecutor {

    //- PUBLIC

    public SimpleBrowserHttpInvokerRequestExecutor() {
        this.hostSubstitutionPattern = Pattern.compile(HOST_REGEX);
        this.sessionInfoHolder = new SessionSupport();

    }

    public void setSession(String host, int port, String sessionId) {
        SessionSupport.SessionInfo info = sessionInfoHolder.getSessionInfo();
        info.host = host;
        info.port = port;
        info.sessionId = sessionId;
    }

    public void setTrustFailureHandler(SSLTrustFailureHandler failureHandler) {
    }

    //- PROTECTED
        
    protected HttpURLConnection openConnection(HttpInvokerClientConfiguration config) throws IOException {
        SessionSupport.SessionInfo info = sessionInfoHolder.getSessionInfo();

        HttpURLConnection connection = super.openConnection(new HttpInvokerClientConfigurationImpl(config, info.host, info.port));
        connection.setRequestProperty("X-Layer7-SessionId", info.sessionId);

        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        return connection;
    }

    @Override
    protected RemoteInvocationResult doExecuteRequest(HttpInvokerClientConfiguration httpInvokerClientConfiguration, ByteArrayOutputStream byteArrayOutputStream) throws IOException, ClassNotFoundException {
        return super.doExecuteRequest(httpInvokerClientConfiguration, byteArrayOutputStream);    //To change body of overridden methods use File | Settings | File Templates.
    }

    //- PRIVATE

    private static final String HOST_REGEX = "(?<=^http[s]?\\://)[a-zA-Z_\\-0-9\\.\\:]{1,1024}";

    private Pattern hostSubstitutionPattern;
    private final SessionSupport sessionInfoHolder;

     private class HttpInvokerClientConfigurationImpl implements HttpInvokerClientConfiguration {
        private final HttpInvokerClientConfiguration delegate;
        private final String host;
        private final int port;

        private HttpInvokerClientConfigurationImpl(final HttpInvokerClientConfiguration config,
                                                   final String host,
                                                   final int port) {
            this.delegate = config;
            this.host = host;
            this.port = port;
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
            return matcher.replaceFirst(host + ":" + port);
        }
    }
}
