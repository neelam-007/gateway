package com.l7tech.external.assertions.esm;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.*;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.wsdl.Wsdl;

import java.util.HashMap;
import java.util.logging.Logger;

/** User: megery */
public class EsmSubscriptionAssertion extends Assertion implements UsesVariables, PolicyReference {
    protected static final Logger logger = Logger.getLogger(EsmSubscriptionAssertion.class.getName());
    private String notificationPolicyGuid;
    private transient Policy notificationPolicy;    
    private static final String ESMSM = "http://metadata.dod.mil/mdr/ns/netops/esm/esmsm";

    public String[] getVariablesUsed() {
        return new String[0]; //Syntax.getReferencedNames(...);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = EsmSubscriptionAssertion.class.getName() + ".metadataInitialized";

    public String getNotificationPolicyGuid() {
        return notificationPolicyGuid;
    }

    public void setNotificationPolicyGuid(String notificationPolicyGuid) {
        this.notificationPolicyGuid = notificationPolicyGuid;
    }

    public Policy retrieveFragmentPolicy() {
        return notificationPolicy;
    }

    public void replaceFragmentPolicy(Policy policy) {
        notificationPolicy = policy;
    }

    public String retrievePolicyGuid() {
        return notificationPolicyGuid;
    }

    @Override
    public void updateTemporaryData(Assertion assertion) {
        if(!(assertion instanceof EsmSubscriptionAssertion)) {
            return;
        }

        EsmSubscriptionAssertion esmAssertion = (EsmSubscriptionAssertion)assertion;
        notificationPolicy = esmAssertion.retrieveFragmentPolicy();
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, new HashMap<String, String[]>());

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "ESM Subscription");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] {"audit"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(POLICY_NODE_NAME, "ESM Subscription Assertion");
        meta.put(AssertionMetadata.POLICY_NODE_CLASSNAME, "com.l7tech.external.assertions.esm.console.EsmSubscriptionAssertionPolicyNode");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, EsmSubscriptionAssertion.Validator.class.getName());

        //set the routing assertion flag
        meta.put(AssertionMetadata.IS_ROUTING_ASSERTION, Boolean.TRUE);

        // request default feature set name for our claslss name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Bogus" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public static class Validator implements AssertionValidator {
        private EsmSubscriptionAssertion assertion;

        public Validator(EsmSubscriptionAssertion a) {
            assertion = a;

        }

        public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
            String polGuid = assertion.getNotificationPolicyGuid();
            if (polGuid == null || "".equals(polGuid))
                 result.addWarning(new PolicyValidatorResult.Warning(
                                    assertion,
                                    path,
                                    "No notification policy has been specified. New or renewed subscriptions will not have an outbound policy",
                                    null));

            // check to see if it's an XML service, display appropriate warning
            if (( wsdl == null) || (!ESMSM.equals(wsdl.getTargetNamespace())) ) {
                result.addWarning(new PolicyValidatorResult.Warning(
                                   assertion,
                                   path,
                                   "Assertion added to a policy not intended for an ESM Subscription service",
                                   null));
            }
        }
    }
}
