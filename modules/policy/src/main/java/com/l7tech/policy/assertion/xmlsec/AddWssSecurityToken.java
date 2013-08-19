package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.wsp.WspSensitive;
import com.l7tech.search.Dependency;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.util.GoidUpgradeMapper;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Creates a Security Token element and adds it to the SOAP security header in the target message.
 *
 * @author alex
 */
@RequiresSOAP
public class AddWssSecurityToken extends MessageTargetableAssertion implements WssDecorationConfig, PrivateKeyable, SecurityHeaderAddressable, RequestIdentityTargetable, UsesVariables, UsesEntities {
    public static final SecurityTokenType[] SUPPORTED_TOKEN_TYPES = new SecurityTokenType[] {
            SecurityTokenType.WSS_USERNAME,
            SecurityTokenType.WSSC_CONTEXT,
            SecurityTokenType.SAML_ASSERTION,
            SecurityTokenType.WSS_ENCRYPTEDKEY
    };

    private String keyReference = KeyReference.BST.getName();
    private boolean protectTokens = true;
    private SecurityTokenType tokenType = SecurityTokenType.WSS_USERNAME;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private IdentityTarget identityTarget;
    private boolean includePassword;
    private final PrivateKeyableSupport privatekeyableSupport = new PrivateKeyableSupport();
    private String digestAlgorithmName;
    private boolean useLastGatheredCredentials = true;
    private boolean includeNonce = true;
    private boolean includeCreated = true;
    private boolean digest = false;
    private boolean encrypt = false;
    private String username;
    private String password;
    private String wsscSessionVariable;
    private boolean omitSecurityContextToken = false;
    private String samlAssertionVariable;

    public AddWssSecurityToken() {
        super(TargetMessageType.RESPONSE, true);
    }

    public SecurityTokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(SecurityTokenType tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    @Override
    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        if (recipientContext == null) recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
        this.recipientContext = recipientContext;
    }

    @Override
    public String getKeyReference() {
        return keyReference;
    }

    @Override
    public void setKeyReference(String keyReference) {
        this.keyReference = keyReference;
    }

    public boolean isIncludePassword() {
        return includePassword;
    }

    public void setIncludePassword(boolean includePassword) {
        this.includePassword = includePassword;
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
        return true;
    }

    @Override
    public String getDigestAlgorithmName() {
        return this.digestAlgorithmName;
    }

    @Override
    public void setDigestAlgorithmName(String digestAlgorithmName) {
        this.digestAlgorithmName = digestAlgorithmName;
    }

    @Override
    public String getKeyAlias() {
        return privatekeyableSupport.getKeyAlias();
    }

    @Override
    public void setKeyAlias(String keyAlias) {
        privatekeyableSupport.setKeyAlias(keyAlias);
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SSGKEY)
    public Goid getNonDefaultKeystoreId() {
        return privatekeyableSupport.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId(Goid nonDefaultKeystoreId) {
        privatekeyableSupport.setNonDefaultKeystoreId(nonDefaultKeystoreId);
    }

    @Deprecated
    public void setNonDefaultKeystoreId(long nonDefaultKeystoreId) {
        privatekeyableSupport.setNonDefaultKeystoreId(GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, nonDefaultKeystoreId));
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return privatekeyableSupport.isUsesDefaultKeyStore();
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefaultKeyStore) {
        privatekeyableSupport.setUsesDefaultKeyStore(usesDefaultKeyStore);
    }

    @Override
    public IdentityTarget getIdentityTarget() {
        return identityTarget;
    }

    @Override
    public void setIdentityTarget(IdentityTarget identityTarget) {
        this.identityTarget = identityTarget;
    }

    public boolean isIncludeNonce() {
        return includeNonce;
    }

    public void setIncludeNonce(boolean includeNonce) {
        this.includeNonce = includeNonce;
    }

    public boolean isIncludeCreated() {
        return includeCreated;
    }

    public void setIncludeCreated(boolean includeCreated) {
        this.includeCreated = includeCreated;
    }

    public boolean isDigest() {
        return digest;
    }

    public void setDigest(boolean digest) {
        this.digest = digest;
    }

    public boolean isEncrypt() {
        return encrypt;
    }

    public void setEncrypt(boolean encrypt) {
        this.encrypt = encrypt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @WspSensitive
    @Dependency(methodReturnType = Dependency.MethodReturnType.VARIABLE, type = Dependency.DependencyType.SECURE_PASSWORD)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseLastGatheredCredentials() {
        return useLastGatheredCredentials;
    }

    public void setUseLastGatheredCredentials(boolean useLastGatheredCredentials) {
        this.useLastGatheredCredentials = useLastGatheredCredentials;
    }

    public String getWsscSessionVariable() {
        return wsscSessionVariable;
    }

    public void setWsscSessionVariable(String wsscSessionVariable) {
        this.wsscSessionVariable = wsscSessionVariable;
    }

    public boolean isOmitSecurityContextToken() {
        return omitSecurityContextToken;
    }

    public void setOmitSecurityContextToken(boolean omitSecurityContextToken) {
        this.omitSecurityContextToken = omitSecurityContextToken;
    }

    public String getSamlAssertionVariable() {
        return samlAssertionVariable;
    }

    public void setSamlAssertionVariable(String samlAssertionVariable) {
        this.samlAssertionVariable = samlAssertionVariable;
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

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed()
                .withExpressions( username, password )
                .withVariables( wsscSessionVariable, samlAssertionVariable );
    }

    final static String baseName = "Add Security Token";
    final static String baseNameSigned = "Add Signed Security Token";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<AddWssSecurityToken>(){
        @Override
        public String getAssertionName( final AddWssSecurityToken assertion, final boolean decorate) {
            final String base = assertion.isProtectTokens() ? baseNameSigned : baseName;
            return (decorate) ? AssertionUtils.decorateName(assertion, base) + " (" + assertion.getTokenType().getName() + ")" : base;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        meta.put(PALETTE_NODE_NAME, baseName);
        meta.put(DESCRIPTION, "Add a WS-Security security token to the message.  This can be a UsernameToken, a SAML Assertion, an EncryptedKey, " +
                "or a WS-SecureConversation session.<p/>The token will be added to the target message when the security requirements are next applied to it.");
        meta.put(PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(PALETTE_NODE_SORT_PRIORITY, 60000);
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.AddWssSecurityTokenDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Security Token Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/About16.gif");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.WssDecorationAssertionValidator");

        return meta;
    }
}
