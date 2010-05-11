package com.l7tech.external.assertions.messagecontext;

import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.*;

import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

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

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();

        meta.put(CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(SHORT_NAME, "Capture Identity of Requestor");
        meta.put(DESCRIPTION, "Record contextual information about the current request by capturing the IP address, authenticated user ID, or a context variable.");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(PALETTE_FOLDERS, new String[] { "audit" });
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/messageContextAssertion.png");

        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.messagecontext.console.MessageContextAssertionPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Requestor Identity Properties");
        // Enable automatically poping up the assertion properties dialog
        //meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_VALIDATOR_CLASSNAME, MessageContextAssertion.Validator.class.getName());

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:MessageContext" rather than "set:modularAssertions"
        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(WSP_EXTERNAL_NAME, "MessageContextAssertion");
        meta.put(META_INITIALIZED, Boolean.TRUE);

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new BeanTypeMapping(MessageContextMapping.class, "mappingInfo"),
            new ArrayTypeMapping(new MessageContextMapping[0], "mappingInfoArray"),
            new Java5EnumTypeMapping(MessageContextMapping.MappingType.class, "mappingType")
        )));

        meta.put(SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.messagecontext.server.ServerMessageContextAssertion");

        return meta;
    }

    public static class Validator implements AssertionValidator {
        private MessageContextAssertion assertion;

        public Validator(MessageContextAssertion assertion) {
            this.assertion = assertion;
        }

        @Override
        public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
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
