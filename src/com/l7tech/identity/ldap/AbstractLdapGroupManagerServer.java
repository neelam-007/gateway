/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.ldap;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.logging.LogManager;

import javax.naming.directory.*;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AbstractLdapGroupManagerServer extends LdapManager implements GroupManager {
    public AbstractLdapGroupManagerServer( IdentityProviderConfig config ) {
        super( config );
    }

    protected String groupMemberToLogin( String member ) {
        return member;
    }

    protected abstract AbstractLdapConstants getConstants();

    protected UserManager getUserManager() {
        IdentityProvider provider = IdentityProviderFactory.makeProvider(config);
        return provider.getUserManager();
    }

    protected abstract User getUserFromGroupMember( String member ) throws FindException;

    public Group findByPrimaryKey(String dn) throws FindException {
        if (!valid) {
            logger.severe("invalid group manager");
            throw new FindException("invalid manager");
        }
        try {
            DirContext context = getBrowseContext();
            Attributes attributes = context.getAttributes(dn);
            AbstractLdapConstants constants = getConstants();
            Group out = new Group();
            out.setProviderId(config.getOid());
            out.setName(dn);
            Object tmp = extractOneAttributeValue(attributes, constants.descriptionAttribute() );
            if (tmp != null) out.setDescription(tmp.toString());
            // this would override the dn
            // tmp = extractOneAttributeValue(attributes, NAME_ATTR_NAME);
            // if (tmp != null) out.setName(tmp.toString());

            // create a header for all "memberUid" attributes
            Attribute valuesWereLookingFor = attributes.get( constants.groupMemberAttribute() );
            if (valuesWereLookingFor != null) {
                Set memberHeaders = new HashSet();
                Set members = new HashSet();
                User u;
                for (int i = 0; i < valuesWereLookingFor.size(); i++) {
                    Object val = valuesWereLookingFor.get(i);
                    if (val != null) {
                        String member = val.toString();
                        u = getUserFromGroupMember( member );
                        if (u != null) {
                            memberHeaders.add( getUserManager().userToHeader(u) );
                            members.add( u );
                        }
                    }
                }
                out.setMembers( members );
                out.setMemberHeaders(memberHeaders);
            }
            context.close();
            return out;
        } catch (NamingException e) {
            logger.log(Level.SEVERE, null, e);
            throw new FindException(e.getMessage(), e);
        }
    }

    public Group findByName(String name) throws FindException {
        // ldap group names are their dn
        //StringBuffer dn = new StringBuffer( config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) );
        //dn.append( ",cn=" );
        //dn.append( name );
        return findByPrimaryKey( name );
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

    public EntityHeader groupToHeader(Group group) {
        return new EntityHeader(group.getName(), EntityType.GROUP, group.getName(), null);
    }

    public Group headerToGroup(EntityHeader header) throws FindException {
        return findByPrimaryKey(header.getStrId());
    }

    public Collection findAllHeaders() throws FindException {
        AbstractLdapConstants constants = getConstants();
        if (!valid) {
            logger.severe("invalid group manager");
            throw new FindException("invalid manager");
        }
        Collection output = new ArrayList();
        if (config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) == null ||
            config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE).length() < 1) {
            throw new FindException("No search base provided");
        }
        try
        {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + constants.groupObjectClass() + ")";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = getBrowseContext();
            answer = context.search(config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            while (answer.hasMore())
            {
                String dn = null;
                String cn = null;
                SearchResult sr = (SearchResult)answer.next();
                dn = sr.getName() + "," + config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                Attributes atts = sr.getAttributes();
                Object tmp = extractOneAttributeValue(atts, constants.userNameAttribute() );
                if (tmp != null) cn = tmp.toString();

                if (cn != null && dn != null) {
                    EntityHeader header = new EntityHeader(dn, EntityType.GROUP, dn, null);
                    output.add(header);
                }
            }
            if (answer != null) answer.close();
            context.close();
        } catch (NamingException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return output;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        AbstractLdapConstants constants = getConstants();
        if (!valid) {
            logger.severe("invalid group manager");
            throw new FindException("invalid manager");
        }
        Collection output = new ArrayList();
        if (config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) == null ||
            config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE).length() < 1) {
            throw new FindException("No search base provided");
        }
        try
        {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + constants.groupObjectClass() + ")";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = getBrowseContext();
            answer = context.search(config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            int count = 0;
            while (answer.hasMore()) {
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
                Object tmp = extractOneAttributeValue(atts, constants.userNameAttribute() );
                if (tmp != null) cn = tmp.toString();

                if (cn != null && dn != null) {
                    EntityHeader header = new EntityHeader(dn, EntityType.GROUP, cn, null);
                    output.add(header);
                }
            }
            if (answer != null) answer.close();
            context.close();
        } catch (NamingException e) {
            logger.log(Level.SEVERE, null, e);
        }
        return output;
    }

    public Collection findAll() throws FindException {
        if (!valid) {
            logger.severe("invalid group manager");
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
            logger.severe("invalid group manager");
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

    private volatile boolean valid = true;
    private Logger logger = LogManager.getInstance().getSystemLogger();
}
