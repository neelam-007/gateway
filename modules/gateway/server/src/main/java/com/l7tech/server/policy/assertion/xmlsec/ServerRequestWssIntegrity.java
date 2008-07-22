package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.security.token.ParsedElement;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.util.CausedIOException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;

/**
 * Enforces that a specific element in a request is signed.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: July 14, 2004<br/>
 */
public class ServerRequestWssIntegrity extends ServerRequestWssOperation {
    private static final Logger logger = Logger.getLogger(ServerRequestWssIntegrity.class.getName());
    private final RequestWssIntegrity data;
    public ServerRequestWssIntegrity(RequestWssIntegrity data, ApplicationContext springContext) {
        super(logger, data, springContext);
        this.data = data;
    }

    protected String getPastTenseOperationName() {
        return "signed";
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus result =  super.checkRequest(context);
        if (result == AssertionStatus.NONE) {
            ProcessorResult wssResults = context.getRequest().getSecurityKnob().getProcessorResult();
            if (wssResults != null && wssResults.isWsse11Seen() && wssResults.getLastSignatureValue() != null) {
                context.addDeferredAssertion(this, deferredSignatureConfirmation(wssResults.getLastSignatureValue()));
            }
        }
        return result;
    }

    // A deferred job that tries to attach a SignatureConfirmation to the response, if the response is SOAP.
    private ServerAssertion deferredSignatureConfirmation(final String signatureConfirmation) {
        return new AbstractServerAssertion(data) {
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException {
                DecorationRequirements wssReq;

                try {
                    if (!context.getResponse().isSoap()) {
                        auditor.logAndAudit(AssertionMessages.REQUEST_WSS_INT_RESPONSE_NOT_SOAP);
                        // FALLTHROUGH: We'll still send the response; it just won't contain a SignatureConfirmation
                    } else {
                        wssReq = context.getResponse().getSecurityKnob().getOrMakeDecorationRequirements();
                        wssReq.setSignatureConfirmation(signatureConfirmation);
                    }
                } catch (SAXException e) {
                    throw new CausedIOException(e);
                }
                return AssertionStatus.NONE;
            }
        };
    }

    protected ParsedElement[] getElementsFoundByProcessor(ProcessorResult wssResults) {
        if (wssResults == null) return new ParsedElement[0];
        return wssResults.getElementsThatWereSigned();
    }

    /**
     * Ensure that any signed elements that not security tokens are signed by
     * the same key.
     */
    protected boolean elementsFoundByProcessorAreValid(ProcessorResult wssResults, ParsedElement[] elements) {
        boolean valid = true;

        if(elements.length>0) {
            SigningSecurityToken sst = null;
            Set securityTokenElements = getSecurityTokenElements(wssResults);

            for (int i = 0; i < elements.length; i++) {
                ParsedElement element = elements[i];
                if(element instanceof SignedElement) {
                    SignedElement signedElement = (SignedElement) element;
                    if(!securityTokenElements.contains(signedElement.asElement())) {
                        if(sst==null) {
                            sst = signedElement.getSigningSecurityToken();
                        }
                        else {
                            if(sst!=signedElement.getSigningSecurityToken()) {
                                //auditor.logAndAudit(AssertionMessages.REQUEST_WSS_INT_REQUEST_MULTI_SIGNED);
                                valid = false;
                                break;
                            }
                        }
                    }
                    else {
                        logger.fine("Not checking single signature source for signed security token.");
                    }
                }
                else {
                    // Can't happen; log and ignore.
                    logger.info("Unable to check element (not signed)");
                }
            }
        }

        return valid;
    }

    protected boolean isAllowIfEmpty() {
        return false;
    }

    private Set getSecurityTokenElements(ProcessorResult wssResults) {
        Set tokenElements = new HashSet();
        SecurityToken[] sts = wssResults.getXmlSecurityTokens();
        if(sts!=null) {
            for (int i = 0; i < sts.length; i++) {
                SecurityToken st = sts[i];
                if(st instanceof SigningSecurityToken) {
                    SigningSecurityToken sst = (SigningSecurityToken) st;
                    tokenElements.add(sst.asElement());
                }
            }
        }
        return tokenElements;
    }
}
