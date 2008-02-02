package com.l7tech.external.assertions.ncesval.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.SecurityKnob;
import com.l7tech.common.security.token.SignedElement;
import com.l7tech.common.security.token.SigningSecurityToken;
import com.l7tech.common.security.token.XmlSecurityToken;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.xml.processor.BadSecurityContextException;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssProcessorImpl;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.external.assertions.ncesval.NcesValidatorAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the NcesValidatorAssertion.
 *
 * @see com.l7tech.external.assertions.ncesval.NcesValidatorAssertion
 */
public class ServerNcesValidatorAssertion extends AbstractServerAssertion<NcesValidatorAssertion> {
    private static final Logger logger = Logger.getLogger(ServerNcesValidatorAssertion.class.getName());

    private final NcesValidatorAssertion assertion;
    private final Auditor auditor;
    private final SecurityTokenResolver securityTokenResolver;

    public ServerNcesValidatorAssertion(NcesValidatorAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.auditor = new Auditor(this, context, logger);
        this.securityTokenResolver = (SecurityTokenResolver)context.getBean("securityTokenResolver");
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message msg;
        final String what;
        switch(assertion.getTarget()) {
            case REQUEST:
                what = "request";
                msg = context.getRequest();
                break;
            case RESPONSE:
                what = "response";
                msg = context.getResponse();
                break;
            case OTHER:
                what = assertion.getOtherMessageVariableName();
                if (what == null) throw new PolicyAssertionException(assertion, "Target is OTHER but OtherMessageVariableName is null");
                msg = context.getRequestMessage(what);
                if (msg == null) {
                    auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, assertion.getClass().getSimpleName(), what + " message variable has not been set; unable to proceed");
                    return AssertionStatus.FAILED;
                }
                break;
            default:
                throw new IllegalStateException("Unsupported TargetMessageType: " + assertion.getTarget());
        }

        try {
            msg.isSoap(true);
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, assertion.getClass().getSimpleName(), "Message is not SOAP");
            return AssertionStatus.NOT_APPLICABLE;
        }

        SecurityKnob sk = (SecurityKnob)msg.getKnob(SecurityKnob.class);

        final ProcessorResult wssResult;
        if (sk != null) {
            wssResult = sk.getProcessorResult();
        } else {
            // Need to Trogdor this message before we can validate it
            try {
                WssProcessorImpl trogdor = new WssProcessorImpl(msg);
                trogdor.setSecurityTokenResolver(securityTokenResolver);
                wssResult = trogdor.processMessage();
            } catch (InvalidDocumentFormatException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (ProcessorException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (BadSecurityContextException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (SAXException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }

        SamlAssertion saml = null;
        if (assertion.isSamlRequired()) {
            for (XmlSecurityToken token : wssResult.getXmlSecurityTokens()) {
                if (token instanceof SamlAssertion) {
                    saml = (SamlAssertion)token;
                }
            }
        }

        SigningSecurityToken samlSigner = null;
        SigningSecurityToken timestampSigner = null;
        SigningSecurityToken messageIdSigner = null;
        SigningSecurityToken bodySigner = null;

        for (SignedElement signedElement : wssResult.getElementsThatWereSigned()) {
            SigningSecurityToken sst = signedElement.getSigningSecurityToken();
            Element el = signedElement.asElement();
            if (saml != null && saml.asElement().isEqualNode(el)) samlSigner = sst;
            if (XmlUtil.elementInNamespace(el, SoapUtil.WSA_NAMESPACE_ARRAY) && SoapUtil.MESSAGEID_EL_NAME.equals(el.getLocalName())) messageIdSigner = sst;
            if (XmlUtil.elementInNamespace(el, SoapUtil.WSU_URIS_ARRAY) && SoapUtil.TIMESTAMP_EL_NAME.equals(el.getLocalName())) timestampSigner = sst;
            try {
                if (SoapUtil.isBody(el)) bodySigner = sst;
            } catch (InvalidDocumentFormatException e) {
                throw new PolicyAssertionException(assertion, "Can't find SOAP Body element", e);
            }
        }

        if (assertion.isSamlRequired() && samlSigner == null) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, assertion.getClass().getSimpleName(), what + " did not contain a signed SAML assertion as expected");
            return AssertionStatus.BAD_REQUEST;
        }

        if (timestampSigner == null) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, assertion.getClass().getSimpleName(), what + " did not contain a signed wsu:Timestamp as expected");
            return AssertionStatus.BAD_REQUEST;
        }

        if (messageIdSigner == null) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, assertion.getClass().getSimpleName(), what + " did not contain a signed wsa:MessageID as expected");
            return AssertionStatus.BAD_REQUEST;
        }

        if (messageIdSigner == null) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, assertion.getClass().getSimpleName(), what + " Body was not signed");
            return AssertionStatus.BAD_REQUEST;
        }

        boolean ok;
        if (assertion.isSamlRequired()) {
            ok = samlSigner == timestampSigner && timestampSigner == messageIdSigner && messageIdSigner == bodySigner;
        } else {
            ok = timestampSigner == messageIdSigner && messageIdSigner == bodySigner;
        }

        if (!ok) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, assertion.getClass().getSimpleName(), what + " contained the expected elements, but they were covered by different Signatures");
            return AssertionStatus.BAD_REQUEST;
        }
        
        return AssertionStatus.NONE;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerNcesValidatorAssertion is preparing itself to be unloaded");
    }
}
