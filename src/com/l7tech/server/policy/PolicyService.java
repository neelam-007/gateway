package com.l7tech.server.policy;

import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.security.xml.WssProcessorImpl;
import com.l7tech.common.security.xml.ProcessorException;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MissingRequiredElementException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.identity.User;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.io.IOException;

/**
 * This is the service that lets a client download a policy for consuming a web service.
 * Whether or not a requestor is allowed to download a policy depends on whether or not he is capable
 * of consuming it. This is determined by looking at the identity assertions inside the target policy
 * comparing them with the identity resuling from the authentication of the policy download request.
 * Document format expected is <sp:Body><PolicyDiscoveryRequest serviceOid="132"/></sp:Body>
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
     * @return the filtered policy
     */
    public Document respondToPolicyDownloadRequest(String policyId,
                                                   User preAuthenticatedUser,
                                                   PolicyGetter policyGetter) {
        // todo
        return null;
    }


    /**
     * The filtered policy is contained in the soapResponse
     */
    public void respondToPolicyDownloadRequest(SoapRequest request,
                                               SoapResponse response,
                                               PolicyGetter policyGetter) {
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
        ServerAssertion policyPolicy = constructPolicyPolicy(targetPolicy);
        AssertionStatus status = null;
        try {
            status = policyPolicy.checkRequest(request, response);
        } catch (IOException e) {
            exceptionToFault(e, response);
        } catch (PolicyAssertionException e) {
            exceptionToFault(e, response);
        }

        Document policyDoc = null;
        if (status == AssertionStatus.NONE) {
            policyDoc = respondToPolicyDownloadRequest(policyId, request.getUser(), policyGetter);
        } else {
            // todo, some special soap fault
        }

        wrapFilteredPolicyInResponse(policyDoc, response);
        return;
    }

    private void wrapFilteredPolicyInResponse(Document policyDoc, SoapResponse response) {
        // todo
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

    private SoapResponse exceptionToFault(Exception e, SoapResponse response) {
        // todo
        return null;
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
    ServerAssertion constructPolicyPolicy(AllAssertion targetPolicy) {
        AllAssertion base = new AllAssertion();
        base.getChildren().add(new OneOrMoreAssertion(allCredentialAssertions));
        List allTargetIdentities = new ArrayList();
        addIdAssertionToList(targetPolicy, allTargetIdentities);
        base.getChildren().add(new OneOrMoreAssertion(allTargetIdentities));
        return ServerPolicyFactory.getInstance().makeServerPolicy(base);
    }

    private void addIdAssertionToList(CompositeAssertion composite, List receptacle) {
        for (Iterator i = composite.getChildren().iterator(); i.hasNext();) {
            Assertion a = (Assertion)i.next();
            if (a instanceof IdentityAssertion) {
                receptacle.add(a);
            } else if (a instanceof CompositeAssertion) {
                addIdAssertionToList((CompositeAssertion)a, receptacle);
            }
        }
    }

    private final List allCredentialAssertions;
    private PrivateKey privateServerKey = null;
    private X509Certificate serverCert = null;
}
