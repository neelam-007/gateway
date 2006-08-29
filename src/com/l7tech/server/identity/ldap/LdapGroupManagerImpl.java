package com.l7tech.server.identity.ldap;

import com.l7tech.identity.ldap.*;
import com.l7tech.identity.GroupBean;
import com.l7tech.objectmodel.*;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GroupManager for ldap identity provider.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 * $Id$<br/>
 */
public class LdapGroupManagerImpl implements LdapGroupManager {
    private final IdentityHeader MAX_EXCEEDED;
    private final long providerOid;

    public LdapGroupManagerImpl(LdapIdentityProvider identityProvider) {
        if (identityProvider == null) {
            throw new IllegalArgumentException("Identity Provider is required");
        }

        this.identityProvider = identityProvider;
        ldapIdentityProviderConfig = (LdapIdentityProviderConfig)identityProvider.getConfig();

        MAX_EXCEEDED = new IdentityHeader(identityProvider.getConfig().getOid(),
            "noid", EntityType.MAXED_OUT_SEARCH_RESULT, "Too Many Entries",
            "This search yields too many entities. Please narrow your search criterion.");
        providerOid = identityProvider.getConfig().getOid();
    }

    /**
     * get group based on dn
     *
     * @return an LdapGroup object, null if group dont exist
     */
    public LdapGroup findByPrimaryKey(String dn) throws FindException {
        DirContext context = null;
        try {
            try {
                context = identityProvider.getBrowseContext();
                Attributes attributes = context.getAttributes(dn);

                GroupMappingConfig[] groupTypes = ldapIdentityProviderConfig.getGroupMappings();
                Attribute objectclasses = attributes.get("objectclass");
                for (GroupMappingConfig groupType : groupTypes) {
                    String grpclass = groupType.getObjClass();
                    if (LdapUtils.attrContainsCaseIndependent(objectclasses, grpclass)) {
                        LdapGroup out = new LdapGroup();
                        out.setProviderId(ldapIdentityProviderConfig.getOid());
                        out.setDn(dn);
                        out.setAttributes(attributes);
                        Object tmp = LdapUtils.extractOneAttributeValue(attributes,
                                groupType.getNameAttrName());
                        if (tmp != null) out.setCn(tmp.toString());
                        return out;
                    }
                }
                return null;
            } finally {
                if (context != null) context.close();
            }
        } catch (NamingException e) {
            logger.log(Level.WARNING, "error building group", e);
            throw new FindException("naming exception", e);
        }
    }

    public LdapGroup reify(GroupBean bean) {
        return new LdapGroup(bean);
    }

    /**
     * does equivalent of LdapIdentityProvider.search(new EntityType[] {EntityType.GROUP}, name);
     */
    public LdapGroup findByName(String name) throws FindException {
        Collection res = identityProvider.search(new EntityType[]{EntityType.GROUP}, name);
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
    public void delete(LdapGroup group) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void deleteAll(long ipoid) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void deleteAllVirtual(long ipoid) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void delete(String identifier) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public String saveGroup(LdapGroup group) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void update(LdapGroup group) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public String save(LdapGroup group, Set userHeaders) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void update(LdapGroup group, Set userHeaders) {
        throw new UnsupportedOperationException();
    }

    public Collection<IdentityHeader> search(String searchString) throws FindException {
        return identityProvider.search(new EntityType[]{EntityType.GROUP}, searchString);
    }

    public Class getImpClass() {
        return LdapGroup.class;
    }

    /**
     * checks if a user is member of a group
     *
     * @return true if user is member of group, false otherwise
     * @throws FindException
     */
    public boolean isMember(LdapUser user, LdapGroup group) throws FindException {
        Set<IdentityHeader> userHeaders = getUserHeaders(group);
        for (IdentityHeader header : userHeaders) {
            String login = header.getName();
            if (login != null && login.equals(user.getLogin())) return true;
        }
        return false;
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void addUsers(LdapGroup group, Set users) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void removeUsers(LdapGroup group, Set users) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void addUser(LdapUser user, Set groups) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void removeUser(LdapUser user, Set groups) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void addUser(LdapUser user, LdapGroup group) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void removeUser(LdapUser user, LdapGroup group) {
        throw new UnsupportedOperationException();
    }

    /**
     * searches for the groups to which the passed user belong to
     *
     * @return a collection containing EntityHeader objects
     */
    public Set<IdentityHeader> getGroupHeaders(LdapUser user) throws FindException {
        GroupMappingConfig[] groupTypes = ldapIdentityProviderConfig.getGroupMappings();
        StringBuffer uberGroupMembershipFilter = new StringBuffer("(|");
        boolean checkOuStrategyToo = false;
        boolean somethingToSearchFor = false;
        for (GroupMappingConfig groupType : groupTypes) {
            String grpclass = groupType.getObjClass();
            String mbmAttrName = groupType.getMemberAttrName();
            MemberStrategy memberstrategy = groupType.getMemberStrategy();
            if (memberstrategy.equals(MemberStrategy.MEMBERS_ARE_LOGIN)) {
                uberGroupMembershipFilter.append("(&" + "(objectClass=").append(grpclass).append(")");
                uberGroupMembershipFilter.append("(").append(mbmAttrName).append("=").append(user.getLogin()).append(")");
                uberGroupMembershipFilter.append(")");
                somethingToSearchFor = true;
            } else if (memberstrategy.equals(MemberStrategy.MEMBERS_BY_OU)) {
                checkOuStrategyToo = true;
            } else {
                uberGroupMembershipFilter.append("(&" + "(objectClass=").append(grpclass).append(")");
                uberGroupMembershipFilter.append("(|");
                uberGroupMembershipFilter.append("(").append(mbmAttrName).append("=cn=").append(user.getName()).append(")");
                uberGroupMembershipFilter.append("(").append(mbmAttrName).append("=").append(user.getId()).append(")");
                uberGroupMembershipFilter.append(")");
                uberGroupMembershipFilter.append(")");
                somethingToSearchFor = true;
            }
        }
        uberGroupMembershipFilter.append(")");

        Set<IdentityHeader> output = new HashSet<IdentityHeader>();
        if (somethingToSearchFor) {
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setCountLimit(identityProvider.getMaxSearchResultSize());
            DirContext context;
            try {
                context = identityProvider.getBrowseContext();
            } catch (NamingException e) {
                String msg = "cannot get context";
                logger.log(Level.WARNING, msg, e);
                throw new FindException(msg, e);
            }

            try {
                NamingEnumeration answer;
                try {
                    answer = context.search(ldapIdentityProviderConfig.getSearchBase(), uberGroupMembershipFilter.toString(), sc);
                } catch (NamingException e) {
                    String msg = "error getting answer";
                    logger.log(Level.WARNING, msg, e);
                    throw new FindException(msg, e);
                }

                try {
                    while (answer.hasMore()) {
                        SearchResult sr = (SearchResult)answer.next();
                        // set the dn (unique id)
                        String dn = sr.getName() + "," + ldapIdentityProviderConfig.getSearchBase();
                        //Attributes atts = sr.getAttributes();
                        IdentityHeader header = identityProvider.searchResultToHeader(sr, dn);
                        if (header != null) {
                            output.add(header);
                            // Groups within groups.
                            // check if this group refer to other groups. if so, then user belongs to those
                            // groups too.
                            output.addAll(getSubGroups(context, dn));
                        }
                    }
                } catch (SizeLimitExceededException e) {
                    // add something to the result that indicates the fact that the search criteria is too wide
                    logger.log(Level.FINE, "the search results exceeded the maximum. adding a " +
                                           "EntityType.MAXED_OUT_SEARCH_RESULT to the results",
                                           e);
                    output.add(MAX_EXCEEDED);
                    // dont throw here, we still want to return what we got
                } catch (NamingException e) {
                    String msg = "error getting next answer";
                    logger.log(Level.WARNING, msg, e);
                    throw new FindException(msg, e);
                }finally {
                    try {
                        answer.close();
                    } catch (NamingException e) {
                        logger.info("error closing answer " + e.getMessage());
                    }
                }
            } finally {
                try {
                    if (context != null) context.close();
                } catch (NamingException e) {
                    logger.info("error closing context " + e.getMessage());
                }
            }
        }

        if (checkOuStrategyToo) {
            // look for OU memberships
            String tmpdn = user.getId();
            int pos = 0;
            int res = tmpdn.indexOf("ou=", pos);
            while (res >= 0) {
                // is there a valid organizational unit there?
                LdapGroup maybegrp;
                try {
                    maybegrp = findByPrimaryKey(tmpdn.substring(res));
                } catch (FindException e) {
                    logger.finest("could not resolve this group " + e.getMessage());
                    maybegrp = null;
                }
                if (maybegrp != null) {
                    output.add(groupToHeader(maybegrp));
                }
                pos = res + 2;
                res = tmpdn.indexOf("ou=", pos);
            }
        }
        return output;
    }

    /**
     * practical equivalent to getGroupHeaders(LdapUserManager.findByPrimaryKey(userId));
     */
    public Set<IdentityHeader> getGroupHeaders(String userId) throws FindException {
        return getGroupHeaders(getUserManager().findByPrimaryKey(userId));
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void setGroupHeaders(LdapUser user, Set groupHeaders) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void setGroupHeaders(String userId, Set groupHeaders) {
        throw new UnsupportedOperationException();
    }

    /**
     * return a Set of groups headers for all the subgroups of the group whose dn is passed.
     * note: this is recursive
     *
     * @param dn the dn of the group to inspect
     * @return a collection containing EntityHeader objects
     */
    private Set<IdentityHeader> getSubGroups(DirContext context, String dn) {
        Set<IdentityHeader> output = new HashSet<IdentityHeader>();
        String filter = subGroupSearchString(dn);
        if (filter != null) {
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setCountLimit(identityProvider.getMaxSearchResultSize());
            NamingEnumeration answer = null;
            try {
                answer = context.search(ldapIdentityProviderConfig.getSearchBase(), filter, sc);
                while (answer.hasMore()) {
                    // get this item
                    SearchResult sr = (SearchResult)answer.next();
                    // set the dn (unique id)
                    String subgroupdn = sr.getName() + "," + ldapIdentityProviderConfig.getSearchBase();
                    IdentityHeader header = identityProvider.searchResultToHeader(sr, subgroupdn);
                    if (header != null && header.getType().equals(EntityType.GROUP)) {
                        output.add(header);
                        output.addAll(getSubGroups(context, subgroupdn));
                    }
                }
            } catch (SizeLimitExceededException e) {
                // add something to the result that indicates the fact that the search criteria is too wide
                logger.log(Level.FINE, "the search results exceeded the maximum. adding a " +
                                       "EntityType.MAXED_OUT_SEARCH_RESULT to the results",
                                       e);
                output.add(MAX_EXCEEDED);
                // dont throw here, we still want to return what we got
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
            LdapGroup maybegrp;
            try {
                maybegrp = findByPrimaryKey(dn.substring(res));
            } catch (FindException e) {
                logger.finest("could not resolve this group " + e.getMessage());
                maybegrp = null;
            }
            if (maybegrp != null) {
                IdentityHeader grpheader = groupToHeader(maybegrp);
                output.add(grpheader);
            }
            pos = res + 2;
            res = dn.indexOf("ou=", pos);
        }
        return output;
    }

    private IdentityHeader groupToHeader(LdapGroup maybegrp) {
        return new IdentityHeader(
                providerOid, maybegrp.getDn(), EntityType.GROUP,
                maybegrp.getCn(), maybegrp.getDescription());
    }

    /**
     * filter string for subgroup search
     *
     * @return returns a search string or null if the group object classes dont allow for groups within groups
     */
    private String subGroupSearchString(String dnOfChildGroup) {
        StringBuffer output = new StringBuffer("(|");
        GroupMappingConfig[] groupTypes = ldapIdentityProviderConfig.getGroupMappings();
        boolean searchStringValid = false;
        for (GroupMappingConfig groupType : groupTypes) {
            if (groupType.getMemberStrategy().equals(MemberStrategy.MEMBERS_ARE_DN) ||
                groupType.getMemberStrategy().equals(MemberStrategy.MEMBERS_ARE_NVPAIR))
            {
                searchStringValid = true;
                output.append("(&" + "(objectClass=");
                output.append(groupType.getObjClass());
                output.append(")" + "(");
                output.append(groupType.getMemberAttrName());
                output.append("=");
                output.append(dnOfChildGroup);
                output.append(")" + ")");
            }
        }
        output.append(")");
        if (!searchStringValid) return null;

        return output.toString();
    }

    /**
     * get members for a group
     *
     * @return a collection containing EntityHeader objects
     */
    public Set<IdentityHeader> getUserHeaders(LdapGroup group) throws FindException {
        NamingEnumeration answer = null;
        DirContext context = null;

        try {
            Set<IdentityHeader> headers = new HashSet<IdentityHeader>();

            String dn = group.getDn();
            context = identityProvider.getBrowseContext();
            Attributes attributes = context.getAttributes(dn);

            GroupMappingConfig[] groupTypes = ldapIdentityProviderConfig.getGroupMappings();
            Attribute objectclasses = attributes.get("objectclass");
            for (GroupMappingConfig groupType : groupTypes) {
                String grpclass = groupType.getObjClass();
                if (LdapUtils.attrContainsCaseIndependent(objectclasses, grpclass)) {
                    if (groupType.getMemberStrategy().equals(MemberStrategy.MEMBERS_ARE_LOGIN)) {
                        Attribute memberAttribute = attributes.get(groupType.getMemberAttrName());
                        if (memberAttribute != null) {
                            for (int ii = 0; ii < memberAttribute.size(); ii++) {
                                Object val = memberAttribute.get(ii);
                                if (val != null) {
                                    String memberlogin = val.toString();
                                    LdapUser u = getUserManager().findByLogin(memberlogin);
                                    if (u != null) {
                                        headers.add(new IdentityHeader(providerOid, u.getName(), EntityType.USER, u.getLogin(), null));
                                    }
                                }
                            }
                        }
                    } else if (groupType.getMemberStrategy().equals(MemberStrategy.MEMBERS_BY_OU)) {
                        collectOUGroupMembers(context, dn, headers);
                    } else {
                        Attribute memberAttribute = attributes.get(groupType.getMemberAttrName());
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
                                        LdapUser u = getUserManager().findByPrimaryKey(memberhint);
                                        if (u != null) {
                                            IdentityHeader newheader = new IdentityHeader(
                                                    providerOid,
                                                    u.getDn(), EntityType.USER, u.getLogin(), null);
                                            if (!headers.contains(newheader))
                                                headers.add(newheader);
                                            done = true;
                                        } else {
                                            // GROUPS WITHIN GROUPS
                                            // fla note: the member hint might actually refer to another group
                                            // insted of a user
                                            LdapGroup subgroup;
                                            try {
                                                subgroup = findByPrimaryKey(memberhint);
                                            } catch (FindException e) {
                                                // nothing on purpose
                                                logger.finest("seems like " + memberhint +
                                                        " is not a group dn" + e.getMessage());
                                                subgroup = null;
                                            }
                                            if (subgroup == null) {
                                                try {
                                                    subgroup = findByName(memberhint);
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
                                                Set<IdentityHeader> subgroupmembers = getUserHeaders(subgroup);
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
        } catch (NamingException ne) {
            logger.log(Level.SEVERE, null, ne);
            throw new FindException(ne.getMessage(), ne);
        } finally {
            try {
                if (answer != null) answer.close();
                if (context != null) context.close();
            } catch (NamingException ne) {
                logger.log(Level.WARNING, "Caught exception closing LDAP connection", ne);
            }
        }
    }

    /**
     * equivalent to getUserHeaders(findByPrimaryKey(groupId));
     */
    public Set<IdentityHeader> getUserHeaders(String groupId) throws FindException {
        return getUserHeaders(findByPrimaryKey(groupId));
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void setUserHeaders(LdapGroup group, Set groupHeaders) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    public void setUserHeaders(String groupId, Set groupHeaders) {
        throw new UnsupportedOperationException();
    }

    /**
     * practical equivalent to LdapIdentityProvider.search(new EntityType[] {EntityType.GROUP}, "*");
     */
    public Collection<IdentityHeader> findAllHeaders() throws FindException {
        return identityProvider.search(new EntityType[]{EntityType.GROUP}, "*");
    }

    /**
     * throws an UnsupportedOperationException
     */
    public Collection<IdentityHeader> findAllHeaders(int offset, int windowSize) {
        throw new UnsupportedOperationException();
    }

    /**
     * like findAllHeaders but contains actual LdapUser objects instead of EntityHeader objects
     */
    public Collection findAll() throws FindException {
        Collection<IdentityHeader> headers = findAllHeaders();
        Collection<LdapGroup> output = new ArrayList<LdapGroup>();
        for (IdentityHeader header : headers) {
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    /**
     * like findAllHeaders but contains actual LdapUser objects instead of EntityHeader objects
     */
    public Collection findAll(int offset, int windowSize) throws FindException {
        Collection<IdentityHeader> headers = findAllHeaders(offset, windowSize);
        Collection<LdapGroup> output = new ArrayList<LdapGroup>();
        for (IdentityHeader header : headers) {
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    public Integer getVersion(long oid) throws FindException {
        return new Integer(0);
    }

    public Map findVersionMap() throws FindException {
        return Collections.EMPTY_MAP;
    }

    public Entity getCachedEntity(long o, int maxAge) {
        throw new UnsupportedOperationException();
    }


    protected LdapUserManager getUserManager() {
        return identityProvider.getUserManager();
    }

    private void collectOUGroupMembers(DirContext context, String dn, Set<IdentityHeader> memberHeaders) throws NamingException {
        long maxSize = identityProvider.getMaxSearchResultSize();
        if (memberHeaders.size() >= maxSize) return;
        // build group memberships
        NamingEnumeration answer;
        String filter = identityProvider.userSearchFilterWithParam("*");
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setCountLimit(maxSize);
        // use dn of group as base
        answer = context.search(dn, filter, sc);
        try {
            while (answer.hasMore()) {
                String entitydn;
                SearchResult sr = (SearchResult)answer.next();
                entitydn = sr.getName() + "," + dn;
                IdentityHeader header = identityProvider.searchResultToHeader(sr, entitydn);
                memberHeaders.add(header);
            }
        } catch (SizeLimitExceededException e) {
            // add something to the result that indicates the fact that the search criteria is too wide
            logger.log(Level.FINE, "the search results exceeded the maximum. adding a " +
                                   "EntityType.MAXED_OUT_SEARCH_RESULT to the results",
                                   e);
            memberHeaders.add(MAX_EXCEEDED);
            // dont throw here, we still want to return what we got
        }
        answer.close();
        if (memberHeaders.size() >= maxSize) return;
        // sub group search
        filter = identityProvider.groupSearchFilterWithParam("*");
        // use dn of group as base
        answer = context.search(dn, filter, sc);
        try {
            while (answer.hasMore()) {
                String entitydn;
                SearchResult sr = (SearchResult)answer.next();
                if (sr != null && sr.getName() != null && sr.getName().length() > 0) {
                    entitydn = sr.getName() + "," + dn;
                    try {
                        LdapGroup subGroup = this.findByPrimaryKey(entitydn);
                        if (subGroup != null) {
                            Set<IdentityHeader> subGroupMembers = getUserHeaders(subGroup);
                            memberHeaders.addAll(subGroupMembers);
                        }
                    } catch (FindException e) {
                        logger.log(Level.FINE, "error looking for sub-group" + entitydn, e);
                    }
                }
            }
        } catch (SizeLimitExceededException e) {
            // add something to the result that indicates the fact that the search criteria is too wide
            logger.log(Level.FINE, "the search results exceeded the maximum. adding a " +
                                   "EntityType.MAXED_OUT_SEARCH_RESULT to the results",
                                   e);
            IdentityHeader maxExceeded = new IdentityHeader(
                    providerOid,
                    "noid",
                    EntityType.MAXED_OUT_SEARCH_RESULT,
                    "Too Many Entries",
                    "This search yields too many entities. " +
                    "Please narrow your search criterion.");
            memberHeaders.add(maxExceeded);
            // dont throw here, we still want to return what we got
        }
        answer.close();
    }

    private void nvSearchForUser(DirContext context, String nvhint, Set<IdentityHeader> memberHeaders) throws NamingException {
        long maxSize = identityProvider.getMaxSearchResultSize();
        if (memberHeaders.size() >= maxSize) return;
        StringBuffer filter = new StringBuffer("(|");
        UserMappingConfig[] userTypes = ldapIdentityProviderConfig.getUserMappings();
        for (UserMappingConfig userType : userTypes) {
            filter.append("(&" + "(objectClass=");
            filter.append(userType.getObjClass());
            filter.append(")" + "(");
            filter.append(nvhint);
            filter.append(")" + ")");
        }
        filter.append(")");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setCountLimit(maxSize);
        NamingEnumeration answer;
        try {
            answer = context.search(ldapIdentityProviderConfig.getSearchBase(), filter.toString(), sc);
        } catch (OperationNotSupportedException e) {
            // this gets thrown by oid for some weird groups
            logger.log(Level.FINE, ldapIdentityProviderConfig.getName() + "directory cannot search on" + nvhint, e);
            return;
        }
        while (answer.hasMore()) {
            String userdn;
            SearchResult sr = (SearchResult)answer.next();
            userdn = sr.getName() + "," + ldapIdentityProviderConfig.getSearchBase();
            IdentityHeader newheader = identityProvider.searchResultToHeader(sr, userdn);
            if (!memberHeaders.contains(newheader))
                memberHeaders.add(newheader);
        }
    }


    private LdapIdentityProviderConfig ldapIdentityProviderConfig;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private LdapIdentityProvider identityProvider;


}
