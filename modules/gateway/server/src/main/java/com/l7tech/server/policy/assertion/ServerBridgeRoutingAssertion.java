package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.StickyFailoverStrategy;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.NullRequestInterceptor;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.SslPeer;
import com.l7tech.proxy.ssl.SslPeerHttpClient;
import com.l7tech.proxy.ssl.SslPeerLazyDelegateSocketFactory;
import com.l7tech.proxy.util.CertificateDownloader;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.processor.BadSecurityContextException;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.server.DefaultStashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.transport.http.HttpConnectionIdleTimeoutManager2;
import com.l7tech.server.util.HttpForwardingRuleEnforcer;
import com.l7tech.util.*;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;

import static com.l7tech.common.io.failover.AbstractFailoverStrategy.makeSynchronized;

/**
 * SSG implementation of a routing assertion that uses the SSB.
 */
public final class ServerBridgeRoutingAssertion extends AbstractServerHttpRoutingAssertion<BridgeRoutingAssertion> {

    private final String[] varNames;
    private HttpConnectionIdleTimeoutManager2 listener;
    private PoolingClientConnectionManager connectionManager;

    private static final Managers.BridgeStashManagerFactory BRIDGE_STASH_MANAGER_FACTORY =
        new Managers.BridgeStashManagerFactory() {
            @Override
            public StashManager createStashManager() {
                return DefaultStashManagerFactory.getInstance().createStashManager();
            }
        };

    static {
        Managers.setBridgeStashManagerFactory(BRIDGE_STASH_MANAGER_FACTORY);
    }

    //- PUBLIC

    public ServerBridgeRoutingAssertion(BridgeRoutingAssertion assertion, ApplicationContext ctx) {
        super(assertion, ctx);

        AssertionRegistry assertionRegistry = applicationContext.getBean("assertionRegistry", AssertionRegistry.class);
        Managers.setAssertionRegistry(assertionRegistry);

        hostnameVerifier = applicationContext.getBean("hostnameVerifier", HostnameVerifier.class);
        trustedCertManager = applicationContext.getBean("trustedCertManager", TrustedCertManager.class);
        wspReader = applicationContext.getBean("wspReader", WspReader.class);
        listener = applicationContext.getBean("httpConnectionIdleTimeoutManager2", HttpConnectionIdleTimeoutManager2.class);

        try {
            signerInfo = ServerAssertionUtils.getSignerInfo(ctx, assertion);
        } catch (KeyStoreException e) {
            throw new RuntimeException("Unable to read private key for outbound message decoration: " + ExceptionUtils.getMessage(e), e);
        }

        varNames = assertion.getVariablesUsed();

        URL url;
        Policy hardcodedPolicy;
        try {
            url = getUrl();
            hardcodedPolicy = getHardCodedPolicy();

        } catch(Exception ise) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, ise.getMessage(), ExceptionUtils.getDebugException(ise));
            ssg = null;
            messageProcessor = null;
            useClientCert = false;
            passwordUsesVariables = false;
            return;
        }

        ssg = newSSG(url.getHost());
        ssg.getRuntime().setCachedServerCert(null);
        ssg.setProperties(assertion.getClientPolicyProperties());

        Pair<Boolean,Boolean> credInfo = initCredentials();
        this.useClientCert = credInfo.left;
        this.passwordUsesVariables = credInfo.right;

        final X509TrustManager trustManager = (X509TrustManager)applicationContext.getBean("routingTrustManager");
        ssg.getRuntime().setTrustManager(trustManager);
        ssg.getRuntime().setCredentialManager(newCredentialManager(trustManager));
        ssg.getRuntime().setSsgKeyStoreManager(new BridgeRoutingKeyStoreManager());

        // Configure failover if enabled
        String addrs[] = assertion.getCustomIpAddresses();
        if (addrs != null && addrs.length > 0 && areValidUrlHostnames(addrs)) {
            ssg.setOverrideIpAddresses(addrs);
            ssg.setUseOverrideIpAddresses(true);
        }

        // Do this after failover config
        ssg.getRuntime().setHttpClient(newHttpClient());

        if (hardcodedPolicy != null)
            ssg.getRuntime().setPolicyManager(new StaticPolicyManager(hardcodedPolicy));

        initSSGPorts(assertion, url);
        ssg.setSsgFile(url.getFile());
        ssg.setUseSslByDefault(assertion.isUseSslByDefault());

        ssg.setCompress(assertion.isGzipEncodeDownstream());

        // Force processing of the security header
        ssg.getProperties().put(SsgRuntime.SSGPROP_STRIPHEADER, "always");

        messageProcessor = new MessageProcessor();
    }

    private X509Certificate findCert(Goid oid) throws FindException, IOException, CertificateException {
        TrustedCert tc = trustedCertManager.findByPrimaryKey(oid);
        if (tc == null)
            return null;
        return tc.getCertificate();
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws PolicyAssertionException {
        context.routingStarted();
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);

        Throwable thrown = null;
        URL url = null;
        try { // routing finished try/finally
            Map<String, Object> vars = context.getVariableMap(varNames, getAudit());
            if (passwordUsesVariables) {
                String expandedPass = ExpandVariables.process(assertion.getPassword(), vars, getAudit());
                final char[] password = expandedPass.toCharArray();
                final char[] oldCachedPass;
                final SsgRuntime ssgRuntime = ssg.getRuntime();
                synchronized (ssg) {
                    oldCachedPass = ssgRuntime.getCachedPassword();
                    if (oldCachedPass == null) {
                        ssgRuntime.setCachedPassword(password);
                    } else if(!Arrays.equals(oldCachedPass, password)) {
                        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                "NOTE: Bridge Routing password has been changed");
                        ssgRuntime.setCachedPassword(password);
                    }
                }
            }

            if (messageProcessor == null || ssg == null) {
                logAndAudit(AssertionMessages.BRIDGEROUTE_BAD_CONFIG);
                return AssertionStatus.FAILED;
            }

            try {
                PublishedService service = context.getService();
                url = getProtectedServiceUrl(service);

                // Obtains the request message source.
                final Message requestMsg = getRequestMessage(context);

                if(!requestMsg.isSoap()) {
                    logAndAudit(AssertionMessages.BRIDGEROUTE_REQUEST_NOT_SOAP);
                    return AssertionStatus.FAILED;
                }

                // DELETE CURRENT SECURITY HEADER IF NECESSARY
                handleProcessedSecurityHeader(requestMsg);

                if (assertion.isAttachSamlSenderVouches()) {
                    doAttachSamlSenderVouches( assertion, requestMsg, context.getDefaultAuthenticationContext().getLastCredentials(), signerInfo);
                }

                try {
                    final HttpRequestKnob httpRequestKnob = requestMsg.getKnob(HttpRequestKnob.class);

                    // TODO support non-SOAP messaging with SSB api
                    String soapAction = "\"\"";
                    QName[] names = requestMsg.getSoapKnob().getPayloadNames();
                    // TODO decide what to do if there are multiple payload namespace URIs
                    String nsUri = names == null || names.length < 1 ? null : names[0].getNamespaceURI();

                    // TODO support SOAP-with-attachments with SSB api
                    if (requestMsg.getMimeKnob().isMultipart())
                        logAndAudit(AssertionMessages.BRIDGEROUTE_NO_ATTACHMENTS);

                    final URL origUrl;
                    try {
                        origUrl = new URL(ssg.getServerUrl());
                    } catch (MalformedURLException e) {
                        logAndAudit(AssertionMessages.HTTPROUTE_BAD_ORIGINAL_URL);
                        return AssertionStatus.SERVER_ERROR;
                    }

                    if (httpRequestKnob != null) {
                        soapAction = httpRequestKnob.getHeaderSingleValue( SoapConstants.SOAPACTION);
                    }

                    PolicyAttachmentKey pak = new PolicyAttachmentKey(nsUri, soapAction, origUrl.getPath());

                    //noinspection UnnecessaryLocalVariable
                    Message bridgeRequest = requestMsg; // TODO see if it is unsafe to reuse this

                    // Determines the response message destination.
                    final Message bridgeResponse;
                    if (assertion.getResponseMsgDest() == null) {
                        // The response will need to be re-initialized
                        bridgeResponse = context.getResponse(); // TODO see if it is unsafe to reuse this
                    } else {
                        bridgeResponse = context.getOrCreateTargetMessage( new MessageTargetableSupport(assertion.getResponseMsgDest()), false );
                        bridgeResponse.attachHttpResponseKnob(new AbstractHttpResponseKnob() {
                            @Override
                            public void addCookie(HttpCookie cookie) {
                                // TODO what to do with the cookie?
                            }
                        });
                    }

                    // enforce xml size limit
                    long xmlSizeLimit = 0;
                    if (assertion.getResponseSize()== null){
                        xmlSizeLimit = com.l7tech.message.Message.getMaxBytes();
                    }
                    else{
                        String maxBytesString = ExpandVariables.process(assertion.getResponseSize(),vars,getAudit());
                        try{
                            xmlSizeLimit = Long.parseLong(maxBytesString); // resolve var
                        }catch (NumberFormatException ex){
                            logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), "Invalid response size limit: " + ExceptionUtils.getMessage(ex));
                            return AssertionStatus.FAILED;
                        }
                    }

                    HeaderHolder hh = new HeaderHolder();
                    long[] latencyHolder = new long[]{-1};
                    bridgeResponse.getMimeKnob().setContentLengthLimit(xmlSizeLimit);
                    PolicyApplicationContext pac = newPolicyApplicationContext(context, bridgeRequest, bridgeResponse, pak, origUrl, hh, latencyHolder);
                    messageProcessor.processMessage(pac);


                    final HttpResponseKnob hrk = bridgeResponse.getKnob(HttpResponseKnob.class);
                    int status = hrk == null ? HttpConstants.STATUS_SERVER_ERROR : hrk.getStatus();
                    if (status == HttpConstants.STATUS_OK)
                        logAndAudit(AssertionMessages.HTTPROUTE_OK);
                    else
                        logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS, url.getPath(), String.valueOf(status));

                    if ( latencyHolder[0] > -1 ) {
                        context.setVariable(HttpRoutingAssertion.VAR_ROUTING_LATENCY, ""+latencyHolder[0]);                                            
                    }

                    // todo: move to abstract routing assertion
                    requestMsg.notifyMessage(bridgeResponse, MessageRole.RESPONSE);
                    bridgeResponse.notifyMessage(requestMsg, MessageRole.REQUEST);


                    HttpInboundResponseKnob inboundResponseKnob = getOrCreateHttpInboundResponseKnob(bridgeResponse);
                    inboundResponseKnob.setHeaderSource(hh);
                    HttpResponseKnob httpResponseKnob = bridgeResponse.getKnob(HttpResponseKnob.class);
                    if (httpResponseKnob != null) {
                        HttpForwardingRuleEnforcer.handleResponseHeaders(httpResponseKnob, getAudit(), hh,
                                                                         assertion.getResponseHeaderRules(), vars,
                                                                         varNames, context);

                        httpResponseKnob.setStatus(status);
                    }

                    context.setRoutingStatus(RoutingStatus.ROUTED);

                    context.getRoutingResultListener().routed(url, status, hh.getHeaders(), context);

                    return AssertionStatus.NONE;

                // TODO [jdk7] Multicatch
                } catch (ConfigurationException e) {
                    thrown = e;
                    logAndAudit(AssertionMessages.HTTPROUTE_ACCESS_DENIED, null,e);
                    return AssertionStatus.SERVER_AUTH_FAILED;
                } catch (OperationCanceledException e) {
                    thrown = e;
                    logAndAudit(AssertionMessages.HTTPROUTE_ACCESS_DENIED, null, e);
                    return AssertionStatus.SERVER_AUTH_FAILED;
                } catch (BadSecurityContextException e) {
                    thrown = e;
                    logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
                } catch (InvalidDocumentFormatException e) {
                    thrown = e;
                    logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
                } catch (GeneralSecurityException e) {
                    thrown = e;
                    logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
                } catch ( HttpChallengeRequiredException e) {
                    thrown = e;
                    logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{e.getMessage()}, e); // can't happen
                } catch ( ResponseValidationException e) {
                    thrown = e;
                    logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
                } catch ( ClientCertificateException e) {
                    thrown = e;
                    logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{e.getMessage()}, e); // can't happen
                } catch (ProcessorException e) {
                    thrown = e;
                    logAndAudit(AssertionMessages.BRIDGEROUTE_WSS_PROCESSING_RESP,
                            new String[]{ExceptionUtils.getMessage(e)},
                            e.getCause() != null ? ExceptionUtils.getDebugException(e) : null );
                } catch ( PolicyLockedException e) {
                    thrown = e;
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
                } catch (NoSuchVariableException e) {
                    thrown = e;
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
                }
            } catch (WSDLException we) {
                logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{we.getMessage()}, ExceptionUtils.getDebugException(we));
            } catch (MalformedURLException mfe) {
                thrown = mfe;
                logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{mfe.getMessage()}, ExceptionUtils.getDebugException(mfe));
            } catch (IOException ioe) {
                thrown = ioe;
                if (ExceptionUtils.causedBy(ioe, SocketTimeoutException.class)) {
                    logAndAudit(AssertionMessages.HTTPROUTE_SOCKET_TIMEOUT);
                } else if (ExceptionUtils.causedBy(ioe, SSLException.class)) {
                    Exception loggableException = ExceptionUtils.getDebugException(ioe);
                    if(loggableException != null) {
                        logAndAudit(AssertionMessages.HTTPROUTE_SSL_INIT_FAILED, null, loggableException);
                    } else {
                        logAndAudit(AssertionMessages.HTTPROUTE_SSL_INIT_FAILED);
                    }
                } else if (ExceptionUtils.causedBy(ioe, ByteLimitInputStream.DataSizeLimitExceededException.class)) {
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,new String[]{ioe.getMessage()}, ExceptionUtils.getDebugException(ioe));
                } else if (ExceptionUtils.causedBy(ioe, CausedIOException.class)) {
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Policy download error: " + ExceptionUtils.getMessage(ioe));
                } else {
                    String errmsg = ExceptionUtils.getMessage(ioe);

                    if (errmsg != null && errmsg.startsWith(CertificateDownloader.GATEWAY_CERT_DISCOVERY_ERROR)) {
                        String detail = "Gateway certificate discovery service error: ";

                        if (errmsg.contains(AssertionStatus.SERVICE_NOT_FOUND.getMessage())) {
                            detail = detail + AssertionStatus.SERVICE_NOT_FOUND.getMessage();
                            if (detail.lastIndexOf('.') == detail.length() - 1) detail = detail.substring(0, detail.length() - 1); // Remove the last extra '.'
                        } else if (errmsg.contains("404 Not Found")) {
                            detail = detail + "The requested resource is not available";
                        }
                        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, detail);
                    } else {
                        // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
                        logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{ioe.getMessage()}, ExceptionUtils.getDebugException(ioe));
                    }
                }
            } catch (SAXException e) {
                thrown = e;
                logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
            } catch (NoSuchPartException e) {
                thrown = e;
                logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
            } catch (GeneralSecurityException e) {
                thrown = e;
                logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
            } finally {
                if(url!=null && thrown!=null) context.getRoutingResultListener().failed(url, thrown, context);
            }

            return AssertionStatus.FAILED;
        }
        finally {
            context.routingFinished();
        }
    }

    //- PRIVATE

    private final SignerInfo signerInfo;
    private final Ssg ssg;
    private final MessageProcessor messageProcessor;
    private final HostnameVerifier hostnameVerifier;
    private final TrustedCertManager trustedCertManager;
    private final WspReader wspReader;
    private X509Certificate serverCert;
    private final boolean useClientCert;
    private final boolean passwordUsesVariables;

    private URL getUrl() {
        try {
            return new URL(assertion.getProtectedServiceUrl());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(" URL is invalid; assertion is therefore nonfunctional.");
        }
    }

    private Policy getHardCodedPolicy() {
        Policy hardcodedPolicy = null;

        String policyXml = assertion.getPolicyXml();
        if (policyXml != null) {
            try {
                Assertion a = wspReader.parsePermissively(policyXml, WspReader.OMIT_DISABLED);
                hardcodedPolicy = new Policy(a, null);
                hardcodedPolicy.setAlwaysValid(true);
            } catch (IOException e) {
                throw new IllegalStateException("Hardcoded policy is invalid; assertion is therefore nonfunctional.",e);
            }
        }

        return hardcodedPolicy;
    }

    private Ssg newSSG(String gatewayHostname) {
        return new Ssg(1, gatewayHostname) {
            @Override
            public void setTrustStoreFile(File file) {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a trust store path");
            }

            @Override
            public void setKeyStoreFile(File file) {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a key store path");
            }

            @Override
            public X509Certificate getServerCertificate() {
                return getRuntime().getCachedServerCert();
            }

            @Override
            public X509Certificate getClientCertificate() {
                return useClientCert ? signerInfo.getCertificateChain()[0] : null;
            }

            @Override
            public PrivateKey getClientCertificatePrivateKey() {
                try {
                    return useClientCert ? signerInfo.getPrivate() : null;
                } catch (UnrecoverableKeyException e) {
                    throw new RuntimeException("Unable to access bridge routing private key: " + ExceptionUtils.getMessage(e), e);
                }
            }
        };
    }

    private void initSSGPorts(BridgeRoutingAssertion assertion, URL url) {
        int port = url.getPort();
        int normalPort;
        int sslPort;

        if ("https".equalsIgnoreCase(url.getProtocol())) {
            sslPort = port;
            normalPort = port - 443 + 80;
            if (normalPort < 0 || normalPort > 65535)
                normalPort = 80;
        } else {
            normalPort = port;
            sslPort = normalPort - 80 + 443;
            if (sslPort < 0 || sslPort > 65535)
                sslPort = 443;
        }

        if (assertion.getHttpPort() > 0) normalPort = assertion.getHttpPort();
        if (assertion.getHttpsPort() > 0) sslPort = assertion.getHttpsPort();

        ssg.setSsgPort(normalPort);
        ssg.setSslPort(sslPort);

    }

    private CredentialManager newCredentialManager(final X509TrustManager trustManager) {
        return new CredentialManagerImpl() {
            @Override
            public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate untrustedCertificate) throws OperationCanceledException {
                if (!(sslPeer instanceof Ssg))
                    throw new OperationCanceledException("Unable to approve certificate import for SslPeer of unexpected type " + sslPeer.getClass().getName());
                Ssg ssg1 = (Ssg)sslPeer;
                if (ssg1 != ssg)
                    throw new OperationCanceledException("Unable to approve certificate import for Ssg other than the one managed by this ServerBridgeRoutingAssertion");
                try {
                    // TODO support DSA certificates
                    trustManager.checkServerTrusted(new X509Certificate[] { untrustedCertificate }, "RSA");
                } catch (CertificateException e) {
                    throw new OperationCanceledException("Unable to approve certificate import for Ssg: " + e, e);
                }
                // Looks good.  Allow the certificate to be imported.
                serverCert = untrustedCertificate;
                ssg.getRuntime().setCachedServerCert(serverCert);
            }

            @Override
            public PasswordAuthentication getNewCredentials(Ssg ssg, boolean displayBadPasswordMessage) throws OperationCanceledException {
                throw new OperationCanceledException(AssertionMessages.HTTPROUTE_ACCESS_DENIED.getMessage());
            }
        };
    }

    private SimpleHttpClient newHttpClient() {
        // Set up HTTP client (use commons client)
        int hmax = getMaxConnectionsPerHost();
        int tmax = getMaxConnectionsAllHosts();
        if (hmax <= 0) {
            hmax = HttpComponentsClient.getDefaultMaxConnectionsPerHost();
            tmax = HttpComponentsClient.getDefaultMaxTotalConnections();
        }

        connectionManager = new PoolingClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(hmax);
        connectionManager.setMaxTotal(tmax);
        listener.notifyHttpConnectionManagerCreated(connectionManager);

        GenericHttpClient client = new HttpComponentsClient(connectionManager, getConnectionTimeout(null), getTimeout(null)) {
            @Override
            public GenericHttpRequest createRequest(HttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
                // override params to match server config
                if ( ConfigFactory.getBooleanProperty( "ioHttpUseExpectContinue", false ) ) {
                    params.setUseExpectContinue(true);
                }
                if ( !assertion.isUseKeepAlives() ) {
                    params.setUseKeepAlives(false);
                }
                if ( "1.0".equals( ConfigFactory.getProperty( "ioHttpVersion", null ) ) ) {
                    params.setHttpVersion(GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_0);
                }
                if ( assertion.getMaxRetries() >= 0 ) {
                    params.setMaxRetries( assertion.getMaxRetries() );
                }

                return super.createRequest(method, params);
            }
        };

        // Attach SSL support
        client = new SslPeerHttpClient(client,
                                       ssg,
                                       new SslPeerLazyDelegateSocketFactory(ssg),
                                       hostnameVerifier);

        if (ssg.isUseOverrideIpAddresses()) {
            // Attach failover client
            FailoverStrategy<String> strategy;
            String[] addrs = ssg.getOverrideIpAddresses();
            try {
                strategy = FailoverStrategyFactory.createFailoverStrategy(assertion.getFailoverStrategyName(), addrs);
            } catch (IllegalArgumentException e) {
                strategy = new StickyFailoverStrategy<String>(addrs);
            }
            int attempts = addrs.length;
            client = new FailoverHttpClient(client, makeSynchronized( strategy ), attempts, logger);
        }

        return new SimpleHttpClient(client);
    }

    private Pair<Boolean,Boolean> initCredentials() {
        boolean useClientCert;
        boolean passwordUsesVariables = false;
        String username = assertion.getLogin();
        String password = null;

        if (username != null) {
            password = assertion.getPassword();
        }

        if (username != null && password != null && username.length() > 0) {
            ssg.setUsername(username);
            passwordUsesVariables = Syntax.getReferencedNames(password).length > 0;
            if (!passwordUsesVariables)
                ssg.getRuntime().setCachedPassword(password.toCharArray());
            useClientCert = false;
        } else {
            useClientCert = true;
        }

        return new Pair<Boolean,Boolean>(useClientCert, passwordUsesVariables);
    }

    private URL getProtectedServiceUrl(PublishedService service) throws WSDLException, MalformedURLException {
        URL url;
        String psurl = assertion.getProtectedServiceUrl();
        if (psurl == null) {
            url = service.serviceUrl();
        } else {
            url = new URL(psurl);
        }
        return url;
    }

    private PolicyApplicationContext newPolicyApplicationContext( final PolicyEnforcementContext context,
                                                                  final Message bridgeRequest,
                                                                  final Message bridgeResponse,
                                                                  final PolicyAttachmentKey pak,
                                                                  final URL origUrl,
                                                                  final HeaderHolder hh,
                                                                  final long[] latencyHolder) {
        return new PolicyApplicationContext(ssg, bridgeRequest, bridgeResponse, new LatencyRequestInterceptor(latencyHolder), pak, origUrl) {
            @Override
            public HttpCookie[] getSessionCookies() {
                int cookieRule = assertion.getRequestHeaderRules().ruleForName("cookie");
                Set cookies = Collections.EMPTY_SET;
                if (cookieRule == HttpPassthroughRuleSet.ORIGINAL_PASSTHROUGH ||
                    cookieRule == HttpPassthroughRuleSet.CUSTOM_AND_ORIGINAL_PASSTHROUGH) {
                    cookies = context.getCookies();
                }
                //noinspection unchecked
                return (HttpCookie[]) cookies.toArray(new HttpCookie[cookies.size()]);
            }
            @Override
            public void setSessionCookies(HttpCookie[] cookies) {
                // todo, fla, we need to handle all response http header rules, not just cookies
                int setcookieRule = assertion.getResponseHeaderRules().ruleForName("set-cookie");
                if (setcookieRule == HttpPassthroughRuleSet.ORIGINAL_PASSTHROUGH ||
                    setcookieRule == HttpPassthroughRuleSet.CUSTOM_AND_ORIGINAL_PASSTHROUGH) {
                    //add or replace cookies
                    for (HttpCookie cookie : cookies) {
                        context.addCookie(cookie);
                    }
                }
            }

            @Override
            public SimpleHttpClient getHttpClient() {
                return newRRLSimpleHttpClient(
                        context,
                        bridgeRequest,
                        super.getHttpClient(),
                        context.getRoutingResultListener(),
                        hh );
            }
        };
    }

    /**
     * @param context   the PEC
     * @return a request message object as configured by this assertion
     */
    private Message getRequestMessage(final PolicyEnforcementContext context) {
        final Message msg;
        if (assertion.getRequestMsgSrc() == null) {
            msg = context.getRequest();
        } else {
            final String variableName = assertion.getRequestMsgSrc();
            try {
                final Object requestSrc = context.getVariable(variableName);
                if (!(requestSrc instanceof Message)) {
                    // Should never happen.
                    throw new RuntimeException("Request message source (\"" + variableName +
                            "\") is a context variable of the wrong type (expected=" + Message.class + ", actual=" + requestSrc.getClass() + ").");
                }
                msg = (Message)requestSrc;

                if (msg.getKnob(HttpRequestKnob.class) == null) {
                    // Make it a request message by inheriting the HttpRequestKnob from the default request.
                    final HttpRequestKnob defaultHRK = context.getRequest().getKnob(HttpRequestKnob.class);
                    if (defaultHRK != null) {
                        msg.attachHttpRequestKnob(defaultHRK);
                    }
                }
            } catch (NoSuchVariableException e) {
                // Should never happen.
                throw new RuntimeException("Request message source is a non-existent context variable (\"" + variableName + "\").");
            }
        }

        return msg;
    }

    /*
     * Create an extended SimpleHttpClient that supports re-routing
     *
     * NOTE: this is NOT compatible with a non-sticky failover client.
     */
    private SimpleHttpClient newRRLSimpleHttpClient( final PolicyEnforcementContext context,
                                                     final Message bridgeRequest,
                                                     final SimpleHttpClient client,
                                                     final RoutingResultListener rrl,
                                                     final HeaderHolder hh ) {
        return new BRASimpleHttpClient(client, context, bridgeRequest, rrl, hh);
    }

    @Override
    public void close() {
        super.close();
        if (listener != null && connectionManager != null) {
            listener.notifyHttpConnectionManagerDestroyed(connectionManager);
        }
    }

    private class BRASimpleHttpClient extends SimpleHttpClient implements RerunnableGenericHttpClient {
        private final GenericHttpClient client;
        private final PolicyEnforcementContext context;
        private final Message bridgeRequest;
        private final RoutingResultListener rrl;
        private final HeaderHolder hh;
        private final HasOutboundHeaders oh;

        private BRASimpleHttpClient(final GenericHttpClient client,
                                    final PolicyEnforcementContext context,
                                    final Message bridgeRequest,
                                    final RoutingResultListener rrl,
                                    final HeaderHolder hh ) {
            super(client);
            this.client = client;
            this.context = context;
            this.bridgeRequest = bridgeRequest;
            this.rrl = rrl;
            this.hh = hh;
            this.oh = bridgeRequest.getKnob(HttpOutboundRequestKnob.class);
        }

        @Override
        public GenericHttpRequest createRequest(final HttpMethod method, final GenericHttpRequestParams params)  {
            // enforce http outgoing rules here
            HttpForwardingRuleEnforcer.handleRequestHeaders(oh, bridgeRequest, params, context, assertion.getRequestHeaderRules(),
                                                            getAudit(), null, varNames);

            if (assertion.isTaiCredentialChaining()) {
                doTaiCredentialChaining(context.getDefaultAuthenticationContext(), params, params.getTargetUrl());
            }

            return new RerunnableHttpRequest() {
                private RerunnableHttpRequest.InputStreamFactory isf = null;
                private InputStream is = null;

                @Override
                public void setInputStreamFactory(RerunnableHttpRequest.InputStreamFactory isf) {
                    this.isf = isf;
                }

                @Override
                public void setInputStream(InputStream is) {
                    this.is = is;
                }

                @Override
                public void close() {
                }

                /*
                 * Get the response, re-routing if directed to do so by a listener.
                 */
                @Override
                public GenericHttpResponse getResponse() throws GenericHttpException {
                    getInputStreamFactory(); // init isf
                    return doGetResponse(true);
                }

                @Override
                public void addParameters(List<String[]> parameters) throws IllegalArgumentException, IllegalStateException {
                    throw new IllegalStateException("The bridge currently does not support form posts");
                }

                private RerunnableHttpRequest.InputStreamFactory getInputStreamFactory() throws GenericHttpException {
                    if(isf==null) {
                        PoolByteArrayOutputStream baos = null;
                        try {
                            baos = new PoolByteArrayOutputStream();
                            IOUtils.copyStream(is, baos);
                            final byte[] data = baos.toByteArray();
                            isf = new RerunnableHttpRequest.InputStreamFactory() {
                                @Override
                                public InputStream getInputStream() {
                                    return new ByteArrayInputStream(data);
                                }
                            };
                        }
                        catch(IOException ioe) {
                            throw new GenericHttpException("Cannot read request data.", ioe);
                        } finally {
                            if (baos != null) baos.close();
                        }
                    }
                    return isf;
                }

                private GenericHttpResponse doGetResponse(boolean allowRerequest) throws GenericHttpException {
                    GenericHttpRequest req = null;
                    GenericHttpResponse res = null;

                    try {
                        req = client.createRequest(method, params);
                        if(isf!=null) {
                            if(req instanceof RerunnableHttpRequest) {
                                ((RerunnableHttpRequest)req).setInputStreamFactory(getInputStreamFactory());
                            }
                            else {
                                req.setInputStream(isf.getInputStream());
                            }
                        }

                        res = req.getResponse();
                        int status = res.getStatus();

                        if(status!=HttpConstants.STATUS_OK && allowRerequest) {
                            if(rrl.reroute(params.getTargetUrl(), status, res.getHeaders(), context)) {
                                logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS_HANDLED, params.getTargetUrl().getPath(), Integer.toString(status));

                                //TODO if we refactor the BRA we should clean this up (params changed by this SimpleHttpClient impl [HACK])
                                params.replaceExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_COOKIE, HttpCookie.getCookieHeader(context.getCookies())));

                                return doGetResponse(false);
                            }
                        }

                        GenericHttpResponse result = res;
                        hh.setHeaders(result.getHeaders());
                        res = null;
                        return result;
                    }
                    finally {
                        if(req!=null) req.close();
                        if(res!=null) res.close();
                    }
                }
            };
        }
    }

    public static class HeaderHolder implements HttpHeadersHaver {
        private HttpHeaders headers;

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        public void setHeaders(HttpHeaders headers) {
            this.headers = headers;
        }
    }

    private static class LatencyRequestInterceptor extends NullRequestInterceptor {
        private final long[] latencyHolder;
        private long latencyTimerStart;

        LatencyRequestInterceptor( final long[] latencyHolder ) {
            this.latencyHolder = latencyHolder;
        }

        @Override
        public void onBackEndRequest( final PolicyApplicationContext context, final List<HttpHeader> headersSent ) {
            latencyTimerStart = System.currentTimeMillis();
        }

        @Override
        public void onBackEndReply( final PolicyApplicationContext context ) {
            if ( latencyTimerStart > 0 ) {
                long latencyTimerEnd = System.currentTimeMillis();
                long latency = latencyTimerEnd - latencyTimerStart;
                latencyHolder[0] = latency;
            }
        }
    }

    private class BridgeRoutingKeyStoreManager extends SsgKeyStoreManager {

        @Override
        public boolean isClientCertUnlocked() throws KeyStoreCorruptException {
            return getClientCertPrivateKey(null) != null;
        }

        @Override
        public void deleteClientCert() throws IOException, KeyStoreException, KeyStoreCorruptException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteStores() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveSsgCertificate(X509Certificate cert) throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException {
            serverCert = cert;
            ssg.getRuntime().setCachedServerCert(cert);
        }

        @Override
        public void saveClientCertificate(PrivateKey privateKey, X509Certificate cert, char[] privateKeyPassword) throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void obtainClientCertificate(PasswordAuthentication credentials) throws BadCredentialsException, GeneralSecurityException, KeyStoreCorruptException, CertificateAlreadyIssuedException, IOException {
            throw new CertificateAlreadyIssuedException("Unable to apply for client certificate; the Gateway uses its SSL certificate as its client certificate.");
        }

        @Override
        public String lookupClientCertUsername() {
            try {
                return CertUtils.extractSingleCommonNameFromCertificate(getClientCert());
            } catch (CertUtils.MultipleCnValuesException e) {
                return null;
            } catch (KeyStoreCorruptException e) {
                throw new CausedIllegalStateException(e); // can't happen
            }
        }

        @Override
        protected X509Certificate getServerCert() throws KeyStoreCorruptException {
            return serverCert;
        }

        @Override
        protected X509Certificate getClientCert() throws KeyStoreCorruptException {
            return useClientCert ? signerInfo.getCertificateChain()[0] : null;
        }

        @Override
        public PrivateKey getClientCertPrivateKey(PasswordAuthentication passwordAuthentication) {
            try {
                return useClientCert ? signerInfo.getPrivate() : null;
            } catch (UnrecoverableKeyException e) {
                throw new RuntimeException("Unable to access bridge routing private key: " + ExceptionUtils.getMessage(e), e);
            }
        }

        @Override
        protected boolean isClientCertAvailabile() throws KeyStoreCorruptException {
            return true;
        }

        @Override
        protected KeyStore getKeyStore(char[] password) throws KeyStoreCorruptException {
            throw new UnsupportedOperationException("Gateway does not have an accessible key store file");
        }

        @Override
        protected KeyStore getTrustStore() throws KeyStoreCorruptException {
            throw new UnsupportedOperationException("Gateway does not have an accessible cert store file");
        }

        @Override
        public void importServerCertificate(File file) {
            throw new UnsupportedOperationException("Gateway is unable to import a server certificate");
        }

        @Override
        public void importClientCertificate(File certFile, char[] pass, CertUtils.AliasPicker aliasPicker, char[] ssgPassword) throws IOException, GeneralSecurityException, KeyStoreCorruptException, AliasNotFoundException {
            throw new UnsupportedOperationException("Gateway is unable to import a client certificate");
        }

        @Override
        public void installSsgServerCertificate( Ssg ssg, PasswordAuthentication credentials) throws IOException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException, CertificateException, KeyStoreException {
            Goid loid = assertion.getServerCertificateGoid();
            if (loid == null) {
                // Attempt normal cert discovery
                super.installSsgServerCertificate(ssg, credentials);
                return;
            }

            // Attempt database lookup of OID
            try {
                X509Certificate cert = findCert(loid);
                if (cert == null)
                    throw new OperationCanceledException("The configured server certificate OID was not found in the trusted certificates table: " + loid);
                saveSsgCertificate(cert);
            } catch (FindException e) {
                throw new KeyStoreException("Unable to access trusted certificates table", e);
            }
        }
    }
}
