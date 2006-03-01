package com.l7tech.server;

import com.l7tech.common.message.Message;
import com.l7tech.common.message.SecurityKnob;
import com.l7tech.common.message.TcpKnob;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.X509SecurityToken;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.XencUtil;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.DecoratorException;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.SoapFaultDetailImpl;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.server.event.system.TokenServiceEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.DuplicateSessionException;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import org.springframework.context.support.ApplicationObjectSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Handles WS Trust RequestSecurityToken requests as well as SAML token requests.
 * The request is originally received by the TokenServiceServlet.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 6, 2004<br/>
 */
public class TokenServiceImpl extends ApplicationObjectSupport implements TokenService {
    private static final SecureRandom rand = new SecureRandom();
    private static final Logger logger = Logger.getLogger(TokenServiceImpl.class.getName());

    private final PrivateKey serverPrivateKey;
    private final X509Certificate serverCert;
    private final ServerAssertion tokenServicePolicy;
    private final SecurityTokenResolver securityTokenResolver;

    /**
     * specify the server key and cert at construction time instead of letting the object try to retreive them
     */
    public TokenServiceImpl(PrivateKey privateServerKey, X509Certificate serverCert, ServerPolicyFactory policyFactory, SecurityTokenResolver securityTokenResolver) {
        if (privateServerKey == null || serverCert == null) {
            throw new IllegalArgumentException("Server key and server cert must be provided to create a TokenService");
        }

        this.serverPrivateKey = privateServerKey;
        this.serverCert = serverCert;
        this.securityTokenResolver = securityTokenResolver;
        this.tokenServicePolicy = policyFactory.makeServerPolicy(getGenericEnforcementPolicy());
    }

    public class TokenServiceException extends Exception {
        public TokenServiceException(Throwable cause) {
            super(cause);
        }
        public TokenServiceException(String message) {
            super(message);
        }
        public TokenServiceException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    /**
     * Handles token service requests using a PolicyEnforcementContext. The policy enforced allows for requests
     * secured in the message or secured in the transport layer. If the response contains a secret, it will be
     * secured as part of the message layer if the request was secured that same way. This allows for such
     * requests to occur over SSL without the need for client certs.
     *
     * @param context contains the request at entry and contains a response document if everything goes well.
     * @return AssertionStatus.NONE if all is good, other return values indicate an error in which case
     * context.getFaultDetail() is to contain an error to return to the requestor.
     */
    public AssertionStatus respondToSecurityTokenRequest(PolicyEnforcementContext context,
                                                         CredentialsAuthenticator authenticator,
                                                         boolean useThumbprintForSamlSignature,
                                                         boolean useThumbprintForSamlSubject)
                                                         throws InvalidDocumentFormatException,
                                                                TokenServiceImpl.TokenServiceException,
                                                                ProcessorException,
                                                                BadSecurityContextException,
                                                                GeneralSecurityException,
                                                                AuthenticationException {
        AssertionStatus status = AssertionStatus.UNDEFINED;
        Throwable toAudit = null;
        try {
            try {
                WssProcessor trogdor = new WssProcessorImpl();
                final SecurityKnob reqSec = context.getRequest().getSecurityKnob();
                ProcessorResult wssOutput = trogdor.undecorateMessage(context.getRequest(),
                                                                      null,
                                                                      serverCert,
                                                                      serverPrivateKey,
                                                                      SecureConversationContextManager.getInstance(),
                                                                      securityTokenResolver);
                reqSec.setProcessorResult(wssOutput);
            } catch (IOException e) {
                status = AssertionStatus.BAD_REQUEST;
                throw new ProcessorException(e);
            } catch (SAXException e) {
                status = AssertionStatus.BAD_REQUEST;
                throw new InvalidDocumentFormatException(e);
            }

            try {
                status = tokenServicePolicy.checkRequest(context);
            } catch (IOException e) {
                throw new ProcessorException(e);
            } catch (PolicyAssertionException e) {
                status = AssertionStatus.FAILED;
                throw new ProcessorException(e);
            }

            // at this point, we should have credentials
            LoginCredentials creds = context.getCredentials();
            User authenticatedUser = null;
            if(creds!=null) {
                authenticatedUser = authenticator.authenticate(creds);
                if (authenticatedUser != null) {
                    context.setAuthenticated(true);
                    context.setAuthenticatedUser(authenticatedUser);
                }
            }

            if (authenticatedUser == null) {
                status = AssertionStatus.AUTH_FAILED;
                String msg = "The request for a token was not authenticated";
                logger.info(msg);
                context.setFaultDetail(new SoapFaultDetailImpl("l7:noauthentication", msg, null));
                return status;
            }

            if (status != AssertionStatus.NONE) {
                String msg = "The internal policy was not respected " + status;
                logger.info(msg);
                if (context.getFaultDetail() == null) {
                    context.setFaultDetail(new SoapFaultDetailImpl("l7:" + status.getMessage(), msg, null));
                }
                return status;
            }
            Map rstTypes = getTrustTokenTypeAndRequestTypeValues(context);
            Document response;
            if (isRequestForSecureConversationContext(rstTypes) || isRequestForSecureConversationContext0502(rstTypes)) {
                response = handleSecureConversationContextRequest(rstTypes, context, authenticatedUser);
            } else if (isRequestForSAML20Token(rstTypes)) {
                // todo, implement this
                String msg = "SAML 2.0 token requested but not yet supported.";
                logger.info(msg);
                context.setFaultDetail(new SoapFaultDetailImpl(SoapFaultUtils.FC_SERVER, msg, null));
                return AssertionStatus.NOT_YET_IMPLEMENTED;
            } else if (isRequestForSAMLToken(rstTypes)) {
                response = handleSamlRequest(rstTypes, context, useThumbprintForSamlSignature, useThumbprintForSamlSubject);
            } else {
                status = AssertionStatus.BAD_REQUEST;
                throw new InvalidDocumentFormatException("This request cannot be recognized as a valid " +
                                                         "RequestSecurityToken");
            }
            // put response document back into context
            context.getResponse().initialize(response);
            return status;
        }
        catch(GeneralSecurityException gse) {
            toAudit = gse;
            throw gse;
        }
        catch(TokenServiceException tse) {
            toAudit = tse;
            throw tse;
        }
        finally {
            try {
                String message = toAudit==null && status==AssertionStatus.NONE ? "Security Token Issued" : "Security Token Error";
                if(toAudit instanceof TokenServiceException) message += ": " + toAudit.getMessage();
                else if(status!=AssertionStatus.NONE) message += ": " + status.getMessage();
                else message += ": processing error";
                User user = getUser(context);
                getApplicationContext().publishEvent(new TokenServiceEvent(this, Level.INFO, getRemoteAddress(context)
                                                    , message, user.getProviderId()
                                                    , getName(user), user.getUniqueIdentifier()));
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Error dispatching event", e);
            }
        }
    }

    /**
     * Constructs the following policy (server-side version)<br/>
     * <pre>
     * AllAssertion:
     *   OneOrMoreAssertion:
     *     AllAssertion:
     *       RequestWssIntegrity pattern=/soapenv:Envelope/soapenv:Body
     *       OneOrMoreAssertion:
     *         RequestWssX509Cert
     *         SamlSecurity
     *         SecureConversation
     *     AllAssertion:
     *       SslAssertion
     *       OneOrMoreAssertion:
     *         HttpBasic
     *         WssBasic
     *         HttpDigest
     *         HttpClientCert
     * </pre>
     */
    private Assertion getGenericEnforcementPolicy() {
        AllAssertion base = new AllAssertion();
        OneOrMoreAssertion root = new OneOrMoreAssertion();

        AllAssertion msgLvlBranch = new AllAssertion();
        OneOrMoreAssertion validCredsOverMsgLvlSec = new OneOrMoreAssertion();
        validCredsOverMsgLvlSec.addChild(new RequestWssX509Cert());
        validCredsOverMsgLvlSec.addChild(new RequestWssSaml());
        validCredsOverMsgLvlSec.addChild(new SecureConversation());
        msgLvlBranch.addChild(validCredsOverMsgLvlSec);
        msgLvlBranch.addChild(new RequestWssIntegrity());

        AllAssertion sslBranch = new AllAssertion();
        sslBranch.addChild(new SslAssertion());
        OneOrMoreAssertion validCredsOverSSL = new OneOrMoreAssertion();
        validCredsOverSSL.addChild(new HttpBasic());
        validCredsOverSSL.addChild(new WssBasic());
        validCredsOverSSL.addChild(new HttpDigest());
        validCredsOverSSL.addChild(new SslAssertion(true));
        RequestWssSaml samlBearerToken = new RequestWssSaml();
        validCredsOverSSL.addChild(samlBearerToken);
        sslBranch.addChild(validCredsOverSSL);

        root.addChild(msgLvlBranch);
        root.addChild(sslBranch);

        base.addChild(root);

        logger.fine("TokenService enforcing policy: " + base.toString());
        return base;
    }

    private Document handleSamlRequest(Map rstTypes, PolicyEnforcementContext context,
                                       boolean useThumbprintForSignature, boolean useThumbprintForSubject)
                                       throws TokenServiceException, GeneralSecurityException
    {
        String clientAddress = null;
        TcpKnob tcpKnob = context.getRequest().getTcpKnob();
        if (tcpKnob != null)
            clientAddress = tcpKnob.getRemoteAddress();
        LoginCredentials creds = context.getCredentials();

        StringBuffer responseXml = new StringBuffer(rstResponsePrefix((String)rstTypes.get(TRUSTNS), (String)rstTypes.get(SCNS)));
        try {
            SamlAssertionGenerator.Options options = new SamlAssertionGenerator.Options();
            if (clientAddress != null) options.setClientAddress(InetAddress.getByName(clientAddress));
            options.setUseThumbprintForSignature(useThumbprintForSignature);
            options.setSignAssertion(true);
            SignerInfo signerInfo = new SignerInfo(serverPrivateKey, new X509Certificate[] { serverCert });
            SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(creds, SubjectStatement.HOLDER_OF_KEY, useThumbprintForSubject);
            SamlAssertionGenerator generator = new SamlAssertionGenerator(signerInfo);
            Document signedAssertionDoc = generator.createAssertion(subjectStatement, options);
            responseXml.append(XmlUtil.nodeToString(signedAssertionDoc));
            responseXml.append(WST_RST_RESPONSE_INFIX);
            responseXml.append(WST_RST_RESPONSE_SUFFIX);
            Document response = XmlUtil.stringToDocument(responseXml.toString());
            return prepareSignedResponse(response);
        } catch ( UnknownHostException e ) {
            throw new TokenServiceException("Couldn't resolve client IP address", e);
        } catch ( IOException e ) {
            throw new TokenServiceException("Couldn't read signing key", e);
        } catch ( SAXException e ) {
            throw new TokenServiceException("Couldn't read signing key", e);
        }
    }

    private Document handleSecureConversationContextRequest(Map rstTypes, PolicyEnforcementContext context,
                                                            User requestor) throws TokenServiceException, GeneralSecurityException {
        SecureConversationSession newSession;
        try {
            newSession = SecureConversationContextManager.getInstance().createContextForUser(requestor, context.getCredentials());
        } catch (DuplicateSessionException e) {
            throw new TokenServiceException(e);
        }

        ProcessorResult wssOutput;
        wssOutput = context.getRequest().getSecurityKnob().getProcessorResult();

        SecurityToken[] tokens = wssOutput.getXmlSecurityTokens();
        X509Certificate clientCert = null;
        for (int i = 0; i < tokens.length; i++) {
            SecurityToken token = tokens[i];
            if (token instanceof X509SecurityToken) {
                X509SecurityToken x509token = (X509SecurityToken)token;
                if (x509token.isPossessionProved()) {
                    if (clientCert != null) {
                        String msg = "Request included more than one X509 security token whose key ownership " +
                                     "was proven";
                        logger.log(Level.WARNING,  msg);
                        throw new TokenServiceException(msg);
                    }
                    clientCert = x509token.getCertificate();
                }
            }
        }

        // different response formats based on whether the request should be encrypted against a requesting cert
        // or whether the response's secret should be in clear and encrypted at transport level.
        Document response;
        Calendar exp = Calendar.getInstance();
        exp.setTimeInMillis(newSession.getExpiration());
        String secretXml; // either an encryptedkey element or a binarysecret element
        if (clientCert != null) {
            secretXml = produceEncryptedKeyXml(newSession.getSharedSecret(), clientCert);
        } else {
            secretXml = produceBinarySecretXml(newSession.getSharedSecret(), (String)rstTypes.get(TRUSTNS));
        }
        try {
            String xmlStr = rstResponsePrefix((String)rstTypes.get(TRUSTNS), (String)rstTypes.get(SCNS)) +
                                      "<wsc:SecurityContextToken>" +
                                        "<wsc:Identifier>" + newSession.getIdentifier() + "</wsc:Identifier>" +
                                      "</wsc:SecurityContextToken>" +
                                      WST_RST_RESPONSE_INFIX +
                                    "<wst:RequestedProofToken>" +
                                      secretXml +
                                    "</wst:RequestedProofToken>" +
                                    "<wst:Lifetime>" +
                                      "<wsu:Expires>" + ISO8601Date.format(exp.getTime()) + "</wsu:Expires>" +
                                    "</wst:Lifetime>" +
                                    WST_RST_RESPONSE_SUFFIX;
            response = XmlUtil.stringToDocument(xmlStr);
        } catch (SAXException e) {
            throw new TokenServiceException(e);
        }
        return prepareSignedResponse(response);
    }

    private Document prepareSignedResponse( Document response ) throws TokenServiceException {
        Element body;
        try {
            body = SoapUtil.getBodyElement(response);
        } catch (InvalidDocumentFormatException e) {
            throw new TokenServiceException(e);
        }

        WssDecorator wssDecorator = new WssDecoratorImpl();
        DecorationRequirements req = new DecorationRequirements();
        req.setSignTimestamp();
        req.setSenderMessageSigningCertificate(serverCert);
        req.setSenderMessageSigningPrivateKey(serverPrivateKey);
        req.getElementsToSign().add(body);

        try {
            wssDecorator.decorateMessage(response, req);
        } catch (InvalidDocumentFormatException e) {
            throw new TokenServiceException(e);
        } catch (GeneralSecurityException e) {
            throw new TokenServiceException(e);
        } catch (DecoratorException e) {
            throw new TokenServiceException(e);
        }
        return response;
    }

    private String produceBinarySecretXml(SecretKey sharedSecret, String trustns) {
        StringBuffer output = new StringBuffer();
        output.append("<wst:BinarySecret Type=\"" + trustns + "/SymmetricKey" + "\">");
        byte[] actualkey = sharedSecret.getEncoded();
        output.append(HexUtils.encodeBase64(actualkey, true));
        output.append("</wst:BinarySecret>");
        return output.toString();
    }

    private String produceEncryptedKeyXml(SecretKey sharedSecret, X509Certificate requestorCert) throws GeneralSecurityException {
        StringBuffer encryptedKeyXml = new StringBuffer();
        // Key info and all
        encryptedKeyXml.append("<xenc:EncryptedKey wsu:Id=\"newProof\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">" +
                                 "<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\" />");

        // append ski if applicable
        String recipSkiB64 = CertUtils.getSki(requestorCert);
        if (recipSkiB64 != null) {
            // add the ski
            String skiRef = "<wsse:SecurityTokenReference>" +
                              "<wsse:KeyIdentifier ValueType=\"" + SoapUtil.VALUETYPE_SKI + "\">" +
                                recipSkiB64 +
                              "</wsse:KeyIdentifier>" +
                            "</wsse:SecurityTokenReference>";

            encryptedKeyXml.append("<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">");
            encryptedKeyXml.append(skiRef);
            encryptedKeyXml.append("</KeyInfo>");
        } else {
            // add a full cert ?
            // todo
        }
        encryptedKeyXml.append("<xenc:CipherData>" +
                                "<xenc:CipherValue>");
        String encryptedKeyValue = HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaAndPad(sharedSecret.getEncoded(),
                                                                    requestorCert.getPublicKey(),
                                                                    rand),
                                                         true);
        encryptedKeyXml.append(encryptedKeyValue);
        encryptedKeyXml.append("</xenc:CipherValue>" +
                             "</xenc:CipherData>" +
                           "</xenc:EncryptedKey>");
        return encryptedKeyXml.toString();
    }

    /**
     * checks if this request is for a sec conv context
     * does not check things like whether the body is signed since this is the
     * responsibility of the policy
     */
    private boolean isRequestForSecureConversationContext(Map rstTypes) {
        String val = (String)rstTypes.get(SoapUtil.WST_TOKENTYPE);
        if (val == null || !"http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct".equals(val)) {
            return false;
        }
        val = (String)rstTypes.get(SoapUtil.WST_REQUESTTYPE);
        if (val == null || !val.endsWith("/trust/Issue")) {
            logger.warning("RequestType not supported." + val);
            return false;
        }
        // will be set to WSSC_NAMESPACE2 if caught by isRequestForSecureConversationContext0502
        rstTypes.put(SCNS, SoapUtil.WSSC_NAMESPACE);
        return true;
    }

    /**
     * checks if this request is for a sec conv context
     * does not check things like whether the body is signed since this is the
     * responsibility of the policy
     */
    private boolean isRequestForSecureConversationContext0502(Map rstTypes) {
        String val = (String)rstTypes.get(SoapUtil.WST_TOKENTYPE);
        if (val == null || !"http://schemas.xmlsoap.org/ws/2005/02/sc/sct".equals(val)) {
            return false;
        }
        if (val == null || !val.endsWith("/trust/Issue")) {
            logger.warning("RequestType not supported." + val);
            return false;
        }
        // will be set to WSSC_NAMESPACE if caught by isRequestForSecureConversationContext
        rstTypes.put(SCNS, SoapUtil.WSSC_NAMESPACE2);
        val = (String)rstTypes.get(SoapUtil.WST_REQUESTTYPE);
        return true;
    }

    /**
     * Checks whether this request is meant to be for a SAML version 2.0 token.
     * expected value of   TokenType is 'urn:oasis:names:tc:SAML:2.0:assertion#Assertion',
     * expected value of RequestType is 'http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue'
     */
    private boolean isRequestForSAML20Token(Map rstTypes) {
        String tokenType = (String)rstTypes.get(SoapUtil.WST_TOKENTYPE);
        if (tokenType == null ||
            !tokenType.startsWith("urn:oasis:names:tc:SAML:2.0:assertion") ||
            !tokenType.endsWith("Assertion")) {
            return false;
        }
        String requestType = (String)rstTypes.get(SoapUtil.WST_REQUESTTYPE);
        if (requestType == null || !requestType.endsWith("/trust/Issue")) {
            return false;
        }
        logger.fine("Request was identified as one for SAML 2.0 Token");
        return true;
    }

    /**
     * Retrieves soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:TokenType/text() and puts it in the return map
     * under the SoapUtil.WST_TOKENTYPE key.
     * Retrieves soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:RequestType/text() and puts it in the return map
     * under the SoapUtil.WST_REQUESTTYPE key.
     *
     * The old way was using too much code duplication and was too innefficient.
     */
    private Map getTrustTokenTypeAndRequestTypeValues(PolicyEnforcementContext context) throws InvalidDocumentFormatException {
        Map output = new HashMap();
        // initial value important (dont remove)
        output.put(SCNS, SoapUtil.WSSC_NAMESPACE);
        Document doc;
        try {
            XmlKnob knob = context.getRequest().getXmlKnob();
            doc = knob.getDocumentReadOnly();
        } catch (SAXException e) {
            // if we can't get the doc, then the request must be bad
            logger.log(Level.WARNING, "Cannot get request's document", e);
            return output;
        } catch (IOException e) {
            // if we can't get the doc, then the request must be bad
            logger.log(Level.WARNING, "Cannot get request's document", e);
            return output;
        }
        Element body = SoapUtil.getBodyElement(doc);
        Element maybeRSTEl = XmlUtil.findFirstChildElement(body);

        // body must include wst:RequestSecurityToken element
        if (!maybeRSTEl.getLocalName().equals(SoapUtil.WST_REQUESTSECURITYTOKEN)) {
            logger.fine("Body's child does not seem to be a RST (" + maybeRSTEl.getLocalName() + ")");
            return output;
        }
        if (!Arrays.asList(SoapUtil.WST_NAMESPACE_ARRAY).contains(maybeRSTEl.getNamespaceURI())) {
            logger.fine("Trust namespace not recognized (" + maybeRSTEl.getNamespaceURI() + ")");
            return output;
        }
        output.put(TRUSTNS, maybeRSTEl.getNamespaceURI());
        Element tokenTypeEl = XmlUtil.findOnlyOneChildElementByName(maybeRSTEl, SoapUtil.WST_NAMESPACE_ARRAY, SoapUtil.WST_TOKENTYPE);
        if (tokenTypeEl == null) {
            logger.warning("Token type not specified. This is not supported.");
        } else {
            output.put(SoapUtil.WST_TOKENTYPE, XmlUtil.getTextValue(tokenTypeEl));
        }

        Element reqTypeEl = XmlUtil.findOnlyOneChildElementByName(maybeRSTEl, SoapUtil.WST_NAMESPACE_ARRAY, SoapUtil.WST_REQUESTTYPE);
        if (reqTypeEl == null) {
            logger.warning("Request type not specified. This is not supported.");
        } else {
            output.put(SoapUtil.WST_REQUESTTYPE, XmlUtil.getTextValue(reqTypeEl));
        }
        return output;
    }

    /** Regexp that recognizes all known SAML token type URIs and qnames. */
    private final Pattern PSAML = Pattern.compile("[:#]Assertion$|^SAML$");

     /**
     * checks if this request is for a saml assertion
     * does not check things like whether the body is signed since this is the
     * responsibility of the policy
     */
    private boolean isRequestForSAMLToken(Map rstTypes) {
        String tokenType = (String)rstTypes.get(SoapUtil.WST_TOKENTYPE);
        if (tokenType == null || !PSAML.matcher(tokenType).find()) {
            logger.fine("TokenType '" + tokenType + "' is not recognized as calling for a saml:Assertion");
            return false;
        }
        String requestType = (String)rstTypes.get(SoapUtil.WST_REQUESTTYPE);
        if (requestType == null || !requestType.endsWith("/trust/Issue")) {
            return false;
        }
        return true;
    }

    private String getRemoteAddress(PolicyEnforcementContext pec) {
        String ip = null;

        if(pec!=null) {
            Message message = pec.getRequest();
            if(message.isHttpRequest()) {
                ip = message.getHttpRequestKnob().getRemoteAddress();
            }
        }

        return ip;
    }

    private User getUser(PolicyEnforcementContext context) {
        User user = null;

        if(context.isAuthenticated()) {
            user = context.getAuthenticatedUser();
        }

        if(user==null) {
            user = new UserBean();
        }

        return user;
    }

    private String getName(User user) {
        return user.getName()!=null ? user.getName() : user.getLogin();      
    }

    private static String rstResponsePrefix(String wstns, String wsscns) {
        return "<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\">" +
              "<soap:Body>" +
                "<wst:RequestSecurityTokenResponse xmlns:wst=\"" + wstns + "\" " +
                  "xmlns:wsu=\"" + SoapUtil.WSU_NAMESPACE + "\" " +
                  "xmlns:wsse=\"" + SoapUtil.SECURITY_NAMESPACE + "\" " +
                  "xmlns:wsc=\"" + wsscns + "\">" +
                  "<wst:RequestedSecurityToken>";
    }
    private static final String WST_RST_RESPONSE_INFIX =
                  "</wst:RequestedSecurityToken>";
    private static final String WST_RST_RESPONSE_SUFFIX =
                "</wst:RequestSecurityTokenResponse>" +
              "</soap:Body>" +
            "</soap:Envelope>";

    /**
     * a key in the Map returned by getTrustTokenTypeAndRequestTypeValues that tells us the ws trust ns value used in the RST
     * so that we can use the same ns in the response document
     */
    private static final String TRUSTNS = "trustns";
    private static final String SCNS = "scns";
}
