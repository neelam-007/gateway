/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.policy.validator.InjectionThreatProtectionAssertionValidator;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.util.Functions;

import java.util.EnumSet;
import java.util.Set;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Provides threat protection against code injection attacks targeting web
 * applications.
 *
 * <h2>Change History</h2>
 * Before 4.3, only one protection type allowed. Since 4.3, one or more protection types allowed.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class CodeInjectionProtectionAssertion extends InjectionThreatProtectionAssertion {
    private final static String baseName = "Protect Against Code Injection";

    /** DEPRECATED: Whether to apply protections to request body. */
    private boolean includeRequestBody = false;

    /** DEPRECATED: Whether to apply protections to response body. */
    private boolean includeResponseBody = false;

    /** Protection types to apply. Replaces previous _protection since 4.3. */
    private CodeInjectionProtectionType[] protections = new CodeInjectionProtectionType[0];

    public CodeInjectionProtectionAssertion() {
        super(null, false);
    }

    @Override
    public Object clone() {
        final CodeInjectionProtectionAssertion clone = (CodeInjectionProtectionAssertion) super.clone();
        clone.protections = protections.clone();
        return clone;
    }

    /**
     * @deprecated this method is only here for deserialization purposes and should not be called directly.
     */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setIncludeRequestUrl(boolean includeUrlQueryString) {
        this.includeUrlQueryString = includeUrlQueryString;

        updateState();
    }

    /**
     * @deprecated this method is only here for deserialization purposes and should not be called directly.
     */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setIncludeRequestBody(boolean includeRequestBody) {
        this.includeRequestBody = includeRequestBody;

        updateState();
    }

    /**
     * @deprecated This assertion is now MessageTargetable
     */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setIncludeResponseBody(boolean includeResponseBody) {
        this.includeResponseBody = includeResponseBody;

        updateState();
    }

    /**
     * @deprecated this method is only here for deserialization purposes and should not be called directly.
     */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setProtection(final CodeInjectionProtectionType protection) {
        protections = new CodeInjectionProtectionType[]{protection};
    }

    /**
     * @return protection types to apply; never null
     */
    public CodeInjectionProtectionType[] getProtections() {
        return protections;
    }

    /**
     * @param protections   protection types to apply; must not be <code>null</code>
     * @throws IllegalArgumentException if <code>protections</code> is <code>null</code>
     */
    public void setProtections(final CodeInjectionProtectionType[] protections) {
        if (protections == null)
            throw new IllegalArgumentException("protections array must not be null");
        this.protections = protections;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Provides basic threat protection against attacks on web applications by blocking malicious code injection.");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/RedYellowShield16.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "threatProtection" });
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.CodeInjectionProtectionAssertionDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Code Injection Protection Properties");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(POLICY_VALIDATOR_CLASSNAME, CodeInjectionProtectionAssertionValidator.class.getName());
        meta.put(POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, CodeInjectionProtectionAssertion>() {
            @Override
            public Set<ValidatorFlag> call(CodeInjectionProtectionAssertion assertion) {
                return EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION);
            }
        });

        return meta;
    }

    @Override
    protected String getBaseName() {
        return baseName;
    }

    /**
     * Updates the state of the assertion based on the values of any deprecated variables that might have been set.
     */
    private void updateState() {
        if (includeResponseBody) {
            if (null == getTarget()) {
                setTarget(TargetMessageType.RESPONSE);
            }
        } else if (includeRequestBody || includeUrlQueryString) {
            if (null == getTarget()) {
                setTarget(TargetMessageType.REQUEST);
            }
        }

        includeBody = includeRequestBody || includeResponseBody;
    }

    public static class CodeInjectionProtectionAssertionValidator extends InjectionThreatProtectionAssertionValidator {
        private final CodeInjectionProtectionAssertion assertion;

        public CodeInjectionProtectionAssertionValidator(CodeInjectionProtectionAssertion assertion) {
            super(assertion);

            this.assertion = assertion;
        }

        @Override
        protected boolean isAtLeastOneProtectionEnabled() {
            return assertion.getProtections().length > 0;
        }
    }
}
