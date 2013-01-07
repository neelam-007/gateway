/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

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
public class CodeInjectionProtectionAssertion extends MessageTargetableAssertion {

    /** Whether to apply protections to request URL. */
    private boolean _includeRequestUrl;

    /** Whether to apply protections to request body. */
    private boolean _includeRequestBody;

    /** Whether to apply protections to response body. */
    private boolean _includeResponseBody;

    /** Protection types to apply. Replaces previous _protection since 4.3. */
    private CodeInjectionProtectionType[] _protections = new CodeInjectionProtectionType[0];

    public CodeInjectionProtectionAssertion() {
        super(null, false);
    }

    @Override
    public Object clone() {
        final CodeInjectionProtectionAssertion clone = (CodeInjectionProtectionAssertion) super.clone();
        clone._protections = _protections.clone();
        return clone;
    }

    public boolean isIncludeRequestUrl() {
        return _includeRequestUrl;
    }

    public void setIncludeRequestUrl(boolean b) {
        _includeRequestUrl = b;
    }

    public boolean isIncludeRequestBody() {
        return _includeRequestBody;
    }

    public void setIncludeRequestBody(boolean b) {
        _includeRequestBody = b;
    }

    /**
     * @deprecated This assertion is now MessageTargetable
     */
    @Deprecated
    public boolean isIncludeResponseBody() {
        return _includeResponseBody;
    }

    /**
     * @deprecated This assertion is now MessageTargetable
     */
    @Deprecated
    public void setIncludeResponseBody(boolean b) {
        _includeResponseBody = b;
    }

    public void setProtection(final CodeInjectionProtectionType protection) {
        _protections = new CodeInjectionProtectionType[]{protection};
    }

    /**
     * @return protection types to apply; never null
     */
    public CodeInjectionProtectionType[] getProtections() {
        return _protections;
    }

    /**
     * @param protections   protection types to apply; must not be <code>null</code>
     * @throws IllegalArgumentException if <code>protections</code> is <code>null</code>
     */
    public void setProtections(final CodeInjectionProtectionType[] protections) {
        if (protections == null)
            throw new IllegalArgumentException("protections array must not be null");
        _protections = protections;
    }

    @Override
    public TargetMessageType getTarget() {
        TargetMessageType target = super.getTarget();

        // If not configured then use the values that were available before
        // this assertion was message targetable.
        if ( target == null ) {
            target = _includeResponseBody ?
                    TargetMessageType.RESPONSE :
                    TargetMessageType.REQUEST;
        }
        return target;
    }

    @Override
    public void setTarget( final TargetMessageType target ) {
        super.setTarget(target);
        if ( target == TargetMessageType.REQUEST &&
             !( _includeRequestUrl || _includeRequestBody ) ) {
            _includeRequestUrl = true;
            _includeRequestBody = true;       
        }
    }

    private final static String baseName = "Protect Against Code Injection";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<CodeInjectionProtectionAssertion>(){
        @Override
        public String getAssertionName( final CodeInjectionProtectionAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return AssertionUtils.decorateName(assertion, baseName);
        }
    };

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
        meta.put(POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, CodeInjectionProtectionAssertion>(){
            @Override
            public Set<ValidatorFlag> call(CodeInjectionProtectionAssertion assertion) {
                return EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION);
            }
        });
        return meta;
    }
}
