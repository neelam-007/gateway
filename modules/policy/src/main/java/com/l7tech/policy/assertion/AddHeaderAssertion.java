package com.l7tech.policy.assertion;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Add a header to a Message that is expected to have at least out HasOutboundHeaders knob.
 */
public class AddHeaderAssertion extends MessageTargetableAssertion implements UsesVariables {
    private String headerName;
    private String headerValue;
    private boolean removeExisting;

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderValue() {
        return headerValue;
    }

    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }

    public boolean isRemoveExisting() {
        return removeExisting;
    }

    public void setRemoveExisting(boolean removeExisting) {
        this.removeExisting = removeExisting;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(headerName, headerValue);
    }

    private static final String META_INITIALIZED = AddHeaderAssertion.class.getName() + ".metadataInitialized";
    private static final String baseName = "Add Header";

    private static final AssertionNodeNameFactory<AddHeaderAssertion> nodeNameFactory = new AssertionNodeNameFactory<AddHeaderAssertion>() {
        @Override
        public String getAssertionName(AddHeaderAssertion assertion, boolean decorate) {
            if(!decorate) return baseName;

            StringBuilder sb = new StringBuilder(baseName);
            sb.append(" ").append(assertion.getHeaderName()).append(": ").append(assertion.getHeaderValue());
            if (assertion.isRemoveExisting()) sb.append(" (replace existing)");
            return AssertionUtils.decorateName( assertion, sb );
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, baseName);
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.AddHeaderAssertionDialog");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(POLICY_NODE_NAME_FACTORY, nodeNameFactory);
        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.AddHeaderAssertionValidator");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
