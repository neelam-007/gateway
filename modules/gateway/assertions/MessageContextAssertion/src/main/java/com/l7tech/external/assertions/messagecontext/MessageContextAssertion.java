package com.l7tech.external.assertions.messagecontext;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_EXTERNAL_NAME;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.ArrayTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

import java.util.*;
import java.util.logging.Logger;

/**
 * Identity a customer by message context mappings
 */
public class MessageContextAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(MessageContextAssertion.class.getName());
    private static final String META_INITIALIZED = MessageContextAssertion.class.getName() + ".metadataInitialized";

    private MessageContextMapping[] mappings = new MessageContextMapping[0];

    public MessageContextAssertion() {
        // Initalize the mapping array using two default mappings.
        mappings = new MessageContextMapping[] {
            MessageContextMapping.getDefaultIPAddressMapping(),
            MessageContextMapping.getDefaultAuthUserMapping(),
        };
    }

    public MessageContextMapping[] getMappings() {
        return mappings;
    }

    public void setMappings(MessageContextMapping[] mappings) {
        this.mappings = mappings;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        if (mappings.length == 0) return new String[0];
        List<String> variableList = new ArrayList<String>();
        for (MessageContextMapping mapping: mappings) {
            String value = mapping.getValue();
            String[] vars = Syntax.getReferencedNames(value);
            variableList.addAll(variableList.size(), Arrays.asList(vars));
        }
        return variableList.toArray(new String[variableList.size()]); 
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Message Context Assertion");
        meta.put(AssertionMetadata.LONG_NAME, "Message Context Assertion");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "audit" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/messageContextAssertion.png");

        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.messagecontext.server.ServerMessageContextAssertion");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.messagecontext.console.MessageContextAssertionPropertiesDialog");

        // Enable automatically poping up the assertion properties dialog
        //meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, MessageContextAssertion.Validator.class.getName());

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/messageContextAssertion.png");
        meta.put(POLICY_NODE_NAME, "Message Context Assertion");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:MessageContext" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(WSP_EXTERNAL_NAME, "MessageContextAssertion");
        meta.put(META_INITIALIZED, Boolean.TRUE);

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new BeanTypeMapping(MessageContextMapping.class, "mappingInfo"),
            new ArrayTypeMapping(new MessageContextMapping[0], "mappingInfoArray"),
            new Java5EnumTypeMapping(MessageContextMapping.MappingType.class, "mappingType")
        )));

        return meta;
    }

    public static class Validator implements AssertionValidator {
        private MessageContextAssertion assertion;

        public Validator(MessageContextAssertion assertion) {
            this.assertion = assertion;
        }

        public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
            // Get all MCAs from the current MCA to the last MCA.
            Assertion[] assertions = path.getPath();
            List<MessageContextAssertion> checkedAssertions = new ArrayList<MessageContextAssertion>();
            for (int i = assertions.length - 1; i >= 0; i--) {
                Assertion anAssertion = assertions[i];
                if (anAssertion.isEnabled() && anAssertion instanceof MessageContextAssertion) {
                    MessageContextAssertion mca = (MessageContextAssertion)anAssertion;
                    checkedAssertions.add(0, mca);
                    if (mca == this.assertion) {
                        break;
                    }
                }
            }

            // Add all mappings to be checked into the list, distinctMappings.
            List<MessageContextMapping> distinctMappings = new ArrayList<MessageContextMapping>();
            for (MessageContextAssertion mca: checkedAssertions) {
                distinctMappings.addAll(Arrays.asList(mca.getMappings()));
            }

            // Remove all overridden mappings, where each such mapping has the same mapping type and key as other mapping's.
            removeOverriddenMappings(distinctMappings);

            if (distinctMappings.size() > 5) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion, path,
                    "Some mappings in this assertion will be dropped due to too many distinct mappings in this policy.", null));
            }
        }

        private void removeOverriddenMappings(List<MessageContextMapping> mappings) {
            for (int i = mappings.size() - 1; i >= 0; i--) {
                for (int j = i - 1; j >= 0; j--) {
                    if (mappings.get(i).hasEqualTypeAndKeyExcludingValue(mappings.get(j))) {
                        mappings.remove(j);
                        i--;
                    }
                }
            }
        }
    }
}
