package com.l7tech.policy.validator;

import com.l7tech.wsdl.Wsdl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;

/**
 * Validates the <code>RequestWssConfidentiality</code> assertion internals. This checks
 * whether the WSSRecipient is non-local.  If so, a warning is issued.
 *
 * Note: This class was pre-existing in the source tree, but appeared to be unused.  Now
 * employed to provide a simple warning.
 *
 * @author emil
 * @author vchan
 */
public class RequestWssConfidentialityValidator implements AssertionValidator {
    /* not used */
    // private static final Logger logger = Logger.getLogger(RequestWssConfidentialityValidator.class.getName());

    protected static final String WARN_WSS_RECIPIENT_NOT_LOCAL =
            "A WSSRecipient other than \"Default\" will not be enforced by the gateway.  This assertion will always succeed.";

    private final RequestWssConfidentiality assertion;
    private boolean warnForAssert;

    /**
     * Constructor.
     *
     * @param ra    The assertion instance to validate
     */
    public RequestWssConfidentialityValidator(RequestWssConfidentiality ra) {
        this.assertion = ra;
        this.warnForAssert = !(assertion.getRecipientContext().localRecipient());
    }

    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {

        if (warnForAssert) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, path, WARN_WSS_RECIPIENT_NOT_LOCAL, null));
        }

        for (Assertion a : path.getPath()) {

            if (a != assertion && a instanceof RequestWssConfidentiality) {
                RequestWssConfidentiality ra = (RequestWssConfidentiality)a;

                // for the Request, we only check to see whether the recipient is local
                if (!ra.getRecipientContext().localRecipient()) {

                    // if so, create a warning
                    result.addWarning(new PolicyValidatorResult.Warning(ra, path, WARN_WSS_RECIPIENT_NOT_LOCAL, null));
                }
            }
        }
    }

}
