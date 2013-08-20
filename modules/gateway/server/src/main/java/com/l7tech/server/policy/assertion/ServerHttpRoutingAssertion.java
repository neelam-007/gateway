package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.*;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.prov.apache.components.ClientConnectionManagerFactory;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.common.io.*;
import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.StickyFailoverStrategy;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.kerberos.KerberosClient;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.DefaultStashManagerFactory;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.server.security.kerberos.KerberosRoutingClient;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.server.util.HttpForwardingRuleEnforcer;
import com.l7tech.server.util.IdentityBindingHttpClientFactory;
import com.l7tech.util.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.jaaslounge.decoding.kerberos.KerberosEncData;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.wsdl.WSDLException;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

/**
 * Server-side implementation of HTTP routing assertion.
 *
 * <p>Related function specifications:
 * <ul>
 *  <li><a href="http://sarek.l7tech.com/mediawiki/index.php?title=XML_Variables">XML Variables</a> (4.3)
 * </ul>
 */
public final class ServerHttpRoutingAssertion extends AbstractServerHttpRoutingAssertion<HttpRoutingAssertion> {

    //Define the failed reason code.
    public static final int HOST_NOT_FOUND = -1; // The Host can not be reached
    public static final int BAD_URL = -2;  // the URL is incorrect.  Most times this will mean the server name contains invalid characters.
    public static final int CONNECTION_TIMEOUT = -3;  // this will indicate connecting to a host timed out.  Thus, no ACK was ever received for the first SYN
    public static final int READ_TIMEOUT = -4;
    public static final int UNDEFINED = -5;

    public static final String USER_AGENT = HttpConstants.HEADER_USER_AGENT;
    public static final String HOST = HttpConstants.HEADER_HOST;

    private final Config config;
    private final SignerInfo senderVouchesSignerInfo;
    private final GenericHttpClientFactory httpClientFactory;
    private final StashManagerFactory stashManagerFactory;
    private final HostnameVerifier hostnameVerifier;
    private final FailoverStrategy<String> failoverStrategy;
    private final String[] varNames;
    private final int maxFailoverAttempts;
    private final SSLSocketFactory socketFactory;
    private final boolean urlUsesVariables;
    private final URL protectedServiceUrl;
    private boolean customURLList;
    private SSLContext sslContext = null;

    public ServerHttpRoutingAssertion(HttpRoutingAssertion assertion, ApplicationContext ctx) throws PolicyAssertionException {
        super(assertion, ctx);

        if ((assertion.getNtlmHost() != null || assertion.isKrbDelegatedAuthentication()) && assertion.getProxyHost() != null) {
            throw new PolicyAssertionException(assertion, "NTLM and Kerberos delegated authentication are not supported when an HTTP proxy is configured");
        }

        config = ctx.getBean( "serverConfig", Config.class );

        customURLList = assertion.getCustomURLs() != null && assertion.getCustomURLs().length > 0;

        // remember if we need to resolve the url at runtime
        if (! customURLList) {
            String tmp = assertion.getProtectedServiceUrl();
            if (tmp != null) {
                urlUsesVariables = tmp.indexOf("${") > -1;
            } else {
                logger.info("this http routing assertion has null url");
                urlUsesVariables = false;
            }
            if (urlUsesVariables || assertion.getProtectedServiceUrl()==null) {
                protectedServiceUrl = null;
            } else {
                URL url = null;
                try {
                    url = new URL(assertion.getProtectedServiceUrl());
                } catch (MalformedURLException murle) {
                    logger.log(Level.WARNING, "Invalid protected service URL: " + murle.getMessage(), ExceptionUtils.getDebugException(murle));
               }
                protectedServiceUrl = url;
            }
        } else {
            urlUsesVariables = false;
            protectedServiceUrl = null;
        }

        hostnameVerifier = applicationContext.getBean("hostnameVerifier", HostnameVerifier.class);
        SignerInfo signerInfo;
        SSLSocketFactory sslSocketFactory;
        try {
            signerInfo = ServerAssertionUtils.getSignerInfo(ctx, assertion);

            final KeyManager[] keyManagers;
            if (assertion.isUsesNoKey()) {
                keyManagers = null;
            } else if (!assertion.isUsesDefaultKeyStore()) {
                X509Certificate[] certChain = signerInfo.getCertificateChain();
                PrivateKey privateKey = signerInfo.getPrivate();
                keyManagers = new KeyManager[] { new SingleCertX509KeyManager(certChain, privateKey) };
            } else {
                final DefaultKey ku = (DefaultKey)applicationContext.getBean("defaultKey");
                keyManagers = ku.getSslKeyManagers();
            }
            sslContext = SSLContext.getInstance("TLS");

            final Goid[] tlsTrustedCertOids = assertion.getTlsTrustedCertGoids();
            Set<Goid> customTrustedCerts = tlsTrustedCertOids == null ? null : new HashSet<Goid>(Arrays.asList(tlsTrustedCertOids));

            final SslClientTrustManager sslClientTrustManager = applicationContext.getBean("routingTrustManager", SslClientTrustManager.class);
            final X509TrustManager trustManager = customTrustedCerts != null
                    ? sslClientTrustManager.createTrustManagerWithCustomTrustedCerts(customTrustedCerts)
                    : sslClientTrustManager;

            sslContext.init(keyManagers, new TrustManager[]{trustManager}, null);
            final int timeout = ConfigFactory.getIntProperty( HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT, HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT );
            sslContext.getClientSessionContext().setSessionTimeout(timeout);
            sslSocketFactory = sslContext.getSocketFactory();

            if (assertion.getTlsCipherSuites() != null || assertion.getTlsVersion() != null) {
                String[] tlsVersions = assertion.getTlsVersion() == null ? null : new String[] {assertion.getTlsVersion()};
                String[] tlsCipherSuites = assertion.getTlsCipherSuites() == null ? null : assertion.getTlsCipherSuites().trim().split("\\s*,\\s*");
                sslSocketFactory = SSLSocketFactoryWrapper.wrapAndSetTlsVersionAndCipherSuites(sslSocketFactory, tlsVersions, tlsCipherSuites);
            }

        } catch (Exception e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logAndAudit(AssertionMessages.HTTPROUTE_SSL_INIT_FAILED, null, ExceptionUtils.getDebugException(e));
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Failed to initialize SSL context: " + ExceptionUtils.getMessage(e), null);
            signerInfo = null;
            sslSocketFactory = null;
            sslContext = null;
        }
        this.senderVouchesSignerInfo = signerInfo;
        this.socketFactory = sslSocketFactory;

        GenericHttpClientFactory httpClientFactory;
        try {
            httpClientFactory = makeHttpClientFactory();
        } catch (Exception e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Could not create HTTP client factory." }, e);
            httpClientFactory = new IdentityBindingHttpClientFactory();
        }
        this.httpClientFactory = httpClientFactory;

        StashManagerFactory smFactory;
        try {
            smFactory = applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not create stash manager factory.", e);
            smFactory = DefaultStashManagerFactory.getInstance();
        }
        stashManagerFactory = smFactory;

        final String[] addrs = assertion.getCustomIpAddresses();
        if (addrs != null && addrs.length > 0 && areValidUrlHostnames(addrs)) {
            final String stratName = assertion.getFailoverStrategyName();
            FailoverStrategy<String> strat;
            try {
                strat = FailoverStrategyFactory.createFailoverStrategy(stratName, addrs);
            } catch (IllegalArgumentException e) {
                logAndAudit(AssertionMessages.HTTPROUTE_BAD_STRATEGY_NAME, new String[] { stratName }, e);
                strat = new StickyFailoverStrategy<String>(addrs);
            }
            failoverStrategy = AbstractFailoverStrategy.makeSynchronized(strat);
            maxFailoverAttempts = addrs.length;
        } else if (customURLList) {
            final String stratName = assertion.getFailoverStrategyName();
            FailoverStrategy<String> strat;
            try {
                strat = FailoverStrategyFactory.createFailoverStrategy(stratName, assertion.getCustomURLs());
            } catch (IllegalArgumentException e) {
                logAndAudit(AssertionMessages.HTTPROUTE_BAD_STRATEGY_NAME, new String[] { stratName }, e);
                strat = new StickyFailoverStrategy<String>(assertion.getCustomURLs());
            }
            failoverStrategy = AbstractFailoverStrategy.makeSynchronized(strat);
            maxFailoverAttempts = assertion.getCustomURLs().length;
        } else {
            failoverStrategy = null;
            maxFailoverAttempts = 1;
        }

        varNames = assertion.getVariablesUsed();
    }

    private GenericHttpClientFactory makeHttpClientFactory() throws FindException {
        final String proxyHost = assertion.getProxyHost();
        if (proxyHost == null) {
            return applicationContext.getBean("httpRoutingHttpClientFactory2", GenericHttpClientFactory.class);
        }

        final String proxyPassword = ServerVariables.expandPasswordOnlyVariable(getAudit(), assertion.getProxyPassword());

        // Use a proxy
        return new GenericHttpClientFactory() {
            @Override
            public GenericHttpClient createHttpClient() {
                return createHttpClient(-1, -1, -1, -1, null);
            }

            @Override
            public GenericHttpClient createHttpClient(int hostConnections, int totalConnections, int connectTimeout, int timeout, Object identity) {
                return new HttpComponentsClient(ClientConnectionManagerFactory.getInstance().createConnectionManager(ClientConnectionManagerFactory.ConnectionManagerType.POOLING, hostConnections, totalConnections),
                        identity, connectTimeout, timeout,  proxyHost, assertion.getProxyPort(), assertion.getProxyUsername(), proxyPassword);
            }
        };
    }


    public static final String PRODUCT = "Layer7-SecureSpan-Gateway";

    public static final String DEFAULT_USER_AGENT = PRODUCT + "/v" + BuildInfo.getProductVersion() + "-b" + BuildInfo.getBuildNumber();

    /**
     * Forwards the request along to a ProtectedService at the configured URL.
     *
     * @param context
     * @return an AssertionStatus indicating the success or failure of the request.
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          if some error preventing the execution of the PolicyAssertion has occurred.
     */
    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        final Message requestMessage = getRequestMessage(context);

        URL u = null;
        try {
            context.routingStarted();
            context.setVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE, UNDEFINED);

            if (! customURLList) {
                PublishedService service = context.getService();
                try {
                    u = getProtectedServiceUrl(service, context);
                } catch (WSDLException we) {
                    logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, we);
                    return AssertionStatus.FAILED;
                } catch (MalformedURLException mue) {
                    setReasonCode(context, mue);
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Invalid routing URL, " + ExceptionUtils.getMessage( mue )}, ExceptionUtils.getDebugException(mue));
                    return AssertionStatus.FAILED;
                }

                firePreRouting(context, requestMessage, u);
                if (failoverStrategy == null)
                    return tryUrl(context, requestMessage, u);
            }

            String failedService = null;
            List<String> blacklist = new ArrayList<String>();
            for (int tries = 0; tries < maxFailoverAttempts; tries++) {
                String failoverService = failoverStrategy.selectService();
                if (failoverService == null) {
                    // strategy says it's time to give up
                    break;
                }
                if (failedService != null)
                    logAndAudit(AssertionMessages.HTTPROUTE_FAILOVER_FROM_TO,
                            failedService, failoverService);
                URL url;
                String failoverServiceExpanded = failoverService.indexOf("${") > -1 ?
                    ExpandVariables.process(failoverService, context.getVariableMap(varNames, getAudit()), getAudit()) : failoverService;
                if (customURLList) {
                    url = new URL(failoverServiceExpanded);
                } else {
                    url = new URL(u.getProtocol(), failoverServiceExpanded, u.getPort(), u.getFile());
                }
                AssertionStatus result = tryUrl(context, requestMessage, url);
                if (result == AssertionStatus.NONE) {
                    failoverStrategy.reportSuccess(failoverService);
                    context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlBlacklist(), blacklist.toArray(new String[blacklist.size()]));
                    return result;
                }
                failedService = failoverService;
                blacklist.add(failoverServiceExpanded);
                failoverStrategy.reportFailure(failoverService);
            }
            context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlBlacklist(), blacklist.toArray(new String[blacklist.size()]));
            logAndAudit(AssertionMessages.HTTPROUTE_TOO_MANY_ATTEMPTS);
            return AssertionStatus.FAILED;
        } finally {
            context.routingFinished();
        }
    }

    private AssertionStatus tryUrl(PolicyEnforcementContext context, Message requestMessage, URL url) throws PolicyAssertionException {
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);
        context.setRoutedServiceUrl(url);
        setHttpRoutingUrlContextVariables(context);

        Throwable thrown = null;
        try {
            GenericHttpRequestParams routedRequestParams = new GenericHttpRequestParams(url);
            routedRequestParams.setSslSocketFactory(socketFactory);
            routedRequestParams.setSslContext(sslContext);
            routedRequestParams.setHostnameVerifier(hostnameVerifier);
            if ( assertion.getMaxRetries() >= 0 ) {
                routedRequestParams.setMaxRetries( assertion.getMaxRetries() );
            }
            routedRequestParams.setForceIncludeRequestBody(assertion.isForceIncludeRequestBody());

            // DELETE CURRENT SECURITY HEADER IF NECESSARY
            handleProcessedSecurityHeader(requestMessage);

            String userAgent = assertion.getUserAgent();
            if (userAgent == null || userAgent.length() == 0) userAgent = DEFAULT_USER_AGENT;
            routedRequestParams.addExtraHeader(new GenericHttpHeader(USER_AGENT, userAgent));

            StringBuilder hostValue = new StringBuilder( url.getHost() );
            int port = url.getPort();
            if (port != -1) {
                hostValue.append(":");
                hostValue.append(port);
            }

            HttpRequestKnob httpRequestKnob = context.getRequest().getKnob(HttpRequestKnob.class);

            if (assertion.isTaiCredentialChaining()) {
                doTaiCredentialChaining(context.getDefaultAuthenticationContext(), routedRequestParams, url);
            }

            String login = assertion.getLogin();
            String password = assertion.getPassword();
            String domain = assertion.getRealm();
            String host = assertion.getNtlmHost();

            Map<String,Object> vars = context.getVariableMap(varNames, getAudit());
            if (login != null && login.length() > 0 && password != null && password.length() > 0) {
                login = ExpandVariables.process(login, vars, getAudit());
                password = ExpandVariables.process(password, vars, getAudit());
                if (domain != null) domain = ExpandVariables.process(domain, vars, getAudit());
                if (host != null) host = ExpandVariables.process(host, vars, getAudit());

                logAndAudit(AssertionMessages.HTTPROUTE_LOGIN_INFO, login);
                if (domain != null && domain.length() > 0) {
                    if (host == null) {
                        host = ConfigFactory.getProperty( "clusterHost", null );
                    }
                    routedRequestParams.setNtlmAuthentication(new NtlmAuthentication(login, password.toCharArray(), domain, host));
                } else {
                    routedRequestParams.setPreemptiveAuthentication(true);
                    routedRequestParams.setPasswordAuthentication(new PasswordAuthentication(login, password.toCharArray()));
                }
            }

            if (assertion.isAttachSamlSenderVouches()) {
                doAttachSamlSenderVouches( assertion, requestMessage, context.getDefaultAuthenticationContext().getLastCredentials(), senderVouchesSignerInfo);
            } else if (assertion.isPassthroughHttpAuthentication() && httpRequestKnob != null) {
                String[] authHeaders = httpRequestKnob.getHeaderValues(HttpConstants.HEADER_AUTHORIZATION);
                boolean passed = false;
                if (assertion.getRequestHeaderRules().isForwardAll()) {
                    // We will pass it through later, along with all other application headers (Bug #10018)
                    passed = true;
                } else {
                    for (String authHeader : authHeaders) {
                        passed = true;
                        routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, authHeader));
                    }
                }
                if (passed) {
                    logAndAudit(AssertionMessages.HTTPROUTE_PASSTHROUGH_REQUEST);
                } else {
                    logAndAudit(AssertionMessages.HTTPROUTE_PASSTHROUGH_REQUEST_NC);
                }
            }

            KerberosEncData kerberosAuthorizationInfo = null;
            // Outbound Kerberos support (for Windows Integrated Auth only)
            if (assertion.isKrbDelegatedAuthentication()) {
                // extract creds from request & get service ticket
                KerberosServiceTicket delegatedKerberosTicket = getDelegatedKerberosTicket(context, url.getHost());
                //set kerberos authorization info
                kerberosAuthorizationInfo = delegatedKerberosTicket.getEncData();
                addKerberosServiceTicketToRequestParam(delegatedKerberosTicket , routedRequestParams);
            } else if (assertion.isKrbUseGatewayKeytab()) {
                // obtain a service ticket using the gateway's keytab
                KerberosRoutingClient client = new KerberosRoutingClient();
                //Find the keytab principal
                String svcPrincipal = KerberosClient.getKerberosAcceptPrincipal(url.getProtocol(), url.getHost(), true);
                String spn = KerberosClient.getServicePrincipalName(url.getProtocol(), url.getHost());
                KerberosServiceTicket kerberosTicket = client.getKerberosServiceTicket(spn, svcPrincipal,  true);
                //set kerberos authorization info
                kerberosAuthorizationInfo = kerberosTicket.getEncData();
                addKerberosServiceTicketToRequestParam(kerberosTicket , routedRequestParams);

            } else if (assertion.getKrbConfiguredAccount() != null) {
                // obtain a service ticket using the configured account in the assertion
                KerberosRoutingClient client = new KerberosRoutingClient();
                String krbAccount = ExpandVariables.process(assertion.getKrbConfiguredAccount(), vars, getAudit());
                String krbPass = assertion.getKrbConfiguredPassword();
                krbPass = krbPass == null ? null : ExpandVariables.process(krbPass, vars, getAudit());
                KerberosServiceTicket kerberosTicket = client.getKerberosServiceTicket(url, krbAccount, krbPass);
                //set kerberos authorization info
                kerberosAuthorizationInfo = kerberosTicket.getEncData();
                addKerberosServiceTicketToRequestParam(kerberosTicket, routedRequestParams);

            }

            //now set Kerberos authorization as an environment variable so it can be consumed later
            if(kerberosAuthorizationInfo != null){
                context.setVariable(HttpRoutingAssertion.KERBEROS_DATA, kerberosAuthorizationInfo);
            }

            return reallyTryUrl(context, requestMessage, routedRequestParams, url, true, vars);
        } catch (MalformedURLException mfe) {
            thrown = mfe;
            logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (IOException ioe) {
            // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
            thrown = ioe;
            logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (SAXException e) {
            thrown = e;
            logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (GeneralSecurityException e) {
            thrown = e;
            logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (KerberosException kex) {
            thrown = kex;
            logAndAudit(AssertionMessages.HTTPROUTE_USING_KERBEROS_ERROR, ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } finally {
            if(context.getRoutingStatus()!=RoutingStatus.ROUTED) {
                RoutingResultListener rrl = context.getRoutingResultListener();
                rrl.failed(url, thrown, context);
            }
        }
        return AssertionStatus.FAILED;
    }

    private Pair<HttpMethod,String> methodFromRequest(PolicyEnforcementContext context, GenericHttpRequestParams routedRequestParams) {
        HttpMethod method = assertion.getHttpMethod();
        if (method != null) {
            return new Pair<HttpMethod, String>(method, assertion.getHttpMethodAsString());
        }

        if (assertion.getRequestMsgSrc() != null) {
            logAndAudit(AssertionMessages.HTTPROUTE_DEFAULT_METHOD_VAR);
            return new Pair<HttpMethod, String>(HttpMethod.POST, null);
        }

        if (!context.getRequest().isHttpRequest()) {
            logAndAudit(AssertionMessages.HTTPROUTE_DEFAULT_METHOD_NON_HTTP);
            return new Pair<HttpMethod, String>(HttpMethod.POST, null);
        }

        final HttpRequestKnob httpRequestKnob = context.getRequest().getHttpRequestKnob();
        final HttpMethod requestMethod = httpRequestKnob.getMethod();
        if (requestMethod == null) {
            logAndAudit(AssertionMessages.HTTPROUTE_UNEXPECTED_METHOD, "null");
            return new Pair<HttpMethod, String>(HttpMethod.POST, null);
        }
        if (requestMethod.isFollowRedirects() && !assertion.isForceIncludeRequestBody())
            routedRequestParams.setFollowRedirects(assertion.isFollowRedirects());
        return new Pair<HttpMethod, String>(requestMethod, httpRequestKnob.getMethodAsString());
    }

    private AssertionStatus reallyTryUrl(PolicyEnforcementContext context, Message requestMessage, final GenericHttpRequestParams routedRequestParams,
                                         URL url, boolean allowRetry, Map<String,?> vars) throws PolicyAssertionException {
        GenericHttpRequest routedRequest = null;
        GenericHttpResponse routedResponse = null;
        Message routedResponseDestination = null;
        int status = -1;
        try {
            final MimeKnob reqMime = requestMessage.getKnob(MimeKnob.class);

            boolean streamingMode = reqMime != null && reqMime.isBufferingDisallowed();

            final long contentLength;
            if (streamingMode) {
                final Long cl;
                // SSG-6682 - We were incorrectly reporting the content length when the original request message was edited.
                // cannot trust getContentLengthFromHeaders so use chunked encoding when we can. We have no choice but to trust it for http 1.0
                if(GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_0.equals(getHttpVersion())){
                    // Avoiding using reqMime.getContentLength() since this may require stashing
                    cl = getContentLengthFromHeaders(requestMessage);
                } else {
                    cl = -1L;
                }
                // Use chunked encoding if declared length unavailable
                contentLength = cl == null ? -1L : cl;
            } else {
                // Fix for Bug #1282 - Must set a content-length on PostMethod or it will try to buffer the whole thing
                contentLength = (reqMime == null || !requestMessage.isInitialized()) ? 0 : reqMime.getContentLength();
            }
            if (contentLength > Integer.MAX_VALUE)
                throw new IOException("Body content is too long to be processed -- maximum is " + Integer.MAX_VALUE + " bytes");

            // this will forward soapaction, content-type, cookies, etc based on assertion settings
            HttpForwardingRuleEnforcer.handleRequestHeaders(
                    requestMessage.getKnob( HttpOutboundRequestKnob.class ),
                    requestMessage,
                    routedRequestParams,
                    context,
                    url.getHost(),
                    assertion.getRequestHeaderRules(),
                    getAudit(),
                    vars,
                    varNames);

            Object connectionId = null;
            if (context.getRequest().isHttpRequest()){
                if (assertion.isPassthroughHttpAuthentication()) {
                    connectionId = context.getRequest().getHttpRequestKnob().getConnectionIdentifier();
                } else {
                    //Fix for bug #10257, do the binding when Authorization is pass through.
                    for ( final HttpHeader header : routedRequestParams.getExtraHeaders() ) {
                        if ( HttpConstants.HEADER_AUTHORIZATION.equalsIgnoreCase(header.getName()) ) {
                            connectionId = context.getRequest().getHttpRequestKnob().getConnectionIdentifier();
                            break;
                        }
                    }
                }
            }

            GenericHttpClient httpClient = httpClientFactory.createHttpClient(
                                                                 getMaxConnectionsPerHost(),
                                                                 getMaxConnectionsAllHosts(),
                                                                 getConnectionTimeout(vars),
                                                                 getTimeout(vars),
                                                                 connectionId);

            if (httpClient instanceof RerunnableGenericHttpClient ||
                (!assertion.isPassthroughHttpAuthentication() &&
                routedRequestParams.getNtlmAuthentication() == null &&
                routedRequestParams.getPasswordAuthentication() == null)) {
                routedRequestParams.setContentLength(contentLength);
            }

            final Pair<HttpMethod,String> methodPair = methodFromRequest(context, routedRequestParams);
            final HttpMethod method = methodPair.left;
            routedRequestParams.setMethodAsString(methodPair.right);

            // dont add content-type for get and deletes
            if (routedRequestParams.needsRequestBody(method)) {
                final String requestContentType = reqMime == null ? "application/octet-stream" : reqMime.getOuterContentType().getFullValue();
                routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, requestContentType));
            }
            if ( ConfigFactory.getBooleanProperty( "ioHttpUseExpectContinue", false ) ) {
                routedRequestParams.setUseExpectContinue(true);
            }
            if ( !assertion.isUseKeepAlives()) {
                routedRequestParams.setUseKeepAlives(false); // note that server config property is for NO Keep-Alives
            }
            GenericHttpRequestParams.HttpVersion httpVersion = getHttpVersion();
            if(httpVersion != null) {
                routedRequestParams.setHttpVersion(httpVersion);
            }

            if ( assertion.isGzipEncodeDownstream() ) {
                routedRequestParams.setGzipEncode( true );
            }

            if ( logger.isLoggable( Level.FINE ) ) {
                for ( final HttpHeader header : routedRequestParams.getExtraHeaders() ) {
                    if ( HttpConstants.HEADER_AUTHORIZATION.equalsIgnoreCase(header.getName()) ) {
                        continue;
                    }
                    logger.log( Level.FINE, "HTTP request header added [{0}]=[{1}]", new String[]{header.getName(), header.getFullValue()} );
                }
            }

            String useHostName = null;
            HttpPassthroughRuleSet ruleSet = assertion.getRequestHeaderRules();
            if(ruleSet!=null && ruleSet.getRules()!=null && ruleSet.getRules().length>0){
                //check for host header rule and set the virtual host based on what was set here.
                HttpPassthroughRule[] rules = ruleSet.getRules();
                for(HttpPassthroughRule r: rules){
                    if(r.getName().toLowerCase().equals("host")){
                        useHostName = r.getCustomizeValue();
                    }
                }
            }

              if(useHostName!=null){
                if (vars == null)
                    vars = context.getVariableMap(varNames, getAudit());

                final String vhostValue = ExpandVariables.process(useHostName, vars, getAudit());
                if (vhostValue != null && vhostValue.length() > 0) {
                    routedRequestParams.setVirtualHost(vhostValue);
                    logger.fine("virtual-host override set: " + vhostValue);
                }
            }

            routedRequest = httpClient.createRequest(method, routedRequestParams);

            List<HttpForwardingRuleEnforcer.Param> paramRes = HttpForwardingRuleEnforcer.
                    handleRequestParameters(context, requestMessage, assertion.getRequestParamRules(), getAudit(), vars, varNames);

            if (!assertion.getRequestParamRules().isForwardAll() && paramRes != null) {
                List<String[]> parameters = new ArrayList<String[]>();

                for (HttpForwardingRuleEnforcer.Param p : paramRes) {
                    String[] parameter = new String[2];
                    parameter[0] = p.name;
                    parameter[1] = p.value;
                    parameters.add(parameter);
                }
                routedRequest.addParameters(parameters);

            } else {
                // only include payload if the method requires one (PUT or POST, or something else but with forced-body-inclusion turned on)
                if (routedRequestParams.needsRequestBody(method)) {
                    if (routedRequest instanceof RerunnableHttpRequest && !streamingMode) {
                        RerunnableHttpRequest rerunnableHttpRequest = (RerunnableHttpRequest) routedRequest;
                        rerunnableHttpRequest.setInputStreamFactory(new RerunnableHttpRequest.InputStreamFactory() {
                            @Override
                            public InputStream getInputStream() {
                                try {
                                    return reqMime == null ? new EmptyInputStream() : reqMime.getEntireMessageBodyAsInputStream();
                                } catch (NoSuchPartException nspe) {
                                    return new IOExceptionThrowingInputStream(new CausedIOException("Cannot access mime part.", nspe));
                                } catch (IOException ioe) {
                                    return new IOExceptionThrowingInputStream(ioe);
                                }
                            }
                        });
                    } else {
                        InputStream bodyInputStream = reqMime == null ? new EmptyInputStream() : reqMime.getEntireMessageBodyAsInputStream(streamingMode);
                        routedRequest.setInputStream(bodyInputStream);
                    }
                }
            }

            long maxBytes = 0L;
            if (assertion.getResponseSize()== null){
                maxBytes = com.l7tech.message.Message.getMaxBytes();
            }
            else{
                String maxBytesString = ExpandVariables.process(assertion.getResponseSize(),vars,getAudit());
                try{
                    maxBytes = Long.parseLong(maxBytesString); // resolve var
                }catch (NumberFormatException ex){
                    logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), "Invalid response size limit: " + ExceptionUtils.getMessage(ex));
                    return AssertionStatus.FAILED;
                }
            }

            long latencyTimerStart = System.currentTimeMillis();
            routedResponse = routedRequest.getResponse();

            if ( logger.isLoggable( Level.FINE ) ) {
                for ( final HttpHeader header : routedResponse.getHeaders().toArray() ) {
                    logger.log( Level.FINE, "HTTP response header [{0}]=[{1}]", new String[]{header.getName(), header.getFullValue()} );
                }
            }

            status = routedResponse.getStatus();
            context.setVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE, status);

            // Determines the routed response destination.
            routedResponseDestination = context.getResponse();
            boolean routedResponseDestinationIsContextVariable = false;
            if (assertion.getResponseMsgDest() != null) {
                routedResponseDestinationIsContextVariable = true;
                routedResponseDestination = context.getOrCreateTargetMessage( new MessageTargetableSupport(assertion.getResponseMsgDest()), false );
                if (routedResponseDestination.getKnob(MimeKnob.class) != null) {
                    //this Message has already been initialized, close it so it can be reused
                    routedResponseDestination.close();
                }
                routedResponseDestination.attachHttpResponseKnob(new AbstractHttpResponseKnob() {
                    @Override
                    public void addCookie(HttpCookie cookie) {
                        // TODO what to do with the cookie?
                    }
                });
            }

            AssertionStatus assertionStatus = readResponse(context, routedResponse, routedResponseDestination, maxBytes);
            long latencyTimerEnd = System.currentTimeMillis();
            if (assertionStatus == AssertionStatus.NONE) {
                long latency = latencyTimerEnd - latencyTimerStart;
                context.setVariable(HttpRoutingAssertion.VAR_ROUTING_LATENCY, ""+latency);
            }

            RoutingResultListener rrl = context.getRoutingResultListener();
            boolean retryRequested = allowRetry && rrl.reroute(url, status, routedResponse.getHeaders(), context); // only call listeners if retry is allowed

            if (status != HttpConstants.STATUS_OK && retryRequested) {
                // retry after if requested by a routing result listener
                logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS_HANDLED, url.getPath(), String.valueOf(status));
                return reallyTryUrl(context, requestMessage, routedRequestParams, url, false, vars);
            }

            if (status == HttpConstants.STATUS_OK)
                logAndAudit(AssertionMessages.HTTPROUTE_OK);
            else if (assertion.isPassthroughHttpAuthentication() && status == HttpConstants.STATUS_UNAUTHORIZED) {
                logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_CHALLENGE);
            }

            // todo: move to abstract routing assertion
            requestMessage.notifyMessage(routedResponseDestination, MessageRole.RESPONSE);
            routedResponseDestination.notifyMessage(requestMessage, MessageRole.REQUEST);

            // Register raw HTTP headers source
            HttpInboundResponseKnob httpInboundResponseKnob = getOrCreateHttpInboundResponseKnob(routedResponseDestination);
            httpInboundResponseKnob.setHeaderSource(routedResponse);

            HttpResponseKnob httpResponseKnob = routedResponseDestination.getKnob(HttpResponseKnob.class);
            if (assertionStatus == AssertionStatus.NONE && httpResponseKnob != null) {
                httpResponseKnob.setStatus(status);

                HttpForwardingRuleEnforcer.handleResponseHeaders(httpInboundResponseKnob,
                                                                 httpResponseKnob,
                                                                 getAudit(),
                                                                 assertion.getResponseHeaderRules(),
                                                                 routedResponseDestinationIsContextVariable,
                                                                 context,
                                                                 routedRequestParams,
                                                                 vars,
                                                                 varNames);
            }
            if (assertion.isPassthroughHttpAuthentication()) {
                boolean passed = false;
                List wwwAuthValues = routedResponse.getHeaders().getValues(HttpConstants.HEADER_WWW_AUTHENTICATE);
                if (wwwAuthValues != null) {
                    for (Object wwwAuthValue : wwwAuthValues) {
                        String value = (String) wwwAuthValue;
                        httpResponseKnob.addChallenge(value);
                        passed = true;
                    }
                }
                if (passed) {
                    logAndAudit(AssertionMessages.HTTPROUTE_PASSTHROUGH_RESPONSE);
                } else if (status != HttpConstants.STATUS_UNAUTHORIZED) {
                    logAndAudit(AssertionMessages.HTTPROUTE_PASSTHROUGH_RESPONSE_NC);
                }
            }

            context.setRoutingStatus(RoutingStatus.ROUTED);

            // notify listeners
            rrl.routed(url, status, routedResponse.getHeaders(), context);

            return assertionStatus;
        } catch (MalformedURLException mfe) {
            setReasonCode(context, mfe);
            logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(mfe));
            logger.log(Level.FINEST, "Problem routing: " + mfe.getMessage(), mfe);
        } catch (UnknownHostException uhe) {
            setReasonCode(context, uhe);
            logAndAudit(AssertionMessages.HTTPROUTE_UNKNOWN_HOST, ExceptionUtils.getMessage(uhe));
            return AssertionStatus.FAILED;
        } catch (SocketException se) {
            setReasonCode(context, se);
            logAndAudit(AssertionMessages.HTTPROUTE_SOCKET_EXCEPTION, ExceptionUtils.getMessage(se));
            return AssertionStatus.FAILED;
        } catch (IOException ioe) {
            // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
            setReasonCode(context, ioe);
            logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(ioe));
            logger.log(Level.FINEST, "Problem routing: " + ioe.getMessage(), ioe);
        } catch (NoSuchPartException e) {
            setReasonCode(context, e);
            logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(e));
            logger.log(Level.FINEST, "Problem routing: " + e.getMessage(), e);
        } catch (NoSuchVariableException e) {
            setReasonCode(context, e);
            logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(e));
            logger.log(Level.FINEST, "Problem routing: " + e.getMessage(), e);
        } finally {
            if (routedRequest != null || routedResponse != null) {
                final GenericHttpRequest req = routedRequest;
                final GenericHttpResponse resp = routedResponse;
                context.runOnClose(new Runnable() {
                    @Override
                    public void run() {
                        if (resp != null) resp.close();
                        if (req != null) req.close();
                    }
                });
            }
            firePostRouting(context, routedResponseDestination, url, status);
        }

        return AssertionStatus.FAILED;
    }

    private GenericHttpRequestParams.HttpVersion getHttpVersion(){
        if (assertion.getHttpVersion() == null) {
            if ( "1.0".equals( ConfigFactory.getProperty( "ioHttpVersion", null ) ) ) {
                return GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_0;
            }
        }
        return assertion.getHttpVersion();
    }

    private static Long getContentLengthFromHeaders(Message message) {
        // Return content length declared in HTTP header, or null if not there or disagreeing values
        HasHeaders hasHeaders = message.getKnob(HttpRequestKnob.class);
        if (hasHeaders == null)
            hasHeaders = message.getKnob(HasHeaders.class);
        if (hasHeaders == null)
            return null;

        String[] strings = hasHeaders.getHeaderValues("Content-Length");
        if (strings == null || strings.length < 1)
            return null;

        Long ret = null;
        for (String s : strings) {
            Long len = parseLong(s);
            if (len == null) {
                return null;
            }
            if (ret == null) {
                ret = len;
                continue;
            }
            if (ret.longValue() != len)
                return null;
        }

        return ret;
    }

    private static Long parseLong(String str) {
        try {
            return str == null ? null : Long.parseLong(str);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private void setReasonCode(PolicyEnforcementContext context, Exception e) {

        Throwable cause = e.getCause();
        if (cause == null) {
            cause = e;
        }

        // either host not found or bad url for the hostname
        if (cause instanceof java.net.UnknownHostException || cause instanceof java.net.ConnectException) {
            context.setVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE, HOST_NOT_FOUND);
        } else if (cause instanceof java.net.SocketTimeoutException) {
            context.setVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE, READ_TIMEOUT);
        } else if (cause instanceof ConnectTimeoutException) {
            context.setVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE, CONNECTION_TIMEOUT);
        } else if (cause instanceof MalformedURLException) {
            context.setVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE, BAD_URL);
        }
    }

    /**
     * @param context   the PEC
     * @return a request message object as configured by this assertion
     */
    private Message getRequestMessage(final PolicyEnforcementContext context) {
        if (assertion.getRequestMsgSrc() == null)
            return context.getRequest();

        final String variableName = assertion.getRequestMsgSrc();
        try {
            final Object requestSrc = context.getVariable(variableName);
            if (!(requestSrc instanceof Message)) {
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Request message source (\"" + variableName +
                        "\") is a context variable of the wrong type (expected=" + Message.class + ", actual=" + requestSrc.getClass() + ").");
            }
            return (Message)requestSrc;
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Request message source is a non-existent context variable (\"" + variableName + "\").");
        }
    }

    /**
     * Read the routing response and copy into the destination message object.
     *
     *
     * @param context           the policy enforcement context
     * @param routedResponse    response from back end
     * @param destination       the destination message object to copy <code>routedResponse</code> into
     * @return <code>AssertionStatus</code> if error reading <code>routedResponse</code> or it is an error response or AssertionStatus.NONE otherwise
     */
    private AssertionStatus readResponse(final PolicyEnforcementContext context,
                                         final GenericHttpResponse routedResponse,
                                         final Message destination,
                                         final long responseMaxSize) {
        AssertionStatus assertionStatus = AssertionStatus.NONE;
        try {
            final int status = routedResponse.getStatus();
            InputStream responseStream = routedResponse.getInputStream();
            // compression addition
            final String maybegzipencoding = routedResponse.getHeaders().getOnlyOneValue(HttpConstants.HEADER_CONTENT_ENCODING);
            if (maybegzipencoding != null && maybegzipencoding.contains("gzip")) { // case of value ?
                if (responseStream != null ){
                    // logger.info("Compression #4");
                    // If decoding is later changed to be optional, ensure the content-encoding header gets passed on when decoding is not performed, ie, remove it from HttpPassthroughRuleSet#HEADERS_NOT_TO_IMPLICITELY_FORWARD
                    logger.fine("detected compression on incoming response");
                    responseStream = new GZIPInputStream(responseStream);
                }
            }
            final String ctype = HttpHeaderUtil.searchHeaderValue(routedResponse.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, ConfigFactory.getProperty("ioHttpHeaderSearchRule"));
            ContentTypeHeader outerContentType = ctype != null ? ContentTypeHeader.create(ctype) : null;
            boolean passthroughSoapFault = false;
            if (assertion.isPassThroughSoapFaults() && status == HttpConstants.STATUS_SERVER_ERROR &&
                context.getService() != null && context.getService().isSoap() &&
                outerContentType != null && ( outerContentType.isXml() || outerContentType.isMultipart()) ) {
                passthroughSoapFault = true;
            }

            if (status == HttpConstants.STATUS_OK && outerContentType == null) {
                outerContentType = getDefaultContentType(false);
            }

            // Handle missing content type error
            if (assertion.isPassthroughHttpAuthentication() && status == HttpConstants.STATUS_UNAUTHORIZED) {
                if ( outerContentType==null ) outerContentType = getDefaultContentType(true);
                destination.initialize(stashManagerFactory.createStashManager(), outerContentType, responseStream, responseMaxSize);
                assertionStatus = AssertionStatus.AUTH_REQUIRED;
            } else if (status >= HttpConstants.STATUS_ERROR_RANGE_START && assertion.isFailOnErrorStatus() && !passthroughSoapFault) {
                logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_BADSTATUS, Integer.toString(status));
                assertionStatus = AssertionStatus.FALSIFIED;
            } else { // response OK
                if ( outerContentType==null ) outerContentType = getDefaultContentType(false);
                if ( outerContentType==null ) outerContentType = ContentTypeHeader.NONE;
                if (responseStream == null) {
                    destination.initialize(outerContentType, new byte[0]);
                } else {
                    StashManager stashManager = stashManagerFactory.createStashManager();
                    destination.initialize(stashManager, outerContentType, responseStream,responseMaxSize);
                }
            }
        } catch(EOFException eofe){
            logAndAudit(AssertionMessages.HTTPROUTE_BAD_GZIP_STREAM);
            assertionStatus = AssertionStatus.FALSIFIED;
        } catch (Exception e) {
            logAndAudit(AssertionMessages.HTTPROUTE_ERROR_READING_RESPONSE, null, e);
            assertionStatus = AssertionStatus.FALSIFIED;
        }
        return assertionStatus;
    }

    private ContentTypeHeader getDefaultContentType( final boolean ensureDefault ) {
        ContentTypeHeader contentTypeHeader = ensureDefault ? ContentTypeHeader.OCTET_STREAM_DEFAULT : null;

        String defaultContentType = config.getProperty( "ioHttpDefaultContentType" );
        if ( defaultContentType != null ) {
            try {
                contentTypeHeader = ContentTypeHeader.create(defaultContentType);
                logAndAudit( AssertionMessages.HTTPROUTE_RESPONSE_DEFCONTENTTYPE);
            } catch ( UncheckedIOException ioe ) {
                logger.log( Level.WARNING,
                        "Error processing default content type '"+ ExceptionUtils.getMessage( ioe )+"'.",
                        ExceptionUtils.getDebugException(ioe));
            }
        }

        return contentTypeHeader;
    }

    private URL getProtectedServiceUrl(PublishedService service, PolicyEnforcementContext context) throws WSDLException, MalformedURLException {
        URL url = protectedServiceUrl; // protectedServiceUrl only set if we are no using variables and url is valid

        if (url == null) {
            String psurl;
            if (urlUsesVariables) {
               psurl = ExpandVariables.process(assertion.getProtectedServiceUrl(), context.getVariableMap(varNames, getAudit()), getAudit());
            } else {
               psurl = assertion.getProtectedServiceUrl();
            }

            if (psurl == null) {
                logAndAudit(AssertionMessages.HTTPROUTE_BAD_ORIGINAL_URL);
                if (service == null) {
                    throw new MalformedURLException("Routing assertion specified no URL, and no resolved service is available to provide one");
                }
                url = service.serviceUrl();
            } else {
                url = new URL(psurl);
            }
        }

        return url;
    }

    /**
     * Adds the kerberos service ticket (if not null) into the HTTP request parameters for
     * outbound kerberos support.
     *
     * @param serviceTicket the service ticket to add
     * @param routedRequestParams the pending HTTP request parameters
     * @throws KerberosException if either the serviceTicke or the routed request parameters
     */
    private void addKerberosServiceTicketToRequestParam(KerberosServiceTicket serviceTicket, GenericHttpRequestParams routedRequestParams)
        throws KerberosException
    {
        if (serviceTicket == null)
            throw new KerberosException("KerberosServiceTicket is null and cannot be added to the request for routing");
        if (routedRequestParams == null)
            throw new KerberosException("Missing Http routing parameters");

        routedRequestParams.addExtraHeader(new GenericHttpHeader(
                HttpConstants.HEADER_AUTHORIZATION,
                "Negotiate " + HexUtils.encodeBase64(serviceTicket.getGSSAPReqTicket().getSPNEGO(), true)));

        logger.log(Level.FINE, "Kerberos ticket added to Http request parameters ({0}|{1})",
                new String[] { serviceTicket.getServicePrincipalName(), serviceTicket.getClientPrincipalName() });
    }

    private void setHttpRoutingUrlContextVariables(PolicyEnforcementContext context) {
        URL url = context.getRoutedServiceUrl();
        if (url == null) return;
        try {
            url = new URL(url.toString());
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "URL cannot be parsed: {0}", new String[] {url.toString()});
            return;
        }
        context.setVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_URL, url.toString());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlHost(), url.getHost());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlProtocol(), url.getProtocol());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlPort(), getHttpRoutingUrlPort(url));
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlFile(), url.getFile());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlPath(), url.getPath());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlQuery(), url.getQuery() == null ? null : "?" + url.getQuery());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlFragment(), url.getRef());
    }

    private Integer getHttpRoutingUrlPort(URL url) {
        if (url == null) return null;
        try {
            url = new URL(url.toString());
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "URL cannot be parsed: {0}", new String[] {url.toString()});
            return null;
        }

        String protocol = url.getProtocol();
        int port = url.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(protocol)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(protocol)) {
                port = 443;
            } else if ("ftp".equalsIgnoreCase(protocol)) {
                port = 21;
            } else if ("smtp".equalsIgnoreCase(protocol)) {
                port = 25;
            } else if ("pop3".equalsIgnoreCase(protocol)) {
                port = 110;
            } else if ("imap".equalsIgnoreCase(protocol)) {
                port = 143;
            }
        }
        return port;
    }
}
