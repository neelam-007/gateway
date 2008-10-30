package com.l7tech.external.assertions.esm;

import com.l7tech.wsdl.Wsdl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_EXTERNAL_NAME;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.validator.AssertionValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 */
public class EsmMetricsAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(EsmMetricsAssertion.class.getName());
    private static final String QOSMW	= "http://metadata.dod.mil/mdr/ns/netops/esm/qosmw";


    public String[] getVariablesUsed() {
        return new String[0]; //Syntax.getReferencedNames(...);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = EsmMetricsAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "ESM Metrics");
        meta.put(AssertionMetadata.LONG_NAME, "");

        // This is a pseudo-assertion and so should appear in no palette folders
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] {"audit"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_CLASSNAME, "com.l7tech.external.assertions.esm.console.EsmMetricsAssertionPolicyNode");
        // Subscribe our crap to the module loading events so it can set up its application listener
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.esm.server.EsmModuleApplicationListener");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, EsmMetricsAssertion.Validator.class.getName());

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(POLICY_NODE_NAME, "ESM Assertion");
        // request default feature set name for our claslss name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Bogus" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(WSP_EXTERNAL_NAME, "ESMAssertion");

        //set the routing assertion flag
        meta.put(AssertionMetadata.IS_ROUTING_ASSERTION, Boolean.TRUE);
        
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public static class Validator implements AssertionValidator {
        private EsmMetricsAssertion assertion;

        public Validator(EsmMetricsAssertion assertion) {
            this.assertion = assertion;
        }

        public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {

            // check to see if it's an XML service, display appropriate warning
            if (wsdl == null) {
                result.addWarning(new PolicyValidatorResult.Warning(
                                   assertion,
                                   path,
                                   "Assertion not intended for this policy. Should be used for SOAP services only",
                                   null));
            }
            // check that the tns of the wsdl definition for the policy matches the expected ESM QosMetrics service
            else if (!QOSMW.equals(wsdl.getTargetNamespace())) {
                // add warning
                result.addWarning(new PolicyValidatorResult.Warning(
                                   assertion,
                                   path,
                                   "Assertion added to a policy not intended for an ESM QosMetrics service",
                                   null));
            }
        }
    }
}
