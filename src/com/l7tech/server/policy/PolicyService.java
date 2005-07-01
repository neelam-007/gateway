package com.l7tech.server.policy;

import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.DecoratorException;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssProcessor;
import com.l7tech.common.security.xml.processor.WssProcessorImpl;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MissingRequiredElementException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.identity.User;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyPathBuilder;
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
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.filter.FilterManager;
import com.l7tech.server.policy.filter.FilteringException;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import org.springframework.context.support.ApplicationObjectSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
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
<<<<<<< PolicyService.java
 * $Id$<br/>
=======
 * $Id$<br/>
>>>>>>> 1.61.6.1.2.1
 */
public class PolicyService extends ApplicationObjectSupport {
    /**
         * The supported credential sources used to determine whether the requester is
         * allowd to download the policy
         */
    private static final Assertion[] ALL_CREDENTIAL_ASSERTIONS_TYPES = new Assertion[]{
          RequestWssSaml.newHolderOfKey(),
          new RequestWssX509Cert(),
          new SecureConversation(),
          new HttpDigest(),
          new WssBasic(),
          new HttpBasic(),
          new SslAssertion(true),
        };

    public interface ServiceInfo {
        Assertion getPolicy();

        String getVersion();
    }

    public interface PolicyGetter {
        /**
         * @return the policy or null if service does not exist
         */
        ServiceInfo getPolicy(String serviceId);
    }

    public PolicyService(PrivateKey privateServerKey, X509Certificate serverCert, ServerPolicyFactory policyFactory, FilterManager filterManager, AuditContext auditContext) {
        // populate all possible credentials sources
        allCredentialAssertions = new ArrayList();
        for (int i = 0; i < ALL_CREDENTIAL_ASSERTIONS_TYPES.length; i++) {
            Assertion assertion = ALL_CREDENTIAL_ASSERTIONS_TYPES[i];

            // TODO confirm this change
            if (assertion instanceof RequestWssSaml) {
                // Lighed saml requirements for policy download
                RequestWssSaml requestWssSaml = (RequestWssSaml)assertion;
                requestWssSaml.setCheckAssertionValidity(false);
                requestWssSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS);
                final SamlAuthenticationStatement as = new SamlAuthenticationStatement();
                as.setAuthenticationMethods(SamlConstants.ALL_AUTHENTICATIONS);
                requestWssSaml.setAuthenticationStatement(as);
            }

            allCredentialAssertions.add(assertion);
        }
        if (privateServerKey == null || serverCert == null) {
            throw new IllegalArgumentException("Server key and server cert must be provided to create a TokenService");
        }
        this.privateServerKey = privateServerKey;
        if (privateServerKey == null) {
            throw new IllegalArgumentException("Private Key is required");
        }
        this.serverCert = serverCert;
        if (serverCert == null) {
            throw new IllegalArgumentException("Server Certificate is required");
        }
        this.policyFactory = policyFactory;
        if (policyFactory == null) {
            throw new IllegalArgumentException("Policy Factory is required");
        }
        if (filterManager == null) {
            throw new IllegalArgumentException("Filter Manager is required");
        }
        this.filterManager = filterManager;

        if (auditContext == null) {
            throw new IllegalArgumentException("Audit Context is required");
        }
        this.auditContext = auditContext;
    }


    /**
     * @return the filtered policy or null if the target policy does not exist or the requestor should not see it
     */
    public Document respondToPolicyDownloadRequest(String policyId,
                                                   User preAuthenticatedUser,
                                                   PolicyGetter policyGetter,
                                                   boolean pre32PolicyCompat) throws FilteringException,
      IOException,
      SAXException 
    {
        WspWriter wspWriter = new WspWriter();
        wspWriter.setPre32Compat(pre32PolicyCompat);

        final ServiceInfo gotPolicy = policyGetter.getPolicy(policyId);
        Assertion targetPolicy = gotPolicy != null ? gotPolicy.getPolicy() : null;
        if (targetPolicy == null) {
            logger.info("cannot find target policy from id: " + policyId);
            return null;
        }

        boolean isanonymous = atLeastOnePathIsAnonymous(targetPolicy);
        if (preAuthenticatedUser == null && !isanonymous) return null;

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
                                               PolicyGetter policyGetter,
                                               boolean pre32PolicyCompat)
      throws IOException, SAXException
    {
        final XmlKnob reqXml = context.getRequest().getXmlKnob();
        final Message response = context.getResponse();

        // We need a Document
        Document requestDoc = null;
        requestDoc = reqXml.getDocumentWritable();

        // Process request for message level security stuff
        ProcessorResult wssOutput = null;
        try {
            WssProcessor trogdor = new WssProcessorImpl();
            wssOutput = trogdor.undecorateMessage(context.getRequest(),
                                                  null, serverCert,
                                                  privateServerKey,
                                                  SecureConversationContextManager.getInstance());
            reqXml.setProcessorResult(wssOutput);
        } catch (Exception e) {
            response.initialize(exceptionToFault(e));
            return;
        }

        // Which policy is requested?
        String policyId = null;
        String relatesTo = null;
        try {
            policyId = getRequestedPolicyId(requestDoc);
            relatesTo = SoapUtil.getL7aMessageId(requestDoc);
        } catch (InvalidDocumentFormatException e) {
            response.initialize(exceptionToFault(e));
            return;
        }
        logger.finest("Policy requested is " + policyId);

        // get target
        ServiceInfo si = policyGetter.getPolicy(policyId);
        Assertion targetPolicy = si.getPolicy();
        if (targetPolicy == null) {
            logger.info("cannot find target policy from id: " + policyId);
            Document fault = null;
            try {
                fault = SoapFaultUtils.generateSoapFaultDocument(SoapFaultUtils.FC_SERVER,
                                                                 "policy " + policyId + " not found",
                                                                 null,
                                                                 "");
            } catch (IOException e) {
                fault = exceptionToFault(e);
            } catch (SAXException e) {
                fault = exceptionToFault(e);
            }
            response.initialize(fault);
            return;
        }

        // if the policy allows anonymous, skip the meta policy
        boolean canSkipMetaPolicyStep = false;
        if (atLeastOnePathIsAnonymous(targetPolicy)) {
            canSkipMetaPolicyStep = true;
        } else {
            logger.fine("The policy does not allow anonymous.");
        }

        // Run the policy-policy
        AssertionStatus status = null;
        if (!canSkipMetaPolicyStep) {
            logger.fine("Running meta-policy.");
            ServerAssertion policyPolicy = constructPolicyPolicy(targetPolicy);
            try {
                context.setAuditContext(auditContext);
                status = policyPolicy.checkRequest(context);
            } catch (IOException e) {
                response.initialize(exceptionToFault(e));
                return;
            } catch (PolicyAssertionException e) {
                response.initialize(exceptionToFault(e));
                return;
            }
        }

        Document policyDoc = null;
        if (canSkipMetaPolicyStep || status == AssertionStatus.NONE) {
            try {
                policyDoc = respondToPolicyDownloadRequest(policyId, context.getAuthenticatedUser(), policyGetter, pre32PolicyCompat);
            } catch (FilteringException e) {
                response.initialize(exceptionToFault(e));
                return;
            } catch (IOException e) {
                response.initialize(exceptionToFault(e));
                return;
            } catch (SAXException e) {
                response.initialize(exceptionToFault(e));
                return;
            }
        } else {
            response.initialize(makeUnauthorizedPolicyDownloadFault(status.getMessage(), context.getFaultDetail()));
            return;
        }

        if (policyDoc == null) {
            response.initialize(makeUnauthorizedPolicyDownloadFault("No such policy available to you.", context.getFaultDetail()));
            return;
        }

        try {
            String policyVersion = policyId + "|" + si.getVersion();
            response.initialize(wrapFilteredPolicyInResponse(policyDoc, policyVersion, relatesTo, signResponse));
        } catch (GeneralSecurityException e) {
            response.initialize(exceptionToFault(e));
            return;
        } catch (DecoratorException e) {
            response.initialize(exceptionToFault(e));
            return;
        }
        return;
    }

    private Document makeUnauthorizedPolicyDownloadFault(String msg, SoapFaultDetail faultDetail) {
        Document fault = null;
        try {
            Element detailEl = null;
            if (msg != null) {
                detailEl = SoapFaultUtils.makeFaultDetailsSubElement("more", msg);
            }
            if (faultDetail != null) {
                final Document factory;
                final Element holder;
                if (detailEl != null) {
                    factory = detailEl.getOwnerDocument();
                    holder = factory.createElement("details");
                } else {
                    factory = XmlUtil.stringToDocument("<holder/>");
                    holder = factory.getDocumentElement();
                }
                if (detailEl != null)
                    holder.appendChild(detailEl);

                final String faultString = faultDetail.getFaultString();
                if (faultString != null) {
                    Element mess = factory.createElement("message");
                    mess.appendChild(XmlUtil.createTextNode(factory, faultString));
                    holder.appendChild(mess);
                }

                final Element fd = faultDetail.getFaultDetail();
                if (fd != null)
                    holder.appendChild(fd);
            }

            fault = SoapFaultUtils.generateSoapFaultDocument(SoapFaultUtils.FC_SERVER,
                                                             "unauthorized policy download",
                                                             detailEl,
                                                             "");
            return fault;
        } catch (IOException e) {
            return exceptionToFault(e);
        } catch (SAXException e) {
            return exceptionToFault(e);
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
              "<L7a:" + SoapUtil.L7_POLICYVERSION_ELEMENT + " xmlns:L7a=\"" +
              SoapUtil.L7_MESSAGEID_NAMESPACE + "\"/>" +
              "</soap:Header>" +
              "<soap:Body>" +
              "<wsx:GetPolicyResponse xmlns:wsx=\"" + SoapUtil.WSX_NAMESPACE + "\"/>" +
              "</soap:Body></soap:Envelope>");
            Element header = SoapUtil.getHeaderElement(responseDoc);
            Element pver = XmlUtil.findFirstChildElement(header);
            pver.appendChild(XmlUtil.createTextNode(responseDoc, policyVersion));
            Element body = SoapUtil.getBodyElement(responseDoc);
            Element gpr = XmlUtil.findFirstChildElement(body);
            gpr.appendChild(responseDoc.importNode(policyDoc.getDocumentElement(), true));
            Element rte = null;
            if (relatesTo != null)
                rte = SoapUtil.setL7aRelatesTo(responseDoc, relatesTo);
            if (signResponse)
                signresponse(responseDoc, pver, rte);
            else
                SoapUtil.addTimestamp(header, SoapUtil.WSU_NAMESPACE, null, 0);
            return responseDoc;
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private void signresponse(Document responseDoc, Element policyVersion, Element relatesTo) throws GeneralSecurityException, DecoratorException {
        WssDecoratorImpl decorator = new WssDecoratorImpl();
        DecorationRequirements reqmts = new DecorationRequirements();
        reqmts.setSenderMessageSigningCertificate(serverCert);
        reqmts.setSenderMessageSigningPrivateKey(privateServerKey);
        reqmts.setSignTimestamp();
        try {
            reqmts.getElementsToSign().add(SoapUtil.getBodyElement(responseDoc));
            if (policyVersion != null)
                reqmts.getElementsToSign().add(policyVersion);
            if (relatesTo != null)
                reqmts.getElementsToSign().add(relatesTo);
            decorator.decorateMessage(responseDoc, reqmts);
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private String getRequestedPolicyId(Document requestDoc) throws InvalidDocumentFormatException {
        Element header = SoapUtil.getHeaderElement(requestDoc);
        if (header == null) throw new MissingRequiredElementException("No SOAP Header was found in the request.");
        Element sidEl = XmlUtil.findOnlyOneChildElementByName(header, SoapUtil.L7_MESSAGEID_NAMESPACE, SoapUtil.L7_SERVICEID_ELEMENT);
        if (sidEl == null) throw new MissingRequiredElementException("No {" + SoapUtil.L7_MESSAGEID_NAMESPACE +
                             "}" + SoapUtil.L7_SERVICEID_ELEMENT +
                             " element was found in the SOAP header.");
        String serviceId = XmlUtil.getTextValue(sidEl);
        if (serviceId == null || serviceId.length() < 1) throw new InvalidDocumentFormatException(SoapUtil.L7_SERVICEID_ELEMENT +
                                                           " element was empty.");

        // Check that this is a proper GetPolicy request.
        Element body = SoapUtil.getBodyElement(requestDoc);
        if (body == null) throw new MissingRequiredElementException("No SOAP Body was found in the request.");
        Element getPolicy = XmlUtil.findOnlyOneChildElementByName(body, SoapUtil.WSX_NAMESPACE, "GetPolicy");
        if (getPolicy == null) throw new MissingRequiredElementException("Request was not a wsx:GetPolicy request");

        return serviceId;
    }

    private Document exceptionToFault(Exception e) {
        logger.log(Level.INFO, e.getMessage(), e);
        try {
            Document fault;
            if (e instanceof SoapFaultDetail) {
                fault = SoapFaultUtils.generateSoapFaultDocument((SoapFaultDetail)e, SoapFaultUtils.FC_SERVER);
            } else {
                fault = SoapFaultUtils.generateSoapFaultDocument(SoapFaultUtils.FC_SERVER,
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
         */
    ServerAssertion constructPolicyPolicy(Assertion targetPolicy) {
        AllAssertion base = new AllAssertion();
        base.addChild(new OneOrMoreAssertion(allCredentialAssertions));
        List allTargetIdentities = new ArrayList();
        addIdAssertionToList(targetPolicy, allTargetIdentities);
        if (allTargetIdentities.size() > 0)
            base.addChild(new OneOrMoreAssertion(allTargetIdentities));
        return policyFactory.makeServerPolicy(base);
    }

    private void addIdAssertionToList(Assertion assertion, List receptacle) {
        if (assertion instanceof IdentityAssertion)
            receptacle.add(assertion);
        if (!(assertion instanceof CompositeAssertion))
            return;
        CompositeAssertion composite = (CompositeAssertion)assertion;
        for (Iterator i = composite.getChildren().iterator(); i.hasNext();) {
            Assertion a = (Assertion)i.next();
            addIdAssertionToList(a, receptacle);
        }
    }

    private boolean atLeastOnePathIsAnonymous(Assertion rootAssertion) {
        PolicyPathResult paths = PolicyPathBuilder.getDefault().generate(rootAssertion);
        for (Iterator iterator = paths.paths().iterator(); iterator.hasNext();) {
            AssertionPath assertionPath = (AssertionPath)iterator.next();
            Assertion[] path = assertionPath.getPath();
            boolean pathContainsIdAssertion = false;
            for (int i = 0; i < path.length; i++) {
                Assertion a = path[i];
                if (a instanceof IdentityAssertion) {
                    pathContainsIdAssertion = true;
                }
            }
            if (!pathContainsIdAssertion) return true;
        }
        return false;
    }

    private final List allCredentialAssertions;
    private final PrivateKey privateServerKey;
    private final X509Certificate serverCert;
    private final ServerPolicyFactory policyFactory;
    private final AuditContext auditContext;
    private final FilterManager filterManager;
    private final Logger logger = Logger.getLogger(PolicyService.class.getName());
}
