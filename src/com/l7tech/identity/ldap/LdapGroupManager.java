package com.l7tech.identity.ldap;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.User;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GroupManager for ldap identity provider.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 * $Id$<br/>
 *
 */
public class LdapGroupManager implements GroupManager {
    public LdapGroupManager(LdapIdentityProviderConfig cfg, LdapIdentityProvider daddy) {
        this.cfg = cfg;
        this.parent = daddy;
    }

    public Group findByPrimaryKey(String dn) throws FindException {
        DirContext context = null;
        try {
            try {
                context = parent.getBrowseContext(cfg);
                Attributes attributes = context.getAttributes(dn);

                GroupMappingConfig[] groupTypes = cfg.getGroupMappings();
                Attribute objectclasses = attributes.get("objectclass");
                for (int i = 0; i < groupTypes.length; i ++) {
                    String grpclass = groupTypes[i].getObjClass();
                    if (LdapIdentityProvider.attrContainsCaseIndependent(objectclasses, grpclass)) {
                        LdapGroup out = new LdapGroup();
                        out.setProviderId(cfg.getOid());
                        out.setDn(dn);
                        Object tmp = LdapIdentityProvider.extractOneAttributeValue(attributes,
                                       groupTypes[i].getNameAttrName());
                        if (tmp != null) out.setCn(tmp.toString());
                        return out;
                    }
                }
                return null;
            } finally {
                if ( context != null ) context.close();
            }
        } catch (NamingException e) {
            logger.log(Level.WARNING, "error building group", e);
            throw new FindException("naming exception", e);
        }
    }

    public Group findByName(String name) throws FindException {
        Collection res = parent.search(new EntityType[] {EntityType.GROUP}, name);
        switch (res.size()) {
            case 0:
                logger.finest("no group found with name " + name);
                return null;
            case 1:
                EntityHeader header = (EntityHeader)res.iterator().next();
                return findByPrimaryKey(header.getStrId());
            default:
                // return 1st one
                logger.warning("search on group name returned more than one " +
                               "result. returning 1st one");
                header = (EntityHeader)res.iterator().next();
                return findByPrimaryKey(header.getStrId());
        }
    }

    public void delete(Group group) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    public String save(Group group) throws SaveException {
        throw new UnsupportedOperationException();
    }

    public void update(Group group) throws UpdateException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    public String save(Group group, Set userHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    public void update(Group group, Set userHeaders) throws UpdateException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
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

    public void addUsers(Group group, Set users) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void removeUsers(Group group, Set users) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void addUser(User user, Set groups) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void removeUser(User user, Set groups) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void addUser(User user, Group group) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void removeUser(User user, Group group) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public Set getGroupHeaders(User user) throws FindException {
        // todo : optimize this!
        Collection allgroups = findAll();
        Set output = new HashSet();
        for (Iterator i = allgroups.iterator(); i.hasNext();) {
            Group agrp = (Group)i.next();
            if (isMember(user, agrp)) {
                output.add(new EntityHeader(agrp.getUniqueIdentifier(), EntityType.GROUP, agrp.getName(),
                            agrp.getDescription()));
            }
        }
        return output;
    }

    public Set getGroupHeaders(String userId) throws FindException {
        return getGroupHeaders( getUserManager().findByPrimaryKey( userId ) );
    }

    public void setGroupHeaders(User user, Set groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void setGroupHeaders(String userId, Set groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public Set getUserHeaders(Group group) throws FindException {
        NamingEnumeration answer = null;
        DirContext context = null;

        try {
            Set headers = new HashSet();

            LdapGroup groupImp = (LdapGroup)group;
            String dn = groupImp.getDn();
            context = parent.getBrowseContext(cfg);
            Attributes attributes = context.getAttributes(dn);

            GroupMappingConfig[] groupTypes = cfg.getGroupMappings();
            Attribute objectclasses = attributes.get("objectclass");
            for (int i = 0; i < groupTypes.length; i ++) {
                String grpclass = groupTypes[i].getObjClass();
                if (LdapIdentityProvider.attrContainsCaseIndependent(objectclasses, grpclass)) {
                    if (groupTypes[i].getMemberStrategy().equals(MemberStrategy.MEMBERS_BY_OU)) {
                        collectOUGroupMembers(context, dn, headers);
                    } else if (groupTypes[i].getMemberStrategy().equals(MemberStrategy.MEMBERS_ARE_LOGIN)) {
                        Attribute memberAttribute = attributes.get(groupTypes[i].getMemberAttrName());
                        if (memberAttribute != null) {
                            for (int ii = 0; ii < memberAttribute.size(); ii++) {
                                Object val = memberAttribute.get(ii);
                                if (val != null) {
                                    String memberlogin = val.toString();
                                    User u = getUserManager().findByLogin(memberlogin);
                                    if (u != null) {
                                        headers.add(new EntityHeader(u.getName(), EntityType.USER, u.getLogin(), null));
                                    }
                                }
                            }
                        }
                    } else {
                        Attribute memberAttribute = attributes.get(groupTypes[i].getMemberAttrName());
                        // for some reason, oracle directory specifies his members more than one (?!)
                        if (memberAttribute != null) {
                            for (int ii = 0; ii < memberAttribute.size(); ii++) {
                                Object val = memberAttribute.get(ii);
                                if (val != null) {
                                    // nv pairs or dn. which is it?
                                    String memberhint = val.toString();
                                    // try to get user through dn
                                    boolean done = false;
                                    try {
                                        LdapUser u = (LdapUser)getUserManager().findByPrimaryKey(memberhint);
                                        if (u != null) {
                                            EntityHeader newheader =
                                                    new EntityHeader(u.getDn(), EntityType.USER, u.getLogin(), null);
                                            if (!headers.contains(newheader))
                                                headers.add(newheader);
                                            done = true;
                                        }
                                    } catch (FindException e) {
                                        logger.log(Level.FINEST, "cannot resolve user through dn, try nv" +
                                                                    "pair method", e);
                                    }
                                    // if that dont work, try search assuming nv pair
                                    if (!done) {
                                        nvSearchForUser(context, memberhint, headers);
                                    }
                                }
                            }
                        }
                    }
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

    public Set getUserHeaders(String groupId) throws FindException {
        return getUserHeaders( findByPrimaryKey( groupId ) );
    }

    public void setUserHeaders(Group group, Set groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void setUserHeaders(String groupId, Set groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public Collection findAllHeaders() throws FindException {
        return parent.search(new EntityType[] {EntityType.GROUP}, "*");
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException();
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

    protected LdapUserManager getUserManager() {
        return (LdapUserManager)parent.getUserManager();
    }

    private void collectOUGroupMembers( DirContext context, String dn, Set memberHeaders) throws NamingException {
        // build group memberships
        NamingEnumeration answer = null;
        String filter = parent.userSearchFilterWithParam("*");
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        // use dn of group as base
        answer = context.search(dn, filter, sc);
        while (answer.hasMore()) {
            String userdn = null;
            SearchResult sr = (SearchResult)answer.next();
            userdn = sr.getName() + "," + dn;
            memberHeaders.add(parent.searchResultToHeader(sr, userdn));
        }

        if (answer != null) answer.close();
    }

    private void nvSearchForUser(DirContext context, String nvhint, Set memberHeaders) throws NamingException {
        StringBuffer filter = new StringBuffer("(|");
        UserMappingConfig[] userTypes = cfg.getUserMappings();
        for (int i = 0; i < userTypes.length; i++) {
            filter.append("(&" +
                            "(objectClass=" + userTypes[i].getObjClass() + ")" +
                            "(" + nvhint+ ")" +
                          ")");
        }
        filter.append(")");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration answer = null;
        try {
            answer = context.search(cfg.getSearchBase(), filter.toString(), sc);
        } catch (OperationNotSupportedException e) {
            // this gets thrown by oid for some weird groups
            logger.log(Level.FINE, cfg.getName() + "directory cannot search on" + nvhint, e);
            return;
        }
        while (answer.hasMore()) {
            String userdn = null;
            SearchResult sr = (SearchResult)answer.next();
            userdn = sr.getName() + "," + cfg.getSearchBase();
            EntityHeader newheader = parent.searchResultToHeader(sr, userdn);
            if (!memberHeaders.contains(newheader))
                memberHeaders.add(newheader);
        }
    }


    private LdapIdentityProviderConfig cfg;
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    private LdapIdentityProvider parent;
}
