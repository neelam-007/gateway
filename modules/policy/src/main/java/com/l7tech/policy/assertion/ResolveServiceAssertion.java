package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * An assertion intended to run pre-service-resolution that can resolve the service with alternate parameters.
 */
public class ResolveServiceAssertion extends Assertion implements UsesVariables, SetsVariables {
    public static final String DEFAULT_VARIABLE_PREFIX = "resolvedService";
    public static final String NAME_SUFFIX = "name";
    public static final String OID_SUFFIX = "oid";
    public static final String SOAP_SUFFIX = "soap";

    private String uri;
    private String prefix = DEFAULT_VARIABLE_PREFIX;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String variable) {
        this.prefix = variable;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return VariableUseSupport.expressions(uri).asArray();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                new VariableMetadata(prefix + "." + NAME_SUFFIX, true, false,
                        prefix + "." + NAME_SUFFIX, false, DataType.STRING),
                new VariableMetadata(prefix + "." + OID_SUFFIX, true, false,
                        prefix + "." + OID_SUFFIX, false, DataType.STRING),
                new VariableMetadata(prefix + "." + SOAP_SUFFIX, true, false,
                        prefix + "." + SOAP_SUFFIX, false, DataType.BOOLEAN)
        };
    }

    private static final String shortName = "Resolve Service";
    private static final String META_INIT = ResolveServiceAssertion.class.getName() + ".metaInit";

    private final AssertionNodeNameFactory<ResolveServiceAssertion> nodeNameFactory = new AssertionNodeNameFactory<ResolveServiceAssertion>() {
        @Override
        public String getAssertionName(ResolveServiceAssertion assertion, boolean decorate) {
            if(!decorate) return shortName;

            StringBuilder sb = new StringBuilder(shortName);
            sb.append(" with URI ").append(assertion.getUri());
            return AssertionUtils.decorateName( assertion, sb );
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INIT)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, shortName);
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/services16.png");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, nodeNameFactory);
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.ResolveServiceAssertionPropertiesDialog");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(META_INIT, Boolean.TRUE);
        return meta;
    }
}
