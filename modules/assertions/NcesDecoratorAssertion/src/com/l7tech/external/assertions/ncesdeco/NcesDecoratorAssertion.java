package com.l7tech.external.assertions.ncesdeco;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Decorates SOAP messages so that they are compliant with the US DOD's NCES requirements, by adding the following:
 * <ul>
 *   <li>A SAML AuthenticationStatement with the Sender-Vouches subject confirmation method;
 *   <li>A Timestamp;
 *   <li>A WS-Addressing MessageID element containing a UUID;
 *   <li>A Signature covering the above three elements as well as the Body.
 * </ul>
 *
 * Several options can be configured:
 * <ul>
 *   <li>The UUID in the MessageID can be specified as {@link #nodeBasedUuid node-based} (e.g. containing the SSG's MAC
 *       address and a timestamp) or purely random;
 *   <li>The UUID can have an optional prefix (MessageID values are supposed to be URIs, but NCES expects them to be
 *       plain UUIDs; setting a prefix allows the MessageID value to read something like
 *       <code>http://services.example.com/fooService/messageId/&lt;UUID&gt;<code> or <code>urn:uuid:&lt;UUID&gt;</code>
 *       instead of just <code>&lt;UUID&gt;</code>
 *   <li>The WS-Addressing namespace URI can be configured;
 *   <li>The reference from the Signature to the SAML assertion can be configured to use either the standards-compliant
 *       {@link #samlUseStrTransform STR Dereference Transform} or a direct, schema-forbidden wsu:Id reference.
 * </ul> 
 */
@RequiresSOAP
public class NcesDecoratorAssertion extends Assertion implements PrivateKeyable, UsesVariables {
    private static final String META_INITIALIZED = NcesDecoratorAssertion.class.getName() + ".metadataInitialized";

    private TargetMessageType target = TargetMessageType.REQUEST;
    private String otherTargetMessageVariable;
    private String messageIdUriPrefix;
    private int samlAssertionVersion;
    private String wsaNamespaceUri;
    private boolean samlIncluded = true;
    private boolean samlUseStrTransform;
    private String samlAssertionTemplate;
    private boolean nodeBasedUuid;
    private boolean usesDefaultKeystore = true;
    private long nonDefaultKeystoreId;
    private String keyAlias;

    public String getMessageIdUriPrefix() {
        return messageIdUriPrefix;
    }

    public void setMessageIdUriPrefix(String messageIdUriPrefix) {
        this.messageIdUriPrefix = messageIdUriPrefix;
    }

    public int getSamlAssertionVersion() {
        return samlAssertionVersion;
    }

    public void setSamlAssertionVersion(int samlAssertionVersion) {
        this.samlAssertionVersion = samlAssertionVersion;
    }

    public String getWsaNamespaceUri() {
        return wsaNamespaceUri;
    }

    public void setWsaNamespaceUri(String wsaNamespaceUri) {
        this.wsaNamespaceUri = wsaNamespaceUri;
    }

    public boolean isSamlUseStrTransform() {
        return samlUseStrTransform;
    }

    public void setSamlUseStrTransform(boolean samlUseStrTransform) {
        this.samlUseStrTransform = samlUseStrTransform;
    }

    public boolean isNodeBasedUuid() {
        return nodeBasedUuid;
    }

    public void setNodeBasedUuid(boolean nodeBasedUuid) {
        this.nodeBasedUuid = nodeBasedUuid;
    }

    /**
     * @deprecated use {@link #getTarget} instead TODO DELETE
     */
    public boolean isOperateOnResponse() {
        return target == TargetMessageType.RESPONSE;
    }

    /**
     * @deprecated use {@link #setTarget} instead TODO DELETE
     */
    public void setOperateOnResponse(boolean operateOnResponse) {
        this.target = operateOnResponse ? TargetMessageType.RESPONSE : TargetMessageType.REQUEST;
    }

    public TargetMessageType getTarget() {
        return target;
    }

    public void setTarget(TargetMessageType target) {
        this.target = target;
    }

    public String getOtherTargetMessageVariable() {
        return otherTargetMessageVariable;
    }

    public void setOtherTargetMessageVariable(String otherTargetMessageVariable) {
        this.otherTargetMessageVariable = otherTargetMessageVariable;
    }

    public String getSamlAssertionTemplate() {
        return samlAssertionTemplate;
    }

    public void setSamlAssertionTemplate(String samlAssertionTemplate) {
        this.samlAssertionTemplate = samlAssertionTemplate;
    }

    public String[] getVariablesUsed() {
        List<String> vars = new ArrayList<String>();
        if (samlIncluded && samlAssertionTemplate != null) vars.addAll(Arrays.asList(Syntax.getReferencedNames(samlAssertionTemplate)));
        if (otherTargetMessageVariable != null) vars.add(otherTargetMessageVariable);
        return vars.toArray(new String[0]);
    }

    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeystore;
    }

    public void setUsesDefaultKeyStore(boolean usesDefault) {
        this.usesDefaultKeystore = usesDefault;
    }

    public long getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    public void setNonDefaultKeystoreId(long nonDefaultId) {
        this.nonDefaultKeystoreId = nonDefaultId;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyid) {
        this.keyAlias = keyid;
    }

    public boolean isSamlIncluded() {
        return samlIncluded;
    }

    public void setSamlIncluded(boolean samlIncluded) {
        this.samlIncluded = samlIncluded;
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "NCES Decorator");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.ncesdeco.console.NcesDecoratorAssertionPropertiesDialog");
        meta.put(AssertionMetadata.POLICY_NODE_CLASSNAME, "com.l7tech.external.assertions.ncesdeco.console.NcesDecoratorPolicyNode");

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
        )));

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:NcesDecorator" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
