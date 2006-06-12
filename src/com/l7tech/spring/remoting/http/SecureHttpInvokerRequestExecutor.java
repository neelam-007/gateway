package com.l7tech.spring.remoting.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.springframework.remoting.httpinvoker.CommonsHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HostConfiguration;

/**
 * Client side HTTP request executor.
 *
 * <p>This sets the host/port info and adds the session identifier to urls.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SecureHttpInvokerRequestExecutor extends CommonsHttpInvokerRequestExecutor {

    //- PUBLIC

    public SecureHttpInvokerRequestExecutor() {
        super();
        hostSubstitutionPattern = Pattern.compile(HOST_REGEX);
    }

    public SecureHttpInvokerRequestExecutor(HttpClient httpClient) {
        super(httpClient);
        hostSubstitutionPattern = Pattern.compile(HOST_REGEX);
    }

    public void setSession(String host, int port, String sessionId) {
        this.host = host;
        this.port = port;
        this.sessionId = sessionId;
    }

    //- PROTECTED

    protected void executePostMethod(HttpInvokerClientConfiguration config, HttpClient httpClient, PostMethod postMethod) throws IOException {
        HostConfiguration hostConfiguration = new HostConfiguration(httpClient.getHostConfiguration());
        hostConfiguration.setHost(postMethod.getURI().getHost(),
                                  postMethod.getURI().getPort(),
                                  httpClient.getHostConfiguration().getProtocol());

        postMethod.setHostConfiguration(hostConfiguration);

        super.executePostMethod(config, httpClient, postMethod);
    }

    protected PostMethod createPostMethod(HttpInvokerClientConfiguration config) throws IOException {
        return super.createPostMethod(new HttpInvokerClientConfigurationImpl(config));
    }

    //- PRIVATE

    /**
     * Regex for replacing the host name in the url with the host[:port] from the login dialog.
     */
    private static final String HOST_REGEX = "(?<=^http[s]?\\://)[a-zA-Z_\\-0-9\\.\\:]{1,1024}";

    private String host;
    private int port;
    private String sessionId;
    private Pattern hostSubstitutionPattern;

    private class HttpInvokerClientConfigurationImpl implements HttpInvokerClientConfiguration {
        private final HttpInvokerClientConfiguration delegate;

        private HttpInvokerClientConfigurationImpl(HttpInvokerClientConfiguration config) {
            delegate = config;
        }

        public String getCodebaseUrl() {
            return decorate(delegate.getCodebaseUrl());
        }

        public String getServiceUrl() {
            return decorate(delegate.getServiceUrl());
        }

        private String decorate(String url) {
            // switch in the correct host/port
            Matcher matcher = hostSubstitutionPattern.matcher(url);
            String decoratedUrl = matcher.replaceFirst(host + ":" + port);

            // ensure HTTPS even if HTTP is specified
            int protEnd = decoratedUrl.indexOf(':');
            if (protEnd > 0) {
                decoratedUrl = "https" + decoratedUrl.substring(protEnd);
            }

            // add session id if we have one
            if (sessionId != null) {
                String encodedId = sessionId;
                try {
                    encodedId = URLEncoder.encode(sessionId, "iso-8859-1");
                }
                catch (UnsupportedEncodingException uee) {
                    logger.warn("Unable to URL encode session identifier, encoding not supported '"+
                            uee.getMessage()+"'.");
                }
                decoratedUrl += "?sessionId="+ encodedId;
            }

            return decoratedUrl;
        }
    }
}
