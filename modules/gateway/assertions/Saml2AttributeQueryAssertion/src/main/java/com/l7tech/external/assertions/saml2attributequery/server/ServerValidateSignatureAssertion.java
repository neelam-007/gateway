package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.external.assertions.saml2attributequery.ValidateSignatureAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.security.xml.processor.BadSecurityContextException;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.token.SignedElement;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 23-Jan-2009
 * Time: 12:24:49 AM
 */
public class ServerValidateSignatureAssertion extends AbstractServerAssertion<ValidateSignatureAssertion> {
    private SecurityTokenResolver securityTokenResolver;

    @SuppressWarnings({"UnusedDeclaration"})
    public ServerValidateSignatureAssertion( final ValidateSignatureAssertion assertion,
                                             final ApplicationContext context )
        throws PolicyAssertionException
    {
        super(assertion);
        securityTokenResolver = context.getBean("securityTokenResolver", SecurityTokenResolver.class);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Saml2WssProcessorImpl securityProcessor = new Saml2WssProcessorImpl(context.getRequest());
        securityProcessor.setSecurityTokenResolver(securityTokenResolver);

        try {
            Object obj = context.getVariable(assertion.getVariableName());
            if(obj == null || !(obj instanceof Element[]) || ((Element[])obj).length < 1) {
                logAndAudit( AssertionMessages.SAML2_AQ_REQUEST_DIGSIG_VAR_UNUSABLE );
                return AssertionStatus.FALSIFIED;
            }

            Element root = ((Element[])obj)[0];

            NodeList signatures = root.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature");
            Element signatureElement = null;
            for(int i = 0;i < signatures.getLength();i++) {
                Element elem = (Element)signatures.item(i);
                if(isSignatureForElement(root, elem)) {
                    signatureElement = elem;
                    break;
                }
            }

            if(signatureElement == null) {
                logAndAudit( AssertionMessages.SAML2_AQ_REQUEST_DIGSIG_NO_SIG );
                return AssertionStatus.FALSIFIED;
            }

            ProcessorResult processorResult = securityProcessor.processMessage(root, signatureElement);

            boolean signatureFound = false;
            for(SignedElement signedElement : processorResult.getElementsThatWereSigned()) {
                if(signedElement.asElement() == root) {
                    signatureFound = true;
                    break;
                }
            }
            if(!signatureFound) {
                logAndAudit( AssertionMessages.SAML2_AQ_REQUEST_DIGSIG_NO_SIG );
                return AssertionStatus.FALSIFIED;
            }

            return AssertionStatus.NONE;
        } catch(NoSuchVariableException nsve) {
            logAndAudit( AssertionMessages.SAML2_AQ_REQUEST_DIGSIG_VAR_UNUSABLE );
        } catch(SAXException se) {
            logAndAudit( AssertionMessages.EXCEPTION_INFO );
        } catch(ProcessorException pe) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, pe.toString() );
        } catch(InvalidDocumentFormatException idfe) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, idfe.toString() );
        } catch(GeneralSecurityException ge) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, ge.toString() );
        } catch(BadSecurityContextException bse) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, bse.toString() );
        }

        return AssertionStatus.FALSIFIED;
    }

    private boolean isSignatureForElement(Element signedElement, Element signature) {
        String id = signedElement.getAttribute("ID");
        if(id == null) {
            return false;
        }
        id = "#" + id;

        NodeList nodes = signature.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "SignedInfo");
        if(nodes.getLength() == 0) {
            return false;
        }

        for(int i = 0;i < nodes.getLength();i++) {
            Element signedInfo = (Element)nodes.item(i);

            NodeList references = signedInfo.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Reference");
            for(int j = 0;j < references.getLength();j++) {
                Element reference = (Element)references.item(j);
                if(id.equals(reference.getAttribute("URI"))) {
                    return true;
                }
            }
        }

        return false;
    }
}
