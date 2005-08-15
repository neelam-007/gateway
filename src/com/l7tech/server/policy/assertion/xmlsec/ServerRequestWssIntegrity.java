package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.security.token.ParsedElement;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

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

    public ServerRequestWssIntegrity(RequestWssIntegrity data, ApplicationContext springContext) {
        super(logger, data, springContext);
    }

    protected String getPastTenseOperationName() {
        return "signed";
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus result =  super.checkRequest(context);
        if (result == AssertionStatus.NONE) {
            XmlKnob reqXml = null;
            ProcessorResult wssResults = null;
            try {
                reqXml = context.getRequest().getXmlKnob();
                wssResults = reqXml.getProcessorResult();
                if (wssResults != null && wssResults.isWsse11Seen() && wssResults.getLastSignatureValue() != null) {
                    context.addDeferredAssertion(this, deferredSignatureConfirmation(wssResults.getLastSignatureValue()));
                }
            } catch (SAXException e) {
                // Can't heppen; log and ignore.
                logger.info("Signature check succeeded, but request not parsed");
            }
        }
        return result;
    }

    // A deferred job that tries to attach a SignatureConfirmation to the response, if the response is SOAP.
    private ServerAssertion deferredSignatureConfirmation(final String signatureConfirmation) {
        return new ServerAssertion() {
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
                DecorationRequirements wssReq;

                try {
                    if (!context.getResponse().isSoap()) {
                        auditor.logAndAudit(AssertionMessages.REQUEST_WSS_INT_RESPONSE_NOT_SOAP);
                        // FALLTHROUGH: We'll still send the response; it just won't contain a SignatureConfirmation
                    } else {
                        wssReq = context.getResponse().getXmlKnob().getOrMakeDecorationRequirements();
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
        return wssResults.getElementsThatWereSigned();
    }

    protected boolean isAllowIfEmpty() {
        return false;
    }
}
