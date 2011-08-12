package com.l7tech.external.assertions.icapantivirusscanner;

import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.CollectionTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.*;

/**
 *
 */
public class IcapAntivirusScannerAssertion extends MessageTargetableAssertion implements UsesVariables {

    private static final String META_INITIALIZED = IcapAntivirusScannerAssertion.class.getName() + ".metadataInitialized";

    private String failoverStrategy = FailoverStrategyFactory.ORDERED.getName();

    private List<IcapConnectionDetail> connectionDetails = new ArrayList<IcapConnectionDetail>();

    private boolean failOnVirusFound = true;

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Icap Anti-Virus Scanner");
        meta.put(AssertionMetadata.LONG_NAME, "Scan virus using the ICAP protocol against an ICAP capable anti-virus server.");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"threatProtection"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:IcapAntivirusScanner" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        //add custom type mapping
        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new CollectionTypeMapping(List.class, IcapConnectionDetail.class, ArrayList.class, "icapConnections"));
        othermappings.add(new BeanTypeMapping(IcapConnectionDetail.class, "connection"));
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, IcapAntivirusScannerAssertion.Validator.class.getName());
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public String getFailoverStrategy() {
        return failoverStrategy;
    }

    public void setFailoverStrategy(final String failoverStrategy) {
        this.failoverStrategy = failoverStrategy;
    }

    public boolean isFailOnVirusFound() {
        return failOnVirusFound;
    }

    public void setFailOnVirusFound(final boolean failOnVirusFound) {
        this.failOnVirusFound = failOnVirusFound;
    }

    public List<IcapConnectionDetail> getConnectionDetails() {
        return connectionDetails;
    }

    public void setConnectionDetails(final List<IcapConnectionDetail> connectionDetails) {
        this.connectionDetails = connectionDetails;
    }

    public static class Validator implements AssertionValidator {

        private final IcapAntivirusScannerAssertion assertion;

        public Validator(IcapAntivirusScannerAssertion assertion) {
            if (assertion == null) {
                throw new IllegalArgumentException("assertion is required");
            }
            this.assertion = assertion;
        }

        @Override
        public void validate(final AssertionPath path, final PolicyValidationContext pvc, final PolicyValidatorResult result) {
            if (assertion.getConnectionDetails().isEmpty()) {
                result.addError(new PolicyValidatorResult.Error(assertion, "Require at least one valid connection to an ICAP anti-virus server.", null));
            }
        }
    }
}
