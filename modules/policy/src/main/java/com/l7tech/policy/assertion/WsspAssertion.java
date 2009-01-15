package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.util.SyspropUtil;

/**
 * Assertion for WS-Security Policy compliance.
 *
 * <p>This is just a marker assertion, no runtime work is performed.</p>
 */
@RequiresSOAP()
public class WsspAssertion extends Assertion {
    public static final String PROP_ALLOW_OVERRIDE_GENERATED_XML = "com.l7tech.policy.wssp.allowOverrideGeneratedXml";
    private static final boolean ALLOW_OVERRIDE_GENERATED_XML = SyspropUtil.getBoolean(PROP_ALLOW_OVERRIDE_GENERATED_XML, false);

    private String basePolicyXml;
    private String inputPolicyXml;
    private String outputPolicyXml;

    public String getBasePolicyXml() {
        return basePolicyXml;
    }

    /**
     * @param basePolicyXml  new base policy xml, or null to allow WSDL proxy to attempt to generate some.
     */
    public void setBasePolicyXml(String basePolicyXml) {
        this.basePolicyXml = emptyToNull(basePolicyXml);
    }

    public String getInputPolicyXml() {
        return inputPolicyXml;
    }

    /**
     * @param inputPolicyXml new input policy xml, or null to allow WSDL proxy to attempt to generate some.
     */
    public void setInputPolicyXml(String inputPolicyXml) {
        this.inputPolicyXml = emptyToNull(inputPolicyXml);
    }

    public String getOutputPolicyXml() {
        return outputPolicyXml;
    }

    /**
     * @param outputPolicyXml new output policy xml, or null to allow WSDL proxy to attempt to generate some.
     */
    public void setOutputPolicyXml(String outputPolicyXml) {
        this.outputPolicyXml = emptyToNull(outputPolicyXml);
    }

    private String emptyToNull(String str) {
        return str == null || str.length() > 0 ? nullAsNull(str) : null;
    }

    private <T> T nullAsNull(T in) {
        return in == null || "null".equals(in.toString()) ? null : in;
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "WS-Security Policy Compliance");
        meta.put(AssertionMetadata.LONG_NAME, "WS-Security Policy Compliance");
        meta.put(AssertionMetadata.DESCRIPTION, "Restrict the policy to contain only assertions compatible with WS-SecurityPolicy");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/policy16.gif");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });

        if (ALLOW_OVERRIDE_GENERATED_XML) {
            meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.WsspAssertionPropertiesDialog");
        } else {
            meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);
        }

        return meta;
    }
}
