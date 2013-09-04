package com.l7tech.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.util.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A reference to a federated id provider. The serialized form will contain the users and groups, so they can
 * be recreated on a different system.
 */
public class FederatedIdProviderReference extends IdProviderReference {
    public static final String REF_EL_NAME = "FederatedIDProviderReference";
    public static final String GROUPS_EL_NAME = "Groups";
    public static final String GROUP_EL_NAME = "Group";
    public static final String VIRTUAL_GROUP_EL_NAME = "VirtualGroup";
    public static final String DESCRIPTION_EL_NAME = "Description";
    public static final String GROUP_MEMBER_EL_NAME = "Member";
    public static final String USERS_EL_NAME = "Users";
    public static final String USER_EL_NAME = "User";
    public static final String USER_SUBJECT_DN_EL_NAME = "SubjectDn";
    public static final String USER_EMAIL_EL_NAME = "Email";
    public static final String USER_LOGIN_EL_NAME = "Login";
    public static final String USER_FIRST_NAME_EL_NAME = "FirstName";
    public static final String USER_LAST_NAME_EL_NAME = "LastName";

    private HashMap<String, FederatedGroup> importedGroups = new HashMap<String, FederatedGroup>();
    private HashMap<String, FederatedUser> importedUsers = new HashMap<String, FederatedUser>();
    private HashMap<String, Set<String>> importedGroupMembership = new HashMap<String, Set<String>>();

    private HashMap<String, String> userUpdateMap = new HashMap<String, String>();
    private HashMap<String, String> groupUpdateMap = new HashMap<String, String>();

    public FederatedIdProviderReference(final ExternalReferenceFinder finder, Goid providerId) {
        super(finder, providerId);
    }

    private FederatedIdProviderReference(final ExternalReferenceFinder finder) {
        super(finder);
    }

    public HashMap<String, FederatedGroup> getImportedGroups() {
        return importedGroups;
    }

    public HashMap<String, FederatedUser> getImportedUsers() {
        return importedUsers;
    }

    public HashMap<String, Set<String>> getImportedGroupMembership() {
        return importedGroupMembership;
    }

    public HashMap<String, String> getUserUpdateMap() {
        return userUpdateMap;
    }

    public HashMap<String, String> getGroupUpdateMap() {
        return groupUpdateMap;
    }
    
    public static FederatedIdProviderReference parseFromElement(final ExternalReferenceFinder context, final Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        FederatedIdProviderReference output = new FederatedIdProviderReference(context);
        String val = getParamFromEl(el, OLD_OID_EL_NAME);
        if (val != null) {
            try {
                output.providerId = GoidUpgradeMapper.mapOid(EntityType.ID_PROVIDER_CONFIG, Long.parseLong(val));
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

        NodeList groupsElements = el.getElementsByTagName(GROUPS_EL_NAME);
        if(groupsElements.getLength() > 0) {
            Element groupsElement = (Element)groupsElements.item(0);
            NodeList groupElements = groupsElement.getElementsByTagName(GROUP_EL_NAME);
            for(int i = 0;i < groupElements.getLength();i++) {
                Element groupElement = (Element)groupElements.item(i);

                FederatedGroup group = new FederatedGroup();
                val = getParamFromEl(groupElement, GOID_EL_NAME);
                if(val != null) {
                    group.setGoid(GoidUpgradeMapper.mapId("fed_group", val));
                }

                val = getParamFromEl(groupElement, NAME_EL_NAME);
                if(val != null) {
                    group.setName(val);
                }

                val = getParamFromEl(groupElement, DESCRIPTION_EL_NAME);
                if(val != null) {
                    group.setDescription(val);
                }

                val = getParamFromEl(groupElement, PROPS_EL_NAME);
                if(val != null) {
                    val = new String(HexUtils.decodeBase64(val, true), Charsets.UTF8);
                    group.setXmlProperties(val);
                }

                NodeList memberElements = groupElement.getElementsByTagName(GROUP_MEMBER_EL_NAME);
                Set<String> members = new HashSet<String>();
                for(int j = 0;j < memberElements.getLength();j++) {
                    Element memberElement = (Element)memberElements.item(j);
                    members.add(memberElement.getTextContent());
                }
                output.importedGroupMembership.put(group.getId(), members);

                output.importedGroups.put(group.getId(), group);
            }

            groupElements = groupsElement.getElementsByTagName(VIRTUAL_GROUP_EL_NAME);
            for(int i = 0;i < groupElements.getLength();i++) {
                Element groupElement = (Element)groupElements.item(i);

                VirtualGroup group = new VirtualGroup();
                val = getParamFromEl(groupElement, GOID_EL_NAME);
                if(val != null) {
                    group.setGoid(GoidUpgradeMapper.mapId("fed_group_virtual", val));
                }

                val = getParamFromEl(groupElement, NAME_EL_NAME);
                if(val != null) {
                    group.setName(val);
                }

                val = getParamFromEl(groupElement, DESCRIPTION_EL_NAME);
                if(val != null) {
                    group.setDescription(val);
                }

                val = getParamFromEl(groupElement, PROPS_EL_NAME);
                if(val != null) {
                    val = new String(HexUtils.decodeBase64(val, true), Charsets.UTF8);
                    group.setXmlProperties(val);
                }

                output.importedGroups.put(group.getId(), group);
            }
        }

        NodeList usersElements = el.getElementsByTagName(USERS_EL_NAME);
        if(usersElements.getLength() > 0) {
            Element usersElement = (Element)usersElements.item(0);
            NodeList userElements = usersElement.getElementsByTagName(USER_EL_NAME);
            for(int i = 0;i < userElements.getLength();i++) {
                Element userElement = (Element)userElements.item(i);

                FederatedUser user = new FederatedUser();
                val = getParamFromEl(userElement, GOID_EL_NAME);
                if(val != null) {
                    user.setGoid(GoidUpgradeMapper.mapId("fed_user", val));
                }

                val = getParamFromEl(userElement, NAME_EL_NAME);
                if(val != null) {
                    user.setName(val);
                }

                val = getParamFromEl(userElement, USER_SUBJECT_DN_EL_NAME);
                if(val != null) {
                    user.setSubjectDn(val);
                }

                val = getParamFromEl(userElement, USER_LOGIN_EL_NAME);
                if(val != null) {
                    user.setLogin(val);
                }

                val = getParamFromEl(userElement, USER_EMAIL_EL_NAME);
                if(val != null) {
                    user.setEmail(val);
                }

                val = getParamFromEl(userElement, USER_FIRST_NAME_EL_NAME);
                if(val != null) {
                    user.setFirstName(val);
                }

                val = getParamFromEl(userElement, USER_LAST_NAME_EL_NAME);
                if(val != null) {
                    user.setLastName(val);
                }

                output.importedUsers.put(user.getId(), user);
            }
        }

        return output;
    }

    private void appendStringValueElement(String elName, String value, Element elFactory, Element containerEl) {
        if(value == null || value.length() == 0) {
            return;
        }

        Element newElement = elFactory.getOwnerDocument().createElement(elName);
        Text txt = XmlUtil.createTextNode(elFactory, value);
        newElement.appendChild(txt);
        containerEl.appendChild(newElement);
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        setTypeAttribute( refEl );
        referencesParentElement.appendChild(refEl);
        Element oidEl = referencesParentElement.getOwnerDocument().createElement(GOID_EL_NAME);
        Text txt = XmlUtil.createTextNode(referencesParentElement, Goid.toString(providerId));
        oidEl.appendChild(txt);
        refEl.appendChild(oidEl);
        if ( providerName != null ) {
            Element nameEl = referencesParentElement.getOwnerDocument().createElement(NAME_EL_NAME);
            txt = XmlUtil.createTextNode(referencesParentElement, providerName);
            nameEl.appendChild(txt);
            refEl.appendChild(nameEl);
        }
        if ( idProviderConfProps != null ) {
            Element propsEl = referencesParentElement.getOwnerDocument().createElement(PROPS_EL_NAME);
            // base 64 the props
            String encoded = HexUtils.encodeBase64(HexUtils.encodeUtf8(idProviderConfProps));
            txt = XmlUtil.createTextNode(referencesParentElement, encoded);
            propsEl.appendChild(txt);
            refEl.appendChild(propsEl);
        }
        if ( idProviderTypeVal > 0 ) {
            Element typeEl = referencesParentElement.getOwnerDocument().createElement(TYPEVAL_EL_NAME);
            txt = XmlUtil.createTextNode(referencesParentElement, Integer.toString(idProviderTypeVal));
            typeEl.appendChild(txt);
            refEl.appendChild(typeEl);
        }
        
        try {
            Collection<IdentityHeader> groupHeaders = getFinder().findAllGroups(providerId);

            Element groupsEl = referencesParentElement.getOwnerDocument().createElement(GROUPS_EL_NAME);
            for(EntityHeader groupHeader : groupHeaders) {
                FederatedGroup group = (FederatedGroup) getFinder().findGroupByID(providerId, groupHeader.getStrId());
                boolean isVirtual = group instanceof VirtualGroup;

                Element groupEl = referencesParentElement.getOwnerDocument().createElement(isVirtual ? VIRTUAL_GROUP_EL_NAME : GROUP_EL_NAME);

                appendStringValueElement(GOID_EL_NAME, group.getId(), referencesParentElement, groupEl);
                appendStringValueElement(NAME_EL_NAME, group.getName(), referencesParentElement, groupEl);
                appendStringValueElement(DESCRIPTION_EL_NAME, group.getDescription(), referencesParentElement, groupEl);

                String value = group.getXmlProperties();
                if(value != null) {
                    value = HexUtils.encodeBase64(HexUtils.encodeUtf8(value));
                    appendStringValueElement(PROPS_EL_NAME, value, referencesParentElement, groupEl);
                }

                if(!isVirtual) {
                    Collection<IdentityHeader> groupMembers = getFinder().getUserHeaders(providerId, group.getId());
                    for(IdentityHeader groupMember : groupMembers) {
                        appendStringValueElement(GROUP_MEMBER_EL_NAME, groupMember.getStrId(), referencesParentElement, groupEl);
                    }
                }

                groupsEl.appendChild(groupEl);
            }

            if(groupHeaders.size() > 0) {
                refEl.appendChild(groupsEl);
            }

            Collection<IdentityHeader> userHeaders = getFinder().findAllUsers(providerId);

            Element usersEl = referencesParentElement.getOwnerDocument().createElement(USERS_EL_NAME);
            for(EntityHeader userHeader : userHeaders) {
                FederatedUser user = (FederatedUser) getFinder().findUserByID(providerId, userHeader.getStrId());
                Element userEl = referencesParentElement.getOwnerDocument().createElement(USER_EL_NAME);

                appendStringValueElement(GOID_EL_NAME, user.getId(), referencesParentElement, userEl);
                appendStringValueElement(NAME_EL_NAME, user.getName(), referencesParentElement, userEl);
                appendStringValueElement(USER_SUBJECT_DN_EL_NAME, user.getSubjectDn(), referencesParentElement, userEl);
                appendStringValueElement(USER_LOGIN_EL_NAME, user.getLogin(), referencesParentElement, userEl);
                appendStringValueElement(USER_EMAIL_EL_NAME, user.getEmail(), referencesParentElement, userEl);
                appendStringValueElement(USER_FIRST_NAME_EL_NAME, user.getFirstName(), referencesParentElement, userEl);
                appendStringValueElement(USER_LAST_NAME_EL_NAME, user.getLastName(), referencesParentElement, userEl);

                usersEl.appendChild(userEl);
            }

            if(userHeaders.size() > 0) {
                refEl.appendChild(usersEl);
            }
        } catch(FindException e) {
        }
    }

    @Override
    protected void localizeLoginOrIdForSpecificUser(SpecificUser su) throws FindException {
        if(userUpdateMap.containsKey(su.getUserUid())) {
            su.setUserUid(userUpdateMap.get(su.getUserUid()));
        } else {
            super.localizeLoginOrIdForSpecificUser(su);
        }
    }

    @Override
    protected void localizeLoginOrIdForSpecificGroup(MemberOfGroup mog) throws FindException {
        if(groupUpdateMap.containsKey(mog.getGroupId())) {
            mog.setGroupId(groupUpdateMap.get(mog.getGroupId()));
        } else {
            super.localizeLoginOrIdForSpecificGroup(mog);
        }
    }
}
