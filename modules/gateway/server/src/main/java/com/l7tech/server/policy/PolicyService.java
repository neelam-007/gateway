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
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
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
import com.l7tech.security.xml.processor.WssProcessor;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.filter.FilterManager;
import com.l7tech.server.policy.filter.FilteringException;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.MissingRequiredElementException;
import com.l7tech.xml.SoapFaultDetail;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.support.ApplicationObjectSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SignatureException;
import java.util.ArrayList;
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
    private static final Logger logger = Logger.getLogger(PolicyService.class.getName());

    private final List<Assertion> allCredentialAssertions;
    private final DefaultKey serverCertFinder;
    private final ServerPolicyFactory policyFactory;
    private final FilterManager filterManager;
    private final SecurityTokenResolver securityTokenResolver;
    private final PolicyPathBuilderFactory policyPathBuilderFactory;

    /**
     * The supported credential sources used to determine whether the requester is
     * allowed to download the policy
     */
    private static final Assertion[] ALL_CREDENTIAL_ASSERTIONS_TYPES = new Assertion[] {
        RequestWssSaml.newHolderOfKey(),
        new RequestWssX509Cert(),
        new SecureConversation(),
        new HttpDigest(),
        new WssBasic(),
        new HttpBasic(),
        new SslAssertion(true),
        new RequestWssKerberos()
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

    public PolicyService(DefaultKey serverCertFinder,
                         ServerPolicyFactory policyFactory,
                         FilterManager filterManager,
                         SecurityTokenResolver securityTokenResolver,
                         PolicyPathBuilderFactory policyPathBuilderFactory)
    {
        if (serverCertFinder == null) throw new IllegalArgumentException("Server key and server cert must be provided to create a TokenService");
        if (policyFactory == null) throw new IllegalArgumentException("Policy Factory is required");
        if (filterManager == null) throw new IllegalArgumentException("Filter Manager is required");
        if (policyPathBuilderFactory == null) throw new IllegalArgumentException("Policy Path Builder Factory is required");

        this.serverCertFinder = serverCertFinder;
        this.policyFactory = policyFactory;
        this.filterManager = filterManager;
        this.securityTokenResolver = securityTokenResolver;
        this.policyPathBuilderFactory = policyPathBuilderFactory;

        // populate all possible credentials sources
        this.allCredentialAssertions = new ArrayList<Assertion>();
        for (Assertion assertion : ALL_CREDENTIAL_ASSERTIONS_TYPES) {
            if (assertion instanceof RequestWssSaml) {
                // Lighten saml requirements for policy download
                RequestWssSaml requestWssSaml = (RequestWssSaml) assertion;
                requestWssSaml.setCheckAssertionValidity(false);
                requestWssSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS);
                SamlAuthenticationStatement as = new SamlAuthenticationStatement();
                as.setAuthenticationMethods(SamlConstants.ALL_AUTHENTICATIONS);
                requestWssSaml.setAuthenticationStatement(as);
                requestWssSaml.setNoSubjectConfirmation(true);
            }

            allCredentialAssertions.add(assertion);
        }
    }

    /**
     * Handle a policy download request and return a policy XML document (as a DOM tree).
     *
     * @param policyId                 the ID of the policy to download.
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
    public Document respondToPolicyDownloadRequest(String policyId,
                                                   User preAuthenticatedUser,
                                                   PolicyGetter policyGetter,
                                                   boolean isFullDoc)
            throws FilteringException, IOException, SAXException, PolicyAssertionException {
        // prepare writer, get policy
        WspWriter wspWriter = new WspWriter();
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

        // We need a Document
        Document requestDoc;
        requestDoc = reqXml.getDocumentWritable();

        // Process request for message level security stuff
        ProcessorResult wssOutput;
        try {
            WssProcessor trogdor = new WssProcessorImpl();
            wssOutput = trogdor.undecorateMessage(context.getRequest(),
                                                  null,
                                                  SecureConversationContextManager.getInstance(),
                                                  securityTokenResolver);
            reqSec.setProcessorResult(wssOutput);
        } catch (Exception e) {
            response.initialize(exceptionToFault(e));
            context.setPolicyResult(AssertionStatus.BAD_REQUEST);
            return;
        }

        // Which policy is requested?
        String policyId;
        String relatesTo;
        try {
            policyId = getRequestedPolicyId(requestDoc);
            relatesTo = SoapUtil.getL7aMessageId(requestDoc);
        } catch (InvalidDocumentFormatException e) {
            response.initialize(exceptionToFault(e));
            context.setPolicyResult(AssertionStatus.BAD_REQUEST);
            return;
        }
        logger.finest("Policy requested is " + policyId);

        // get target
        ServiceInfo si = policyGetter.getPolicy(policyId);
        Assertion targetPolicy = si.getPolicy();
        if (targetPolicy == null) {
            logger.info("cannot find target policy from id: " + policyId);
            Document fault;
            try {
                fault = SoapFaultUtils.generateSoapFaultDocument(SoapUtil.FC_SERVER,
                                                                 "policy " + policyId + " not found",
                                                                 null,
                                                                 "");
            } catch (IOException e) {
                fault = exceptionToFault(e);
            } catch (SAXException e) {
                fault = exceptionToFault(e);
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
            try {
                ServerAssertion policyPolicy = constructPolicyPolicy(targetPolicy);
                status = policyPolicy.checkRequest(context);
                context.setPolicyResult(status);
            } catch (IOException e) {
                response.initialize(exceptionToFault(e));
                logger.log(Level.WARNING, "problem running policy download policy", e);
                context.setPolicyResult(AssertionStatus.SERVER_ERROR);
                return;
            } catch (PolicyAssertionException e) {
                response.initialize(exceptionToFault(e));
                logger.log(Level.WARNING, "problem running policy download policy", e);
                context.setPolicyResult(AssertionStatus.SERVER_ERROR);
                return;
            }
        }

        Document policyDoc = null;
        if (canSkipMetaPolicyStep || status == AssertionStatus.NONE) {
            try {
                User user = context.getLastAuthenticatedUser();
                policyDoc = respondToPolicyDownloadRequest(policyId, user, policyGetter, false);
            } catch (FilteringException e) {
                response.initialize(exceptionToFault(e));
                logger.log(Level.WARNING, "problem preparing response", e);
                context.setPolicyResult(AssertionStatus.SERVER_ERROR);
                return;
            } catch (IOException e) {
                response.initialize(exceptionToFault(e));
                logger.log(Level.WARNING, "problem preparing response", e);
                context.setPolicyResult(AssertionStatus.SERVER_ERROR);
                return;
            } catch (SAXException e) {
                response.initialize(exceptionToFault(e));
                logger.log(Level.WARNING, "problem preparing response", e);
                context.setPolicyResult(AssertionStatus.SERVER_ERROR);
                return;
            }
        }

        if (policyDoc == null) {
            SoapFaultLevel fault = new SoapFaultLevel();
            fault.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
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
            response.initialize(exceptionToFault(e));
            logger.log(Level.WARNING, "problem preparing response", e);
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
        } catch (DecoratorException e) {
            response.initialize(exceptionToFault(e));
            logger.log(Level.WARNING, "problem preparing response", e);
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
        reqmts.setSignTimestamp();
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

    private Document exceptionToFault(Exception e) {
        logger.log(Level.INFO, e.getMessage(), e);
        try {
            Document fault;
            if (e instanceof SoapFaultDetail) {
                fault = SoapFaultUtils.generateSoapFaultDocument((SoapFaultDetail)e, SoapUtil.FC_SERVER);
            } else {
                fault = SoapFaultUtils.generateSoapFaultDocument(SoapUtil.FC_SERVER,
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
     * @return the policy that should be validated by the policy download request for the passed target
     * @throws ServerPolicyException if targetPolicy could not be compiled into server assertion instances
     */
    ServerAssertion constructPolicyPolicy(Assertion targetPolicy) throws ServerPolicyException {
        AllAssertion base = new AllAssertion();
        base.addChild(new OneOrMoreAssertion(allCredentialAssertions));
        List<Assertion> allTargetIdentities = new ArrayList<Assertion>();
        addIdAssertionToList(targetPolicy, allTargetIdentities);
        if (allTargetIdentities.size() > 0)
            base.addChild(new OneOrMoreAssertion(allTargetIdentities));
        try {
            return policyFactory.compilePolicy(base, false); // dogfood policy is allowed to use whatever assertions it wants, regardless of license
        } catch (LicenseException e) {
            throw new RuntimeException(e); // can't happen, we said no license enforcement
        }
    }

    private void addIdAssertionToList(Assertion assertion, List<Assertion> receptacle) {
        if (assertion instanceof IdentityAssertion)
            receptacle.add(assertion);
        if (!(assertion instanceof CompositeAssertion))
            return;
        CompositeAssertion composite = (CompositeAssertion)assertion;
        for (Object o : composite.getChildren()) {
            Assertion a = (Assertion) o;
            addIdAssertionToList(a, receptacle);
        }
    }

    private boolean atLeastOnePathIsAnonymous(Assertion rootAssertion) throws InterruptedException, PolicyAssertionException {
        PolicyPathResult paths = policyPathBuilderFactory.makePathBuilder().generate(rootAssertion);
        for (Object o : paths.paths()) {
            AssertionPath assertionPath = (AssertionPath) o;
            Assertion[] path = assertionPath.getPath();
            boolean pathContainsIdAssertion = false;
            for (Assertion a : path) {
                if (a instanceof IdentityAssertion) {
                    pathContainsIdAssertion = true;
                }
            }
            if (!pathContainsIdAssertion) return true;
        }
        return false;
    }

}
