package com.l7tech.policy.assertion;

import com.l7tech.message.HeadersKnob;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Add a header to a Message that is expected to have at least out HasOutboundHeaders knob.
 */
public class AddHeaderAssertion extends MessageTargetableAssertion implements UsesVariables {
    private Operation operation = Operation.ADD;
    private String headerName;
    private String headerValue;
    private String metadataType = HeadersKnob.HEADER_TYPE_HTTP;
    private boolean removeExisting;
    private boolean evaluateNameAsExpression;
    private boolean evaluateValueExpression;

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

    public String getMetadataType() {
        return metadataType;
    }

    public void setMetadataType(String metadataType) {
        this.metadataType = metadataType;
    }

    /**
     * Only applies to Add.
     *
     * @return true if adding a header should remove (overwrite) any existing value for the header or false otherwise.
     */
    public boolean isRemoveExisting() {
        return removeExisting;
    }

    /**
     * Only applies to Add.
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
     * Only applies to Remove.
     *
     * @return true if the header name should be treated as a regular expression.
     */
    public boolean isEvaluateNameAsExpression() {
        return evaluateNameAsExpression;
    }

    /**
     * Only applies to Remove.
     *
     * @param evaluateNameAsExpression set to true if the header name should be treated as a regular expression.
     */
    public void setEvaluateNameAsExpression(final boolean evaluateNameAsExpression) {
        this.evaluateNameAsExpression = evaluateNameAsExpression;
    }

    /**
     * Only applies to Remove.
     *
     * @return true if the header value should be treated as a regular expression.
     */
    public boolean isEvaluateValueExpression() {
        return evaluateValueExpression;
    }

    /**
     * Only applies to Remove.
     *
     * @param evaluateValueExpression set to true if the header value should be treated as a regular expression.
     */
    public void setEvaluateValueExpression(final boolean evaluateValueExpression) {
        this.evaluateValueExpression = evaluateValueExpression;
    }

    public static enum Operation {
        ADD("Add"), REMOVE("Remove");

        private Operation(@NotNull final String name) {
            this.name = name;
        }

        @NotNull
        public String getName() {
            return name;
        }

        private final String name;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(headerName, headerValue);
    }

    private static final String META_INITIALIZED = AddHeaderAssertion.class.getName() + ".metadataInitialized";
    private static final String baseName = "Manage Transport Properties/Headers";

    private static final AssertionNodeNameFactory<AddHeaderAssertion> nodeNameFactory = new AssertionNodeNameFactory<AddHeaderAssertion>() {
        @Override
        public String getAssertionName(final AddHeaderAssertion assertion, final boolean decorate) {
            if (!decorate) return baseName;

            StringBuilder sb = new StringBuilder(assertion.getOperation().getName());
            sb.append(" ").append(assertion.getMetadataType());

            if (assertion.getOperation() == Operation.REMOVE) {
                // possible to remove more than one header
                sb.append("(s)");
            }

            sb.append(" ").append(assertion.getHeaderName());

            if (assertion.getOperation() == Operation.ADD || (assertion.getOperation() == Operation.REMOVE && StringUtils.isNotBlank(assertion.getHeaderValue()))) {
                sb.append(":").append(assertion.getHeaderValue());
            }

            if (assertion.getOperation() == Operation.ADD && assertion.isRemoveExisting())
                sb.append(" (replace existing)");

            return AssertionUtils.decorateName(assertion, sb);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Add, replace or remove header(s).");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(PALETTE_FOLDERS, new String[] {"routing"});
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.AddHeaderAssertionDialog");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(POLICY_NODE_NAME_FACTORY, nodeNameFactory);
        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.AddHeaderAssertionValidator");
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Collections.<TypeMapping>singletonList(new Java5EnumTypeMapping(Operation.class, "operation"))));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
