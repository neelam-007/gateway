package com.l7tech.external.assertions.esm;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.wsdl.Wsdl;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * 
 */
@SuppressWarnings({ "serial" })
public class EsmMetricsAssertion extends Assertion {
    protected static final Logger logger = Logger.getLogger(EsmMetricsAssertion.class.getName());
    private static final String QOSMW	= "http://metadata.dod.mil/mdr/ns/netops/esm/qosmw";

    //
    // Metadata
    //
    private static final String META_INITIALIZED = EsmMetricsAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(SHORT_NAME, "Collect WSDM Metrics");
        meta.put(DESCRIPTION, "Collect metrics for a specified resource based on the Joint Web Services Distributed Management (WSDM) Specification.");

        meta.put(PALETTE_FOLDERS, new String[] {"internalAssertions"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.external.assertions.esm.console.EsmMetricsAssertionPolicyNode");
        // Subscribe our crap to the module loading events so it can set up its application listener
        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.esm.server.EsmModuleApplicationListener");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_VALIDATOR_CLASSNAME, EsmMetricsAssertion.Validator.class.getName());

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Bogus" rather than "set:modularAssertions"
        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(WSP_EXTERNAL_NAME, "ESMAssertion");

        //set the routing assertion flag
        meta.put(IS_ROUTING_ASSERTION, Boolean.TRUE);
        
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public static class Validator implements AssertionValidator {
        private EsmMetricsAssertion assertion;

        public Validator(EsmMetricsAssertion assertion) {
            this.assertion = assertion;
        }

        @Override
        public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
            final Wsdl wsdl = pvc.getWsdl();            

            // check to see if it's an XML service, display appropriate warning

            if ( (wsdl == null) || (!QOSMW.equals(wsdl.getTargetNamespace())) ){
                // add warning
                result.addWarning(new PolicyValidatorResult.Warning(
                                   assertion,
                        "Assertion added to a policy not intended for a WSDM QosMetrics service",
                                   null));
            }
        }
    }
}
