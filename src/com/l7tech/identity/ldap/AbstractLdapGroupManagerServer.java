/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.ldap;

import com.l7tech.identity.*;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;

import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AbstractLdapGroupManagerServer implements GroupManager {
    public AbstractLdapGroupManagerServer( IdentityProviderConfig config ) {
        _ldapManager = new LdapManager( config );
        _config = config;
    }

    protected abstract AbstractLdapConstants getConstants();

    protected UserManager getUserManager() {
        IdentityProvider provider = IdentityProviderFactory.makeProvider( _config );
        return provider.getUserManager();
    }

    protected abstract User getUserFromGroupMember( String member ) throws FindException;

    public Group findByPrimaryKey(String dn) throws FindException {
        if (!valid) {
            logger.severe("invalid group manager");
            throw new FindException("invalid manager");
        }
        try {
            // if the group is a posixGroup, use standard method
            if (dn.toLowerCase().indexOf(getConstants().groupNameAttribute().toLowerCase() + "=") >= 0) {
                return buildSelfDescriptiveGroup(dn);
            } else {
                // otherwise, use the OU method for building a group
                return buildOUGroup(dn);
            }
        } catch ( NamingException ne ) {
            throw new FindException( ne.getMessage(), ne );
        }
    }

    protected abstract String doGetGroupMembershipFilter( LdapUser user );

    public Set getGroupHeaders( User user ) throws FindException {
        LdapUser userImp = (LdapUser)user;
        AbstractLdapConstants constants = getConstants();
        NamingEnumeration answer = null;
        DirContext context = null;
        Set headers = new HashSet();
        try {
            String filter = doGetGroupMembershipFilter( userImp );
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            context = _ldapManager.getBrowseContext();
            answer = context.search(_config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);

            while (answer.hasMore()) {
                SearchResult sr = (SearchResult)answer.next();

                String dn = null;
                String cn = null;
                String description = null;

                Attributes atts = sr.getAttributes();
                Object tmp = _ldapManager.extractOneAttributeValue(atts, constants.userNameAttribute() );
                if (tmp != null) cn = tmp.toString();
                tmp = _ldapManager.extractOneAttributeValue(atts, constants.descriptionAttribute() );
                if (tmp != null) description = tmp.toString();

                dn = sr.getName() + "," + _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);

                EntityHeader grpheader = new EntityHeader(dn, EntityType.GROUP, cn, description);
                headers.add(grpheader);
            }
        } catch (NamingException e) {
            // if nothing can be found, just trace this exception and return empty collection
            logger.log(Level.SEVERE, null, e);
        } finally {
            try {
                if ( answer != null) answer.close();
                if ( context != null ) context.close();
            } catch ( NamingException ne ) {
                throw new FindException( ne.getMessage(), ne );
            }
        }
        return headers;
    }


    public Collection findAllHeaders() throws FindException {
        Collection res = buildSelfDescriptiveGroupHeaders();
        // list the OU based groups
        try {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + getConstants().oUObjClassName() + ")";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = _ldapManager.getBrowseContext();
            answer = context.search( _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            while (answer.hasMore())
            {
                EntityHeader header = headerFromOuSearchResult((SearchResult)answer.next());
                if (header != null) {
                    res.add(header);
                }
            }
        } catch (NamingException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return res;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        Collection res = buildSelfDescriptiveGroupHeaders(offset, windowSize);
        if (res.size() < windowSize) {
            int sofar = res.size();
            try {
                NamingEnumeration answer = null;
                String filter = "(objectclass=" + getConstants().oUObjClassName() + ")";
                SearchControls sc = new SearchControls();
                sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
                DirContext context = _ldapManager.getBrowseContext();
                answer = context.search(_config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
                while (answer.hasMore())
                {
                    EntityHeader header = headerFromOuSearchResult((SearchResult)answer.next());
                    if (header != null) {
                        res.add(header);
                        ++sofar;
                        if (sofar >= windowSize) break;
                    }
                }
            } catch (NamingException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return res;
    }

    public EntityHeader headerFromOuSearchResult(SearchResult sr) throws NamingException {
        Attributes atts = sr.getAttributes();
        Object tmp = _ldapManager.extractOneAttributeValue(atts, getConstants().oUObjAttrName());
        String ou = null;
        if (tmp != null) ou = tmp.toString();

        if (ou != null && ou != null) {
            String dn = sr.getName() + "," + _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
            EntityHeader header = new EntityHeader(dn, EntityType.GROUP, dn, null);
            return header;
        }
        return null;
    }

    public boolean isMember(User user, Group group) throws FindException {
        Set userHeaders = getUserHeaders( group );
        for (Iterator i = userHeaders.iterator(); i.hasNext();) {
            EntityHeader header = (EntityHeader) i.next();
            String login = header.getName();
            if ( login != null && login.equals( user.getLogin() ) ) return true;
        }
        return false;
    }

    public Set getUserHeaders( Group group ) throws FindException {
        NamingEnumeration answer = null;
        DirContext context = null;

        try {
            Set headers = new HashSet();

            LdapGroup groupImp = (LdapGroup)group;
            String dn = groupImp.getDn();
            context = _ldapManager.getBrowseContext();
            AbstractLdapConstants constants = getConstants();
            User user;

            if (dn.toLowerCase().indexOf(getConstants().groupNameAttribute().toLowerCase() + "=") < 0) {
                // It's an OU

                String filter = "(objectclass=" + constants.userObjectClass() + ")";
                SearchControls sc = new SearchControls();
                sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

                // use dn of group as base
                answer = context.search(dn, filter, sc);
                while (answer.hasMore()) {
                    String login = null;
                    String userdn = null;
                    SearchResult sr = (SearchResult)answer.next();
                    userdn = sr.getName() + "," + dn;
                    Attributes atts = sr.getAttributes();
                    Object tmp = _ldapManager.extractOneAttributeValue(atts, constants.userLoginAttribute());
                    if (tmp != null) login = tmp.toString();
                    if (login != null && userdn != null) {
                        user = getUserManager().findByPrimaryKey(userdn);
                        headers.add( new EntityHeader( user.getUniqueIdentifier(), EntityType.USER, user.getLogin(), null ) );
                    }
                }
            } else {
                // Regular group
                Attributes attributes = context.getAttributes(dn);
                Attribute valuesWereLookingFor = attributes.get( constants.groupMemberAttribute() );
                if (valuesWereLookingFor == null) return headers;
                for (int i = 0; i < valuesWereLookingFor.size(); i++) {
                    Object val = valuesWereLookingFor.get(i);
                    if (val == null) continue;

                    String member = val.toString();
                    user = getUserFromGroupMember( member );
                    if (user != null) headers.add( new EntityHeader( user.getUniqueIdentifier(), EntityType.USER, user.getName(), null ) );
                }
            }

            return headers;
        } catch ( NamingException ne ) {
            logger.log(Level.SEVERE, null, ne);
            throw new FindException( ne.getMessage(), ne );
        } finally {
            try {
                if ( answer != null ) answer.close();
                if ( context != null ) context.close();
            } catch ( NamingException ne ) {
                throw new FindException( ne.getMessage(), ne );
            }
        }
    }

    public Set getGroupHeaders(String userId) throws FindException {
        return getGroupHeaders( getUserManager().findByPrimaryKey( userId ) );
    }

    public void setGroupHeaders(User user, Set groupHeaders) throws FindException, UpdateException {
        throw new FindException( UNSUPPORTED );
    }

    public void setGroupHeaders(String userId, Set groupHeaders) throws FindException, UpdateException {
        throw new FindException( UNSUPPORTED );
    }

    public Set getUserHeaders(String groupId) throws FindException {
        return getUserHeaders( findByPrimaryKey( groupId ) );
    }

    public void setUserHeaders(Group group, Set groupHeaders) throws FindException, UpdateException {
        throw new FindException( UNSUPPORTED );
    }

    public void setUserHeaders(String groupId, Set groupHeaders) throws FindException, UpdateException {
        throw new FindException( UNSUPPORTED );
    }

    private Group buildOUGroup(String dn) throws NamingException {
        DirContext context = null;
        try {
            context = _ldapManager.getBrowseContext();

            NameParser np = context.getNameParser( dn );
            Name name = np.parse( dn );
            String ou = null;
            for (int i = 0; i < name.size(); i++) {
                String part = name.get(i);
                int epos = part.indexOf("=");
                if ( epos >= 0 )
                    ou = part.substring(epos+1).trim();
                else
                    continue;
            }

            LdapGroup out = new LdapGroup();
            out.setProviderId(_config.getOid());
            out.setDn(dn);
            out.setCn(ou);
            return out;
        } finally {
            if ( context != null ) context.close();
        }
    }

    /**
     * Builds a group object from a ldap objectClass that lists its members as part of its attributes
     * @param dn dn of the group
     * @return
     */
    protected Group buildSelfDescriptiveGroup(String dn) throws NamingException {
        DirContext context = null;
        try {
            context = _ldapManager.getBrowseContext();
            Attributes attributes = context.getAttributes(dn);
            AbstractLdapConstants constants = getConstants();

            LdapGroup out = new LdapGroup();
            out.setProviderId(_config.getOid());
            out.setDn(dn);
            Object tmp = _ldapManager.extractOneAttributeValue(attributes, constants.groupNameAttribute() );
            if ( tmp != null ) out.setCn( tmp.toString() );
            tmp = _ldapManager.extractOneAttributeValue(attributes, constants.descriptionAttribute() );
            if (tmp != null) out.setDescription(tmp.toString());
            return out;
        } finally {
            if ( context != null ) context.close();
        }
    }

    public Group findByName(String name) throws FindException {
        // ldap group names are their dn
        //StringBuffer dn = new StringBuffer( config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) );
        //dn.append( ",cn=" );
        //dn.append( name );
        return findByPrimaryKey( name );
    }

    public EntityHeader groupToHeader(Group group) {
        return new EntityHeader(group.getName(), EntityType.GROUP, group.getName(), null);
    }

    public Group headerToGroup(EntityHeader header) throws FindException {
        return findByPrimaryKey(header.getStrId());
    }


    /**
     * builds a collection of group headers for all self descriptive groups
     */
    protected Collection buildSelfDescriptiveGroupHeaders() throws FindException {
        AbstractLdapConstants constants = getConstants();
        if (!valid) {
            logger.severe("invalid group manager");
            throw new FindException("invalid manager");
        }
        Collection output = new ArrayList();
        if (_config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) == null ||
            _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE).length() < 1) {
            throw new FindException("No search base provided");
        }
        try
        {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + constants.groupObjectClass() + ")";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = _ldapManager.getBrowseContext();
            answer = context.search(_config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            while (answer.hasMore())
            {
                String dn = null;
                String cn = null;
                SearchResult sr = (SearchResult)answer.next();
                dn = sr.getName() + "," + _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                Attributes atts = sr.getAttributes();
                Object tmp = _ldapManager.extractOneAttributeValue(atts, constants.userNameAttribute() );
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

    /**
     * builds a collection of group headers for all self descriptive groups
     */
    protected Collection buildSelfDescriptiveGroupHeaders(int offset, int windowSize) throws FindException {
        AbstractLdapConstants constants = getConstants();
        if (!valid) {
            logger.severe("invalid group manager");
            throw new FindException("invalid manager");
        }
        Collection output = new ArrayList();
        if (_config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) == null ||
            _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE).length() < 1) {
            throw new FindException("No search base provided");
        }
        try
        {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + constants.groupObjectClass() + ")";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = _ldapManager.getBrowseContext();
            answer = context.search(_config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
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
                dn = sr.getName() + "," + _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                Attributes atts = sr.getAttributes();
                Object tmp = _ldapManager.extractOneAttributeValue(atts, constants.userNameAttribute() );
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

    public void delete(String identifier) throws DeleteException {
        throw new UnsupportedOperationException( UNSUPPORTED );
    }

    public String save(GroupBean group) throws SaveException {
        throw new UnsupportedOperationException( UNSUPPORTED );
    }

    public void update(GroupBean group) throws UpdateException {
        throw new UnsupportedOperationException( UNSUPPORTED );
    }

    public void addUsers(Group group, Set users) throws FindException, UpdateException {
        throw new UpdateException( UNSUPPORTED );
    }

    public void removeUsers(Group group, Set users) throws FindException, UpdateException {
        throw new UpdateException( UNSUPPORTED );
    }

    public void addUser(User user, Set groups) throws FindException, UpdateException {
        throw new UpdateException( UNSUPPORTED );
    }

    public void removeUser(User user, Set groups) throws FindException, UpdateException {
        throw new UpdateException( UNSUPPORTED );
    }

    public void addUser(User user, Group group) throws FindException, UpdateException {
        throw new UpdateException( UNSUPPORTED );
    }

    public void removeUser(User user, Group group) throws FindException, UpdateException {
        throw new UpdateException( UNSUPPORTED );
    }

    public void delete(Group group) throws DeleteException {
        throw new DeleteException( UNSUPPORTED );
    }

    public String save(Group group) throws SaveException {
        throw new SaveException( UNSUPPORTED );
    }

    public String save( Group group, Set userHeaders ) throws SaveException {
        throw new SaveException( UNSUPPORTED );
    }

    public void update(Group group) throws UpdateException {
        throw new UpdateException( UNSUPPORTED );
    }

    public void update(Group group, Set userHeaders) throws UpdateException {
        throw new UpdateException( UNSUPPORTED );
    }

    private volatile boolean valid = true;
    private LdapManager _ldapManager;
    protected Logger logger = LogManager.getInstance().getSystemLogger();
    private IdentityProviderConfig _config;
    public static final String UNSUPPORTED = "This operation is not supported in an LDAP-style GroupManager!";

}
