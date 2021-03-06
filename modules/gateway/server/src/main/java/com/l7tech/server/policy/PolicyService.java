package com.l7tech.server.policy;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.identity.User;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.PolicyPathResult;
import com.l7tech.policy.PolicyTooComplexException;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.SecurityContextFinder;
import com.l7tech.security.xml.processor.WssProcessor;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.filter.FilterManager;
import com.l7tech.server.policy.filter.FilteringException;
import com.l7tech.server.policy.filter.HideDisabledAssertions;
import com.l7tech.util.*;
import com.l7tech.xml.SoapFaultDetail;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.xpath.XpathExpression;
import org.springframework.context.support.ApplicationObjectSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the service that lets a client download a policy for consuming a web service.
 * Whether or not a requestor is allowed to download a policy depends on whether or not he is capable
 * of consuming it. This is determined by looking at the identity assertions inside the target policy
 * comparing them with the identity resuling from the authentication of the policy download request.
 * <p/>
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 23, 2004<br/>
 */
public class PolicyService extends ApplicationObjectSupport {
    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger(PolicyService.class.getName());
    private static final boolean disableSecurityChecks = ConfigFactory.getBooleanProperty( "com.l7tech.server.policy.policyServiceSkipSecurityChecks", false );
    private static final boolean fullSecurityChecks = ConfigFactory.getBooleanProperty( "com.l7tech.server.policy.policyServiceFullSecurityChecks", false );
    private static final boolean enableLegacyKerberos = ConfigFactory.getBooleanProperty( "com.l7tech.server.policy.policyServiceEnableLegacyKerberos", false );
    private static final String SERVICE_VALIDATE_WSS_TIMESTAMPS = "service.validateWssTimestamps";

    private final List<Assertion> allMessageCredentialAssertions;
    private final List<Assertion> allTransportCredentialAssertions;
    private final Config config;
    private final DefaultKey serverCertFinder;
    private final ServerPolicyFactory policyFactory;
    private final FilterManager filterManager;
    private final SecurityTokenResolver securityTokenResolver;
    private final PolicyPathBuilderFactory policyPathBuilderFactory;
    private final SecurityContextFinder securityContextFinder;

    /**
     * The supported message credential sources used to determine whether the requester is
     * allowed to download the policy
     */
    private static final Assertion[] ALL_MESSAGE_CREDENTIAL_ASSERTIONS_TYPES = new Assertion[] {
        RequireWssSaml.newHolderOfKey(),
        new RequireWssX509Cert(),
        new SecureConversation(),
        new RequestWssKerberos()
    };

    /**
     * The supported transport credential sources used to determine whether the requester is
     * allowed to download the policy
     */
    private static final Assertion[] ALL_TRANSPORT_CREDENTIAL_ASSERTIONS_TYPES = new Assertion[] {
            new WssBasic(), // included as transport since it requires a secure transport
            new HttpBasic(),
            new SslAssertion(true),
    };

    public interface ServiceInfo {
        Assertion getPolicy() throws PolicyAssertionException;

        String getVersion();
    }

    public interface PolicyGetter {
        /**
         * @param serviceId the ID of the service whose policy to query for
         * @return the policy or null if service does not exist
         */
        ServiceInfo getPolicy(String serviceId);
    }

    public PolicyService( final Config config,
                          final DefaultKey serverCertFinder,
                          final ServerPolicyFactory policyFactory,
                          final FilterManager filterManager,
                          final SecurityTokenResolver securityTokenResolver,
                          final PolicyPathBuilderFactory policyPathBuilderFactory,
                          final SecurityContextFinder securityContextFinder )
    {
        if (config == null) throw new IllegalArgumentException("Config is required");
        if (serverCertFinder == null) throw new IllegalArgumentException("Server cert finder is required");
        if (policyFactory == null) throw new IllegalArgumentException("Policy Factory is required");
        if (filterManager == null) throw new IllegalArgumentException("Filter Manager is required");
        if (policyPathBuilderFactory == null) throw new IllegalArgumentException("Policy Path Builder Factory is required");

        this.config = config;
        this.serverCertFinder = serverCertFinder;
        this.policyFactory = policyFactory;
        this.filterManager = filterManager;
        this.securityTokenResolver = securityTokenResolver;
        this.policyPathBuilderFactory = policyPathBuilderFactory;
        this.securityContextFinder = securityContextFinder;

        // populate all possible credentials sources
        this.allMessageCredentialAssertions = buildCredentialAssertions( ALL_MESSAGE_CREDENTIAL_ASSERTIONS_TYPES );
        
        final boolean enableDigest = config.getBooleanProperty("httpDigest.enable", false);
        final List<Assertion> assertionList = new ArrayList<Assertion>(//need a modifiable list
                Arrays.asList(ALL_TRANSPORT_CREDENTIAL_ASSERTIONS_TYPES));
        if(enableDigest) {
            assertionList.add(0, new HttpDigest());
        }

        this.allTransportCredentialAssertions = buildCredentialAssertions( assertionList.toArray(new Assertion[assertionList.size()]) );
    }

    private List<Assertion> buildCredentialAssertions( final Assertion[] assertions ) {
        final List<Assertion> allCredentialAssertions = new ArrayList<Assertion>();

        for ( final Assertion assertion : assertions ) {
            if (assertion instanceof RequireWssSaml) {
                // Lighten saml requirements for policy download
                RequireWssSaml requestWssSaml = (RequireWssSaml) assertion;
                requestWssSaml.setCheckAssertionValidity(false);
                requestWssSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS);
                SamlAuthenticationStatement as = new SamlAuthenticationStatement();
                as.setAuthenticationMethods(SamlConstants.ALL_AUTHENTICATIONS);
                requestWssSaml.setAuthenticationStatement(as);
                requestWssSaml.setNoSubjectConfirmation(true);
            }

            allCredentialAssertions.add(assertion);
        }

        return Collections.unmodifiableList( allCredentialAssertions );
    }

    /**
     * Handle a policy download request and return a policy XML document (as a DOM tree).
     *
     * @param policyId                 the ID of the policy to download.
     * @param clientVersion            the client software version (may be null)
     * @param preAuthenticatedUser     the already-authenticated User that wishes to download this policy.
     * @param policyGetter             a PolicyGetter implementation that can be used to look up the policy.
     * @param isFullDoc                if true, we will not filter out any non-client-visible assertions from the policy
     *                                 before returning it.
     * @return the filtered policy or null if the target policy does not exist or the requestor should not see it
     * @throws com.l7tech.server.policy.filter.FilteringException if an exception occurs while filtering the policy
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if a policy is invalid (ie, an include cannot be expanded,
     *                                                              or an assertion cannot be translated)
     *
     * @throws java.io.IOException if an exception occurs while serializing policy XML
     * @throws org.xml.sax.SAXException if an exception occurs while parsing policy XML
     */
    public Document respondToPolicyDownloadRequest( final String policyId,
                                                    final String clientVersion,
                                                    final User preAuthenticatedUser,
                                                    final PolicyGetter policyGetter,
                                                    final boolean isFullDoc )
            throws FilteringException, IOException, SAXException, PolicyAssertionException {
        // prepare writer, get policy
        WspWriter wspWriter = new WspWriter();
        wspWriter.setTargetVersion(clientVersion);
        final ServiceInfo gotPolicy = policyGetter.getPolicy(policyId);
        Assertion targetPolicy = gotPolicy != null ? gotPolicy.getPolicy() : null;
        if (targetPolicy == null) {
            logger.info("cannot find target policy from id: " + policyId);
            return null;
        }

        // in this case, we're returning a non-filtered policy document as per the
        // the passed argument. this flag trumps the case below where the policy
        // does not allow anonymous and there are no authenticated requestors
        if (isFullDoc) {
            wspWriter.setPolicy(targetPolicy);
            return XmlUtil.stringToDocument(wspWriter.getPolicyXmlAsString());
        }

        // in case where the request is anonyymous but request was not authenticated, we return null
        boolean isanonymous;
        try {
            isanonymous = atLeastOnePathIsAnonymous(targetPolicy);
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // can't happen here
        }
        if (preAuthenticatedUser == null && !isanonymous) return null;

        // filter the policy for the requestor's identity and return the resulting policy
        Assertion filteredAssertion = filterManager.applyAllFilters(preAuthenticatedUser, targetPolicy);
        if (filteredAssertion == null) {
            if (isanonymous) {
                wspWriter.setPolicy(null);
                return XmlUtil.stringToDocument(wspWriter.getPolicyXmlAsString());
            } else {
                return null;
            }
        }
        wspWriter.setPolicy(filteredAssertion);
        return XmlUtil.stringToDocument(wspWriter.getPolicyXmlAsString());
    }

    /**
     * The filtered policy is contained in the soapResponse
     */
    public void respondToPolicyDownloadRequest(PolicyEnforcementContext context,
                                               boolean signResponse,
                                               PolicyGetter policyGetter)
            throws IOException, SAXException, PolicyAssertionException {
        final XmlKnob reqXml = context.getRequest().getXmlKnob();
        final SecurityKnob reqSec = context.getRequest().getSecurityKnob();
        final Message response = context.getResponse();
        final SoapVersion soapVersion = (context.getService() != null && context.getService().getSoapVersion() == SoapVersion.SOAP_1_2) ? SoapVersion.SOAP_1_2 : SoapVersion.SOAP_1_1;

        // We need a Document
        Document requestDoc;
        requestDoc = reqXml.getDocumentWritable();

        // Process request for message level security stuff
        ProcessorResult wssOutput;
        try {
            WssProcessor trogdor = new WssProcessorImpl();
            wssOutput = trogdor.undecorateMessage(context.getRequest(),
                    securityContextFinder,
                                                  securityTokenResolver);
            reqSec.setProcessorResult(wssOutput);
        } catch (Exception e) {
            response.initialize(exceptionToFault(soapVersion, e));
            context.setPolicyResult(AssertionStatus.BAD_REQUEST);
            return;
        }

        // Which policy is requested?
        String clientVersion;
        String policyId;
        String relatesTo;
        try {
            clientVersion = getRequestClientVersion(requestDoc);
            policyId = getRequestedPolicyId(requestDoc);
            relatesTo = SoapUtil.getL7aMessageId(requestDoc);
        } catch (InvalidDocumentFormatException e) {
            response.initialize(exceptionToFault(soapVersion, e));
            context.setPolicyResult(AssertionStatus.BAD_REQUEST);
            return;
        }
        logger.finest("Policy requested is " + policyId + " for client version " + clientVersion);

        // get target
        ServiceInfo si = policyGetter.getPolicy(policyId);
        Assertion targetPolicy = si.getPolicy();
        if (targetPolicy == null) {
            logger.info("cannot find target policy from id: " + policyId);
            Document fault;
            try {
                fault = SoapFaultUtils.generateSoapFaultDocument(soapVersion,
                                                                 SoapUtil.FC_SERVER,
                                                                 "policy " + policyId + " not found",
                                                                 null,
                                                                 "");
            } catch (IOException e) {
                fault = exceptionToFault(soapVersion, e);
            } catch (SAXException e) {
                fault = exceptionToFault(soapVersion, e);
            }
            response.initialize(fault);
            context.setPolicyResult(AssertionStatus.BAD_REQUEST);
            return;
        }

        // if the policy allows anonymous, skip the meta policy
        boolean canSkipMetaPolicyStep = false;
        try {
            if (atLeastOnePathIsAnonymous(targetPolicy)) {
                canSkipMetaPolicyStep = true;
            } else {
                logger.fine("The policy does not allow anonymous.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // can't happen here
        }

        // Run the policy-policy
        AssertionStatus status = null;
        if (!canSkipMetaPolicyStep) {
            logger.fine("Running meta-policy.");
            ServerAssertion policyPolicy = null;
            try {
                final boolean validateTimestamps = config.getBooleanProperty( SERVICE_VALIDATE_WSS_TIMESTAMPS, true );
                policyPolicy = constructPolicyPolicy(targetPolicy, validateTimestamps);
                status = policyPolicy.checkRequest(context);
                context.setPolicyResult(status);
            } catch (IOException e) {
                response.initialize(exceptionToFault(soapVersion, e));
                logger.log(Level.WARNING, "problem running policy download policy", ExceptionUtils.getDebugException(e));
                context.setPolicyResult(AssertionStatus.SERVER_ERROR);
                return;
            } catch (PolicyAssertionException e) {
                response.initialize(exceptionToFault(soapVersion, e));
                logger.log(Level.WARNING, "problem running policy download policy", ExceptionUtils.getDebugException(e));
                context.setPolicyResult(AssertionStatus.SERVER_ERROR);
                return;
            } finally {
                ResourceUtils.closeQuietly( policyPolicy );
            }
        }

        Document policyDoc = null;
        if (canSkipMetaPolicyStep || status == AssertionStatus.NONE) {
            try {
                User user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
                policyDoc = respondToPolicyDownloadRequest(policyId, clientVersion, user, policyGetter, false);
            } catch (FilteringException e) {
                response.initialize(exceptionToFault(soapVersion, e));
                logger.log(Level.WARNING, "problem preparing response: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                context.setPolicyResult(AssertionStatus.SERVER_ERROR);
                return;
            } catch (IOException e) {
                response.initialize(exceptionToFault(soapVersion, e));
                logger.log(Level.WARNING, "problem preparing response: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                context.setPolicyResult(AssertionStatus.SERVER_ERROR);
                return;
            } catch (SAXException e) {
                response.initialize(exceptionToFault(soapVersion, e));
                logger.log(Level.WARNING, "problem preparing response: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                context.setPolicyResult(AssertionStatus.SERVER_ERROR);
                return;
            }
        }

        if (policyDoc == null) {
            SoapFaultLevel fault = new SoapFaultLevel();
            fault.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
            if(soapVersion == SoapVersion.SOAP_1_2) {
                fault.setFaultTemplate("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\" " +
                        "                  xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <soapenv:Code>\n" +
                        "                <soapenv:Value>soapenv:Receiver</soapenv:Value>\n" +
                        "            </soapenv:Code>\n" +
                        "            <soapenv:Reason>\n" +
                        "                <soapenv:Text xml:lang=\"en-US\">unauthorized policy download</soapenv:Text>\n" +
                        "            </soapenv:Reason>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>");
            } else {
                fault.setFaultTemplate("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "                  xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>Server</faultcode>\n" +
                        "            <faultstring>unauthorized policy download</faultstring>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>");
            }
            context.setFaultlevel(fault);
            context.setPolicyResult(AssertionStatus.AUTH_FAILED);

            logger.fine("resulting policy is empty");
            return;
        }

        try {
            String policyVersion = policyId + "|" + si.getVersion();
            response.initialize(wrapFilteredPolicyInResponse(policyDoc, policyVersion, relatesTo, signResponse));
            context.setPolicyResult(AssertionStatus.NONE);
        } catch (GeneralSecurityException e) {
            response.initialize(exceptionToFault(soapVersion, e));
            logger.log(Level.WARNING, "problem preparing response: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
        } catch (DecoratorException e) {
            response.initialize(exceptionToFault(soapVersion, e));
            logger.log(Level.WARNING, "problem preparing response: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
        }
    }

    private Document wrapFilteredPolicyInResponse(Document policyDoc,
                                                  String policyVersion,
                                                  String relatesTo,
                                                  boolean signResponse)
      throws GeneralSecurityException, DecoratorException {
        Document responseDoc;
        try {
            responseDoc = XmlUtil.stringToDocument("<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\">" +
              "<soap:Header>" +
              "<L7a:" + SoapConstants.L7_POLICYVERSION_ELEMENT + " xmlns:L7a=\"" +
              SoapConstants.L7_MESSAGEID_NAMESPACE + "\"/>" +
              "</soap:Header>" +
              "<soap:Body>" +
              "<wsx:GetPolicyResponse xmlns:wsx=\"" + SoapConstants.WSX_NAMESPACE + "\"/>" +
              "</soap:Body></soap:Envelope>");
            Element header = SoapUtil.getHeaderElement(responseDoc);
            Element pver = DomUtils.findFirstChildElement(header);
            pver.appendChild(DomUtils.createTextNode(responseDoc, policyVersion));
            Element body = SoapUtil.getBodyElement(responseDoc);
            Element gpr = DomUtils.findFirstChildElement(body);
            gpr.appendChild(responseDoc.importNode(policyDoc.getDocumentElement(), true));
            Element rte = null;
            if (relatesTo != null)
                rte = SoapUtil.setL7aRelatesTo(responseDoc, relatesTo);
            if (signResponse)
                signresponse(responseDoc, pver, rte, serverCertFinder.getSslInfo());
            else
                SoapUtil.addTimestamp(header, SoapConstants.WSU_NAMESPACE, null, 0, 0);
            return responseDoc;
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        } catch (IOException e) {
            throw new SignatureException("Unable to sign policy: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void signresponse(Document responseDoc, Element policyVersion, Element relatesTo, SsgKeyEntry signer) throws GeneralSecurityException, DecoratorException {
        WssDecoratorImpl decorator = new WssDecoratorImpl();
        DecorationRequirements reqmts = new DecorationRequirements();
        reqmts.setSenderMessageSigningCertificate(signer.getCertificate());
        reqmts.setSenderMessageSigningPrivateKey(signer.getPrivate());
        reqmts.setSignTimestamp(true);
        try {
            reqmts.getElementsToSign().add(SoapUtil.getBodyElement(responseDoc));
            if (policyVersion != null)
                reqmts.getElementsToSign().add(policyVersion);
            if (relatesTo != null)
                reqmts.getElementsToSign().add(relatesTo);
            decorator.decorateMessage(new Message(responseDoc), reqmts);
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e); 
        }
    }

    private String getRequestClientVersion(Document requestDoc) throws InvalidDocumentFormatException {
        String clientVersion;

        Element header = SoapUtil.getHeaderElement(requestDoc);
        if (header == null) throw new MissingRequiredElementException("No SOAP Header was found in the request.");
        Element cverEl = DomUtils.findOnlyOneChildElementByName(header, SoapConstants.L7_MESSAGEID_NAMESPACE, SoapConstants.L7_CLIENTVERSION_ELEMENT);
        if (cverEl == null) {
            clientVersion = "5.0"; // 5.0 or earlier does not pass a version
        } else {
            clientVersion = DomUtils.getTextValue(cverEl).trim();
        }
        if (clientVersion.length() < 1) throw new InvalidDocumentFormatException( SoapConstants.L7_CLIENTVERSION_ELEMENT +
                                                           " element was empty.");

        return clientVersion;
    }

    private String getRequestedPolicyId(Document requestDoc) throws InvalidDocumentFormatException {
        Element header = SoapUtil.getHeaderElement(requestDoc);
        if (header == null) throw new MissingRequiredElementException("No SOAP Header was found in the request.");
        Element sidEl = DomUtils.findOnlyOneChildElementByName(header, SoapConstants.L7_MESSAGEID_NAMESPACE, SoapConstants.L7_SERVICEID_ELEMENT);
        if (sidEl == null) throw new MissingRequiredElementException("No {" + SoapConstants.L7_MESSAGEID_NAMESPACE +
                             "}" + SoapConstants.L7_SERVICEID_ELEMENT +
                             " element was found in the SOAP header.");
        String serviceId = DomUtils.getTextValue(sidEl);
        if (serviceId == null || serviceId.length() < 1) throw new InvalidDocumentFormatException( SoapConstants.L7_SERVICEID_ELEMENT +
                                                           " element was empty.");

        // Check that this is a proper GetPolicy request.
        Element body = SoapUtil.getBodyElement(requestDoc);
        if (body == null) throw new MissingRequiredElementException("No SOAP Body was found in the request.");
        Element getPolicy = DomUtils.findOnlyOneChildElementByName(body, SoapConstants.WSX_NAMESPACE, "GetPolicy");
        if (getPolicy == null) throw new MissingRequiredElementException("Request was not a wsx:GetPolicy request");

        return serviceId;
    }

    private Document exceptionToFault(SoapVersion soapVersion, Exception e) {
        logger.log(Level.FINE, "Building fault due to: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        try {
            Document fault;
            if (e instanceof SoapFaultDetail) {
                fault = SoapFaultUtils.generateSoapFaultDocument(soapVersion, (SoapFaultDetail)e, SoapUtil.FC_SERVER);
            } else {
                fault = SoapFaultUtils.generateSoapFaultDocument(soapVersion,
                                                                 SoapUtil.FC_SERVER,
                                                                 e.getMessage(),
                                                                 null,
                                                                 e.getClass().getName());
            }
            return fault;
        } catch (IOException e1) {
            throw new RuntimeException(e1); // can't happen
        } catch (SAXException e1) {
            throw new RuntimeException(e1); // can't happen
        }
    }

    /**
     * Constructs a policy that determines if a requestor should be allowed to download a policy.
     *
     * @param targetPolicy the policy targeted by a requestor.
     * @param validateTimestamps True to validate WS-Security timestamps
     * @return the policy that should be validated by the policy download request for the passed target
     * @throws ServerPolicyException if targetPolicy could not be compiled into server assertion instances
     */
    ServerAssertion constructPolicyPolicy(Assertion targetPolicy, boolean validateTimestamps) throws ServerPolicyException {
        final AllAssertion messageAll = new AllAssertion();
        messageAll.addChild( new OneOrMoreAssertion(allMessageCredentialAssertions) );
        if ( validateTimestamps ) {
            messageAll.addChild( RequireWssTimestamp.newInstance() );
        }
        if (!disableSecurityChecks) {
            final OneOrMoreAssertion signedBody = new OneOrMoreAssertion();
            signedBody.addChild( new RequireWssSignedElement( new XpathExpression("/s:Envelope/s:Body", "s", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE) ) );
            signedBody.addChild( new RequireWssSignedElement( new XpathExpression("/s:Envelope/s:Body", "s", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE) ) );
            messageAll.addChild( signedBody );
            if ( fullSecurityChecks ) {
                messageAll.addChild( new RequireWssSignedElement( new XpathExpression("/*[local-name()='Envelope']/*[local-name()='Header']/*[namespace-uri()='"+SoapUtil.L7_MESSAGEID_NAMESPACE+"']") ) );
            } else {
                // Prior to 5.3.1 some addressing elements were not signed, to allow
                // for this we only validate the service id by default
                messageAll.addChild( new RequireWssSignedElement( new XpathExpression("/*[local-name()='Envelope']/*[local-name()='Header']/*[namespace-uri()='"+SoapUtil.L7_MESSAGEID_NAMESPACE+"' and local-name()='"+SoapUtil.L7_SERVICEID_ELEMENT+"']") ) );
            }
        }

        final AllAssertion transportAll = new AllAssertion();
        if (!disableSecurityChecks) {
            transportAll.addChild( new SslAssertion() );
        }
        transportAll.addChild( new OneOrMoreAssertion(allTransportCredentialAssertions) );

        final AllAssertion legacyKerberosAll;
        if ( enableLegacyKerberos ) {
            // This allows pre 5.3.1 XVCs to download using Kerberos Token Profile.
            // Remove when 5.3.0 and earlier are no longer supported.
            legacyKerberosAll = new AllAssertion();
            legacyKerberosAll.addChild( new SslAssertion() );
            legacyKerberosAll.addChild( new OneOrMoreAssertion( Arrays.asList( new RequestWssKerberos() ) ) );
        } else {
            legacyKerberosAll = null;
        }

        final OneOrMoreAssertion messageOrTransport = new OneOrMoreAssertion();
        messageOrTransport.addChild( messageAll );
        messageOrTransport.addChild( transportAll );
        if ( legacyKerberosAll != null ) {
            messageOrTransport.addChild( legacyKerberosAll );
        }

        final AllAssertion base = new AllAssertion();
        base.addChild(messageOrTransport);

        final List<Assertion> allTargetIdentities = new ArrayList<Assertion>();
        addIdAssertionToList(targetPolicy, allTargetIdentities);
        if (allTargetIdentities.size() > 0)
            base.addChild(new OneOrMoreAssertion(allTargetIdentities));
        try {
            Assertion.filterOutDisabledAssertions(base);
            return policyFactory.compilePolicy(base, false); // dogfood policy is allowed to use whatever assertions it wants, regardless of license
        } catch (LicenseException e) {
            throw new RuntimeException(e); // can't happen, we said no license enforcement
        }
    }

    private void addIdAssertionToList(Assertion assertion, List<Assertion> receptacle) {
        if (!assertion.isEnabled())
            return;

        if ( assertion instanceof IdentityAssertion && Assertion.isRequest( assertion ) )
            receptacle.add(assertion);

        if (!(assertion instanceof CompositeAssertion))
            return;

        CompositeAssertion composite = (CompositeAssertion)assertion;
        for (Object o : composite.getChildren()) {
            Assertion a = (Assertion) o;
            addIdAssertionToList(a, receptacle);
        }
    }

    private boolean atLeastOnePathIsAnonymous( final Assertion originalRootAssertion ) throws InterruptedException, PolicyAssertionException {
        Assertion rootAssertion = new HideDisabledAssertions().filter(null, originalRootAssertion);
        if (rootAssertion == null) {
            if ( originalRootAssertion instanceof CompositeAssertion ) {
                final CompositeAssertion compRootAssertion = (CompositeAssertion) originalRootAssertion;
                if ( compRootAssertion.isEmpty() && compRootAssertion.permitsEmpty() ) {
                    return true;
                }
            }
            return false; // normally can't happen
        }
        
        
        PolicyPathResult paths;
        try {
            paths = policyPathBuilderFactory.makePathBuilder().generate(rootAssertion);
        } catch (PolicyTooComplexException e) {
            // Fail closed -- assume the policy does not contain any anonymous paths
            logger.log(Level.INFO, "Policy too complex to analyze -- assuming it contains no paths allowing unauthenticated access");
            return false;
        }
        for (AssertionPath assertionPath : paths.paths()) {
            Assertion[] path = assertionPath.getPath();
            boolean pathContainsIdAssertion = false;
            for ( Assertion a : path ) {
                if ( a instanceof IdentityAssertion && Assertion.isRequest( a ) ) {
                    pathContainsIdAssertion = true;
                }
            }
            if (!pathContainsIdAssertion) return true;
        }
        return false;
    }

}
