package com.l7tech.policy.exporter;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Arrays;

/**
 * A reference to an id provider.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 16, 2004<br/>
 * $Id$<br/>
 */
public class IdProviderReference extends ExternalReference {
    /**
     * Constructor meant to be used by the export process.
     * This will actually retreive the id provider and remember its
     * settings.
     */
    public IdProviderReference(long providerId) {
        // try to retrieve this id provider to remember its settings
        IdentityAdmin idAdmin = Registry.getDefault().getIdentityAdmin();
        IdentityProviderConfig config = null;
        if (idAdmin != null) {
            try {
                config = idAdmin.findIdentityProviderConfigByPrimaryKey(providerId);
            } catch (RemoteException e) {
                logger.log(Level.WARNING, "error finding id provider config", e);
            } catch (FindException e) {
                logger.log(Level.WARNING, "error finding id provider config", e);
            }
        }
        if (config == null) {
            logger.severe("This policy is referring to an Id provider that cannot be retrieved.");
        } else {
            try {
                setIdProviderConfProps(config.getSerializedProps());
                setProviderName(config.getName());
                setIdProviderTypeVal(config.getTypeVal());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error getting properties from id provider", e);
            }
        }
        setProviderId(providerId);
    }

    public static IdProviderReference parseFromElement(Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        IdProviderReference output = new IdProviderReference();
        String val = getParamFromEl(el, OID_EL_NAME);
        if (val != null) {
            output.providerId = Long.parseLong(val);
        }
        output.providerName = getParamFromEl(el, NAME_EL_NAME);
        String b64edProps = getParamFromEl(el, PROPS_EL_NAME);
        if (b64edProps != null) {
            try {
                output.idProviderConfProps = new String(HexUtils.decodeBase64(b64edProps, true));
            } catch (IOException e) {
                throw new InvalidDocumentFormatException("could not un-b64 the provider props");
            }
        } else output.idProviderConfProps = null;
        val = getParamFromEl(el, TYPEVAL_EL_NAME);
        if (val != null) {
            output.idProviderTypeVal = Integer.parseInt(val);
        }
        return output;
    }

    private IdProviderReference() {
        super();
    }

    void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        refEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, IdProviderReference.class.getName());
        referencesParentElement.appendChild(refEl);
        Element oidEl = referencesParentElement.getOwnerDocument().createElement(OID_EL_NAME);
        Text txt = XmlUtil.createTextNode(referencesParentElement, Long.toString(providerId));
        oidEl.appendChild(txt);
        refEl.appendChild(oidEl);
        Element nameEl = referencesParentElement.getOwnerDocument().createElement(NAME_EL_NAME);
        txt = XmlUtil.createTextNode(referencesParentElement, providerName);
        nameEl.appendChild(txt);
        refEl.appendChild(nameEl);
        Element propsEl = referencesParentElement.getOwnerDocument().createElement(PROPS_EL_NAME);
        if (idProviderConfProps != null) {
            // base 64 the props
            String encoded = HexUtils.encodeBase64(idProviderConfProps.getBytes());
            txt = XmlUtil.createTextNode(referencesParentElement, encoded);
            propsEl.appendChild(txt);
        }
        refEl.appendChild(propsEl);
        Element typeEl = referencesParentElement.getOwnerDocument().createElement(TYPEVAL_EL_NAME);
        txt = XmlUtil.createTextNode(referencesParentElement, Integer.toString(idProviderTypeVal));
        typeEl.appendChild(txt);
        refEl.appendChild(typeEl);
    }

    /**
     * Checks whether or not an external reference can be mapped on this local
     * system without administrator interaction.
     *
     * LOGIC:
     * 1. Look for same oid and name. If that exists, => record perfect match.
     * 2. Look for same properties. If that exists, => record corresponding match.
     * 3. Otherwise => this reference if 'not verified'.
     */
    boolean verifyReference() {
        // 1. Look for same oid and name. If that exists, => record perfect match.
        IdentityAdmin idAdmin = Registry.getDefault().getIdentityAdmin();
        // We can't do anything without the id provider config manager.
        if (idAdmin == null) {
            logger.severe("Cannot get an IdentityAdmin");
            return false;
        }
        IdentityProviderConfig configOnThisSystem = null;
        try {
            configOnThisSystem = idAdmin.findIdentityProviderConfigByPrimaryKey(getProviderId());
        } catch (FindException e) {
            logger.log(Level.WARNING, "error getting id provider config", e);
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "error getting id provider config", e);
        }
        if (configOnThisSystem != null && configOnThisSystem.getName().equals(getProviderName())) {
            // PERFECT MATCH!
            logger.fine("The id provider reference found the same provider locally.");
            localizeType = LocaliseAction.REPLACE;
            locallyMatchingProviderId = getProviderId();
            return true;
        }
        // 2. Look for same properties. If that exists, => record corresponding match.
        EntityHeader[] allConfigHeaders = null;
        try {
            allConfigHeaders = idAdmin.findAllIdentityProviderConfig();
        } catch (FindException e) {
            logger.log(Level.WARNING, "error getting all id provider config", e);
            return false;
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "error getting all id provider config", e);
            return false;
        }
        if (allConfigHeaders != null) {
            for (int i = 0; i < allConfigHeaders.length; i++) {
                try {
                    configOnThisSystem = idAdmin.findIdentityProviderConfigByPrimaryKey(allConfigHeaders[i].getOid());
                } catch (RemoteException e) {
                    logger.log(Level.WARNING, "cannot get id provider config", e);
                    continue;
                } catch (FindException e) {
                    logger.log(Level.WARNING, "cannot get id provider config", e);
                    continue;
                }
                if (configOnThisSystem.getTypeVal() != getIdProviderTypeVal()) {
                    logger.info("Type mismatch " + configOnThisSystem.getTypeVal() + " vs " + getIdProviderTypeVal());
                    continue;
                }
                String localProps = null;
                try {
                    localProps = configOnThisSystem.getSerializedProps();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Cannot get serialized properties");
                    return false;
                }
                if (equalsProps(localProps, getIdProviderConfProps())) {
                    // WE GOT A MATCH!
                    localizeType = LocaliseAction.REPLACE;
                    locallyMatchingProviderId = configOnThisSystem.getOid();
                    logger.fine("the provider was matched using the config's properties.");
                    return true;
                }
            }
        }
        // 3. Otherwise => this reference if 'not verified' and will require manual resolution.
        logger.fine("this reference cannot be established locally (" + getProviderName() + ").");
        return false;
    }

    private boolean equalsProps(String props1, String props2) {
        if (props1 == null || props1.equals("")) {
            if (props2 == null || props2.equals("")) return true;
            return false;
        }
        if (props2 == null || props2.equals("")) return false;

        ByteArrayInputStream in = new ByteArrayInputStream(props1.getBytes());
        java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in);
        Map map1 = (Map)decoder.readObject();
        in = new ByteArrayInputStream(props2.getBytes());
        decoder = new java.beans.XMLDecoder(in);
        Map map2 = (Map)decoder.readObject();

        return mapEquals(map1, map2);
    }

    private boolean mapEquals(Map map1, Map map2) {
        if (map1 == null) {
            if (map2 == null) return true;
            return false;
        }
        if (map2 == null) return false;
        // make sure that all objects in map1 are also in map2 and their values are the same
        Set keys = map1.keySet();
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            Object key = (Object) iterator.next();
            Object val1 = map1.get(key);
            Object val2 = map2.get(key);
            // either a map or a string
            if (val1 instanceof Map) {
                if (!mapEquals((Map)val1, (Map)val2)) return false;
            } else if (val1 instanceof Object[] && val2 instanceof Object[]) {
                if (!Arrays.equals((Object[])val1, (Object[])val2)) return false;
            } else if (val1 instanceof long[] && val2 instanceof long[]) {
                if (!Arrays.equals((long[])val1, (long[])val2)) return false;
            } else if (val1 instanceof boolean[] && val2 instanceof boolean[]) {
                if (!Arrays.equals((boolean[])val1, (boolean[])val2)) return false;
            } else if (val1 instanceof byte[] && val2 instanceof byte[]) {
                if (!Arrays.equals((byte[])val1, (byte[])val2)) return false;
            } else if (val1 instanceof char[] && val2 instanceof char[]) {
                if (!Arrays.equals((char[])val1, (char[])val2)) return false;
            } else if (val1 instanceof double[] && val2 instanceof double[]) {
                if (!Arrays.equals((double[])val1, (double[])val2)) return false;
            } else if (val1 instanceof float[] && val2 instanceof float[]) {
                if (!Arrays.equals((float[])val1, (float[])val2)) return false;
            } else if (val1 instanceof int[] && val2 instanceof int[]) {
                if (!Arrays.equals((int[])val1, (int[])val2)) return false;
            } else if (val1 instanceof short[] && val2 instanceof short[]) {
                if (!Arrays.equals((short[])val1, (short[])val2)) return false;
            } else if (!val1.equals(val2)) {
                logger.info("Mismatch on properties " + key + "" + val1 + "" + val2);
                return false;
            }
        }
        return true;
    }

    /**
     * return false if the localized assertion should be deleted from the tree
     */
    boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType != LocaliseAction.IGNORE) {
            if (assertionToLocalize instanceof IdentityAssertion) {
                IdentityAssertion idass = (IdentityAssertion)assertionToLocalize;
                if (idass.getIdentityProviderOid() == providerId) {
                    if (localizeType == LocaliseAction.REPLACE) {
                        idass.setIdentityProviderOid(locallyMatchingProviderId);
                        logger.info("The provider id of the imported id assertion has been changed " +
                                    "from " + providerId + " to " + locallyMatchingProviderId);
                        localizeLoginOrId(idass);
                    } else if (localizeType == LocaliseAction.DELETE) {
                        logger.info("Deleted this assertin from the tree.");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Does the following:
     * 1. if the id referred by the assertion does not exist given the id, try to find it by login
     * 2. if found by login but not by id, switch the id
     * 3. if found by id, make sure the login fits the assertion's login
     */
    private void localizeLoginOrId(IdentityAssertion a) {
        long providerId = a.getIdentityProviderOid();
        IdentityAdmin idAdmin = Registry.getDefault().getIdentityAdmin();
        try {
            if (a instanceof SpecificUser) {
                SpecificUser su = (SpecificUser)a;
                User userFromId = idAdmin.findUserByPrimaryKey(providerId, su.getUserUid());
                if (userFromId != null) {
                    if (userFromId.getLogin() != null && !userFromId.getLogin().equals(su.getUserLogin())) {
                        String oldLogin = su.getUserLogin();
                        su.setUserLogin(userFromId.getLogin());
                        logger.info("The login was changed from " + oldLogin + " to " + userFromId.getLogin());
                    }
                } else {
                    User userFromLogin = idAdmin.findUserByLogin(providerId, su.getUserLogin());
                    if (userFromLogin != null) {
                        logger.info("Changing " + su.getUserLogin() + "'s id from " +
                                    su.getUserUid() + " to " + userFromLogin.getUniqueIdentifier());
                        su.setUserUid(userFromLogin.getUniqueIdentifier());
                    }
                }
            } else if (a instanceof MemberOfGroup) {
                MemberOfGroup mog = (MemberOfGroup)a;
                if (idAdmin.findGroupByPrimaryKey(providerId, mog.getGroupId()) != null) {
                    // nothing?
                }
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "problem getting identity", e);
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "problem getting identity", e);
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdProviderReference)) return false;
        final IdProviderReference idProviderReference = (IdProviderReference)o;
        if (providerId != idProviderReference.providerId) return false;
        return true;
    }

    public int hashCode() {
        return (int) (providerId ^ (providerId >>> 32));
    }

    public long getProviderId() {
        return providerId;
    }

    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    public String getIdProviderConfProps() {
        return idProviderConfProps;
    }

    public void setIdProviderConfProps(String idProviderConfProps) {
        this.idProviderConfProps = idProviderConfProps;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public long getLocallyMatchingProviderId() {
        return locallyMatchingProviderId;
    }

    public int getIdProviderTypeVal() {
        return idProviderTypeVal;
    }

    public void setIdProviderTypeVal(int idProviderTypeVal) {
        this.idProviderTypeVal = idProviderTypeVal;
    }

    /**
     * Tell the reference that the localization should replace the
     * id provider of concerned assertions with another id provider.
     * @param alternateIdprovider the local provider value
     */
    public void setLocalizeReplace(long alternateIdprovider) {
        localizeType = LocaliseAction.REPLACE;
        locallyMatchingProviderId = alternateIdprovider;
    }

    /**
     * Tell the reference that the localization should ignore the
     * assertions that refer to the remote id provider (let the
     * assertions as is).
     */
    public void setLocalizeIgnore() {
        localizeType = LocaliseAction.REPLACE;
    }

    /**
     * Tell the reference that the localization process should remove
     * any assertions that refer to the remote id provider.
     */
    public void setLocalizeDelete() {
        localizeType = LocaliseAction.DELETE;
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

            if (val != localiseAction.val) return false;

            return true;
        }

        public int hashCode() {
            return val;
        }

        private int val = 0;
    }

    private LocaliseAction localizeType = null;
    private long providerId;
    private long locallyMatchingProviderId;
    private int idProviderTypeVal;
    private String providerName;
    private String idProviderConfProps;
    private final Logger logger = Logger.getLogger(IdProviderReference.class.getName());

    public static final String REF_EL_NAME = "IDProviderReference";
    public static final String PROPS_EL_NAME = "Props";
    public static final String OID_EL_NAME = "OID";
    public static final String NAME_EL_NAME = "Name";
    public static final String TYPEVAL_EL_NAME = "TypeVal";
}
