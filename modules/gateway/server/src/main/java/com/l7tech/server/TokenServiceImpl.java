package com.l7tech.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.message.TcpKnob;
import com.l7tech.message.XmlKnob;
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
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlAssertionGenerator;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.X509SecurityToken;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.*;
import com.l7tech.server.event.system.TokenServiceEvent;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.DuplicateSessionException;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.util.*;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.context.support.ApplicationObjectSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
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

    private final DefaultKey defaultKey;
    private final ServerAssertion tokenServicePolicy;
    private final SecurityTokenResolver securityTokenResolver;

    /**
     * specify the server key and cert at construction time instead of letting the object try to retreive them
     * @param defaultKey     used to find the default SSL private key
     * @param policyFactory  used to compile policies into server policies
     * @param securityTokenResolver used to locate security tokens
     */
    public TokenServiceImpl(DefaultKey defaultKey, ServerPolicyFactory policyFactory, SecurityTokenResolver securityTokenResolver) {
        if (defaultKey == null) {
            throw new IllegalArgumentException("DefaultKey must be provided to create a TokenService");
        }

        this.defaultKey = defaultKey;
        this.securityTokenResolver = securityTokenResolver;
        try {
            // Compile with license enforcement disabled (dogfood policy can use any assertion it wants)
            this.tokenServicePolicy = policyFactory.compilePolicy(getGenericEnforcementPolicy(), false);
        } catch (ServerPolicyException e) {
            throw new RuntimeException(e); // can't happen
        } catch ( LicenseException e) {
            throw new RuntimeException(e); // can't happen, we said no license enforcement
        }
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
     * context.getFaultLevel() is to contain a template for an error to return to the requestor.
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
            LoginCredentials creds = context.getLastCredentials();
            User authenticatedUser = null;
            if(creds!=null) {
                authenticatedUser = authenticator.authenticate(creds);
            }

            if (authenticatedUser == null) {
                status = AssertionStatus.AUTH_FAILED;
                logger.info("The request for a token was not authenticated");
                SoapFaultLevel fault = new SoapFaultLevel();
                fault.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
                if(context.getService() != null && context.getService().getSoapVersion() == SoapVersion.SOAP_1_2) {
                    fault.setFaultTemplate("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\" " +
                            "                  xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\">\n" +
                            "    <soapenv:Body>\n" +
                            "        <soapenv:Fault>\n" +
                            "            <soapenv:Code>\n" +
                            "                <soapenv:Value>l7:noauthentication</soapenv:Value>\n" +
                            "            </soapenv:Code>\n" +
                            "            <soapenv:Reason>\n" +
                            "                <soapenv:Text xml:lang=\"en-US\">The request for a token was not authenticated</soapenv:Text>\n" +
                            "            </soapenv:Reason>\n" +
                            "            <soapenv:Role>${request.url}</soapenv:Role>\n" +
                            "        </soapenv:Fault>\n" +
                            "    </soapenv:Body>\n" +
                            "</soapenv:Envelope>");
                } else {
                    fault.setFaultTemplate("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                            "                  xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\">\n" +
                            "    <soapenv:Body>\n" +
                            "        <soapenv:Fault>\n" +
                            "            <faultcode>l7:noauthentication</faultcode>\n" +
                            "            <faultstring>The request for a token was not authenticated</faultstring>\n" +
                            "            <faultactor>${request.url}</faultactor>\n" +
                            "        </soapenv:Fault>\n" +
                            "    </soapenv:Body>\n" +
                            "</soapenv:Envelope>");
                }
                context.setFaultlevel(fault);
                return status;
            }

            context.addAuthenticationResult(new AuthenticationResult(authenticatedUser, null, false));

            if (status != AssertionStatus.NONE) {
                String msg = "The internal policy was not respected " + status;
                logger.info(msg);
                return status;
            }
            Map<String, String> rstTypes = getTrustTokenTypeAndRequestTypeValues(context);
            Document response;
            if (isRequestForSecureConversationContext(rstTypes)) {
                response = handleSecureConversationContextRequest(rstTypes, context, authenticatedUser, SoapConstants.WSSC_NAMESPACE);
            } else if (isRequestForSecureConversationContext0502(rstTypes)) {
                response = handleSecureConversationContextRequest(rstTypes, context, authenticatedUser, SoapConstants.WSSC_NAMESPACE2);
            } else if (isRequestForSAML20Token(rstTypes)) {
                response = handleSamlRequest(rstTypes, SamlConstants.NS_SAML2, context, useThumbprintForSamlSignature, useThumbprintForSamlSubject);
            } else if (isRequestForSAMLToken(rstTypes)) {
                response = handleSamlRequest(rstTypes, SamlConstants.NS_SAML, context, useThumbprintForSamlSignature, useThumbprintForSamlSubject);
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
                boolean success = toAudit==null && status==AssertionStatus.NONE;
                String message = success ? "Security Token Issued" : "Security Token Error";
                if (!success) {
                    if(toAudit instanceof TokenServiceException) message += ": " + toAudit.getMessage();
                    else if(status!=AssertionStatus.NONE) message += ": " + status.getMessage();
                    else message += ": processing error";
                }
                User user = getUser(context);
                getApplicationContext().publishEvent(new TokenServiceEvent(this, Level.INFO, getRemoteAddress(context)
                                                    , message, user.getProviderId()
                                                    , getName(user), user.getId()));
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Error dispatching event", e);
            }
        }
    }

    /*
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

    private Document handleSamlRequest(Map rstTypes, String samlNs, PolicyEnforcementContext context,
                                       boolean useThumbprintForSignature, boolean useThumbprintForSubject)
                                       throws TokenServiceException, GeneralSecurityException
    {
        String clientAddress = null;
        TcpKnob tcpKnob = context.getRequest().getTcpKnob();
        if (tcpKnob != null)
            clientAddress = tcpKnob.getRemoteAddress();
        LoginCredentials creds = context.getLastCredentials();

        // Generate the SAML assertion
        SamlAssertionGenerator.Options options = new SamlAssertionGenerator.Options();
        if (clientAddress != null) try {
            options.setClientAddress(InetAddress.getByName(clientAddress));
        } catch (UnknownHostException e) {
            throw new TokenServiceException("Couldn't resolve client IP address", e);
        }
        options.setIssuerKeyInfoType(useThumbprintForSignature ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT);
        options.setSignAssertion(true);
        if (SamlConstants.NS_SAML2.equals(samlNs)) {
            options.setVersion(SamlAssertionGenerator.Options.VERSION_2);
        }
        SignerInfo signerInfo;
        try {
            signerInfo = defaultKey.getSslInfo();
        } catch (IOException e) {
            throw new TokenServiceException("Unable to get default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
        KeyInfoInclusionType keyInfoType = useThumbprintForSubject ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT;
        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(creds,
                                                                                           SubjectStatement.HOLDER_OF_KEY,
                                                                                           keyInfoType, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        
        // [Bugzilla #3616] the reason we are using this system property mechanism to pass this information is because
        // the saml generator is common code and can be also used in the bridge which does not have access to the
        // cluster config. doing this enables us to support this functionality in both the SSG and the bridge
        if (getApplicationContext() != null) {
            ServerConfig sg = (ServerConfig)getApplicationContext().getBean("serverConfig");
            if (sg != null) {
                int beforeoffset = sg.getIntProperty("samlBeforeOffsetMinute", 2);
                System.setProperty(SamlAssertionGenerator.BEFORE_OFFSET_SYSTEM_PROPERTY, Integer.toString(beforeoffset));
            }
        }
        SamlAssertionGenerator generator = new SamlAssertionGenerator(signerInfo);
        Document signedAssertionDoc = generator.createAssertion(subjectStatement, options);

        // Prepare the response
        StringBuffer responseXml = new StringBuffer(rstResponsePrefix((String)rstTypes.get(TRUSTNS), (String)rstTypes.get(SCNS)));
        try {
            responseXml.append( XmlUtil.nodeToString(signedAssertionDoc));
            responseXml.append(WST_RST_RESPONSE_INFIX);
            responseXml.append(WST_RST_RESPONSE_SUFFIX);
            Document response = XmlUtil.stringToDocument(responseXml.toString());
            return prepareSignedResponse(response);
        } catch ( IOException e ) {
            throw new TokenServiceException("Couldn't read signing key", e);
        } catch ( SAXException e ) {
            throw new TokenServiceException("Couldn't read signing key", e);
        }
    }

    private Document handleSecureConversationContextRequest(Map rstTypes, PolicyEnforcementContext context,
                                                            User requestor,
                                                            String scns) throws TokenServiceException, GeneralSecurityException {
        SecureConversationSession newSession;
        try {
            newSession = SecureConversationContextManager.getInstance().createContextForUser(requestor,
                                                                                             context.getLastCredentials(),
                                                                                             scns);
        } catch (DuplicateSessionException e) {
            throw new TokenServiceException(e);
        }

        ProcessorResult wssOutput;
        wssOutput = context.getRequest().getSecurityKnob().getProcessorResult();

        SecurityToken[] tokens = wssOutput.getXmlSecurityTokens();
        X509Certificate clientCert = null;
        for (SecurityToken token : tokens) {
            if (token instanceof X509SecurityToken) {
                X509SecurityToken x509token = (X509SecurityToken) token;
                if (x509token.isPossessionProved()) {
                    if (clientCert != null) {
                        String msg = "Request included more than one X509 security token whose key ownership " +
                                "was proven";
                        logger.log(Level.WARNING, msg);
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
        SsgKeyEntry signer;
        Element body;
        try {
            body = SoapUtil.getBodyElement(response);
            signer = defaultKey.getSslInfo();
        } catch (InvalidDocumentFormatException e) {
            throw new TokenServiceException(e);
        } catch (IOException e) {
            throw new TokenServiceException("Unable to get default SSL key: " + ExceptionUtils.getMessage(e), e);
        }

        WssDecorator wssDecorator = new WssDecoratorImpl();
        DecorationRequirements req = new DecorationRequirements();
        req.setSignTimestamp();
        req.getElementsToSign().add(body);

        try {
            req.setSenderMessageSigningCertificate(signer.getCertificate());
            req.setSenderMessageSigningPrivateKey(signer.getPrivateKey());
            wssDecorator.decorateMessage(new Message(response), req);
        } catch (InvalidDocumentFormatException e) {
            throw new TokenServiceException(e);
        } catch (GeneralSecurityException e) {
            throw new TokenServiceException(e);
        } catch (DecoratorException e) {
            throw new TokenServiceException(e);
        } catch (SAXException e) {
            throw new TokenServiceException(e);
        } catch (IOException e) {
            throw new TokenServiceException(e);
        }
        return response;
    }

    private String produceBinarySecretXml(byte[] sharedSecret, String trustns) {
        StringBuffer output = new StringBuffer();
        output.append("<wst:BinarySecret Type=\"").append(trustns).append("/SymmetricKey" + "\">");
        output.append(HexUtils.encodeBase64(sharedSecret, true));
        output.append("</wst:BinarySecret>");
        return output.toString();
    }

    private String produceEncryptedKeyXml(byte[] sharedSecret, X509Certificate requestorCert) throws GeneralSecurityException {
        StringBuffer encryptedKeyXml = new StringBuffer();
        // Key info and all
        encryptedKeyXml.append("<xenc:EncryptedKey wsu:Id=\"newProof\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">" +
                                 "<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\" />");

        // append ski if applicable
        String recipSkiB64 = CertUtils.getSki(requestorCert);
        if (recipSkiB64 != null) {
            // add the ski
            String skiRef = "<wsse:SecurityTokenReference>" +
                              "<wsse:KeyIdentifier ValueType=\"" + SoapConstants.VALUETYPE_SKI + "\">" +
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
        String encryptedKeyValue = HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaAndPad(sharedSecret, requestorCert, requestorCert.getPublicKey()),
                                                         true);
        encryptedKeyXml.append(encryptedKeyValue);
        encryptedKeyXml.append("</xenc:CipherValue>" +
                             "</xenc:CipherData>" +
                           "</xenc:EncryptedKey>");
        return encryptedKeyXml.toString();
    }

    /*
     * checks if this request is for a sec conv context
     * does not check things like whether the body is signed since this is the
     * responsibility of the policy
     */
    private boolean isRequestForSecureConversationContext(Map<String, String> rstTypes) {
        String val = rstTypes.get( SoapConstants.WST_TOKENTYPE);
        if (val == null || !"http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct".equals(val)) {
            return false;
        }
        val = rstTypes.get( SoapConstants.WST_REQUESTTYPE);
        if (val == null || !val.endsWith("/trust/Issue")) {
            logger.warning("RequestType not supported." + val);
            return false;
        }
        // will be set to WSSC_NAMESPACE2 if caught by isRequestForSecureConversationContext0502
        rstTypes.put(SCNS, SoapConstants.WSSC_NAMESPACE);
        return true;
    }

    /*
     * checks if this request is for a sec conv context
     * does not check things like whether the body is signed since this is the
     * responsibility of the policy
     */
    private boolean isRequestForSecureConversationContext0502(Map<String, String> rstTypes) {
        String val = rstTypes.get( SoapConstants.WST_TOKENTYPE);
        if (val == null || !"http://schemas.xmlsoap.org/ws/2005/02/sc/sct".equals(val)) {
            return false;
        }
        String requestType = rstTypes.get( SoapConstants.WST_REQUESTTYPE);
        if (!requestType.endsWith("/trust/Issue")) {
            logger.warning("RequestType not supported." + requestType);
            return false;
        }
        // will be set to WSSC_NAMESPACE if caught by isRequestForSecureConversationContext
        rstTypes.put(SCNS, SoapConstants.WSSC_NAMESPACE2);
        return true;
    }

    /*
     * Checks whether this request is meant to be for a SAML version 2.0 token.
     * expected value of   TokenType is 'urn:oasis:names:tc:SAML:2.0:assertion#Assertion',
     * expected value of RequestType is 'http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue'
     */
    private boolean isRequestForSAML20Token(Map rstTypes) {
        String tokenType = (String)rstTypes.get( SoapConstants.WST_TOKENTYPE);
        if (tokenType == null ||
            !tokenType.startsWith("urn:oasis:names:tc:SAML:2.0:assertion") ||
            !tokenType.endsWith("Assertion")) {
            return false;
        }
        String requestType = (String)rstTypes.get( SoapConstants.WST_REQUESTTYPE);
        if (requestType == null || !requestType.endsWith("/trust/Issue")) {
            return false;
        }
        logger.fine("Request was identified as one for SAML 2.0 Token");
        return true;
    }

    /*
     * Retrieves soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:TokenType/text() and puts it in the return map
     * under the SoapUtil.WST_TOKENTYPE key.
     * Retrieves soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:RequestType/text() and puts it in the return map
     * under the SoapUtil.WST_REQUESTTYPE key.
     *
     * The old way was using too much code duplication and was too innefficient.
     */
    private Map<String, String> getTrustTokenTypeAndRequestTypeValues(PolicyEnforcementContext context) throws InvalidDocumentFormatException {
        Map<String, String> output = new HashMap<String, String>();
        // initial value important (dont remove)
        output.put(SCNS, SoapConstants.WSSC_NAMESPACE);
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
        Element maybeRSTEl = DomUtils.findFirstChildElement(body);

        // body must include wst:RequestSecurityToken element
        if (!maybeRSTEl.getLocalName().equals( SoapConstants.WST_REQUESTSECURITYTOKEN)) {
            logger.fine("Body's child does not seem to be a RST (" + maybeRSTEl.getLocalName() + ")");
            return output;
        }
        if (!Arrays.asList( SoapConstants.WST_NAMESPACE_ARRAY).contains(maybeRSTEl.getNamespaceURI())) {
            logger.fine("Trust namespace not recognized (" + maybeRSTEl.getNamespaceURI() + ")");
            return output;
        }
        output.put(TRUSTNS, maybeRSTEl.getNamespaceURI());
        Element tokenTypeEl = DomUtils.findOnlyOneChildElementByName(maybeRSTEl, SoapConstants.WST_NAMESPACE_ARRAY, SoapConstants.WST_TOKENTYPE);
        if (tokenTypeEl == null) {
            logger.warning("Token type not specified. This is not supported.");
        } else {
            output.put( SoapConstants.WST_TOKENTYPE, DomUtils.getTextValue(tokenTypeEl));
        }

        Element reqTypeEl = DomUtils.findOnlyOneChildElementByName(maybeRSTEl, SoapConstants.WST_NAMESPACE_ARRAY, SoapConstants.WST_REQUESTTYPE);
        if (reqTypeEl == null) {
            logger.warning("Request type not specified. This is not supported.");
        } else {
            output.put( SoapConstants.WST_REQUESTTYPE, DomUtils.getTextValue(reqTypeEl));
        }
        return output;
    }

    /** Regexp that recognizes all known SAML token type URIs and qnames. */
    private final Pattern PSAML = Pattern.compile("[:#]Assertion$|^SAML$");

    /*
     * checks if this request is for a saml assertion
     * does not check things like whether the body is signed since this is the
     * responsibility of the policy
     */
    private boolean isRequestForSAMLToken(Map rstTypes) {
        String tokenType = (String)rstTypes.get( SoapConstants.WST_TOKENTYPE);
        if (tokenType == null || !PSAML.matcher(tokenType).find()) {
            logger.fine("TokenType '" + tokenType + "' is not recognized as calling for a saml:Assertion");
            return false;
        }
        String requestType = (String)rstTypes.get( SoapConstants.WST_REQUESTTYPE);
        return !(requestType == null || !requestType.endsWith("/trust/Issue"));
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
            user = context.getLastAuthenticatedUser();
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
                  "xmlns:wsu=\"" + SoapConstants.WSU_NAMESPACE + "\" " +
                  "xmlns:wsse=\"" + SoapConstants.SECURITY_NAMESPACE + "\" " +
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
