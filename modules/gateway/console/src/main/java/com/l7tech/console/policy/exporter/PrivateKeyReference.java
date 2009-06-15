package com.l7tech.console.policy.exporter;

import org.w3c.dom.Element;
import org.w3c.dom.Text;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.DomUtils;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.console.util.Registry;

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

    private LocaliseAction localizeType;

    public PrivateKeyReference() {
    }

    public PrivateKeyReference(PrivateKeyable keyable) {
        if (keyable != null) {
            if (keyable.isUsesDefaultKeyStore()) {
                isDefaultKey = true;
            } else {
                isDefaultKey = false;
                keystoreOid = keyable.getNonDefaultKeystoreId();
                keyAlias = keyable.getKeyAlias();
            }
        }
        localizeType = LocaliseAction.IGNORE;
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

    public void setLocalizeReplace(boolean isDefaultKey, String keyAlias, long keystoreOid) {
        localizeType = LocaliseAction.REPLACE;
        localIsDefaultKey = isDefaultKey;

        if (!isDefaultKey) {
            localKeyAlias = keyAlias;
            localKeystoreOid = keystoreOid;
        }
    }

    public static PrivateKeyReference parseFromElement(Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }

        PrivateKeyReference output = new PrivateKeyReference();
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

    void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        refEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, PrivateKeyReference.class.getName());
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

    boolean verifyReference() throws InvalidPolicyStreamException {
        if (isDefaultKey) {
            localizeType = LocaliseAction.IGNORE;
            return true;
        }

        try {
            SsgKeyEntry foundKey = Registry.getDefault().getTrustedCertManager().findKeyEntry(keyAlias, keystoreOid);
            if (foundKey == null) {
                logger.warning("The private key with alias '" + keyAlias + "' and keystore OID '" + keystoreOid +
                    "' does not exist in this SecureSpan Gateway.");
                localizeType = LocaliseAction.REPLACE;
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "error looking for private key", e);
            localizeType = LocaliseAction.REPLACE;
            return false;
        }
    }

    boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType == LocaliseAction.REPLACE) {
            if (assertionToLocalize instanceof PrivateKeyable) {
                PrivateKeyable keyable = (PrivateKeyable)assertionToLocalize;
                if (!keyable.isUsesDefaultKeyStore() && keyable.getKeyAlias() != null && keyable.getKeyAlias().equals(keyAlias)) {
                    if (localIsDefaultKey) {
                        keyable.setUsesDefaultKeyStore(true);
                    } else {
                        keyable.setUsesDefaultKeyStore(false);
                        keyable.setKeyAlias(localKeyAlias);
                        keyable.setNonDefaultKeystoreId(localKeystoreOid);
                    }
                }
            }
        }  else if (localizeType == LocaliseAction.DELETE) {
            logger.info("Deleted this assertion from the tree.");
            return false;
        }

        return true;
    }

    /**
     * Enum-type class for the type of localization to use.
     */
    public static class LocaliseAction {
        public static final LocaliseAction IGNORE = new LocaliseAction(1);
        public static final LocaliseAction DELETE = new LocaliseAction(2);
        public static final LocaliseAction REPLACE = new LocaliseAction(3);
        private LocaliseAction(int val) {
            this.val = val;
        }
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LocaliseAction)) return false;

            final LocaliseAction localiseAction = (LocaliseAction) o;

            return val == localiseAction.val;
        }

        public int hashCode() {
            return val;
        }

        private int val = 0;
    }
}
