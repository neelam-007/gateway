package com.l7tech.server.policy;

import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.server.filter.FilterManager;
import com.l7tech.policy.server.filter.FilteringException;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.security.xml.WssProcessorImpl;
import com.l7tech.common.security.xml.ProcessorException;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MissingRequiredElementException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.identity.User;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Logger;
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
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 23, 2004<br/>
 * $Id$<br/>
 */
public class PolicyService {

    interface PolicyGetter {
        AllAssertion getPolicy(String serviceId);
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
                                                   PolicyGetter policyGetter) throws FilteringException, IOException, SAXException{
        Assertion targetPolicy = policyGetter.getPolicy(policyId);
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

        // Run the policy-policy
        AllAssertion targetPolicy = policyGetter.getPolicy(policyId);
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
        }
        ServerAssertion policyPolicy = constructPolicyPolicy(targetPolicy);
        AssertionStatus status = null;
        try {
            status = policyPolicy.checkRequest(request, response);
        } catch (IOException e) {
            exceptionToFault(e, response);
            return;
        } catch (PolicyAssertionException e) {
            exceptionToFault(e, response);
            return;
        }

        Document policyDoc = null;
        if (status == AssertionStatus.NONE) {
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
        }
        wrapFilteredPolicyInResponse(policyDoc, response);
        return;
    }

    private void wrapFilteredPolicyInResponse(Document policyDoc, SoapResponse response) {
        Document responseDoc;
        try {
            responseDoc = XmlUtil.stringToDocument("<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\">" +
                                                   "<soap:Body>" +
                                                   "<wsx:GetPolicyResponse xmlns:wsx=\"" + SoapUtil.WSX_NAMESPACE + "\"/>" +
                                                   "</soap:Body></soap:Envelope>");
            Element body = SoapUtil.getBodyElement(responseDoc);
            Element gpr = XmlUtil.findFirstChildElement(body);
            gpr.appendChild(responseDoc.importNode(policyDoc.getDocumentElement(), true));
            response.setResponseXml(XmlUtil.nodeToString(responseDoc));
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
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

    private final List allCredentialAssertions;
    private PrivateKey privateServerKey = null;
    private X509Certificate serverCert = null;
    private final Logger logger = Logger.getLogger(PolicyService.class.getName());
}
