package com.l7tech.external.assertions.esm;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.wsdl.Wsdl;

import java.util.HashMap;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/** User: megery */
@SuppressWarnings({ "serial" })
public class EsmSubscriptionAssertion extends Assertion implements UsesEntities, PolicyReference {
    protected static final Logger logger = Logger.getLogger(EsmSubscriptionAssertion.class.getName());
    private String notificationPolicyGuid;
    private transient Policy notificationPolicy;    
    private static final String ESMSM = "http://metadata.dod.mil/mdr/ns/netops/esm/esmsm";

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

    @Override
    public Policy retrieveFragmentPolicy() {
        return notificationPolicy;
    }

    @Override
    public void replaceFragmentPolicy(Policy policy) {
        notificationPolicy = policy;
    }

    @Override
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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        meta.put(CLUSTER_PROPERTIES, new HashMap<String, String[]>());

        // Set description for GUI
        meta.put(SHORT_NAME, "Subscribe to WSDM Resource");
        meta.put(DESCRIPTION, "Send subscription requests to a specified resource based on the WSDM specification.");

        meta.put(PALETTE_FOLDERS, new String[] {"internalAssertions"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.external.assertions.esm.console.EsmSubscriptionAssertionPolicyNode");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_VALIDATOR_CLASSNAME, EsmSubscriptionAssertion.Validator.class.getName());

        meta.put(PROPERTIES_ACTION_NAME, "WSDM Subscription Properties");
        //set the routing assertion flag
        meta.put(IS_ROUTING_ASSERTION, Boolean.TRUE);

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Bogus" rather than "set:modularAssertions"
        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.OPTIONAL, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        GuidEntityHeader header = new GuidEntityHeader(notificationPolicyGuid, EntityType.POLICY, null, null, null);
        header.setGuid( notificationPolicyGuid ); // MigrationManagerImpl.resolveHeader() should fill in the rest of the details
        return new EntityHeader[] {
            header
        };
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (!(newEntityHeader instanceof PolicyHeader)) throw new IllegalArgumentException("newEntityHeader is not a PolicyHeader");

        notificationPolicyGuid = ((GuidEntityHeader) newEntityHeader).getGuid();
    }

    public static class Validator implements AssertionValidator {
        private EsmSubscriptionAssertion assertion;

        public Validator(EsmSubscriptionAssertion a) {
            assertion = a;

        }

        @Override
        public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
            final Wsdl wsdl = pvc.getWsdl();
            String polGuid = assertion.getNotificationPolicyGuid();
            if (polGuid == null || "".equals(polGuid))
                 result.addWarning(new PolicyValidatorResult.Warning(
                                    assertion,
                         "No notification policy has been specified. New or renewed subscriptions will not have an outbound policy",
                                    null));

            // check to see if it's an XML service, display appropriate warning
            if (( wsdl == null) || (!ESMSM.equals(wsdl.getTargetNamespace())) ) {
                result.addWarning(new PolicyValidatorResult.Warning(
                                   assertion,
                        "Assertion added to a policy not intended for a WSDM Subscription service",
                                   null));
            }
        }
    }
}
