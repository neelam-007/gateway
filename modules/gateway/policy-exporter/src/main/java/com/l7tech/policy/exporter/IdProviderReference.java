package com.l7tech.policy.exporter;

import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.util.*;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A reference to an id provider.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 16, 2004<br/>
 */
public class IdProviderReference extends ExternalReference {
    /**
     * Constructor meant to be used by the export process.
     * This will actually retrieve the id provider and remember its
     * settings.
     */
    public IdProviderReference( final ExternalReferenceFinder finder,
                                final Goid providerId) {
        this( finder );

        // try to retrieve this id provider to remember its settings
        IdentityProviderConfig config = null;
        try {
            config = finder.findIdentityProviderConfigByID(providerId);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "error finding id provider config", e);
        } catch (FindException e) {
            logger.log(Level.WARNING, "error finding id provider config", e);
        }

        if (config == null) {
            logger.severe("This policy is referring to an Id provider that cannot be retrieved.");
        } else {
            try {
                setIdProviderConfProps(config.getExportableSerializedProps());
                setProviderName(config.getName());
                setIdProviderTypeVal(config.getTypeVal());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error getting properties from id provider", e);
            }
        }
        setProviderId(providerId);
    }

    public static IdProviderReference parseFromElement(final ExternalReferenceFinder context, final Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        IdProviderReference output = new IdProviderReference(context);
        String val = getParamFromEl(el, OLD_OID_EL_NAME);
        if (val != null) {
            try {
                Long oldOid = Long.parseLong(val);
                if(oldOid.equals(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OLD_OID ) ){
                    output.providerId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;
                }else{
                    output.providerId = GoidUpgradeMapper.mapOid(EntityType.ID_PROVIDER_CONFIG, Long.parseLong(val));
                }
            } catch (NumberFormatException nfe) {
                output.providerId = PersistentEntity.DEFAULT_GOID;
            }
        }

        val = getParamFromEl(el, GOID_EL_NAME);
        if (val != null) {
            try {
                output.providerId = new Goid(val);
            } catch (IllegalArgumentException e) {
                throw new InvalidDocumentFormatException("Invalid identity provider goid: " + ExceptionUtils.getMessage(e), e);
            }
        }

        output.providerName = getParamFromEl(el, NAME_EL_NAME);
        String b64edProps = getParamFromEl(el, PROPS_EL_NAME);
        if (b64edProps != null) {
            output.idProviderConfProps = new String(HexUtils.decodeBase64(b64edProps, true), Charsets.UTF8);
        } else output.idProviderConfProps = null;
        val = getParamFromEl(el, TYPEVAL_EL_NAME);
        if (val != null) {
            output.idProviderTypeVal = Integer.parseInt(val);
        }
        return output;
    }

    protected IdProviderReference( final ExternalReferenceFinder finder ) {
        super( finder );
    }

    @Override
    public String getRefId() {
        String id = null;

        if ( providerId!=null &&  !providerId.equals(IdentityProviderConfig.DEFAULT_GOID)) {
            id = Goid.toString( providerId );
        }

        return id;
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        setTypeAttribute( refEl );
        referencesParentElement.appendChild(refEl);
        Element oidEl = referencesParentElement.getOwnerDocument().createElement(GOID_EL_NAME);
        Text txt = DomUtils.createTextNode(referencesParentElement, Goid.toString(providerId));
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
    @Override
    protected boolean verifyReference() {
        // 1. Look for same oid and name. If that exists, => record perfect match.
        IdentityProviderConfig configOnThisSystem = null;
        try {
            configOnThisSystem = getFinder().findIdentityProviderConfigByID(getProviderId());
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
            allConfigHeaders = getFinder().findAllIdentityProviderConfig();
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
                    configOnThisSystem = getFinder().findIdentityProviderConfigByID(header.getGoid());
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
                if (equalsProps(localProps, getIdProviderConfProps()) && permitMapping( providerId, configOnThisSystem.getGoid() )) {
                    // WE GOT A MATCH!
                    setLocalizeReplace( configOnThisSystem.getGoid() );
                    logger.fine("the provider was matched using the config's properties.");
                    return true;
                } else {
                    // try to do a smart comparison match for LDAP
                    Map localPropsMap = deserializeIDPProps(localProps);
                    Map otherPropsMap = deserializeIDPProps(getIdProviderConfProps());
                    if (getIdProviderTypeVal() == IdentityProviderType.LDAP.toVal() && !otherPropsMap.isEmpty()) {
                        //check NTLM properties
                         Map<String, String> localNtlmMap = (Map<String, String>)localPropsMap.get(LdapIdentityProviderConfig.NTLM_AUTHENTICATION_PROVIDER_PROPERTIES);
                        Map<String, String> otherNtlmMap = (Map<String, String>)otherPropsMap.get(LdapIdentityProviderConfig.NTLM_AUTHENTICATION_PROVIDER_PROPERTIES);
                        if(verifyNtlmProperties(localNtlmMap, otherNtlmMap)) {
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
                                       if (s1.equalsIgnoreCase(s2) && permitMapping( providerId, configOnThisSystem.getGoid() )) {
                                           setLocalizeReplace( configOnThisSystem.getGoid() );
                                           logger.fine("LDAP URL common to both id providers (" + s1 + ")");
                                           return true;
                                       }
                                    }
                                }
                            } else {
                                logger.fine("The search base are not the same " + val1 + " vs " + val2);
                            }
                        } else {
                            logger.fine("NTLM properties are not the same!");
                        }
                    }
                }
            }
        }
        // 3. Otherwise => this reference if 'not verified' and will require manual resolution.
        logger.fine("this reference cannot be established locally (" + getProviderName() + ").");
        return false;
    }

    boolean verifyNtlmProperties(final Map<String, String> local, final Map<String, String> other) {
        if(other == null || !Boolean.parseBoolean(other.get("enabled"))){
            return true;//if NTLM properties disabled in the imported policy the rest doesn't matter
        }
        //check required NTLM properties
        boolean match = false;

        if(local == null || !Boolean.parseBoolean(local.get("enabled"))) {
            return false;
        }
        if(other.containsKey("server.dns.name") && local.containsKey("server.dns.name")) {
           match = other.get("server.dns.name").equals(local.get("server.dns.name"));
        }
        else{
            return false;
        }
        if(other.containsKey("service.account") && local.containsKey("service.account")){
            match &= other.get("service.account").equalsIgnoreCase(local.get("service.account"));
        }
        else {
            return false;
        }
        if(other.containsKey("domain.netbios.name") && local.containsKey("domain.netbios.name")){
           match &= other.get("domain.netbios.name").equalsIgnoreCase(local.get("domain.netbios.name"));
        }
        else {
            return false;
        }
        if(other.containsKey("host.netbios.name") && local.containsKey("host.netbios.name")){
            match &= other.get("host.netbios.name").equalsIgnoreCase(local.get("host.netbios.name"));
        }
        else {
            return false;
        }

        return match;
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
    @Override
    protected boolean localizeAssertion(final @Nullable Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE) {
            if (assertionToLocalize instanceof IdentityAssertion) {
                IdentityAssertion idass = (IdentityAssertion)assertionToLocalize;
                if (idass.getIdentityProviderOid().equals(providerId)){
                    if (localizeType == LocalizeAction.REPLACE) {
                        if ( !locallyMatchingProviderId.equals(providerId) ) {
                            idass.setIdentityProviderOid(locallyMatchingProviderId);
                            logger.info("The provider id of the imported id assertion has been changed " +
                                        "from " + providerId + " to " + locallyMatchingProviderId);
                        }
                        localizeLoginOrId(idass);
                    } else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertin from the tree.");
                        return false;
                    }
                }
            } else if(assertionToLocalize instanceof UsesEntities) {
                UsesEntities entitiesUser = (UsesEntities)assertionToLocalize;
                for(EntityHeader entityHeader : entitiesUser.getEntitiesUsed()) {
                    if(entityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG) && entityHeader.getGoid()!=null&& entityHeader.getGoid().equals(providerId)) {
                        if(localizeType == LocalizeAction.REPLACE) {
                            if(!locallyMatchingProviderId.equals( providerId)) {
                                EntityHeader newEntityHeader = new EntityHeader(locallyMatchingProviderId, EntityType.ID_PROVIDER_CONFIG, null, null);
                                entitiesUser.replaceEntity(entityHeader, newEntityHeader);

                                logger.info("The provider id of the imported id assertion has been changed " +
                                        "from " + providerId + " to " + locallyMatchingProviderId);
                                
                                break;
                            }
                        } else if(localizeType == LocalizeAction.DELETE) {
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
        try {
            if (a instanceof SpecificUser) {
                SpecificUser su = (SpecificUser)a;
                localizeLoginOrIdForSpecificUser(su);
            } else if (a instanceof MemberOfGroup) {
                MemberOfGroup mog = (MemberOfGroup) a;
                localizeLoginOrIdForSpecificGroup(mog);
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "problem getting identity", e);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "problem getting identity", e);
        }
    }

    protected void localizeLoginOrIdForSpecificUser(SpecificUser su) throws FindException {
        Goid providerId = su.getIdentityProviderOid();
        User userFromId = getFinder().findUserByID(providerId, su.getUserUid());
        if (userFromId != null) {
            if (userFromId.getLogin() != null && !userFromId.getLogin().equals(su.getUserLogin())) {
                String oldLogin = su.getUserLogin();
                su.setUserLogin(userFromId.getLogin());
                logger.info("The login was changed from " + oldLogin + " to " + userFromId.getLogin());
            }
        } else {
            User userFromLogin = getFinder().findUserByLogin(providerId, su.getUserLogin());
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
                        "the target Gateway. You should remove\n" +
                        "or replace the identity assertion from the policy.";
                logger.warning(msg);
                warning( "Unresolved identity", msg );
            }
        }
    }

    protected void localizeLoginOrIdForSpecificGroup(MemberOfGroup mog) throws FindException {
        Goid providerId = mog.getIdentityProviderOid();
        Group groupFromId = getFinder().findGroupByID(providerId, mog.getGroupId());
        if ( groupFromId != null ) {
            if (groupFromId.getName() != null && !groupFromId.getName().equals(mog.getGroupName())) {
                String oldName = mog.getGroupName();
                mog.setGroupName(groupFromId.getName());
                logger.info("The group name was changed from " + oldName + " to " + groupFromId.getName());
            }
        } else {
            Group groupFromName = getFinder().findGroupByName(providerId, mog.getGroupName());
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
                        "the target Gateway. You should remove\n" +
                        "or replace the identity assertion from the policy.";
                logger.warning(msg);
                warning( "Unresolved identity", msg );
            }
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdProviderReference)) return false;
        final IdProviderReference idProviderReference = (IdProviderReference)o;
        return providerId!=null ? providerId.equals( idProviderReference.providerId) : idProviderReference.providerId == null;
    }

    public int hashCode() {
        return providerId != null ? providerId.hashCode(): 0 ;
    }

    public Goid getProviderId() {
        return providerId;
    }

    public void setProviderId(Goid providerId) {
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

    public Goid getLocallyMatchingProviderId() {
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
    @Override
    public boolean setLocalizeReplace(Goid alternateIdprovider) {
        localizeType = LocalizeAction.REPLACE;
        locallyMatchingProviderId = alternateIdprovider;
        return true;
    }

    /**
     * Tell the reference that the localization should ignore the
     * assertions that refer to the remote id provider (let the
     * assertions as is).
     */
    @Override
    public void setLocalizeIgnore() {
        localizeType = LocalizeAction.IGNORE;
    }

    /**
     * Tell the reference that the localization process should remove
     * any assertions that refer to the remote id provider.
     */
    @Override
    public boolean setLocalizeDelete() {
        localizeType = LocalizeAction.DELETE;
        return true;
    }

    private LocalizeAction localizeType = null;
    protected Goid providerId;
    private Goid locallyMatchingProviderId;
    protected int idProviderTypeVal;
    protected String providerName;
    protected String idProviderConfProps;
    private final Logger logger = Logger.getLogger(IdProviderReference.class.getName());

    public static final String REF_EL_NAME = "IDProviderReference";
    public static final String PROPS_EL_NAME = "Props";
    public static final String OLD_OID_EL_NAME = "OID";
    public static final String GOID_EL_NAME = "GOID";
    public static final String NAME_EL_NAME = "Name";
    public static final String TYPEVAL_EL_NAME = "TypeVal";
}
