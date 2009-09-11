package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.policy.assertion.*;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.EntityHeader;

/**
 * Enforces XML security on the message elements or the entire message.
 * <p/>
 * <code>ElementSecurity</code> list.
 * <p/>
 *
 * @author flascell
 */
public class WssEncryptElement extends XmlSecurityAssertionBase implements RequestIdentityTargetable, UsesEntities {
    public WssEncryptElement() {
        this(XpathExpression.soapBodyXpathValue());
    }

    public WssEncryptElement(final XpathExpression xpath) {
        super(TargetMessageType.RESPONSE);
        setXpathExpression(xpath);
    }

    /**
     * Get the requested xml encrytpion algorithm. Defaults to http://www.w3.org/2001/04/xmlenc#aes128-cbc
     * @return the encrytion algorithm requested
     */
    public String getXEncAlgorithm() {
        return xencAlgorithm;
    }

    /**
     * Set the xml encryption algorithm.
     * @param xencAlgorithm
     */
    public void setXEncAlgorithm(String xencAlgorithm) {
        if (xencAlgorithm == null) {
            throw new IllegalArgumentException();
        }
        this.xencAlgorithm = xencAlgorithm;
    }

    public String getKeyEncryptionAlgorithm() {
        return xencKeyAlgorithm;
    }

    public void setKeyEncryptionAlgorithm(String keyEncryptionAlgorithm) {
        this.xencKeyAlgorithm = keyEncryptionAlgorithm;
    }

    @Override
    public IdentityTarget getIdentityTarget() {
        return identityTarget;
    }

    @Override
    public void setIdentityTarget( final IdentityTarget identityTarget ) {
        this.identityTarget = identityTarget;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.USERGROUP)
    public EntityHeader[] getEntitiesUsed() {
        return identityTarget != null ?
                identityTarget.getEntitiesUsed():
                new EntityHeader[0];
    }

    @Override
    public void replaceEntity( final EntityHeader oldEntityHeader,
                               final EntityHeader newEntityHeader ) {
        if ( identityTarget != null ) {
            identityTarget.replaceEntity(oldEntityHeader, newEntityHeader);
        }
    }

    final static String baseName = "Encrypt Element";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<WssEncryptElement>(){
        @Override
        public String getAssertionName( final WssEncryptElement assertion, final boolean decorate) {
            StringBuilder name = new StringBuilder(baseName + " ");
            if (assertion.getXpathExpression() == null) {
                name.append("[XPath expression not set]");
            } else {
                name.append(assertion.getXpathExpression().getExpression());
            }
            return (decorate)? AssertionUtils.decorateName(assertion, name): baseName;
        }
    };
    
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Encrypt one or more elements of the message.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, 70000);
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Encrypt Element Properties");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientResponseWssConfidentiality");
        meta.put(AssertionMetadata.CLIENT_ASSERTION_TARGETS, new String[]{"response"} );
        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/xmlencryption.gif");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.WssEncryptElementValidator");

        return meta;
    }

    private IdentityTarget identityTarget;
    private String xencAlgorithm = XencUtil.AES_128_CBC;
    private String xencKeyAlgorithm = null;
}
