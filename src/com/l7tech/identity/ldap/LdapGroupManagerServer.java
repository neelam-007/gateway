package com.l7tech.identity.ldap;

import com.l7tech.identity.GroupManager;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.logging.LogManager;

import javax.naming.directory.*;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.logging.Level;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 * List groups in a directory including the member users.
 * This version assumes usage of the posixGroup object class in the directory.
 * This member users are in the memberUid attributes.
 *  
 */
public class LdapGroupManagerServer extends LdapManager implements GroupManager {

    public LdapGroupManagerServer(IdentityProviderConfig config) {
        super(config);
    }

    public Group findByPrimaryKey(String dn) throws FindException {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "invalid group manager");
            throw new FindException("invalid manager");
        }
        try {
            DirContext context = getAnonymousContext();
            Attributes attributes = context.getAttributes(dn);
            Group out = new Group();
            out.setProviderId(config.getOid());
            out.setName(dn);
            Object tmp = extractOneAttributeValue(attributes, DESCRIPTION_ATTR);
            if (tmp != null) out.setDescription(tmp.toString());
            // this would override the dn
            // tmp = extractOneAttributeValue(attributes, NAME_ATTR_NAME);
            // if (tmp != null) out.setName(tmp.toString());

            // create a header for all "memberUid" attributes
            Attribute valuesWereLookingFor = attributes.get(GROUPOBJ_MEMBER_ATTR);
            if (valuesWereLookingFor != null) {
                HashSet membersSet = new HashSet();
                for (int i = 0; i < valuesWereLookingFor.size(); i++) {
                    Object val = valuesWereLookingFor.get(i);
                    if (val != null) {
                        String memberUid = val.toString();
                        EntityHeader userHeader = getUserHeaderFromUid(memberUid);
                        if (userHeader != null) {
                            membersSet.add(userHeader);
                        }
                    }
                }
                out.setMemberHeaders(membersSet);
            }

            context.close();
            return out;
        } catch (NamingException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new FindException(e.getMessage(), e);
        }
    }

    public void delete(Group group) throws DeleteException {
        throw new DeleteException("Not supported in LdapGroupManagerServer");
    }

    public long save(Group group) throws SaveException {
        throw new SaveException("Not supported in LdapGroupManagerServer");
    }

    public void update(Group group) throws UpdateException {
        throw new UpdateException("Not supported in LdapGroupManagerServer");
    }

    public Collection findAllHeaders() throws FindException {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "invalid group manager");
            throw new FindException("invalid manager");
        }
        Collection output = new ArrayList();
        if (config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) == null || config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE).length() < 1) throw new FindException("No search base provided");
        try
        {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + GROUP_OBJCLASS + ")";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = getAnonymousContext();
            answer = context.search(config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            while (answer.hasMore())
            {
                String dn = null;
                String cn = null;
                SearchResult sr = (SearchResult)answer.next();
                dn = sr.getName() + "," + config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                Attributes atts = sr.getAttributes();
                Object tmp = extractOneAttributeValue(atts, NAME_ATTR_NAME);
                if (tmp != null) cn = tmp.toString();

                if (cn != null && dn != null) {
                    EntityHeader header = new EntityHeader(dn, EntityType.GROUP, cn, null);
                    output.add(header);
                }
            }
            if (answer != null) answer.close();
            context.close();
        }
        catch (NamingException e)
        {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new FindException(e.getMessage(), e);
        }
        return output;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "invalid group manager");
            throw new FindException("invalid manager");
        }
        Collection output = new ArrayList();
        if (config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) == null || config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE).length() < 1) throw new FindException("No search base provided");
        try
        {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + GROUP_OBJCLASS + ")";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = getAnonymousContext();
            answer = context.search(config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            int count = 0;
            while (answer.hasMore())
            {
                if (count < offset) {
                    ++count;
                    continue;
                }
                if (count >= offset+windowSize) {
                    break;
                }
                ++count;
                String dn = null;
                String cn = null;
                SearchResult sr = (SearchResult)answer.next();
                dn = sr.getName() + "," + config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                Attributes atts = sr.getAttributes();
                Object tmp = extractOneAttributeValue(atts, NAME_ATTR_NAME);
                if (tmp != null) cn = tmp.toString();

                if (cn != null && dn != null) {
                    EntityHeader header = new EntityHeader(dn, EntityType.GROUP, cn, null);
                    output.add(header);
                }
            }
            if (answer != null) answer.close();
            context.close();
        }
        catch (NamingException e)
        {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new FindException(e.getMessage(), e);
        }
        return output;
    }

    public Collection findAll() throws FindException {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "invalid group manager");
            throw new FindException("invalid manager");
        }
        Collection headers = findAllHeaders();
        Collection output = new ArrayList();
        Iterator i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "invalid group manager");
            throw new FindException("invalid manager");
        }
        Collection headers = findAllHeaders(offset, windowSize);
        Collection output = new ArrayList();
        Iterator i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    public void invalidate() {
        valid = false;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private EntityHeader getUserHeaderFromUid(String uid) throws NamingException {
        NamingEnumeration answer = null;
        String filter = "(&(objectclass=" + USER_OBJCLASS + ")(" + LOGIN_ATTR_NAME + "=" + uid + "))";
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        DirContext context = getAnonymousContext();
        answer = context.search(config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
        while (answer.hasMore())
        {
            String login = null;
            String dn = null;
            SearchResult sr = (SearchResult)answer.next();
            dn = sr.getName() + "," + config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
            Attributes atts = sr.getAttributes();
            Object tmp = extractOneAttributeValue(atts, LOGIN_ATTR_NAME);
            if (tmp != null) login = tmp.toString();
            if (login != null && dn != null) {
                EntityHeader header = new EntityHeader(dn, EntityType.USER, login, null);
                answer.close();
                context.close();
                return header;
            }
        }
        answer.close();
        context.close();
        return null;
    }

    private static final String GROUP_OBJCLASS = "posixGroup";
    private volatile boolean valid = true;
}
