package com.l7tech.identity.ldap;

import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 * This manager class lists users in a ldap directory given a LdapIdentityProviderConfig object
 * This manager does not support save, update or delete.
 *
 */
public class LdapUserManagerServer extends LdapManager implements UserManager {

    public LdapUserManagerServer(LdapIdentityProviderConfig config) {
        super(config);
    }

    /**
     * Get a User object for the given dn
     *
     * @param dn the distinguished name to look for in the directory
     * @throws FindException
     */
    public User findByPrimaryKey(String dn) throws FindException {
        try {
            Attributes attributes = getAnonymousContext().getAttributes(dn);
            LdapUser out = new LdapUser();
            out.setDN(dn);
            Object tmp = extractOneAttributeValue(attributes, EMAIL_ATTR_NAME);
            if (tmp != null) out.setEmail(tmp.toString());
            tmp = extractOneAttributeValue(attributes, FIRSTNAME_ATTR_NAME);
            if (tmp != null) out.setFirstName(tmp.toString());
            tmp = extractOneAttributeValue(attributes, LASTNAME_ATTR_NAME);
            if (tmp != null) out.setLastName(tmp.toString());
            tmp = extractOneAttributeValue(attributes, LOGIN_ATTR_NAME);
            if (tmp != null) out.setLogin(tmp.toString());
            tmp = extractOneAttributeValue(attributes, NAME_ATTR_NAME);
            if (tmp != null) out.setName(tmp.toString());
            tmp = extractOneAttributeValue(attributes, PASSWD_ATTR_NAME);
            if (tmp != null) out.setPassword(tmp.toString());
            Collection groupHeaders = findGroupMembershipsAsHeaders(out);
            out.setGroupHeaders(new HashSet(groupHeaders));
            return out;
        } catch (NamingException e) {
            e.printStackTrace(System.err);
            throw new FindException(e.getMessage(), e);
        }
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public void delete(User user) throws DeleteException {
        throw new DeleteException("Not supported in LdapUserManagerServer");
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public long save(User user) throws SaveException {
        throw new SaveException("Not supported in LdapUserManagerServer");
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public void update(User user) throws UpdateException {
        throw new UpdateException("Not supported in LdapUserManagerServer");
    }

    public Collection findAllHeaders() throws FindException {
        Collection output = new ArrayList();
        if (config.getSearchBase() == null || config.getSearchBase().length() < 1) throw new FindException("No search base provided");
        try
        {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + USER_OBJCLASS + ")";
            SearchControls sc = new SearchControls();
            // String[] attrToReturn = {LOGIN_ATTR_NAME, NAME_ATTR_NAME};
            //answer = getAnonymousContext().search(config.getSearchBase(), null, attrToReturn);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            answer = getAnonymousContext().search(config.getSearchBase(), filter, sc);
            while (answer.hasMore())
            {
                String login = null;
                String dn = null;
                SearchResult sr = (SearchResult)answer.next();
                dn = sr.getName() + "," + config.getSearchBase();
                Attributes atts = sr.getAttributes();
                Object tmp = extractOneAttributeValue(atts, LOGIN_ATTR_NAME);
                if (tmp != null) login = tmp.toString();
                if (login != null && dn != null) {
                    EntityHeader header = new EntityHeader(dn, EntityType.USER, login, null);
                    output.add(header);
                }
            }
        }
        catch (NamingException e)
        {
            // if nothing can be found, just trace this exception and return empty collection
            e.printStackTrace(System.err);
        }
        return output;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        Collection output = new ArrayList();
        String[] attrToReturn = {LOGIN_ATTR_NAME, NAME_ATTR_NAME};

        if (config.getSearchBase() == null || config.getSearchBase().length() < 1) throw new FindException("No search base provided");
        try
        {
            int count = 0;
            NamingEnumeration answer = null;
            answer = getAnonymousContext().search(config.getSearchBase(), null, attrToReturn);
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
                String login = null;
                String dn = null;
                SearchResult sr = (SearchResult)answer.next();
                dn = sr.getName() + "," + config.getSearchBase();
                Attributes atts = sr.getAttributes();
                Object tmp = extractOneAttributeValue(atts, LOGIN_ATTR_NAME);
                if (tmp != null) login = tmp.toString();
                if (login != null && dn != null) {
                    EntityHeader header = new EntityHeader(dn, EntityType.USER, login, null);
                    output.add(header);
                }
            }
        }
        catch (NamingException e)
        {
            // if nothing can be found, just trace this exception and return empty collection
            e.printStackTrace(System.err);
        }
        return output;
    }

    public Collection findAll() throws FindException {
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
        Collection headers = findAllHeaders(offset, windowSize);
        Collection output = new ArrayList();
        Iterator i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    public static void main(String[] args) throws Exception {
        LdapIdentityProviderConfig config = new LdapIdentityProviderConfig();
        // use this url when ssh forwarding locally
        config.setLdapHostURL("ldap://localhost:3899");
        // use this url when in the office
        //config.setLdapHostURL("ldap://spock:389");
        config.setSearchBase("dc=layer7-tech,dc=com");
        LdapUserManagerServer me = new LdapUserManagerServer(config);

        Collection headers = me.findAllHeaders();
        Iterator i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            User usr = me.findByPrimaryKey(header.getStrId());
            System.out.println(usr);
        }

    }

    private Collection findGroupMembershipsAsHeaders(LdapUser user) {
        Collection out = new ArrayList();
        try
        {
            NamingEnumeration answer = null;
            String filter = "(" + GROUPOBJ_MEMBER_ATTR + "=" + user.getLogin() + ")";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            answer = getAnonymousContext().search(config.getSearchBase(), filter, sc);
            while (answer.hasMore())
            {
                SearchResult sr = (SearchResult)answer.next();

                String dn = null;
                String cn = null;
                String description = null;

                Attributes atts = sr.getAttributes();
                Object tmp = extractOneAttributeValue(atts, NAME_ATTR_NAME);
                if (tmp != null) cn = tmp.toString();
                tmp = extractOneAttributeValue(atts, DESCRIPTION_ATTR);
                if (tmp != null) description = tmp.toString();

                dn = sr.getName() + "," + config.getSearchBase();
                EntityHeader grpheader = new EntityHeader(dn, EntityType.USER, cn, description);
                out.add(grpheader);
            }
        }
        catch (NamingException e)
        {
            // if nothing can be found, just trace this exception and return empty collection
            e.printStackTrace(System.err);
        }
        return out;
    }

    // mappings for attribute names
    // these may become properties of the LdapIdentityProviderConfig
    private static final String LOGIN_ATTR_NAME = "uid";
    private static final String EMAIL_ATTR_NAME = "mail";
    private static final String FIRSTNAME_ATTR_NAME = "givenName";
    private static final String LASTNAME_ATTR_NAME = "sn";
    private static final String NAME_ATTR_NAME = "cn";
    private static final String PASSWD_ATTR_NAME = "userPassword";
    private static final String USER_OBJCLASS = "inetOrgPerson";
    private static final String GROUPOBJ_MEMBER_ATTR = "memberUid";
    private static final String DESCRIPTION_ATTR = "description";
}
