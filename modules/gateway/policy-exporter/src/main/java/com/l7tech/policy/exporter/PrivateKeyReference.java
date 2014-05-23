package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.entity.EntitiesResolver;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.security.SignerServices;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.InvalidDocumentFormatException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ghuang
 */
public class PrivateKeyReference extends ExternalReference {
    private final Logger logger = Logger.getLogger(TrustedCertReference.class.getName());

    public static final String REF_EL_NAME = "PrivateKeyReference";
    public static final String KEYSTORE_OID_EL_NAME = "KeystoreOID";
    public static final String KEY_ALIAS_EL_NAME = "KeyAlias";
    public static final String IS_DEFAULT_KEY_EL_NAME = "IsDefaultKey";

    private String keyAlias;
    private Goid keystoreGoid;
    private Long keystoreOid;
    private boolean isDefaultKey;

    private String localKeyAlias;
    private Goid localKeystoreGoid;
    private boolean localIsDefaultKey;

    private LocalizeAction localizeType;

    public PrivateKeyReference( final ExternalReferenceFinder finder ) {
        super( finder );
    }

    public PrivateKeyReference( final ExternalReferenceFinder finder,
                                final PrivateKeyable keyable ) {
        this( finder );
        if (keyable != null) {
            if (keyable.isUsesDefaultKeyStore()) {
                isDefaultKey = true;
            } else {
                isDefaultKey = false;
                keystoreGoid = keyable.getNonDefaultKeystoreId();
                keyAlias = keyable.getKeyAlias();
            }
        }
        localizeType = LocalizeAction.IGNORE;
    }

    /**
     * Creates <code>PrivateKeyReference</code>.
     *
     * @param finder the external reference finder
     * @param isDefaultKey true if using default key, false otherwise
     * @param keystoreGoid the keystore GOID. Ignored if isDefaultKey is true.
     * @param keyAlias the key alias. Ignored if isDefaultKey is true.
     */
    public PrivateKeyReference( final ExternalReferenceFinder finder,
                                final boolean isDefaultKey,
                                final Goid keystoreGoid,
                                final String keyAlias) {
        this(finder);
        if (isDefaultKey) {
            this.isDefaultKey = true;
        } else {
            if (keystoreGoid == null) {
                throw new IllegalArgumentException("The keystore GOID cannot be null.");
            }
            if (keyAlias == null || keyAlias.length() == 0) {
                throw new IllegalArgumentException("The key alias cannot be null or empty.");
            }

            this.isDefaultKey = false;
            this.keystoreGoid = keystoreGoid;
            this.keyAlias = keyAlias;
        }
        localizeType = LocalizeAction.IGNORE;
    }

    @Override
    public String getRefId() {
        String id = null;

        if(keystoreOid != null && keystoreOid > 0 && keyAlias!=null) {
            id = keystoreOid + ":" + keyAlias;
        }else if (GoidRange.ZEROED_PREFIX.isInRange(keystoreGoid) && keystoreGoid.getLow() > 0 && keyAlias!=null ) {
            id = keystoreGoid + ":" + keyAlias;
        }

        return id;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public Goid getKeystoreGoid() {
        return keystoreGoid;
    }

    public boolean isDefaultKey() {
        return isDefaultKey;
    }

    @Override
    public boolean setLocalizeReplace( final String identifier ) {
        boolean ok = false;

        if ( identifier == null ) {
            setLocalizeReplace( true, null, PersistentEntity.DEFAULT_GOID);
            ok = true;
        } else {
            String[] keystoreAndAlias = identifier.split( ":", 2 );
            if ( keystoreAndAlias.length==2 ) {
                try {
                    final Goid keystoreGoid = GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, keystoreAndAlias[0]);
                    final String alias = keystoreAndAlias[1].trim();

                    if ( !alias.isEmpty() ) {
                        setLocalizeReplace( false, alias, keystoreGoid );
                        ok = true;
                    }
                } catch ( IllegalArgumentException iae ) {
                    // not ok
                }
            }
        }
        return ok;
    }

    @Override
    public boolean setLocalizeDelete() {
        localizeType = LocalizeAction.DELETE;
        return true;
    }

    @Override
    public void setLocalizeIgnore() {
        localizeType = LocalizeAction.IGNORE;
    }

    public void setLocalizeReplace(boolean isDefaultKey, String keyAlias, Goid keystoreGoid) {
        localizeType = LocalizeAction.REPLACE;
        localIsDefaultKey = isDefaultKey;

        if (!isDefaultKey) {
            localKeyAlias = keyAlias;
            localKeystoreGoid = keystoreGoid;
        }
    }

    public static PrivateKeyReference parseFromElement( final ExternalReferenceFinder context, final Element el ) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }

        PrivateKeyReference output = new PrivateKeyReference(context);
        output.isDefaultKey = Boolean.parseBoolean(getParamFromEl(el, IS_DEFAULT_KEY_EL_NAME));

        if (!output.isDefaultKey) {
            String keystoreId = getParamFromEl(el, KEYSTORE_OID_EL_NAME);
            if (keystoreId != null) {
                output.keystoreGoid = GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, keystoreId);
                try {
                    // check if the length is less 21 (the max length of a long with a '-')
                    // This check is needed because a goid that looks like this will be parsed as a valid long and we don't want that! 00000000000000000000000000000002
                    if(keystoreId.length() <= 20){
                        output.keystoreOid = Long.parseLong(keystoreId);
                    }
                } catch(NumberFormatException e) {
                    //do nothing. If it isn't a oid then that's ok.
                }
            }
            output.keyAlias = getParamFromEl(el, KEY_ALIAS_EL_NAME);
        }

        return output;
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        setTypeAttribute( refEl );
        referencesParentElement.appendChild(refEl);

        Element isDefaultKeyEl = referencesParentElement.getOwnerDocument().createElement(IS_DEFAULT_KEY_EL_NAME);
        Text txt = DomUtils.createTextNode(referencesParentElement, String.valueOf(isDefaultKey));
        isDefaultKeyEl.appendChild(txt);
        refEl.appendChild(isDefaultKeyEl);

        if (!isDefaultKey && keyAlias != null) {
            Element keystoreOidEl = referencesParentElement.getOwnerDocument().createElement(KEYSTORE_OID_EL_NAME);
            txt = DomUtils.createTextNode(referencesParentElement, keystoreGoid != null ? Goid.toString(keystoreGoid) : null);
            keystoreOidEl.appendChild(txt);
            refEl.appendChild(keystoreOidEl);

            Element keyAliasEl = referencesParentElement.getOwnerDocument().createElement(KEY_ALIAS_EL_NAME);
            txt = DomUtils.createTextNode(referencesParentElement, keyAlias);
            keyAliasEl.appendChild(txt);
            refEl.appendChild(keyAliasEl);
        }
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        if (isDefaultKey) {
            localizeType = LocalizeAction.IGNORE;
            return true;
        }

        try {
            SsgKeyEntry foundKey = getFinder().findKeyEntry(keyAlias, keystoreGoid);
            if (foundKey == null) {
                logger.warning("The private key with alias '" + keyAlias + "' and keystore GOID '" + keystoreGoid +
                    "' does not exist in this Gateway.");
                localizeType = LocalizeAction.REPLACE;
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "error looking for private key", e);
            localizeType = LocalizeAction.REPLACE;
            return false;
        }
    }

    @Override
    protected boolean localizeAssertion(final @Nullable Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE){
            if (assertionToLocalize instanceof PrivateKeyable) {
                PrivateKeyable keyable = (PrivateKeyable)assertionToLocalize;
                if (!keyable.isUsesDefaultKeyStore() && keyable.getKeyAlias() != null && keyable.getKeyAlias().equals(keyAlias)) {
                    if (localizeType == LocalizeAction.REPLACE) {
                        if (localIsDefaultKey) {
                            keyable.setUsesDefaultKeyStore(true);
                        } else {
                            keyable.setUsesDefaultKeyStore(false);
                            keyable.setKeyAlias(localKeyAlias);
                            keyable.setNonDefaultKeystoreId(localKeystoreGoid);
                        }
                    }  else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            } else {
                final EntitiesResolver entitiesResolver =
                    EntitiesResolver.builder()
                        .keyValueStore(getFinder().getCustomKeyValueStore())
                        .classNameToSerializerFunction(new Functions.Unary<CustomEntitySerializer, String>() {
                            @Override
                            public CustomEntitySerializer call(String entitySerializerClassName) {
                                return getFinder().getCustomKeyValueEntitySerializer(entitySerializerClassName);
                            }
                        })
                        .build();
                String keyId = keystoreGoid+":"+keyAlias;
                String localKeyId = localKeystoreGoid+":"+localKeyAlias;
                for(EntityHeader entityHeader : entitiesResolver.getEntitiesUsed(assertionToLocalize)) {
                    if (EntityType.SSG_KEY_ENTRY.equals(entityHeader.getType()) && keyId.equals(entityHeader.getStrId())) {
                        // No need to check for default key. Only none default keys are referenced.
                        //
                        if(localizeType == LocalizeAction.REPLACE) {
                            if (!localKeyId.equals(keyId)) {
                                if (localIsDefaultKey) {
                                    SsgKeyHeader newEntityHeader = new SsgKeyHeader(SignerServices.KEY_ID_SSL, Goid.DEFAULT_GOID, null, null);
                                    entitiesResolver.replaceEntity(assertionToLocalize, entityHeader, newEntityHeader);
                                    logger.info("The private key ID of the imported assertion has been changed from " + keyId + " to " + "default key");
                                } else {
                                    SsgKeyHeader newEntityHeader = new SsgKeyHeader(localKeyId, localKeystoreGoid, localKeyAlias, null);
                                    entitiesResolver.replaceEntity(assertionToLocalize, entityHeader, newEntityHeader);
                                    logger.info("The private key ID of the imported assertion has been changed from " + keyId + " to " + localKeyId);
                                    break;
                                }
                            }
                        } else if(localizeType == LocalizeAction.DELETE) {
                            logger.info("Deleted this assertion from the tree.");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        final PrivateKeyReference that = (PrivateKeyReference) o;

        if ( isDefaultKey != that.isDefaultKey ) return false;
        if ( !Goid.equals(keystoreGoid, that.keystoreGoid) ) return false;
        if ( keyAlias != null ? !keyAlias.equals( that.keyAlias ) : that.keyAlias != null ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = keyAlias != null ? keyAlias.hashCode() : 0;
        result = 31 * result + (keystoreGoid != null ? keystoreGoid.hashCode() : 0);
        result = 31 * result + (isDefaultKey ? 1 : 0);
        return result;
    }
}