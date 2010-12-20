package com.l7tech.policy.exporter;

import org.w3c.dom.Element;
import org.w3c.dom.Text;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.DomUtils;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * User: ghuang
 */
public class PrivateKeyReference extends ExternalReference {
    private final Logger logger = Logger.getLogger(TrustedCertReference.class.getName());

    public static final String REF_EL_NAME = "PrivateKeyReference";
    public static final String KEYSTORE_OID_EL_NAME = "KeystoreOID";
    public static final String KEY_ALIAS_EL_NAME = "KeyAlias";
    public static final String IS_DEFAULT_KEY_EL_NAME = "IsDefaultKey";

    private String keyAlias;
    private long keystoreOid;
    private boolean isDefaultKey;

    private String localKeyAlias;
    private long localKeystoreOid;
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
                keystoreOid = keyable.getNonDefaultKeystoreId();
                keyAlias = keyable.getKeyAlias();
            }
        }
        localizeType = LocalizeAction.IGNORE;
    }

    @Override
    public String getRefId() {
        String id = null;

        if ( keystoreOid > 0 && keyAlias!=null ) {
            id = keystoreOid + ":" + keyAlias;
        }

        return id;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public long getKeystoreOid() {
        return keystoreOid;
    }

    public boolean isDefaultKey() {
        return isDefaultKey;
    }

    @Override
    public boolean setLocalizeReplace( final String identifier ) {
        boolean ok = false;

        if ( identifier == null ) {
            setLocalizeReplace( true, null, -1 );
            ok = true;
        } else {
            String[] keystoreAndAlias = identifier.split( ":", 2 );
            if ( keystoreAndAlias.length==2 ) {
                try {
                    final long keystoreOid = Long.parseLong( keystoreAndAlias[0] );
                    final String alias = keystoreAndAlias[1].trim();

                    if ( !alias.isEmpty() ) {
                        setLocalizeReplace( false, alias, keystoreOid );
                        ok = true;
                    }
                } catch ( NumberFormatException nfe ) {
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

    public void setLocalizeReplace(boolean isDefaultKey, String keyAlias, long keystoreOid) {
        localizeType = LocalizeAction.REPLACE;
        localIsDefaultKey = isDefaultKey;

        if (!isDefaultKey) {
            localKeyAlias = keyAlias;
            localKeystoreOid = keystoreOid;
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
                output.keystoreOid = Long.parseLong(keystoreId);
            }
            output.keyAlias = getParamFromEl(el, KEY_ALIAS_EL_NAME);
        }

        return output;
    }

    @Override
    void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        setTypeAttribute( refEl );
        referencesParentElement.appendChild(refEl);

        Element isDefaultKeyEl = referencesParentElement.getOwnerDocument().createElement(IS_DEFAULT_KEY_EL_NAME);
        Text txt = DomUtils.createTextNode(referencesParentElement, String.valueOf(isDefaultKey));
        isDefaultKeyEl.appendChild(txt);
        refEl.appendChild(isDefaultKeyEl);

        if (!isDefaultKey && keyAlias != null) {
            Element keystoreOidEl = referencesParentElement.getOwnerDocument().createElement(KEYSTORE_OID_EL_NAME);
            txt = DomUtils.createTextNode(referencesParentElement, Long.toString(keystoreOid));
            keystoreOidEl.appendChild(txt);
            refEl.appendChild(keystoreOidEl);

            Element keyAliasEl = referencesParentElement.getOwnerDocument().createElement(KEY_ALIAS_EL_NAME);
            txt = DomUtils.createTextNode(referencesParentElement, keyAlias);
            keyAliasEl.appendChild(txt);
            refEl.appendChild(keyAliasEl);
        }
    }

    @Override
    boolean verifyReference() throws InvalidPolicyStreamException {
        if (isDefaultKey) {
            localizeType = LocalizeAction.IGNORE;
            return true;
        }

        try {
            SsgKeyEntry foundKey = getFinder().findKeyEntry(keyAlias, keystoreOid);
            if (foundKey == null) {
                logger.warning("The private key with alias '" + keyAlias + "' and keystore OID '" + keystoreOid +
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
    boolean localizeAssertion(Assertion assertionToLocalize) {
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
                            keyable.setNonDefaultKeystoreId(localKeystoreOid);
                        }
                    }  else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
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
        if ( keystoreOid != that.keystoreOid ) return false;
        if ( keyAlias != null ? !keyAlias.equals( that.keyAlias ) : that.keyAlias != null ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = keyAlias != null ? keyAlias.hashCode() : 0;
        result = 31 * result + (int) (keystoreOid ^ (keystoreOid >>> 32));
        result = 31 * result + (isDefaultKey ? 1 : 0);
        return result;
    }
}
