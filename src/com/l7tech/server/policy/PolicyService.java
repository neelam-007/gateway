package com.l7tech.server.policy;

import com.l7tech.common.security.xml.*;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MissingRequiredElementException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.identity.User;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyPathBuilder;
import com.l7tech.policy.PolicyPathResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.server.filter.FilterManager;
import com.l7tech.policy.server.filter.FilteringException;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
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
 *
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 23, 2004<br/>
 * $Id$<br/>
 */
public class PolicyService {

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
     * @return the filtered policy or null if the target policy does not exist or the requestor should not see it
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
        if (filteredAssertion == null) {
            if (atLeastOnePathIsAnonymous(targetPolicy)) {
                return XmlUtil.stringToDocument(WspWriter.getPolicyXml(null));
            } else {
                return null;
            }
        }
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
        String relatesTo = null;
        try {
            policyId = getRequestedPolicyId(wssOutput.getUndecoratedMessage());
            relatesTo = SoapUtil.getL7aMessageId(wssOutput.getUndecoratedMessage());
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
            wrapFilteredPolicyInResponse(policyDoc, policyVersion, relatesTo, response);
        } catch (GeneralSecurityException e) {
            exceptionToFault(e, response);
            return;
        } catch (WssDecorator.DecoratorException e) {
            exceptionToFault(e, response);
            return;
        }
        return;
    }

    private void wrapFilteredPolicyInResponse(Document policyDoc, String policyVersion, String relatesTo, SoapResponse response)
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
            Element rte = null;
            if (relatesTo != null)
                rte = SoapUtil.setL7aRelatesTo(responseDoc, relatesTo);
            signresponse(responseDoc, pver, rte);
            response.setResponseXml(XmlUtil.nodeToString(responseDoc));
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private void signresponse(Document responseDoc, Element policyVersion, Element relatesTo) throws GeneralSecurityException, WssDecorator.DecoratorException {
        WssDecoratorImpl decorator = new WssDecoratorImpl();
        WssDecorator.DecorationRequirements reqmts = new WssDecorator.DecorationRequirements();
        reqmts.setSenderCertificate(serverCert);
        reqmts.setSenderPrivateKey(privateServerKey);
        reqmts.setSignTimestamp(true);
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
    private PrivateKey privateServerKey = null;
    private X509Certificate serverCert = null;
    private final Logger logger = Logger.getLogger(PolicyService.class.getName());
}
