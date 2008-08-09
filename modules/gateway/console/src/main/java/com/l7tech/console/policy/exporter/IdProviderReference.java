package com.l7tech.console.policy.exporter;

import com.l7tech.util.HexUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.Group;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
                config = idAdmin.findIdentityProviderConfigByID(providerId);
            } catch (RuntimeException e) {
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
                output.idProviderConfProps = new String(HexUtils.decodeBase64(b64edProps, true), "UTF-8");
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

    protected IdProviderReference() {
        super();
    }

    void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        refEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, IdProviderReference.class.getName());
        referencesParentElement.appendChild(refEl);
        Element oidEl = referencesParentElement.getOwnerDocument().createElement(OID_EL_NAME);
        Text txt = DomUtils.createTextNode(referencesParentElement, Long.toString(providerId));
        oidEl.appendChild(txt);
        refEl.appendChild(oidEl);
        if ( providerName != null ) { 
            Element nameEl = referencesParentElement.getOwnerDocument().createElement(NAME_EL_NAME);
            txt = DomUtils.createTextNode(referencesParentElement, providerName);
            nameEl.appendChild(txt);
            refEl.appendChild(nameEl);
        }
        if ( idProviderConfProps != null ) {
            Element propsEl = referencesParentElement.getOwnerDocument().createElement(PROPS_EL_NAME);
            // base 64 the props
            String encoded = HexUtils.encodeBase64(HexUtils.encodeUtf8(idProviderConfProps));
            txt = DomUtils.createTextNode(referencesParentElement, encoded);
            propsEl.appendChild(txt);
            refEl.appendChild(propsEl);
        }
        if ( idProviderTypeVal > 0 ) {
            Element typeEl = referencesParentElement.getOwnerDocument().createElement(TYPEVAL_EL_NAME);
            txt = DomUtils.createTextNode(referencesParentElement, Integer.toString(idProviderTypeVal));
            typeEl.appendChild(txt);
            refEl.appendChild(typeEl);
        }
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
            configOnThisSystem = idAdmin.findIdentityProviderConfigByID(getProviderId());
        } catch (FindException e) {
            logger.log(Level.WARNING, "error getting id provider config", e);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "error getting id provider config", e);
        }
        if (configOnThisSystem != null && (configOnThisSystem.getName().equals(getProviderName()) || getProviderName()==null)) {
            // PERFECT MATCH!
            logger.fine("The id provider reference found the same provider locally.");
            setLocalizeReplace( getProviderId() );
            return true;
        }
        // 2. Look for same properties. If that exists, => record corresponding match.
        EntityHeader[] allConfigHeaders;
        try {
            allConfigHeaders = idAdmin.findAllIdentityProviderConfig();
        } catch (FindException e) {
            logger.log(Level.WARNING, "error getting all id provider config", e);
            return false;
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "error getting all id provider config", e);
            return false;
        }
        if (allConfigHeaders != null) {
            for (EntityHeader header : allConfigHeaders) {
                try {
                    configOnThisSystem = idAdmin.findIdentityProviderConfigByID(header.getOid());
                } catch (RuntimeException e) {
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
                String localProps;
                try {
                    localProps = configOnThisSystem.getSerializedProps();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Cannot get serialized properties");
                    return false;
                }
                if (equalsProps(localProps, getIdProviderConfProps())) {
                    // WE GOT A MATCH!
                    setLocalizeReplace( configOnThisSystem.getOid() );
                    logger.fine("the provider was matched using the config's properties.");
                    return true;
                } else {
                    // try to do a smart comparison match for LDAP
                    Map localPropsMap = deserializeIDPProps(localProps);
                    Map otherPropsMap = deserializeIDPProps(getIdProviderConfProps());
                    if (getIdProviderTypeVal() == IdentityProviderType.LDAP.toVal()) {
                        // use LdapIdentityProviderConfig.URL and LdapIdentityProviderConfig.SEARCH_BASE
                        String val1 = (String)localPropsMap.get(LdapIdentityProviderConfig.SEARCH_BASE);
                        String val2 = (String)otherPropsMap.get(LdapIdentityProviderConfig.SEARCH_BASE);
                        val1 = val1.trim();
                        val2 = val2.trim();
                        if (val1.equalsIgnoreCase(val2)) {
                            logger.fine("same Search base established");
                            Object tmp = localPropsMap.get(LdapIdentityProviderConfig.URL);
                            String[] urls1;
                            if (tmp instanceof String) urls1 = new String[]{(String)tmp};
                            else urls1 = (String[])tmp;

                            tmp = otherPropsMap.get(LdapIdentityProviderConfig.URL);
                            String[] urls2;
                            if (tmp instanceof String) urls2 = new String[]{(String)tmp};
                            else urls2 = (String[])tmp;

                            // check that at least one url is common
                            for (String s1 : urls1) {
                                for (String s2 : urls2) {
                                   if (s1.equalsIgnoreCase(s2)) {
                                       logger.fine("LDAP URL common to both id providers (" + s1 + ")");
                                       return true;
                                   }
                                }
                            }
                        } else {
                            logger.fine("The search base are not the same " + val1 + " vs " + val2);
                        }
                    }
                }
            }
        }
        // 3. Otherwise => this reference if 'not verified' and will require manual resolution.
        logger.fine("this reference cannot be established locally (" + getProviderName() + ").");
        return false;
    }

    private Map deserializeIDPProps(String serializedProps) {
        if (serializedProps == null) {
            return new HashMap();
        }
        ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedProps));
        java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in);
        return (Map)decoder.readObject();
    }

    private boolean equalsProps(String props1, String props2) {
        if (props1 == null) props1 = "";
        if (props2 == null) props2 = "";
        return props1.equalsIgnoreCase(props2);
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
                        if ( locallyMatchingProviderId != providerId ) {
                            idass.setIdentityProviderOid(locallyMatchingProviderId);
                            logger.info("The provider id of the imported id assertion has been changed " +
                                        "from " + providerId + " to " + locallyMatchingProviderId);
                        }
                        localizeLoginOrId(idass);
                    } else if (localizeType == LocaliseAction.DELETE) {
                        logger.info("Deleted this assertin from the tree.");
                        return false;
                    }
                }
            } else if(assertionToLocalize instanceof UsesEntities) {
                UsesEntities entitiesUser = (UsesEntities)assertionToLocalize;
                for(EntityHeader entityHeader : entitiesUser.getEntitiesUsed()) {
                    if(entityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG) && entityHeader.getOid() == providerId) {
                        if(localizeType == LocaliseAction.REPLACE) {
                            if(locallyMatchingProviderId != providerId) {
                                EntityHeader newEntityHeader = new EntityHeader(locallyMatchingProviderId, EntityType.ID_PROVIDER_CONFIG, null, null);
                                entitiesUser.replaceEntity(entityHeader, newEntityHeader);

                                logger.info("The provider id of the imported id assertion has been changed " +
                                        "from " + providerId + " to " + locallyMatchingProviderId);
                                
                                break;
                            }
                        } else if(localizeType == LocaliseAction.DELETE) {
                            logger.info("Deleted this assertion from the tree.");
                            return false;
                        }
                    }
                    //TODO Add support fo updating other types of entities (users, groups, etc)
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
    protected void localizeLoginOrId(IdentityAssertion a) {
        IdentityAdmin idAdmin = Registry.getDefault().getIdentityAdmin();
        try {
            if (a instanceof SpecificUser) {
                SpecificUser su = (SpecificUser)a;
                localizeLoginOrIdForSpecificUser(idAdmin, su);
            } else if (a instanceof MemberOfGroup) {
                MemberOfGroup mog = (MemberOfGroup) a;
                localizeLoginOrIdForSpecificGroup(idAdmin, mog);
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "problem getting identity", e);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "problem getting identity", e);
        }
    }

    protected void localizeLoginOrIdForSpecificUser(IdentityAdmin idAdmin, SpecificUser su) throws FindException {
        long providerId = su.getIdentityProviderOid();
        User userFromId = idAdmin.findUserByID(providerId, su.getUserUid());
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
                        su.getUserUid() + " to " + userFromLogin.getId());
                su.setUserUid(userFromLogin.getId());
            } else {
                // the user is not found with the id nor the login
                String userRef = su.getUserLogin();
                if (userRef == null || userRef.length() < 1) {
                    userRef = su.getUserUid();
                }
                String msg = "The user \"" + userRef + "\" does not exist on\n" +
                        "the target SecureSpan Gateway. You should remove\n" +
                        "or replace the identity assertion from the policy.";
                logger.warning(msg);
                JOptionPane.showMessageDialog(null, msg, "Unresolved identity", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    protected void localizeLoginOrIdForSpecificGroup(IdentityAdmin idAdmin, MemberOfGroup mog) throws FindException {
        long providerId = mog.getIdentityProviderOid();
        Group groupFromId = idAdmin.findGroupByID(providerId, mog.getGroupId());
        if ( groupFromId != null ) {
            if (groupFromId.getName() != null && !groupFromId.getName().equals(mog.getGroupName())) {
                String oldName = mog.getGroupName();
                mog.setGroupName(groupFromId.getName());
                logger.info("The group name was changed from " + oldName + " to " + groupFromId.getName());
            }
        } else {
            Group groupFromName = idAdmin.findGroupByName(providerId, mog.getGroupName());
            if (groupFromName != null) {
                logger.info("Changing " + mog.getGroupName() + "'s id from " +
                        mog.getGroupId() + " to " + groupFromName.getId());
                mog.setGroupId(groupFromName.getId());
            } else {
                // group not found for id or name
                String groupRef = mog.getGroupName();
                if (groupRef == null || groupRef.length() < 1) {
                    groupRef = mog.getGroupId();
                }
                String msg = "The group \"" + groupRef + "\" does not exist on\n" +
                        "the target SecureSpan Gateway. You should remove\n" +
                        "or replace the identity assertion from the policy.";
                logger.warning(msg);
                JOptionPane.showMessageDialog(null, msg, "Unresolved identity", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdProviderReference)) return false;
        final IdProviderReference idProviderReference = (IdProviderReference)o;
        return providerId == idProviderReference.providerId;
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
        localizeType = LocaliseAction.IGNORE;
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

            return val == localiseAction.val;
        }

        public int hashCode() {
            return val;
        }

        private int val = 0;
    }

    private LocaliseAction localizeType = null;
    protected long providerId;
    private long locallyMatchingProviderId;
    protected int idProviderTypeVal;
    protected String providerName;
    protected String idProviderConfProps;
    private final Logger logger = Logger.getLogger(IdProviderReference.class.getName());

    public static final String REF_EL_NAME = "IDProviderReference";
    public static final String PROPS_EL_NAME = "Props";
    public static final String OID_EL_NAME = "OID";
    public static final String NAME_EL_NAME = "Name";
    public static final String TYPEVAL_EL_NAME = "TypeVal";
}
