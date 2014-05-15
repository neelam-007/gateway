package com.l7tech.external.assertions.managecookie;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.NameValuePair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_SUBTYPE_FINDER;

public class ManageCookieAssertion extends MessageTargetableAssertion implements UsesVariables {
    public static final String NAME = "name";
    public static final String DOMAIN = "domain";
    public static final String PATH = "path";
    public static final String VALUE = "value";
    public static final String VERSION = "version";
    public static final String MAX_AGE = "max-age";
    public static final String COMMENT = "comment";
    public static final String SECURE = "secure";
    public static final String HTTP_ONLY = "httpOnly";

    public static enum Operation {
        ADD("Add"), REMOVE("Remove"), UPDATE("Update"), ADD_OR_REPLACE("Add or Replace");

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

    @NotNull
    public Map<String, CookieCriteria> getCookieCriteria() {
        return cookieCriteria;
    }

    public void setCookieCriteria(@NotNull final Map<String, CookieCriteria> cookieCriteria) {
        this.cookieCriteria.clear();
        this.cookieCriteria.putAll(cookieCriteria);
    }

    @NotNull
    public Map<String, CookieAttribute> getCookieAttributes() {
        return cookieAttributes;
    }

    public void setCookieAttributes(@NotNull final Map<String, CookieAttribute> cookieAttributes) {
        this.cookieAttributes.clear();
        this.cookieAttributes.putAll(cookieAttributes);
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        final List<String> varsUsed = new ArrayList<>();
        for (final CookieCriteria criteria : cookieCriteria.values()) {
            varsUsed.add(criteria.getValue());
        }
        for (final CookieAttribute attribute : cookieAttributes.values()) {
            varsUsed.add(attribute.getValue());
        }
        return super.doGetVariablesUsed().withExpressions(varsUsed);
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;
        meta.put(AssertionMetadata.SHORT_NAME, BASE_NAME);
        meta.put(AssertionMetadata.DESCRIPTION, "Add, replace, update or remove cookie(s) to/from a message.");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Cookie Properties");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"routing"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/cookie.png");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping(Operation.class, "operation"),
                new BeanTypeMapping(CookieAttribute.class, "cookieAttribute"),
                new BeanTypeMapping(CookieCriteria.class, "cookieCriteria")
        )));
        meta.put(POLICY_NODE_NAME_FACTORY, NODE_NAME_FACTORY);
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private static final String META_INITIALIZED = ManageCookieAssertion.class.getName() + ".metadataInitialized";

    private static final String BASE_NAME = "Manage Cookie";

    private static final AssertionNodeNameFactory<ManageCookieAssertion> NODE_NAME_FACTORY = new AssertionNodeNameFactory<ManageCookieAssertion>() {
        @Override
        public String getAssertionName(final ManageCookieAssertion assertion, final boolean decorate) {
            if (decorate) {
                final StringBuilder sb = new StringBuilder();
                sb.append(assertion.getOperation().getName());
                sb.append(" Cookie");
                if (assertion.getCookieAttributes().containsKey(NAME) && (assertion.getOperation() == Operation.ADD || assertion.getOperation() == Operation.ADD_OR_REPLACE)) {
                    sb.append(" " + assertion.getCookieAttributes().get(NAME).getValue());
                    if (assertion.getOperation() == Operation.ADD && assertion.getCookieAttributes().containsKey(VALUE)) {
                        sb.append("=");
                        sb.append(assertion.getCookieAttributes().get(VALUE).getValue());
                    }
                }
                return AssertionUtils.decorateName(assertion, sb);
            } else {
                return BASE_NAME;
            }
        }
    };

    private Operation operation = Operation.ADD;

    private final Map<String, CookieCriteria> cookieCriteria = new HashMap<>();

    private final Map<String, CookieAttribute> cookieAttributes = new HashMap<>();

    public static class CookieAttribute extends NameValuePair {
        private boolean useOriginalValue;

        public CookieAttribute() {
        }

        public CookieAttribute(final String name, final String value, final boolean useOriginalValue) {
            super(name, value);
            this.useOriginalValue = useOriginalValue;
        }

        public boolean isUseOriginalValue() {
            return useOriginalValue;
        }

        public void setUseOriginalValue(final boolean useOriginalValue) {
            this.useOriginalValue = useOriginalValue;
        }
    }

    public static class CookieCriteria extends NameValuePair {
        private boolean regex;

        public CookieCriteria() {
        }

        public CookieCriteria(final String name, final String value, final boolean regex) {
            super(name, value);
            this.regex = regex;
        }

        public boolean isRegex() {
            return regex;
        }

        public void setRegex(final boolean regex) {
            this.regex = regex;
        }
    }
}
