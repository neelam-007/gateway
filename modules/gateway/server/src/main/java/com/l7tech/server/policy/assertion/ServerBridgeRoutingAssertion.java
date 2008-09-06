/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.http.prov.apache.StaleCheckingHttpConnectionManager;
import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.StickyFailoverStrategy;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.NoSuchVariableException;
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
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.processor.BadSecurityContextException;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.server.DefaultStashManagerFactory;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.xmlsec.ServerResponseWssSignature;
import com.l7tech.server.util.HttpForwardingRuleEnforcer;
import com.l7tech.util.CausedIllegalStateException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.HostnameVerifier;
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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSG implementation of a routing assertion that uses the SSB.
 */
public final class ServerBridgeRoutingAssertion extends AbstractServerHttpRoutingAssertion<BridgeRoutingAssertion> {

    private final String[] varNames;

    private static final Managers.BridgeStashManagerFactory BRIDGE_STASH_MANAGER_FACTORY =
        new Managers.BridgeStashManagerFactory() {
            public StashManager createStashManager() {
                return DefaultStashManagerFactory.getInstance().createStashManager();
            }
        };

    static {
        Managers.setBridgeStashManagerFactory(BRIDGE_STASH_MANAGER_FACTORY);
    }

    //- PUBLIC

    public ServerBridgeRoutingAssertion(BridgeRoutingAssertion assertion, ApplicationContext ctx) {
        super(assertion, ctx, logger);

        AssertionRegistry assertionRegistry = (AssertionRegistry)applicationContext.getBean("assertionRegistry", AssertionRegistry.class);
        Managers.setAssertionRegistry(assertionRegistry);

        hostnameVerifier = (HostnameVerifier)applicationContext.getBean("hostnameVerifier", HostnameVerifier.class);
        trustedCertManager = (TrustedCertManager)applicationContext.getBean("trustedCertManager", TrustedCertManager.class);
        wspReader = (WspReader)applicationContext.getBean("wspReader", WspReader.class);

        try {
            signerInfo = ServerResponseWssSignature.getSignerInfo(ctx, assertion);
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
            logger.log(Level.WARNING, ise.getMessage(), ise.getCause());
            ssg = null;
            messageProcessor = null;
            useClientCert = false;
            return;
        }

        ssg = newSSG(url.getHost());
        ssg.getRuntime().setCachedServerCert(null);

        useClientCert = initCredentials();

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

        messageProcessor = new MessageProcessor();
    }

    private X509Certificate findCert(long oid) throws FindException, IOException, CertificateException {
        TrustedCert tc = trustedCertManager.findByPrimaryKey(oid);
        if (tc == null)
            return null;
        return tc.getCertificate();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws PolicyAssertionException {
        context.routingStarted();
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);

        Throwable thrown = null;
        URL url = null;
        try {
            if (messageProcessor == null || ssg == null) {
                auditor.logAndAudit(AssertionMessages.BRIDGEROUTE_BAD_CONFIG);
                return AssertionStatus.FAILED;
            }

            try {
                PublishedService service = context.getService();
                url = getProtectedServiceUrl(service);

                // Obtains the request message source.
                final Message requestMsg = getRequestMessage(context);

                if(!requestMsg.isSoap()) {
                    auditor.logAndAudit(AssertionMessages.BRIDGEROUTE_REQUEST_NOT_SOAP);
                    return AssertionStatus.FAILED;
                }

                // DELETE CURRENT SECURITY HEADER IF NECESSARY
                handleProcessedSecurityHeader(context,
                                              data.getCurrentSecurityHeaderHandling(),
                                              data.getXmlSecurityActorToPromote());

                if (data.isAttachSamlSenderVouches()) {
                    doAttachSamlSenderVouches(context, signerInfo);
                }

                try {
                    final HttpRequestKnob httpRequestKnob = (HttpRequestKnob) requestMsg.getKnob(HttpRequestKnob.class);

                    Map vars = null;

                    // TODO support non-SOAP messaging with SSB api
                    String soapAction = "\"\"";
                    QName[] names = requestMsg.getSoapKnob().getPayloadNames();
                    // TODO decide what to do if there are multiple payload namespace URIs
                    String nsUri = names == null || names.length < 1 ? null : names[0].getNamespaceURI();

                    // TODO support SOAP-with-attachments with SSB api
                    if (requestMsg.getMimeKnob().isMultipart())
                        auditor.logAndAudit(AssertionMessages.BRIDGEROUTE_NO_ATTACHMENTS);

                    final URL origUrl;
                    try {
                        origUrl = new URL(ssg.getServerUrl());
                    } catch (MalformedURLException e) {
                        auditor.logAndAudit(AssertionMessages.HTTPROUTE_BAD_ORIGINAL_URL);
                        return AssertionStatus.SERVER_ERROR;
                    }

                    if (httpRequestKnob != null) {
                        soapAction = httpRequestKnob.getHeaderSingleValue( SoapConstants.SOAPACTION);
                    }

                    PolicyAttachmentKey pak = new PolicyAttachmentKey(nsUri, soapAction, origUrl.getPath());
                    Message bridgeRequest = requestMsg; // TODO see if it is unsafe to reuse this

                    // Determines the response message destination.
                    Message bridgeResponse = null;
                    if (assertion.getResponseMsgDest() == null) {
                        // The response will need to be re-initialized
                        bridgeResponse = context.getResponse(); // TODO see if it is unsafe to reuse this
                    } else {
                        bridgeResponse = new Message();
                        bridgeResponse.attachHttpResponseKnob(new AbstractHttpResponseKnob() {
                            public void addCookie(HttpCookie cookie) {
                                // TODO what to do with the cookie?
                            }
                        });
                        context.setVariable(assertion.getResponseMsgDest(), bridgeResponse);
                    }

                    HeaderHolder hh = new HeaderHolder();
                    PolicyApplicationContext pac = newPolicyApplicationContext(context, bridgeRequest, bridgeResponse, pak, origUrl, hh);
                    messageProcessor.processMessage(pac);

                    final HttpResponseKnob hrk = (HttpResponseKnob) bridgeResponse.getKnob(HttpResponseKnob.class);
                    int status = hrk == null ? HttpConstants.STATUS_SERVER_ERROR : hrk.getStatus();
                    if (status == HttpConstants.STATUS_OK)
                        auditor.logAndAudit(AssertionMessages.HTTPROUTE_OK);
                    else
                        auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS, url.getPath(), String.valueOf(status));

                    HttpResponseKnob httpResponseKnob = (HttpResponseKnob) bridgeResponse.getKnob(HttpResponseKnob.class);
                    if (httpResponseKnob != null) {
                        HttpForwardingRuleEnforcer.handleResponseHeaders(httpResponseKnob, auditor, hh,
                                                                         data.getResponseHeaderRules(), vars,
                                                                         varNames, context);

                        httpResponseKnob.setStatus(status);
                    }

                    context.setRoutingStatus(RoutingStatus.ROUTED);

                    context.getRoutingResultListener().routed(url, status, hh.getHeaders(), context);

                    return AssertionStatus.NONE;

                } catch (ConfigurationException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_ACCESS_DENIED, null, e);
                    return AssertionStatus.SERVER_AUTH_FAILED;
                } catch (OperationCanceledException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_ACCESS_DENIED, null, e);
                    return AssertionStatus.SERVER_AUTH_FAILED;
                } catch (BadSecurityContextException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                } catch (InvalidDocumentFormatException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                } catch (GeneralSecurityException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                } catch ( HttpChallengeRequiredException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e); // can't happen
                } catch ( ResponseValidationException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                } catch ( ClientCertificateException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e); // can't happen
                } catch (ProcessorException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                } catch ( PolicyLockedException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING, null, e);
                }
            } catch (WSDLException we) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, we);
            } catch (MalformedURLException mfe) {
                thrown = mfe;
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, mfe);
            } catch (IOException ioe) {
                thrown = ioe;
                if (ExceptionUtils.causedBy(ioe, SocketTimeoutException.class)) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_SOCKET_TIMEOUT);
                } else {
                    // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, ioe);
                }
            } catch (SAXException e) {
                thrown = e;
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
            } catch (NoSuchPartException e) {
                thrown = e;
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
            } catch (SignatureException e) {
                thrown = e;
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
            } catch (CertificateException e) {
                thrown = e;
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
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

    private static final Logger logger = Logger.getLogger(ServerBridgeRoutingAssertion.class.getName());

    private final SignerInfo signerInfo;
    private final Ssg ssg;
    private final MessageProcessor messageProcessor;
    private final HostnameVerifier hostnameVerifier;
    private final TrustedCertManager trustedCertManager;
    private final WspReader wspReader;
    private X509Certificate serverCert;
    private final boolean useClientCert;

    private URL getUrl() {
        try {
            return new URL(data.getProtectedServiceUrl());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(" URL is invalid; assertion is therefore nonfunctional.");
        }
    }

    private Policy getHardCodedPolicy() {
        Policy hardcodedPolicy = null;

        String policyXml = data.getPolicyXml();
        if (policyXml != null) {
            try {
                Assertion a = wspReader.parsePermissively(policyXml);
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
            public void setTrustStoreFile(File file) {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a trust store path");
            }

            public void setKeyStoreFile(File file) {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a key store path");
            }

            public X509Certificate getServerCertificate() {
                return getRuntime().getCachedServerCert();
            }

            public X509Certificate getClientCertificate() {
                return useClientCert ? signerInfo.getCertificateChain()[0] : null;
            }

            public PrivateKey getClientCertificatePrivateKey() {
                return useClientCert ? signerInfo.getPrivate() : null;
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
            hmax = CommonsHttpClient.getDefaultMaxConnectionsPerHost();
            tmax = CommonsHttpClient.getDefaultMaxTotalConnections();
        }

        StaleCheckingHttpConnectionManager connectionManager = new StaleCheckingHttpConnectionManager();
        HttpConnectionManagerParams params = connectionManager.getParams();
        params.setDefaultMaxConnectionsPerHost(hmax);
        params.setMaxTotalConnections(tmax);
        connectionManager.setPerHostStaleCleanupCount(getStaleCheckCount());
        GenericHttpClient client = new CommonsHttpClient(connectionManager, getConnectionTimeout(), getTimeout()) {
            public GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
                // override params to match server config
                if ( Boolean.valueOf(ServerConfig.getInstance().getPropertyCached("ioHttpUseExpectContinue")) ) {
                    params.setUseExpectContinue(true);
                }
                if ( Boolean.valueOf(ServerConfig.getInstance().getPropertyCached("ioHttpNoKeepAlive")) ) {
                    params.setUseKeepAlives(false); // note that server config property is for NO Keep-Alives
                }
                if ( "1.0".equals(ServerConfig.getInstance().getPropertyCached("ioHttpVersion")) ) {
                    params.setHttpVersion(GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_0);
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
            FailoverStrategy strategy;
            String[] addrs = ssg.getOverrideIpAddresses();
            try {
                strategy = FailoverStrategyFactory.createFailoverStrategy(data.getFailoverStrategyName(), addrs);
            } catch (IllegalArgumentException e) {
                strategy = new StickyFailoverStrategy(addrs);
            }
            int attempts = addrs.length;
            client = new FailoverHttpClient(client, strategy, attempts, logger);
        }

        return new SimpleHttpClient(client);
    }

    private boolean initCredentials() {
        boolean useClientCert;
        String username = data.getLogin();
        char[] password = null;

        if (username != null) {
            String pass = data.getPassword();
            password = pass == null ? null : pass.toCharArray();
        }

        if (username != null && password != null && username.length() > 0) {
            ssg.setUsername(username);
            ssg.getRuntime().setCachedPassword(password);
            useClientCert = false;
        } else {
            useClientCert = true;
        }

        return useClientCert;
    }

    private URL getProtectedServiceUrl(PublishedService service) throws WSDLException, MalformedURLException {
        URL url;
        String psurl = data.getProtectedServiceUrl();
        if (psurl == null) {
            url = service.serviceUrl();
        } else {
            url = new URL(psurl);
        }
        return url;
    }

    private PolicyApplicationContext newPolicyApplicationContext(final PolicyEnforcementContext context, Message bridgeRequest, Message bridgeResponse, PolicyAttachmentKey pak, URL origUrl, final HeaderHolder hh) {
        return new PolicyApplicationContext(ssg, bridgeRequest, bridgeResponse, NullRequestInterceptor.INSTANCE, pak, origUrl) {
            public HttpCookie[] getSessionCookies() {
                int cookieRule = data.getRequestHeaderRules().ruleForName("cookie");
                Set cookies = Collections.EMPTY_SET;
                if (cookieRule == HttpPassthroughRuleSet.ORIGINAL_PASSTHROUGH ||
                    cookieRule == HttpPassthroughRuleSet.CUSTOM_AND_ORIGINAL_PASSTHROUGH) {
                    cookies = context.getCookies();
                }
                //noinspection unchecked
                return (HttpCookie[]) cookies.toArray(new HttpCookie[cookies.size()]);
            }
            public void setSessionCookies(HttpCookie[] cookies) {
                // todo, fla, we need to handle all response http header rules, not just cookies
                int setcookieRule = data.getResponseHeaderRules().ruleForName("set-cookie");
                if (setcookieRule == HttpPassthroughRuleSet.ORIGINAL_PASSTHROUGH ||
                    setcookieRule == HttpPassthroughRuleSet.CUSTOM_AND_ORIGINAL_PASSTHROUGH) {
                    //add or replace cookies
                    for (HttpCookie cookie : cookies) {
                        context.addCookie(cookie);
                    }
                }
            }

            public SimpleHttpClient getHttpClient() {
                return newRRLSimpleHttpClient(context, super.getHttpClient(), context.getRoutingResultListener(), hh);
            }
        };
    }

    /**
     * @param context   the PEC
     * @return a request message object as configured by this assertion
     */
    private Message getRequestMessage(final PolicyEnforcementContext context) {
        Message msg = null;
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
                    final HttpRequestKnob defaultHRK = (HttpRequestKnob)context.getRequest().getKnob(HttpRequestKnob.class);
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
    private SimpleHttpClient newRRLSimpleHttpClient(final PolicyEnforcementContext context, final SimpleHttpClient client, final RoutingResultListener rrl, final HeaderHolder hh) {
        return new BRASimpleHttpClient(client, context, rrl, hh);
    }

    private class BRASimpleHttpClient extends SimpleHttpClient implements RerunnableGenericHttpClient {
        private final GenericHttpClient client;
        private final PolicyEnforcementContext context;
        private final RoutingResultListener rrl;
        private final HeaderHolder hh;

        private BRASimpleHttpClient(final GenericHttpClient client,
                                    final PolicyEnforcementContext context,
                                    final RoutingResultListener rrl,
                                    final HeaderHolder hh) {
            super(client);
            this.client = client;
            this.context = context;
            this.rrl = rrl;
            this.hh = hh;
        }

        public GenericHttpRequest createRequest(final GenericHttpMethod method, final GenericHttpRequestParams params)  {
            // enforce http outgoing rules here
            HttpForwardingRuleEnforcer.handleRequestHeaders(params, context, assertion.getRequestHeaderRules(),
                                                            auditor, null, varNames);

            if (data.isTaiCredentialChaining()) {
                doTaiCredentialChaining(context, params, params.getTargetUrl());
            }

            return new RerunnableHttpRequest() {
                private RerunnableHttpRequest.InputStreamFactory isf = null;
                private InputStream is = null;

                public void setInputStreamFactory(RerunnableHttpRequest.InputStreamFactory isf) {
                    this.isf = isf;
                }

                public void setInputStream(InputStream is) {
                    this.is = is;
                }

                public void close() {
                }

                /*
                 * Get the response, re-routing if directed to do so by a listener.
                 */
                public GenericHttpResponse getResponse() throws GenericHttpException {
                    getInputStreamFactory(); // init isf
                    return doGetResponse(true);
                }

                public void addParameter(String paramName, String paramValue) throws IllegalArgumentException, IllegalStateException {
                    throw new IllegalStateException("The bridge currently does not support form posts");
                }

                private RerunnableHttpRequest.InputStreamFactory getInputStreamFactory() throws GenericHttpException {
                    if(isf==null) {
                        BufferPoolByteArrayOutputStream baos = null;
                        try {
                            baos = new BufferPoolByteArrayOutputStream();
                            IOUtils.copyStream(is, baos);
                            final byte[] data = baos.toByteArray();
                            isf = new RerunnableHttpRequest.InputStreamFactory() {
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
                                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS_HANDLED, params.getTargetUrl().getPath(), Integer.toString(status));

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

    public class HeaderHolder {
        private HttpHeaders headers;

        public HttpHeaders getHeaders() {
            return headers;
        }

        public void setHeaders(HttpHeaders headers) {
            this.headers = headers;
        }
    }

    private class BridgeRoutingKeyStoreManager extends SsgKeyStoreManager {

        public boolean isClientCertUnlocked() throws KeyStoreCorruptException {
            return true;
        }

        public void deleteClientCert() throws IOException, KeyStoreException, KeyStoreCorruptException {
            throw new UnsupportedOperationException();
        }

        public boolean isPasswordWorkedForPrivateKey() {
            return true;
        }

        public boolean deleteStores() {
            throw new UnsupportedOperationException();
        }

        public void saveSsgCertificate(X509Certificate cert) throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException {
            serverCert = cert;
            ssg.getRuntime().setCachedServerCert(cert);
        }

        public void saveClientCertificate(PrivateKey privateKey, X509Certificate cert, char[] privateKeyPassword) throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException {
            throw new UnsupportedOperationException();
        }

        public void obtainClientCertificate(PasswordAuthentication credentials) throws BadCredentialsException, GeneralSecurityException, KeyStoreCorruptException, CertificateAlreadyIssuedException, IOException {
            throw new CertificateAlreadyIssuedException("Unable to apply for client certificate; the Gateway uses its SSL certificate as its client certificate.");
        }

        public String lookupClientCertUsername() {
            try {
                return CertUtils.extractSingleCommonNameFromCertificate(getClientCert());
            } catch (CertUtils.MultipleCnValuesException e) {
                return null;
            } catch (KeyStoreCorruptException e) {
                throw new CausedIllegalStateException(e); // can't happen
            }
        }

        protected X509Certificate getServerCert() throws KeyStoreCorruptException {
            return serverCert;
        }

        protected X509Certificate getClientCert() throws KeyStoreCorruptException {
            return useClientCert ? signerInfo.getCertificateChain()[0] : null;
        }

        public PrivateKey getClientCertPrivateKey(PasswordAuthentication passwordAuthentication) throws NoSuchAlgorithmException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException {
            return useClientCert ? signerInfo.getPrivate() : null;
        }

        protected boolean isClientCertAvailabile() throws KeyStoreCorruptException {
            return true;
        }

        protected KeyStore getKeyStore(char[] password) throws KeyStoreCorruptException {
            throw new UnsupportedOperationException("Gateway does not have an accessible key store file");
        }

        protected KeyStore getTrustStore() throws KeyStoreCorruptException {
            throw new UnsupportedOperationException("Gateway does not have an accessible cert store file");
        }

        public void importServerCertificate(File file) {
            throw new UnsupportedOperationException("Gateway is unable to import a server certificate");
        }

        public void importClientCertificate(File certFile, char[] pass, CertUtils.AliasPicker aliasPicker, char[] ssgPassword) throws IOException, GeneralSecurityException, KeyStoreCorruptException, AliasNotFoundException {
            throw new UnsupportedOperationException("Gateway is unable to import a client certificate");
        }

        public void installSsgServerCertificate( Ssg ssg, PasswordAuthentication credentials) throws IOException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException, CertificateException, KeyStoreException {
            Long loid = data.getServerCertificateOid();
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
