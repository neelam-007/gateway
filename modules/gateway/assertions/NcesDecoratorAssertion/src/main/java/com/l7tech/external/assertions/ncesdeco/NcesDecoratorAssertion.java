package com.l7tech.external.assertions.ncesdeco;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.GoidUpgradeMapper;

import java.util.Arrays;

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
public class NcesDecoratorAssertion
    extends MessageTargetableAssertion
    implements PrivateKeyable, UsesVariables
{
    private static final String META_INITIALIZED = NcesDecoratorAssertion.class.getName() + ".metadataInitialized";

    private String messageIdUriPrefix;
    private int samlAssertionVersion;
    private String wsaNamespaceUri;
    private boolean samlIncluded = true;
    private boolean samlUseStrTransform;
    private String samlAssertionTemplate;
    private boolean nodeBasedUuid;
    private boolean usesDefaultKeystore = true;
    private Goid nonDefaultKeystoreId;
    private String keyAlias;
    private boolean deferDecoration = false;
    private boolean useExistingWsa = false;

    public NcesDecoratorAssertion() {
        super(true);
    }

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
    @Deprecated
    public boolean isOperateOnResponse() {
        return getTarget() == TargetMessageType.RESPONSE;
    }

    /**
     * @deprecated use {@link #setTarget} instead TODO DELETE
     */
    @Deprecated
    public void setOperateOnResponse(boolean operateOnResponse) {
        setTarget(operateOnResponse ? TargetMessageType.RESPONSE : TargetMessageType.REQUEST);
    }

    public String getSamlAssertionTemplate() {
        return samlAssertionTemplate;
    }

    public void setSamlAssertionTemplate(String samlAssertionTemplate) {
        this.samlAssertionTemplate = samlAssertionTemplate;
    }

    public boolean isUseExistingWsa() {
        return useExistingWsa;
    }

    public void setUseExistingWsa(boolean useExistingWsa) {
        this.useExistingWsa = useExistingWsa;
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeystore;
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefault) {
        this.usesDefaultKeystore = usesDefault;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SSGKEY)
    public Goid getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    @Override
    public void setNonDefaultKeystoreId(Goid nonDefaultId) {
        this.nonDefaultKeystoreId = nonDefaultId;
    }

    @Deprecated
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        this.nonDefaultKeystoreId = GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, nonDefaultId);
    }

    @Override
    public String getKeyAlias() {
        return keyAlias;
    }

    @Override
    public void setKeyAlias(String keyid) {
        this.keyAlias = keyid;
    }

    public boolean isSamlIncluded() {
        return samlIncluded;
    }

    public void setSamlIncluded(boolean samlIncluded) {
        this.samlIncluded = samlIncluded;
    }

    /**
     * @return false if decoration should always be applied immediately when the server assertion runs;
     *     true if decoration should be deferred (if applicable) to allow additional decoration requirements to be accumulated.
     *     <p/>
     *     Currently this is only meaningful when decorating the Response message.
     */
    public boolean isDeferDecoration() {
        return deferDecoration;
    }

    /**
     * @param deferDecoration  false if decoration should always be applied immediately when the server assertion runs;
     *     true if decoration should be deferred (if applicable) to allow additional decoration requirements to be accumulated.
     *     <p/>
     *     Currently this is only meaningful when decorating the Response message.
     */
    public void setDeferDecoration(boolean deferDecoration) {
        this.deferDecoration = deferDecoration;
    }

    final static String baseName = "Apply NCES Elements to Message";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<NcesDecoratorAssertion>(){
        @Override
        public String getAssertionName( final NcesDecoratorAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return AssertionUtils.decorateName(assertion, baseName);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        final String baseName = "Apply NCES Elements to Message";
        meta.put(AssertionMetadata.SHORT_NAME, baseName);

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.ncesdeco.console.NcesDecoratorAssertionPropertiesDialog");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "NCES Elements Properties");
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
        )));

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:NcesDecorator" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions( samlIncluded ? samlAssertionTemplate : null );
    }
}
