package com.l7tech.external.assertions.uddinotification;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.wsdl.Wsdl;

/**
 * 
 */
public class UDDINotificationAssertion extends Assertion {
    private static final String META_INITIALIZED = UDDINotificationAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "Handle UDDI Subscription Notification");
        meta.put(DESCRIPTION, "Process a UDDI subscription notification and generate a result message.");
        meta.put(PALETTE_FOLDERS, new String[] { "audit" });
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(IS_ROUTING_ASSERTION, Boolean.TRUE);
        meta.put(POLICY_VALIDATOR_CLASSNAME, UDDINotificationAssertion.Validator.class.getName());
        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.uddinotification.server.UDDINotificationModuleLifecycle");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public static class Validator implements AssertionValidator {
        private UDDINotificationAssertion assertion;

        public Validator( final UDDINotificationAssertion assertion ) {
            this.assertion = assertion;
        }

        @Override
        public void validate( final AssertionPath path,
                              final Wsdl wsdl,
                              final boolean soap,
                              final PolicyValidatorResult result ) {
            if ( (wsdl == null) || (!"urn:uddi-org:subr_v3".equals(wsdl.getTargetNamespace())) ){
                result.addWarning(new PolicyValidatorResult.Warning(
                                   assertion,
                                   path,
                                   "Assertion is for use only with a UDDI Notification Service",
                                   null));
            }
        }
    }

}
