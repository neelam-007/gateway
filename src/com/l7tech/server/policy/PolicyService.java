package com.l7tech.server.policy;

import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.server.filter.FilterManager;
import com.l7tech.policy.server.filter.FilteringException;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.common.security.xml.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MissingRequiredElementException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.FindException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.io.IOException;

/**
 * This is the service that lets a client download a policy for consuming a web service.
 * Whether or not a requestor is allowed to download a policy depends on whether or not he is capable
 * of consuming it. This is determined by looking at the identity assertions inside the target policy
 * comparing them with the identity resuling from the authentication of the policy download request.
 *
 * Todo, special case for TAM
 * Todo, special case for policies that have a path without id assertions
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 23, 2004<br/>
 * $Id$<br/>
 */
public class PolicyService {

    interface ServiceInfo {
        Assertion getPolicy();
        String getVersion();
    }

    interface PolicyGetter {
        /**
         * @return the policy or null if service does not exist
         */
        ServiceInfo getPolicy(String serviceId);
    }

    public PolicyService(PrivateKey privateServerKey, X509Certificate serverCert) {
        // populate all possible credentials sources
        allCredentialAssertions = new ArrayList();
        for (int i = 0; i < CredentialSourceAssertion.ALL_CREDENTIAL_ASSERTIONS_TYPES.length; i++) {
            CredentialSourceAssertion assertion = CredentialSourceAssertion.ALL_CREDENTIAL_ASSERTIONS_TYPES[i];
            allCredentialAssertions.add(assertion);
        }
        if (privateServerKey == null || serverCert == null) {
            throw new IllegalArgumentException("Server key and server cert must be provided to create a TokenService");
        }
        this.privateServerKey = privateServerKey;
        this.serverCert = serverCert;
    }

    /**
     * @return the filtered policy or null if the target policy does not exist
     */
    public Document respondToPolicyDownloadRequest(String policyId,
                                                   User preAuthenticatedUser,
                                                   PolicyGetter policyGetter) throws FilteringException,
                                                                                     IOException,
                                                                                     SAXException {
        Assertion targetPolicy = policyGetter.getPolicy(policyId).getPolicy();
        if (targetPolicy == null) {
            logger.info("cannot find target policy from id: " + policyId);
            return null;
        }
        Assertion filteredAssertion = FilterManager.getInstance().applyAllFilters(preAuthenticatedUser, targetPolicy);
        return XmlUtil.stringToDocument(WspWriter.getPolicyXml(filteredAssertion));
    }


    /**
     * The filtered policy is contained in the soapResponse
     */
    public void respondToPolicyDownloadRequest(SoapRequest request,
                                               SoapResponse response,
                                               PolicyGetter policyGetter)
    {
        // We need a Document
        Document requestDoc = null;
        try {
            requestDoc = request.getDocument();
        } catch (SAXException e) {
            throw new IllegalArgumentException("Request must contain an xml document");
        } catch (IOException e) {
            throw new IllegalArgumentException("Request must contain an xml document");
        }

        // Process request for message level security stuff
        WssProcessor.ProcessorResult wssOutput = null;
        try {
            wssOutput = processMessageLevelSecurity(requestDoc);
        } catch (ProcessorException e) {
            exceptionToFault(e, response);
            return;
        } catch (InvalidDocumentFormatException e) {
            exceptionToFault(e, response);
            return;
        } catch (GeneralSecurityException e) {
            exceptionToFault(e, response);
            return;
        } catch (WssProcessor.BadContextException e) {
            exceptionToFault(e, response);
            return;
        }
        if (wssOutput.getUndecoratedMessage() != null) {
            request.setDocument(wssOutput.getUndecoratedMessage());
        }
        request.setWssProcessorOutput(wssOutput);

        // Which policy is requested?
        String policyId = null;
        try {
            policyId = getRequestedPolicyId(wssOutput.getUndecoratedMessage());
        } catch (InvalidDocumentFormatException e) {
            exceptionToFault(e, response);
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
                fault = SoapFaultUtils.generateSoapFault(SoapFaultUtils.FC_SERVER,
                                                         "policy " + policyId + " not found",
                                                         "the policy requested does not exist",
                                                         "");
            } catch (IOException e) {
                exceptionToFault(e, response);
                return;
            } catch (SAXException e) {
                exceptionToFault(e, response);
                return;
            }
            response.setDocument(fault);
            return;
        }

        // if the policy allows anonymous, skip the meta policy
        boolean canSkipMetaPolicyStep = false;
        try {
            if (policyAllowAnonymous(targetPolicy)) {
                canSkipMetaPolicyStep = true;
            } else {
                logger.fine("The policy does not allow anonymous.");
            }
        } catch (IOException e) {
            exceptionToFault(e, response);
            return;
        }

        // Run the policy-policy
        AssertionStatus status = null;
        if (!canSkipMetaPolicyStep) {
            logger.fine("Running meta-policy.");
            ServerAssertion policyPolicy = constructPolicyPolicy(targetPolicy);
            try {
                status = policyPolicy.checkRequest(request, response);
            } catch (IOException e) {
                exceptionToFault(e, response);
                return;
            } catch (PolicyAssertionException e) {
                exceptionToFault(e, response);
                return;
            }
        }

        Document policyDoc = null;
        if (canSkipMetaPolicyStep || status == AssertionStatus.NONE) {
            try {
                policyDoc = respondToPolicyDownloadRequest(policyId, request.getUser(), policyGetter);
            } catch (FilteringException e) {
                exceptionToFault(e, response);
                return;
            } catch (IOException e) {
                exceptionToFault(e, response);
                return;
            } catch (SAXException e) {
                exceptionToFault(e, response);
                return;
            }
        } else {
            Document fault = null;
            try {
                fault = SoapFaultUtils.generateSoapFault(SoapFaultUtils.FC_SERVER,
                                                         "unauthorized policy download",
                                                         status.getMessage(),
                                                         "");
            } catch (IOException e) {
                exceptionToFault(e, response);
                return;
            } catch (SAXException e) {
                exceptionToFault(e, response);
                return;
            }
            response.setDocument(fault);
            return;
        }
        try {
            String policyVersion = policyId + "|" + si.getVersion();
            wrapFilteredPolicyInResponse(policyDoc, policyVersion, response);
        } catch (GeneralSecurityException e) {
            exceptionToFault(e, response);
            return;
        } catch (WssDecorator.DecoratorException e) {
            exceptionToFault(e, response);
            return;
        }
        return;
    }

    private void wrapFilteredPolicyInResponse(Document policyDoc, String policyVersion, SoapResponse response)
                            throws GeneralSecurityException, WssDecorator.DecoratorException {
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
            pver.appendChild(responseDoc.createTextNode(policyVersion));
            Element body = SoapUtil.getBodyElement(responseDoc);
            Element gpr = XmlUtil.findFirstChildElement(body);
            gpr.appendChild(responseDoc.importNode(policyDoc.getDocumentElement(), true));
            signresponse(responseDoc, pver);
            response.setResponseXml(XmlUtil.nodeToString(responseDoc));
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private void signresponse(Document responseDoc, Element policyVersion) throws GeneralSecurityException, WssDecorator.DecoratorException {
        WssDecoratorImpl decorator = new WssDecoratorImpl();
        WssDecorator.DecorationRequirements reqmts = new WssDecorator.DecorationRequirements();
        reqmts.setSenderCertificate(serverCert);
        reqmts.setSenderPrivateKey(privateServerKey);
        reqmts.setSignTimestamp(true);
        try {
            reqmts.getElementsToSign().add(SoapUtil.getBodyElement(responseDoc));
            if (policyVersion != null)
                reqmts.getElementsToSign().add(policyVersion);
            decorator.decorateMessage(responseDoc, reqmts);
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private String getRequestedPolicyId(Document requestDoc) throws InvalidDocumentFormatException {
        // TODO add correlation ID in the response (L7a:AppliesTo)
        Element header = SoapUtil.getHeaderElement(requestDoc);
        if (header == null) throw new MissingRequiredElementException("No SOAP Header was found in the request.");
        Element sidEl = XmlUtil.findOnlyOneChildElementByName(header, SoapUtil.L7_MESSAGEID_NAMESPACE, SoapUtil.L7_SERVICEID_ELEMENT);
        if (sidEl == null) throw new MissingRequiredElementException("No {" +SoapUtil.L7_MESSAGEID_NAMESPACE +
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

    private void exceptionToFault(Exception e, SoapResponse response) {
        logger.log(Level.INFO, e.getMessage(), e);
        try {
            Document fault;
            if (e instanceof SoapFaultDetail)
                fault = SoapFaultUtils.generateSoapFault((SoapFaultDetail)e, SoapFaultUtils.FC_SERVER);
            else
                fault = SoapFaultUtils.generateSoapFault(SoapFaultUtils.FC_SERVER,
                                                         e.getMessage(),
                                                         e.toString(),
                                                         e.getClass().getName());
            response.setDocument(fault);
        } catch (IOException e1) {
            throw new RuntimeException(e1); // can't happen
        } catch (SAXException e1) {
            throw new RuntimeException(e1); // can't happen
        }
    }

    private WssProcessor.ProcessorResult processMessageLevelSecurity(Document request)
                                            throws ProcessorException, InvalidDocumentFormatException,
                                                   GeneralSecurityException, WssProcessor.BadContextException {
        WssProcessor trogdor = new WssProcessorImpl();
        return trogdor.undecorateMessage(request,
                                         serverCert,
                                         privateServerKey,
                                         SecureConversationContextManager.getInstance());
    }

    /**
     * Constructs a policy that determines if a requestor should be allowed to download a policy.
     * @param targetPolicy the policy targeted by a requestor.
     * @return the policy that should be validated by the policy download request for the passed target
     */
    ServerAssertion constructPolicyPolicy(Assertion targetPolicy) {
        AllAssertion base = new AllAssertion();
        base.getChildren().add(new OneOrMoreAssertion(allCredentialAssertions));
        List allTargetIdentities = new ArrayList();
        addIdAssertionToList(targetPolicy, allTargetIdentities);
        if (allTargetIdentities.size() > 0)
            base.getChildren().add(new OneOrMoreAssertion(allTargetIdentities));
        return ServerPolicyFactory.getInstance().makeServerPolicy(base);
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

    /**
     * copied from AuthenticatableHttpServlet
     * Todo, fix this so that a policy allows anonymous is AT LEAST ONE OF IT"S PATH DOES
     */
    protected boolean policyAllowAnonymous(Assertion rootassertion) throws IOException {
        // logic: a policy allows anonymous if and only if it does not contains any CredentialSourceAssertion
        // com.l7tech.policy.assertion.credential.CredentialSourceAssertion
        Iterator it = rootassertion.preorderIterator();
        boolean allIdentitiesAreFederated = true;
        while (it.hasNext()) {
            Assertion a = (Assertion)it.next();
            if (a instanceof CustomAssertionHolder) {
                CustomAssertionHolder ca = (CustomAssertionHolder)a;
                if (Category.ACCESS_CONTROL.equals(ca.getCategory())) {
                    return true;
                }
            } else if (a instanceof IdentityAssertion) {
                IdentityAssertion ia = (IdentityAssertion)a;
                final String msg = "Policy refers to a nonexistent identity provider";
                try {
                    IdentityProvider provider = IdentityProviderFactory.getProvider(ia.getIdentityProviderOid());
                    if ( provider == null ) {
                        logger.warning(msg);
                        return false;
                    }
                    if ( provider.getConfig().type() != IdentityProviderType.FEDERATED ) allIdentitiesAreFederated = false;
                } catch ( FindException e ) {
                    logger.warning(msg);
                    return false;
                }
            }
        }

        if ( allIdentitiesAreFederated ) {
            // TODO support federated credentials in PolicyServlet
            logger.info("All IdentityAssertions point to a Federated IDP. Treating as anonymous");
            return true;
        }

        if (findCredentialAssertion(rootassertion) != null) {
            logger.info("Policy does not allow anonymous requests.");
            return false;
        }
        logger.info("Policy does allow anonymous requests.");
        return true;
    }

    /**
     * Look for an assertion extending CredentialSourceAssertion in the assertion passed
     * and all it's decendents.
     * Returns null if not there.
     * (recursive method)
     * copied from AuthenticatableHttpServlet
     */
    private CredentialSourceAssertion findCredentialAssertion(Assertion arg) {
        if (arg instanceof CredentialSourceAssertion) {
            return (CredentialSourceAssertion)arg;
        }
        if (arg instanceof CompositeAssertion) {
            CompositeAssertion root = (CompositeAssertion)arg;
            Iterator i = root.getChildren().iterator();
            while (i.hasNext()) {
                Assertion child = (Assertion)i.next();
                CredentialSourceAssertion res = findCredentialAssertion(child);
                if (res != null) return res;
            }
        }
        return null;
    }

    private final List allCredentialAssertions;
    private PrivateKey privateServerKey = null;
    private X509Certificate serverCert = null;
    private final Logger logger = Logger.getLogger(PolicyService.class.getName());
}
