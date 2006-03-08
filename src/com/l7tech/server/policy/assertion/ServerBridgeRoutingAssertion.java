/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.StickyFailoverStrategy;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TcpKnob;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.processor.BadSecurityContextException;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.NullRequestInterceptor;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.ClientProxySecureProtocolSocketFactory;
import com.l7tech.proxy.ssl.SslPeer;
import com.l7tech.proxy.ssl.SslPeerHttpClient;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSG implementation of a routing assertion that uses the SSB.
 */
public class ServerBridgeRoutingAssertion extends ServerRoutingAssertion {

    //- PUBLIC

    public ServerBridgeRoutingAssertion(BridgeRoutingAssertion assertion, ApplicationContext ctx) {
        super(assertion, ctx);
        this.bridgeRoutingAssertion = assertion;
        this.auditor = new Auditor(this, ctx, logger);

        final KeystoreUtils ku = (KeystoreUtils)applicationContext.getBean("keystore");
        try {
            signerInfo = ku.getSslSignerInfo();
        } catch (IOException e) {
            throw new RuntimeException("Can't read the keystore for signing outbound SAML", e);
        }

        URL url;
        Policy hardcodedPolicy;
        try {
            url = getUrl();
            hardcodedPolicy = getHardCodedPolicy();

            String serverCertBase64 = assertion.getServerCertBase64();
            if (serverCertBase64 != null) {
                serverCertBase64 = serverCertBase64.replaceAll("----+\\s*BEGIN (TRUSTED )?CERTIFICATE----+\\s*", "");
                serverCertBase64 = serverCertBase64.replaceAll("----+\\s*END (TRUSTED )?CERTIFICATE----+\\s*", "");
                byte[] serverCertBytes = HexUtils.decodeBase64(serverCertBase64, true);
                serverCert = CertUtils.decodeCert(serverCertBytes);
            }
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

        final SslClientTrustManager trustManager = (SslClientTrustManager)applicationContext.getBean("httpRoutingAssertionTrustManager");
        ssg.getRuntime().setTrustManager(trustManager);
        ssg.getRuntime().setCredentialManager(newCredentialManager(trustManager));
        ssg.getRuntime().setSsgKeyStoreManager(new BridgeRoutingKeyStoreManager());

        // Configure failover if enabled
        String addrs[] = assertion.getCustomIpAddresses();
        if (addrs != null && addrs.length > 0 && areValidUrlHostnames(addrs, auditor)) {
            ssg.setOverrideIpAddresses(addrs);
            ssg.setUseOverrideIpAddresses(true);
        }

        // Do this after failover config
        ssg.getRuntime().setHttpClient(newHttpClient());

        if (hardcodedPolicy != null)
            ssg.getRuntime().setPolicyManager(new StaticPolicyManager(hardcodedPolicy));

        initSSGPorts(url);
        ssg.setSsgFile(url.getFile());
        ssg.setUseSslByDefault(true); // TODO make this fully configurable

        messageProcessor = new MessageProcessor();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
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
                if(!context.getRequest().isSoap()) return AssertionStatus.FAILED;

                // DELETE CURRENT SECURITY HEADER IF NECESSARY
                handleProcessedSecurityHeader(context,
                                              bridgeRoutingAssertion.getCurrentSecurityHeaderHandling(),
                                              bridgeRoutingAssertion.getXmlSecurityActorToPromote());

                if (bridgeRoutingAssertion.isTaiCredentialChaining()) {
                    throw new PolicyAssertionException(data, "BridgeRoutingAssertion unable to support TAI credential chaining");
                }

                if (bridgeRoutingAssertion.isAttachSamlSenderVouches()) {
                    attachSamlSenderVouches(context);
                }

                try {
                    // TODO support non-SOAP messaging with SSB api
                    String soapAction = context.getRequest().getHttpRequestKnob().getHeaderSingleValue(SoapUtil.SOAPACTION);
                    String[] uris = context.getRequest().getSoapKnob().getPayloadNamespaceUris();
                    // TODO decide what to do if there are multiple payload namespace URIs 
                    String nsUri = uris == null || uris.length < 1 ? null : uris[0];

                    // TODO support SOAP-with-attachments with SSB api
                    if (context.getRequest().getMimeKnob().isMultipart())
                        auditor.logAndAudit(AssertionMessages.BRIDGEROUTE_NO_ATTACHMENTS);

                    final HttpRequestKnob httpRequestKnob = context.getRequest().getHttpRequestKnob();

                    URL origUrl = DEFAULT_ORIG_URL;
                    if(httpRequestKnob != null) {
                        try {
                            origUrl = new URL(httpRequestKnob.getRequestUrl());
                        } catch (MalformedURLException e) {
                            auditor.logAndAudit(AssertionMessages.HTTPROUTE_BAD_ORIGINAL_URL);
                        }
                    }

                    PolicyAttachmentKey pak = new PolicyAttachmentKey(nsUri, soapAction, origUrl.getPath());
                    Message bridgeRequest = context.getRequest(); // TODO see if it is unsafe to reuse this

                    // The response will need to be re-initialized
                    Message bridgeResponse = context.getResponse(); // TODO see if it is unsafe to reuse this

                    HeaderHolder hh = new HeaderHolder();
                    PolicyApplicationContext pac = newPolicyApplicationContext(context, bridgeRequest, bridgeResponse, pak, origUrl, hh);
                    messageProcessor.processMessage(pac);

                    final HttpResponseKnob hrk = bridgeResponse.getHttpResponseKnob();
                    int status = hrk == null ? HttpConstants.STATUS_SERVER_ERROR : hrk.getStatus();
                    if (status == HttpConstants.STATUS_OK)
                        auditor.logAndAudit(AssertionMessages.HTTPROUTE_OK);
                    else
                        auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS, new String[] {url.getPath(), String.valueOf(status)});

                    context.getResponse().getHttpResponseKnob().setStatus(status);

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
                } catch (HttpChallengeRequiredException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e); // can't happen
                } catch (ResponseValidationException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                } catch (ClientCertificateException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e); // can't happen
                } catch (ProcessorException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                } catch (PolicyLockedException e) {
                    thrown = e;
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING, null, e);
                }
            } catch (WSDLException we) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, we);
            } catch (MalformedURLException mfe) {
                thrown = mfe;
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, mfe);
            } catch (IOException ioe) {
                // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
                thrown = ioe;
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, ioe);
            } catch (SAXException e) {
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

    private static final URL DEFAULT_ORIG_URL;

    static {
        try {
            DEFAULT_ORIG_URL = new URL("http://layer7tech.com/no-original-uri");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private final BridgeRoutingAssertion bridgeRoutingAssertion;
    private final SignerInfo signerInfo;
    private final Ssg ssg;
    private final MessageProcessor messageProcessor;
    private X509Certificate serverCert;
    private final Auditor auditor;
    private final boolean useClientCert;

    private URL getUrl() {
        try {
            return new URL(bridgeRoutingAssertion.getProtectedServiceUrl());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(" URL is invalid; assertion is therefore nonfunctional.");
        }
    }

    private Policy getHardCodedPolicy() {
        Policy hardcodedPolicy = null;

        String policyXml = bridgeRoutingAssertion.getPolicyXml();
        if (policyXml != null) {
            try {
                Assertion a = WspReader.parsePermissively(policyXml);
                hardcodedPolicy = new Policy(a, null);
                hardcodedPolicy.setAlwaysValid(true);
            } catch (IOException e) {
                throw new IllegalStateException("Hardcoded policy is invalid; assertion is therefore nonfunctional.",e);
            }
        }

        return hardcodedPolicy;
    }

    private boolean areValidUrlHostnames(String[] addrs, Auditor auditor) {
        for (int i = 0; i < addrs.length; i++) {
            String addr = addrs[i];
            try {
                new URL("http", addr, 777, "/foo/bar");
            } catch (MalformedURLException e) {
                auditor.logAndAudit(AssertionMessages.IP_ADDRESS_INVALID, new String[] { addr });
                return false;
            }
        }
        return true;
    }

    private Ssg newSSG(String gatewayHostname) {
        return new Ssg(1, gatewayHostname) {
            public synchronized String getKeyStorePath() {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a key store path");
            }

            public void setKeyStorePath(String keyStorePath) {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a key store path");
            }

            public synchronized String getTrustStorePath() {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a trust store path");
            }

            public void setTrustStorePath(String trustStorePath) {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a trust store path");
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

    private void initSSGPorts(URL url) {
        int port = url.getPort();
        int normalPort;
        int sslPort;

        // TODO make ports fully configurable
        // TODO move this heuristic elsewhere
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

        ssg.setSsgPort(normalPort);
        ssg.setSslPort(sslPort);

    }

    private CredentialManager newCredentialManager(final SslClientTrustManager trustManager) {
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
                throw new OperationCanceledException(((AuditDetailMessage)AssertionMessages.HTTPROUTE_ACCESS_DENIED).getMessage());
            }
        };
    }

    private SimpleHttpClient newHttpClient() {
        // Set up HTTP client (use commons client)
        GenericHttpClient client = new CommonsHttpClient(ssg.getRuntime().getHttpConnectionManager(), getConnectionTimeout(), getTimeout());

        // Attach SSL support
        client = new SslPeerHttpClient(client,
                                       ssg,
                                       ClientProxySecureProtocolSocketFactory.getInstance());

        if (ssg.isUseOverrideIpAddresses()) {
            // Attach failover client
            FailoverStrategy strategy;
            String[] addrs = ssg.getOverrideIpAddresses();
            try {
                strategy = FailoverStrategyFactory.createFailoverStrategy(bridgeRoutingAssertion.getFailoverStrategyName(), addrs);
            } catch (IllegalArgumentException e) {
                strategy = new StickyFailoverStrategy(addrs);
            }
            int attempts = addrs.length;
            client = new FailoverHttpClient(client, strategy, attempts, logger);
        }

        return new SimpleHttpClient(client);
    }

    private void attachSamlSenderVouches(PolicyEnforcementContext context) throws SAXException, IOException, SignatureException, CertificateException{
        Document document = context.getRequest().getXmlKnob().getDocumentWritable();
        SamlAssertionGenerator ag = new SamlAssertionGenerator(signerInfo);
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        TcpKnob requestTcp = (TcpKnob)context.getRequest().getKnob(TcpKnob.class);
        if (requestTcp != null) {
            try {
                InetAddress clientAddress = InetAddress.getByName(requestTcp.getRemoteAddress());
                samlOptions.setClientAddress(clientAddress);
            } catch (UnknownHostException e) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_CANT_RESOLVE_IP, null, e);
            }
        }
        samlOptions.setExpiryMinutes(bridgeRoutingAssertion.getSamlAssertionExpiry());
        samlOptions.setUseThumbprintForSignature(bridgeRoutingAssertion.isUseThumbprintInSamlSignature());
        SubjectStatement statement = SubjectStatement.createAuthenticationStatement(context.getCredentials(), SubjectStatement.SENDER_VOUCHES, bridgeRoutingAssertion.isUseThumbprintInSamlSubject());
        ag.attachStatement(document, statement, samlOptions);

    }

    private boolean initCredentials() {
        boolean useClientCert;
        String username = bridgeRoutingAssertion.getLogin();
        char[] password = null;

        if (username != null) {
            String pass = bridgeRoutingAssertion.getPassword();
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
        String psurl = bridgeRoutingAssertion.getProtectedServiceUrl();
        if (psurl == null) {
            URL wsdlUrl = service.serviceUrl();
            url = wsdlUrl;
        } else {
            url = new URL(psurl);
        }
        return url;
    }

    private PolicyApplicationContext newPolicyApplicationContext(final PolicyEnforcementContext context, Message bridgeRequest, Message bridgeResponse, PolicyAttachmentKey pak, URL origUrl, final HeaderHolder hh) {
        return new PolicyApplicationContext(ssg, bridgeRequest, bridgeResponse, NullRequestInterceptor.INSTANCE, pak, origUrl) {
            public HttpCookie[] getSessionCookies() {
                Set cookies = bridgeRoutingAssertion.isCopyCookies() ? context.getCookies() : Collections.EMPTY_SET;
                return (HttpCookie[]) cookies.toArray(new HttpCookie[cookies.size()]);
            }

            public void setSessionCookies(HttpCookie[] cookies) {
                if(bridgeRoutingAssertion.isCopyCookies()) {
                    //add or replace cookies
                    for (int i = 0; i < cookies.length; i++) {
                        HttpCookie cookie = cookies[i];
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
     * Create an extended SimpleHttpClient that supports re-routing
     *
     * NOTE: this is NOT compatible with a non-sticky failover client.
     */
    private SimpleHttpClient newRRLSimpleHttpClient(final PolicyEnforcementContext context, final SimpleHttpClient client, final RoutingResultListener rrl, final HeaderHolder hh) {
        return new SimpleHttpClient(client) {
            public GenericHttpRequest createRequest(final GenericHttpMethod method, final GenericHttpRequestParams params) throws GenericHttpException {
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

                    /**
                     * Get the response, re-routing if directed to do so by a listener.
                     *
                     * @return
                     * @throws GenericHttpException
                     */
                    public GenericHttpResponse getResponse() throws GenericHttpException {
                        getInputStreamFactory(); // init isf
                        return doGetResponse(true);
                    }

                    private RerunnableHttpRequest.InputStreamFactory getInputStreamFactory() throws GenericHttpException {
                        if(isf==null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            try {
                                HexUtils.copyStream(is, baos);
                            }
                            catch(IOException ioe) {
                                throw new GenericHttpException("Cannot read request data.", ioe);
                            }
                            final byte[] data = baos.toByteArray();
                            isf = new RerunnableHttpRequest.InputStreamFactory() {
                                public InputStream getInputStream() {
                                    return new ByteArrayInputStream(data);
                                }
                            };
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
                                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS_HANDLED, new String[]{params.getTargetUrl().getPath(), Integer.toString(status)});

                                    //TODO if we refactor the BRA we should clean this up (params changed by this SimpleHttpClient impl [HACK])
                                    HttpHeader[] headers = params.getExtraHeaders();
                                    HttpHeader newCookieHeader = new GenericHttpHeader(HttpConstants.HEADER_COOKIE, getSessionCookiesHeaderValue(context));
                                    HttpHeader[] newHeaders = combineHeaders(headers, newCookieHeader);
                                    params.setExtraHeaders(newHeaders);

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
        };
    }

    private HttpHeader[] combineHeaders(HttpHeader[] headers, HttpHeader header) {
        List newHeaders = new ArrayList();

        for(int h=0; h<headers.length; h++) {
            if(!headers[h].getName().equalsIgnoreCase(header.getName())) {
                newHeaders.add(headers[h]);
            }
        }

        newHeaders.add(header);

        return (HttpHeader[]) newHeaders.toArray(new HttpHeader[newHeaders.size()]);
    }

    /**
     * Get the cookies as a string.
     *
     * @return a string like "foo=bar; baz=blat; bloo=blot".  May be empty, but never null.
     */
    private String getSessionCookiesHeaderValue(final PolicyEnforcementContext context) {
        StringBuffer sb = new StringBuffer();

        Set cookies = context.getCookies();
        if (cookies != null) {
            for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
                HttpCookie cook = (HttpCookie) iterator.next();;
                sb.append(cook.getV0CookieHeaderPart());
                if (iterator.hasNext()) sb.append("; ");
            }
        }

        return sb.toString();
    }

    private class HeaderHolder {
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
                return CertUtils.extractCommonNameFromClientCertificate(getClientCert());
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

        public PrivateKey getClientCertPrivateKey() throws NoSuchAlgorithmException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException {
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

        public void importClientCertificate(File certFile, char[] pass, AliasPicker aliasPicker, char[] ssgPassword) throws IOException, GeneralSecurityException, KeyStoreCorruptException, AliasNotFoundException {
            throw new UnsupportedOperationException("Gateway is unable to import a client certificate");
        }

        public void installSsgServerCertificate(Ssg ssg, PasswordAuthentication credentials) throws IOException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException, CertificateException, KeyStoreException {
            if (bridgeRoutingAssertion.getServerCertBase64() == null) {
                super.installSsgServerCertificate(ssg, credentials);
                return;
            }

            if (serverCert == null)
                throw new CertificateException("Hardcoded server cert was not trusted by the Bridge Routing Assertion"); // shouldn't be possible

            saveSsgCertificate(serverCert);
        }
    }
}
