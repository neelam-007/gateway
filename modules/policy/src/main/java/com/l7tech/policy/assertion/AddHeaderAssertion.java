package com.l7tech.policy.assertion;

import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Add a header to a Message that is expected to have at least out HasOutboundHeaders knob.
 */
public class AddHeaderAssertion extends MessageTargetableAssertion implements UsesVariables {
    private Operation operation = Operation.ADD;
    private String headerName;
    private String headerValue;
    private boolean removeExisting;
    private boolean matchValueForRemoval;

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

    /**
     * Only applies to {@link Operation.ADD}
     *
     * @return true if adding a header should remove (overwrite) any existing value for the header or false otherwise.
     */
    public boolean isRemoveExisting() {
        return removeExisting;
    }

    /**
     * Only applies to {@link Operation.ADD}
     *
     * @param removeExisting set to true if adding a header should remove (overwrite) any existing value for the header or false otherwise.
     */
    public void setRemoveExisting(boolean removeExisting) {
        this.removeExisting = removeExisting;
    }

    /**
     * @return the header {@link Operation} to perform.
     */
    @NotNull
    public Operation getOperation() {
        return operation;
    }

    /**
     * @param operation the header {@link Operation} to perform.
     */
    public void setOperation(@NotNull final Operation operation) {
        this.operation = operation;
    }

    /**
     * Only applies to {@link Operation.REMOVE}
     *
     * @return true if the header value should match the configured value for header removal.
     */
    public boolean isMatchValueForRemoval() {
        return matchValueForRemoval;
    }

    /**
     * Only applies to {@link Operation.REMOVE}
     *
     * @param matchValueForRemoval set to true if the header value should match the configured value for header removal.
     */
    public void setMatchValueForRemoval(final boolean matchValueForRemoval) {
        this.matchValueForRemoval = matchValueForRemoval;
    }

    public static enum Operation {
        ADD, REMOVE;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(headerName, headerValue);
    }

    private static final String META_INITIALIZED = AddHeaderAssertion.class.getName() + ".metadataInitialized";
    private static final String baseName = "Add or Remove Header";

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
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Collections.<TypeMapping>singletonList(new Java5EnumTypeMapping(Operation.class, "operation"))));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
