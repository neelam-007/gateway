package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.policy.assertion.*;
import org.jetbrains.annotations.Nullable;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Enforces XML security on the message elements or the entire message.
 * <p/>
 * <code>ElementSecurity</code> list.
 * <p/>
 *
 * @author flascell
 */
public class WssSignElement extends XmlSecurityAssertionBase implements WssDecorationConfig, PrivateKeyable {

    //- PUBLIC

    public WssSignElement() {
        this(compatOrigDefaultXpathValue());
    }

    public WssSignElement(XpathExpression xpath) {
        super( TargetMessageType.RESPONSE, true );
        setXpathExpression(xpath);
    }

    @Override
    public String getKeyReference() {
        return keyReference;
    }

    @Override
    public void setKeyReference(String keyReference) {
        this.keyReference = keyReference;
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeyStore;
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefault) {
        this.usesDefaultKeyStore = usesDefault;
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
        return keyId;
    }

    @Override
    public void setKeyAlias(String keyid) {
        this.keyId = keyid;
    }

    @Override
    public boolean isProtectTokens() {
        return protectTokens;
    }

    @Override
    public void setProtectTokens(boolean protectTokens) {
        this.protectTokens = protectTokens;
    }

    @Override
    public boolean isUsingProtectTokens() {
        // Exposed via "Signed" checkbox in XPathBasedAssertion GUI
        return true;
    }

    @Nullable
    @Override
    public String getDigestAlgorithmName() {
        return digestAlgorithmName;
    }

    @Override
    public void setDigestAlgorithmName(@Nullable String digestAlgorithmName) {
        this.digestAlgorithmName = digestAlgorithmName;
    }

    final static String baseName = "Sign Element";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<WssSignElement>(){
        @Override
        public String getAssertionName( final WssSignElement assertion, final boolean decorate) {
            StringBuilder name = new StringBuilder(baseName + " ");
            if (assertion.getXpathExpression() == null) {
                name.append("[XPath expression not set]");
            } else {
                name.append(assertion.getXpathExpression().getExpression());
            }
            return (decorate) ? AssertionUtils.decorateName(assertion, name) : baseName;
        }
    };
    
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Sign one or more elements of the message.");
        meta.put(PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(PALETTE_NODE_SORT_PRIORITY, 80000);
        meta.put(PROPERTIES_ACTION_NAME, "Sign Element Properties");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.ASSERTION_FACTORY, new XpathBasedAssertionFactory<WssSignElement>(WssSignElement.class));
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientResponseWssIntegrity");
        meta.put(CLIENT_ASSERTION_TARGETS, new String[]{"response"});
        meta.put(USED_BY_CLIENT, Boolean.TRUE);
        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.WssSignElementValidator");

        return meta;
    }

    //- PRIVATE

    private String keyReference = KeyReference.BST.getName();
    private boolean protectTokens = false;
    private boolean usesDefaultKeyStore = true;
    private Goid nonDefaultKeystoreId;
    private String keyId;
    private @Nullable String digestAlgorithmName;
    
}
