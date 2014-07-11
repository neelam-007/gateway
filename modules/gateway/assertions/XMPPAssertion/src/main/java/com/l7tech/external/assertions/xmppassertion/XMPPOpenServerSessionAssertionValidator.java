package com.l7tech.external.assertions.xmppassertion;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 30/01/13
 * Time: 10:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPOpenServerSessionAssertionValidator implements AssertionValidator {
    private XMPPOpenServerSessionAssertion assertion;

    public XMPPOpenServerSessionAssertionValidator(XMPPOpenServerSessionAssertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        XMPPConnectionEntityAdmin impObject =
                Registry.getDefault().getExtensionInterface(XMPPConnectionEntityAdmin.class, null);

        String errorMessage = "Invalid outbound XMPP connection configured.  " +
                "Please ensure the assertion is configured with a valid outbound XMPP connection";
        PolicyValidatorResult.Error error = new PolicyValidatorResult.Error(assertion, errorMessage, null);

        XMPPConnectionEntity entity = null;

        //validate the outbound XMPP connection
        try {
            entity = impObject.find(assertion.getXMPPConnectionId());

            if (entity == null) {
                result.addError(error);
            } else if(entity.isInbound()) {
                result.addError(error);
            }
        } catch(FindException fE) {
            result.addError(error);
        }
    }
}
