package com.l7tech.server.identity.ldap;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.User;
import com.l7tech.identity.ldap.*;
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

    /**
     * get group based on dn
     * @return an LdapGroup object, null if group dont exist
     */
    public Group findByPrimaryKey(String dn) throws FindException {
        DirContext context = null;
        try {
            try {
                context = LdapIdentityProvider.getBrowseContext(cfg);
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

    /**
     * does equivalent of LdapIdentityProvider.search(new EntityType[] {EntityType.GROUP}, name);
     */
    public Group findByName(String name) throws FindException {
        Collection res = parent.search(new EntityType[] {EntityType.GROUP}, name);
        switch (res.size()) {
            case 0:
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

    /**
     * throws an UnsupportedOperationException
     */
    public void delete(Group group) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void deleteAll(long ipoid) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public String save(Group group) throws SaveException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void update(Group group) throws UpdateException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public String save(Group group, Set userHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void update(Group group, Set userHeaders) throws UpdateException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    public Collection search( String searchString ) throws FindException {
        return parent.search(new EntityType[] { EntityType.GROUP }, searchString );
    }

    public Class getImpClass() {
        return LdapGroup.class;
    }

    /**
     * checks if a user is member of a group
     * @return true if user is member of group, false otherwise
     * @throws FindException
     */
    public boolean isMember(User user, Group group) throws FindException {
        Set userHeaders = getUserHeaders( group );
        for (Iterator i = userHeaders.iterator(); i.hasNext();) {
            EntityHeader header = (EntityHeader) i.next();
            String login = header.getName();
            if ( login != null && login.equals( user.getLogin() ) ) return true;
        }
        return false;
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void addUsers(Group group, Set users) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void removeUsers(Group group, Set users) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void addUser(User user, Set groups) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void removeUser(User user, Set groups) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void addUser(User user, Group group) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void removeUser(User user, Group group) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    /**
     * searches for the groups to which the passed user belong to
     * @return a collection containing EntityHeader objects
     */
    public Set getGroupHeaders(User user) throws FindException {
        GroupMappingConfig[] groupTypes = cfg.getGroupMappings();
        StringBuffer uberGroupMembershipFilter = new StringBuffer("(|");
        boolean checkOuStrategyToo = false;
        boolean somethingToSearchFor = false;
        for (int i = 0; i < groupTypes.length; i ++) {
            String grpclass = groupTypes[i].getObjClass();
            String mbmAttrName = groupTypes[i].getMemberAttrName();
            MemberStrategy memberstrategy = groupTypes[i].getMemberStrategy();
            if (memberstrategy.equals(MemberStrategy.MEMBERS_ARE_LOGIN)) {
                uberGroupMembershipFilter.append("(&" +"(objectClass=" + grpclass + ")");
                uberGroupMembershipFilter.append("(" + mbmAttrName + "=" + user.getLogin() + ")");
                uberGroupMembershipFilter.append(")");
                somethingToSearchFor = true;
            } else if (memberstrategy.equals(MemberStrategy.MEMBERS_BY_OU)) {
                checkOuStrategyToo = true;
                continue;
            } else {
                uberGroupMembershipFilter.append("(&" +"(objectClass=" + grpclass + ")");
                uberGroupMembershipFilter.append("(|");
                uberGroupMembershipFilter.append("(" + mbmAttrName + "=cn=" + user.getName() + ")");
                uberGroupMembershipFilter.append("(" + mbmAttrName + "=" + user.getUniqueIdentifier() + ")");
                uberGroupMembershipFilter.append(")");
                uberGroupMembershipFilter.append(")");
                somethingToSearchFor = true;
            }
        }
        uberGroupMembershipFilter.append(")");

        Set output = new HashSet();
        if (somethingToSearchFor) {
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = null;
            try {
                context = LdapIdentityProvider.getBrowseContext(cfg);
            } catch (NamingException e) {
                String msg = "cannot get context";
                logger.log(Level.WARNING, msg, e);
                throw new FindException(msg, e);
            }

            try {
                NamingEnumeration answer = null;
                try {
                    answer = context.search(cfg.getSearchBase(), uberGroupMembershipFilter.toString(), sc);
                } catch (NamingException e) {
                    String msg = "error getting answer";
                    logger.log(Level.WARNING, msg, e);
                    throw new FindException(msg, e);
                }

                try {
                    while (answer.hasMore()) {
                        SearchResult sr = (SearchResult)answer.next();
                        // set the dn (unique id)
                        String dn = sr.getName() + "," + cfg.getSearchBase();
                        //Attributes atts = sr.getAttributes();
                        EntityHeader header = parent.searchResultToHeader(sr, dn);
                        if (header != null) {
                            output.add(header);
                            // Groups within groups.
                            // check if this group refer to other groups. if so, then user belongs to those
                            // groups too.
                            output.addAll(getSubGroups(context, dn));
                        }
                    }
                } catch (NamingException e) {
                    String msg = "error getting next answer";
                    logger.log(Level.WARNING, msg, e);
                    throw new FindException(msg, e);
                } finally {
                    try {
                        answer.close();
                    } catch (NamingException e) {
                        logger.info("error closing answer " + e.getMessage());
                    }
                }
            } finally {
                try {
                    if ( context != null ) context.close();
                } catch (NamingException e) {
                    logger.info("error closing context " + e.getMessage());
                }
            }
        }

        if (checkOuStrategyToo) {
            // look for OU memberships
            String tmpdn = user.getUniqueIdentifier();
            int pos = 0;
            int res = tmpdn.indexOf("ou=", pos);
            while (res >= 0) {
                // is there a valid organizational unit there?
                Group maybegrp = null;
                try {
                    maybegrp = findByPrimaryKey(tmpdn.substring(res));
                } catch (FindException e) {
                    logger.finest("could not resolve this group " + e.getMessage());
                    maybegrp = null;
                }
                if (maybegrp != null) {
                    LdapGroup ldapgrp = (LdapGroup)maybegrp;
                    EntityHeader grpheader = new EntityHeader(ldapgrp.getDn(), EntityType.GROUP, ldapgrp.getCn(),
                                                ldapgrp.getDescription());
                    output.add(grpheader);
                }
                pos = res+2;
                res = tmpdn.indexOf("ou=", pos);
            }
        }
        return output;
    }

    /**
     * practical equivalent to getGroupHeaders(LdapUserManager.findByPrimaryKey(userId));
     */
    public Set getGroupHeaders(String userId) throws FindException {
        return getGroupHeaders( getUserManager().findByPrimaryKey( userId ) );
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void setGroupHeaders(User user, Set groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void setGroupHeaders(String userId, Set groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    /**
     * return a Set of groups headers for all the subgroups of the group whose dn is passed.
     * note: this is recursive
     *
     * @param dn the dn of the group to inspect
     * @return a collection containing EntityHeader objects
     */
    private Set getSubGroups(DirContext context, String dn) {
        Set output = new HashSet();
        String filter = subGroupSearchString(dn);
        if (filter != null) {
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration answer = null;
            try {
                answer = context.search(cfg.getSearchBase(), filter, sc);
                while (answer.hasMore()) {
                    // get this item
                    SearchResult sr = (SearchResult)answer.next();
                    // set the dn (unique id)
                    String subgroupdn = sr.getName() + "," + cfg.getSearchBase();
                    EntityHeader header = parent.searchResultToHeader(sr, subgroupdn);
                    if (header != null && header.getType().equals(EntityType.GROUP)) {
                        output.add(header);
                        output.addAll(getSubGroups(context, subgroupdn));
                    }
                }
            } catch (NamingException e) {
                logger.log(Level.WARNING, "naming exception with filter " + filter, e);
            } finally {
                try {
                    if (answer != null) answer.close();
                } catch (NamingException e) {
                    logger.log(Level.WARNING, "naming exception closing answer", e);
                }
            }
        }
        // look for sub-OU memberships
        int pos = 0;
        int res = dn.indexOf("ou=", pos);
        while (res >= 0) {
            // is there a valid organizational unit there?
            Group maybegrp = null;
            try {
                maybegrp = findByPrimaryKey(dn.substring(res));
            } catch (FindException e) {
                logger.finest("could not resolve this group " + e.getMessage());
                maybegrp = null;
            }
            if (maybegrp != null) {
                LdapGroup ldapgrp = (LdapGroup)maybegrp;
                EntityHeader grpheader = new EntityHeader(ldapgrp.getDn(), EntityType.GROUP, ldapgrp.getCn(),
                                            ldapgrp.getDescription());
                output.add(grpheader);
            }
            pos = res+2;
            res = dn.indexOf("ou=", pos);
        }
        return output;
    }

    /**
     * filter string for subgroup search
     * @return returns a search string or null if the group object classes dont allow for groups within groups
     */
    private String subGroupSearchString(String dnOfChildGroup) {
        StringBuffer output = new StringBuffer("(|");
        GroupMappingConfig[] groupTypes = cfg.getGroupMappings();
        boolean searchStringValid = false;
        for (int i = 0; i < groupTypes.length; i++) {
            if (groupTypes[i].getMemberStrategy().equals(MemberStrategy.MEMBERS_ARE_DN) ||
                groupTypes[i].getMemberStrategy().equals(MemberStrategy.MEMBERS_ARE_NVPAIR)) {
                searchStringValid = true;
                output.append("(&" +
                                "(objectClass=" + groupTypes[i].getObjClass() + ")" +
                                "(" + groupTypes[i].getMemberAttrName() + "=" + dnOfChildGroup + ")" +
                              ")");
            }
        }
        output.append(")");
        if (!searchStringValid) return null;

        return output.toString();
    }

    /**
     * get members for a group
     * @return a collection containing EntityHeader objects
     */
    public Set getUserHeaders(Group group) throws FindException {
        NamingEnumeration answer = null;
        DirContext context = null;

        try {
            Set headers = new HashSet();

            LdapGroup groupImp = (LdapGroup)group;
            String dn = groupImp.getDn();
            context = LdapIdentityProvider.getBrowseContext(cfg);
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
                                        } else {
                                            // GROUPS WITHIN GROUPS
                                            // fla note: the member hint might actually refer to another group
                                            // insted of a user
                                            LdapGroup subgroup = null;
                                            try {
                                                subgroup = (LdapGroup)findByPrimaryKey(memberhint);
                                            } catch (FindException e) {
                                                // nothing on purpose
                                                logger.finest("seems like " + memberhint +
                                                              " is not a group dn" + e.getMessage());
                                                subgroup = null;
                                            }
                                            if (subgroup == null) {
                                                try {
                                                    subgroup = (LdapGroup)findByName(memberhint);
                                                } catch (FindException e) {
                                                    // nothing on purpose
                                                    logger.finest("seems like " + memberhint +
                                                                  " is not a group name" + e.getMessage());
                                                    subgroup = null;
                                                }
                                            }
                                            if (subgroup != null) {
                                                // todo, prevent explosion
                                                // (what if some asshole has circular reference of groups?)
                                                // note. i dont think this is possible
                                                Set subgroupmembers = getUserHeaders(subgroup);
                                                headers.addAll(subgroupmembers);
                                            }
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

    /**
     * equivalent to getUserHeaders(findByPrimaryKey(groupId));
     */
    public Set getUserHeaders(String groupId) throws FindException {
        return getUserHeaders( findByPrimaryKey( groupId ) );
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void setUserHeaders(Group group, Set groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void setUserHeaders(String groupId, Set groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    /**
     * practical equivalent to LdapIdentityProvider.search(new EntityType[] {EntityType.GROUP}, "*");
     */
    public Collection findAllHeaders() throws FindException {
        return parent.search(new EntityType[] {EntityType.GROUP}, "*");
    }

    /**
     * throws an UnsupportedOperationException
     */
    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException();
    }

    /**
     * like findAllHeaders but contains actual LdapUser objects instead of EntityHeader objects
     */
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

    /**
     * like findAllHeaders but contains actual LdapUser objects instead of EntityHeader objects
     */
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

    public Integer getVersion( long oid ) throws FindException {
        return new Integer(0);
    }

    public Map findVersionMap() throws FindException {
        return Collections.EMPTY_MAP;
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
            String entitydn = null;
            SearchResult sr = (SearchResult)answer.next();
            entitydn = sr.getName() + "," + dn;
            EntityHeader header = parent.searchResultToHeader(sr, entitydn);
            memberHeaders.add(header);
        }
        if (answer != null) answer.close();

        filter = parent.groupSearchFilterWithParam("*");
        // use dn of group as base
        answer = context.search(dn, filter, sc);
        while (answer.hasMore()) {
            String entitydn = null;
            SearchResult sr = (SearchResult)answer.next();
            if (sr != null && sr.getName() != null && sr.getName().length() > 0) {
                entitydn = sr.getName() + "," + dn;
                try {
                    Group subGroup = this.findByPrimaryKey(entitydn);
                    if (subGroup != null) {
                        Set subGroupMembers = getUserHeaders(subGroup);
                        memberHeaders.addAll(subGroupMembers);
                    }
                } catch (FindException e) {
                    logger.log(Level.FINE, "error looking for sub-group" + entitydn, e);
                }
            }
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
    private final Logger logger = Logger.getLogger(getClass().getName());
    private LdapIdentityProvider parent;
}
