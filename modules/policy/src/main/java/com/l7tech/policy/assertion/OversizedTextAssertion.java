/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.annotation.HardwareAccelerated;
import com.l7tech.policy.assertion.annotation.RequiresXML;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.util.Functions;

import java.util.EnumSet;
import java.util.Set;

import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static com.l7tech.policy.assertion.annotation.HardwareAccelerated.Type.TOKENSCAN;

/**
 * Assertion that can limit length of attribute and text nodes.
 */
@RequiresXML
@HardwareAccelerated(type=TOKENSCAN)
public class OversizedTextAssertion extends MessageTargetableAssertion {
    public static final long DEFAULT_ATTR_LIMIT = 2048;
    public static final long DEFAULT_TEXT_LIMIT = 16384;
    public static final int DEFAULT_ATTR_NAME_LIMIT = 128;
    public static final int DEFAULT_NESTING_LIMIT = 32;
    public static final int DEFAULT_PAYLOAD_LIMIT = 0;     // Unlimited by default
    public static final int DEFAULT_NAMESPACE_DECLARATON_COUNT = 0; // unlimited
    public static final int DEFAULT_NAMESPACE_PREFIX_DECLARATION_COUNT = 0; // unlimited
    public static final int MIN_NESTING_LIMIT = 2;         // Constrain to prevent useless check
    public static final int MAX_NESTING_LIMIT = 10000;     // Constrain to prevent enormous value

    private static final String XPATH_TEXT_START = "(//*/text())[string-length() > ";
    private static final String XPATH_TEXT_END = "][1]";

    private static final String XPATH_NESTING_STEP = "/*";

    private static final String XPATH_PAYLOAD_START = " /*[local-name() = 'Envelope']/*[local-name() = \"Body\"]/*[";
    private static final String XPATH_PAYLOAD_END = "]";

    private boolean limitTextChars = true;
    private long maxTextChars = DEFAULT_TEXT_LIMIT;
    private boolean limitAttrChars = true;
    private long maxAttrChars = DEFAULT_ATTR_LIMIT;
    private boolean limitNestingDepth = true;
    private int maxNestingDepth = DEFAULT_NESTING_LIMIT;
    private int maxPayloadElements = DEFAULT_PAYLOAD_LIMIT;
    private boolean requireValidSoapEnvelope = false;
    private boolean limitAttrNameChars = true;
    private int maxAttrNameChars = DEFAULT_ATTR_NAME_LIMIT;

    private boolean limitNamespaceCount = false;
    private int maxNamespaceCount = DEFAULT_NAMESPACE_DECLARATON_COUNT;
    private boolean limitNamespacePrefixCount = false;
    private int maxNamespacePrefixCount = DEFAULT_NAMESPACE_PREFIX_DECLARATION_COUNT;

    public OversizedTextAssertion() {
        super(false);
    }

    public boolean isLimitTextChars() {
        return limitTextChars;
    }

    public void setLimitTextChars(boolean limitTextChars) {
        this.limitTextChars = limitTextChars;
    }

    public long getMaxTextChars() {
        return maxTextChars;
    }

    public void setMaxTextChars(long maxTextChars) {
        this.maxTextChars = maxTextChars;
    }

    public boolean isLimitAttrChars() {
        return limitAttrChars;
    }

    public void setLimitAttrChars(boolean limitAttrChars) {
        this.limitAttrChars = limitAttrChars;
    }

    public long getMaxAttrChars() {
        return maxAttrChars;
    }

    public void setMaxAttrChars(long maxAttrChars) {
        if (maxAttrChars < 0) maxAttrChars = 0;
        this.maxAttrChars = maxAttrChars;
    }

    public boolean isLimitNestingDepth() {
        return limitNestingDepth;
    }

    public void setLimitNestingDepth(boolean limitNestingDepth) {
        this.limitNestingDepth = limitNestingDepth;
    }

    public int getMaxNestingDepth() {
        return maxNestingDepth;
    }

    public void setMaxNestingDepth(int maxNestingDepth) {
        if (maxNestingDepth < 0) maxNestingDepth = 0;
        this.maxNestingDepth = maxNestingDepth;
    }

    public void setRequireValidSoapEnvelope(boolean req) {
        this.requireValidSoapEnvelope = req;
    }

    public boolean isRequireValidSoapEnvelope() {
        return requireValidSoapEnvelope;
    }

    public void setMaxPayloadElements(int max) {
        if (max < 0) max = 0;
        this.maxPayloadElements = max;
    }

    public int getMaxPayloadElements() {
        return maxPayloadElements;
    }

    public boolean isLimitAttrNameChars() {
        return limitAttrNameChars;
    }

    public void setLimitAttrNameChars(boolean limitAttrNameChars) {
        this.limitAttrNameChars = limitAttrNameChars;
    }

    public int getMaxAttrNameChars() {
        return maxAttrNameChars;
    }

    public void setMaxAttrNameChars(int maxAttrNameChars) {
        this.maxAttrNameChars = maxAttrNameChars;
    }

    public boolean isLimitNamespaceCount() {
        return limitNamespaceCount;
    }

    public void setLimitNamespaceCount(boolean limitNamespaceCount) {
        this.limitNamespaceCount = limitNamespaceCount;
    }

    public int getMaxNamespaceCount() {
        return maxNamespaceCount;
    }

    public void setMaxNamespaceCount(int maxNamespaceCount) {
        if (maxNamespaceCount < 0) maxNamespaceCount = 0;
        this.maxNamespaceCount = maxNamespaceCount;
    }

    public boolean isLimitNamespacePrefixCount() {
        return limitNamespacePrefixCount;
    }

    public void setLimitNamespacePrefixCount(boolean limitNamespacePrefixCount) {
        this.limitNamespacePrefixCount = limitNamespacePrefixCount;
    }

    public int getMaxNamespacePrefixCount() {
        return maxNamespacePrefixCount;
    }

    public void setMaxNamespacePrefixCount(int maxNamespacePrefixCount) {
        if (maxNamespacePrefixCount < 0) maxNamespacePrefixCount = 0;
        this.maxNamespacePrefixCount = maxNamespacePrefixCount;
    }

    /**
     * @return an XPath 1.0 expression that matches the first text node that exceeds the size limit, or null
     *         if limitTextChars is false;
     */
    public String makeTextXpath() {
        return isLimitTextChars() ? XPATH_TEXT_START + maxTextChars + XPATH_TEXT_END : null;
    }

    /**
     * @return a parallelizable Tarari normal form XPath that matches the first node whose nesting depth exceeds
     *         the configured limit, or null if limitNestingDepth is false.
     */
    public String makeNestingXpath() {
        if (!isLimitNestingDepth()) return null;
        int depth = getMaxNestingDepth();

        // last-ditch sanity checks
        if (depth < MIN_NESTING_LIMIT) depth = MIN_NESTING_LIMIT;
        if (depth > MAX_NESTING_LIMIT) depth = MAX_NESTING_LIMIT;

        // Allow depth, but disallow the depth+1'th nested element.
        depth++;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; ++i) {
            sb.append(XPATH_NESTING_STEP);
        }

        return sb.toString();
    }

    /**
     * @return a parallelizable Tarari normal form XPath that matches the first payload element beyond the
     *         configured maximum, or null if maxPayloadElements is zero.
     */
    public String makePayloadLimitXpath() {
        final int maxPayloads = getMaxPayloadElements();
        if (maxPayloads < 1) return null;

        return XPATH_PAYLOAD_START + (maxPayloads + 1) + XPATH_PAYLOAD_END;
    }

    private final static String baseName = "Protect Against XML Document Structure Threats";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<OversizedTextAssertion>(){
        @Override
        public String getAssertionName( final OversizedTextAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return AssertionUtils.decorateName(assertion, baseName);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Enable protection against oversized nodes, overdeep nesting, and trailers.");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/OversizedElement16.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "threatProtection" });
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.OversizedTextDialog");
        meta.put(PROPERTIES_ACTION_NAME, "XML Document Structure Threat Protection Properties");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, OversizedTextAssertion>(){
            @Override
            public Set<ValidatorFlag> call(OversizedTextAssertion assertion) {
                return EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION);
            }
        });
        meta.put(POLICY_VALIDATOR_CLASSNAME, OversizedTextAssertion.Validator.class.getName());
        return meta;
    }

    public static class Validator implements AssertionValidator {
        private final OversizedTextAssertion assertion;
        private final String warningStr;

        public Validator(final OversizedTextAssertion assertion) {
            this.assertion = assertion;
            if (assertion.isLimitTextChars() ||
                assertion.isLimitAttrChars() ||
                assertion.isLimitAttrNameChars() ||
                assertion.isLimitNestingDepth() ||
                assertion.isRequireValidSoapEnvelope() ||
                assertion.getMaxPayloadElements() > 0 ||
                assertion.isLimitNamespaceCount() ||
                assertion.isLimitNamespacePrefixCount() ) {
                warningStr = null;
            } else {
                warningStr = "No XML Document Structure Threat Protections have been specified";
            }
        }

        @Override
        public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
            if ( warningStr != null ) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion, warningStr, null));
            }
        }
    }
}
