package com.l7tech.external.assertions.addorremovecookie;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_SUBTYPE_FINDER;

public class AddOrRemoveCookieAssertion extends Assertion implements UsesVariables {

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

    @NotNull
    public Operation getOperation() {
        return operation;
    }

    public void setOperation(@NotNull final Operation operation) {
        this.operation = operation;
    }

    public String getName() {
        return name;
    }

    public void setName(@NotNull final String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(@NotNull final String value) {
        this.value = value;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(@Nullable final String domain) {
        this.domain = domain;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(@Nullable final String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }

    public String getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(@Nullable final String maxAge) {
        this.maxAge = maxAge;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(@Nullable final String comment) {
        this.comment = comment;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(final boolean secure) {
        this.secure = secure;
    }

    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(name, value, domain, cookiePath, maxAge, comment);
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;
        meta.put(AssertionMetadata.SHORT_NAME, BASE_NAME);
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"routing"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Collections.<TypeMapping>singletonList(new Java5EnumTypeMapping(Operation.class, "operation"))));
        meta.put(POLICY_NODE_NAME_FACTORY, NODE_NAME_FACTORY);
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private static final String META_INITIALIZED = AddOrRemoveCookieAssertion.class.getName() + ".metadataInitialized";

    private static final String BASE_NAME = "Add or Remove Cookie";

    private static final AssertionNodeNameFactory<AddOrRemoveCookieAssertion> NODE_NAME_FACTORY = new AssertionNodeNameFactory<AddOrRemoveCookieAssertion>() {
        @Override
        public String getAssertionName(final AddOrRemoveCookieAssertion assertion, final boolean decorate) {
            if (decorate) {
                final StringBuilder sb = new StringBuilder();
                sb.append(assertion.getOperation().getName());
                sb.append(" Cookie ");
                sb.append(assertion.getName());
                if (assertion.getOperation() == Operation.ADD) {
                    sb.append("=");
                    sb.append(assertion.getValue());
                }
                return sb.toString();
            } else {
                return BASE_NAME;
            }
        }
    };

    private Operation operation = Operation.ADD;

    private String name;

    private String value;

    private String domain;

    private String cookiePath;

    private int version = 1;

    private String maxAge;

    private String comment;

    private boolean secure;
}
