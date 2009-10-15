package com.l7tech.server.identity.ldap;

import com.l7tech.identity.ldap.*;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.Pair;
import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.server.Lifecycle;
import com.l7tech.server.LifecycleException;
import com.whirlycott.cache.Cache;

import javax.naming.ldap.LdapName;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.SizeLimitExceededException;
import javax.naming.AuthenticationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
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
 */
@LdapClassLoaderRequired
public class LdapGroupManagerImpl implements LdapGroupManager, Lifecycle {

    public LdapGroupManagerImpl() {
    }

    @Override
    public synchronized void configure(LdapIdentityProvider provider) {
        identityProvider = provider;
        identityProviderConfig = identityProvider.getConfig();

        cacheSize = getConfigValue( identityProviderConfig.getGroupCacheSize(), DEFAULT_GROUP_CACHE_SIZE ) * 2; // double since we cache by name and dn
        cacheMaxAge = getConfigValue( identityProviderConfig.getGroupCacheMaxAge(), DEFAULT_GROUP_CACHE_HIERARCHY_MAXAGE );
        groupMaxNesting = getConfigValue( identityProviderConfig.getGroupMaxNesting(), DEFAULT_GROUP_MAX_NESTING );

        ldapTemplate = new LdapUtils.LdapTemplate(
                identityProviderConfig.getSearchBase(),
                getReturningAttributes() ){
            @Override
            DirContext getDirContext() throws NamingException {
                return identityProvider.getBrowseContext();
            }
        };
    }

    public void setLdapRuntimeConfig( final LdapRuntimeConfig ldapRuntimeConfig ) {
        this.ldapRuntimeConfig = ldapRuntimeConfig;
    }

    @Override
    public void start() throws LifecycleException {
        groupCache = cacheSize < 1 ? null :
                WhirlycacheFactory.createCache("LDAP Group Cache ("+getId()+")", cacheSize, 33, WhirlycacheFactory.POLICY_LFU);
    }

    @Override
    public void stop() throws LifecycleException {
        Cache cache = groupCache;
        groupCache = null;
        if ( cache != null ) {
            WhirlycacheFactory.shutdown(cache);
        }
    }

    /**
     * get group based on dn
     *
     * @return an LdapGroup object, null if group dont exist
     */
    @Override
    public LdapGroup findByPrimaryKey( final String dn ) throws FindException {
        final LdapGroup[] groupHolder = new LdapGroup[1];

        groupHolder[0] = getCachedGroupByDn(dn);
        if ( groupHolder[0]==null ) {
            final LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();
            try {
                ldapTemplate.attributes( dn, new LdapUtils.LdapListener(){
                    @Override
                    void attributes( final String dn, final Attributes attributes ) throws NamingException {
                        groupHolder[0] = buildGroup( dn, attributes );
                        cacheGroup( groupHolder[0] );
                    }
                });
            } catch (NameNotFoundException nnfe) {
                if ( logger.isLoggable(Level.FINEST )) {
                    logger.finest("Group " + dn + " does not exist in" + ldapIdentityProviderConfig.getName() + " (" + ExceptionUtils.getMessage(nnfe) + ")");
                }
            } catch (AuthenticationException ae) {
                logger.log(Level.WARNING, "LDAP authentication error, while building group: " + ae.getMessage(),
                        ExceptionUtils.getDebugException(ae));
                throw new FindException("naming exception", ae);
            } catch (NamingException e) {
                logger.log(Level.WARNING, "LDAP error, while building group", e);
                throw new FindException("naming exception", e);
            }
        }

        return groupHolder[0];
    }

    @Override
    public LdapGroup reify(GroupBean bean) {
        LdapGroup lg = new LdapGroup(bean.getProviderId(), bean.getId(), bean.getName());
        lg.setDescription(bean.getDescription());
        return lg;
    }

    /**
     * does equivalent of LdapIdentityProvider.search(new EntityType[] {EntityType.GROUP}, name);
     */
    @Override
    public LdapGroup findByName( final String name ) throws FindException {
        LdapGroup ldapGroup = getCachedGroupByCn(name);

       if ( ldapGroup == null ) {
            final LdapIdentityProvider identityProvider = getIdentityProvider();
            final String filter = identityProvider.groupSearchFilterWithParam( name );
            final LdapGroup[] groupHolder = new LdapGroup[1];
            try {
                ldapTemplate.search( filter, 2, null, new LdapUtils.LdapListener(){
                    @Override
                    void searchResults( final NamingEnumeration<SearchResult> results ) throws NamingException {
                        if ( results.hasMore() ) {
                            SearchResult result = results.next();
                            groupHolder[0] = buildGroup( result.getNameInNamespace(), result.getAttributes() );
                            cacheGroup( groupHolder[0] );
                        }

                        if ( results.hasMore() ) {
                            logger.warning("search on group name returned more than one " +
                              "result. returning 1st one");
                        }
                    }
                } );
            } catch (AuthenticationException ae) {
                logger.log(Level.WARNING, "LDAP authentication error, while building group: " + ae.getMessage(),
                        ExceptionUtils.getDebugException(ae));
                throw new FindException("naming exception", ae);
            } catch (NamingException e) {
                logger.log(Level.WARNING, "LDAP error, while building group", e);
                throw new FindException("naming exception", e);
            }

            ldapGroup = groupHolder[0];
        }

        return ldapGroup;
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void delete(LdapGroup group) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void deleteAll(long ipoid) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void deleteAllVirtual(long ipoid) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void delete(String identifier) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public String saveGroup(LdapGroup group) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void update(LdapGroup group) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public String save(LdapGroup group, Set userHeaders) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void update(LdapGroup group, Set userHeaders) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<IdentityHeader> search(String searchString) throws FindException {
        LdapIdentityProvider identityProvider = getIdentityProvider();
        return identityProvider.search(new EntityType[]{EntityType.GROUP}, searchString);
    }

    @Override
    public Class getImpClass() {
        return LdapGroup.class;
    }

    /**
     * checks if a user is member of a group
     *
     * @return true if user is member of group, false otherwise
     * @throws FindException
     */
    @Override
    public boolean isMember( final User user,
                             final LdapGroup group ) throws FindException {
        if (user.getProviderId() != getProviderOid()) {
            logger.log(Level.FINE, "User is not from this Identity Provider");
            return false;
        }

        return isMember( user, group, 1 );
    }

    private boolean isMember( final User user,
                              final LdapGroup group,
                              final int depth ) throws FindException {
        LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();
        for(GroupMappingConfig groupMappingConfig : ldapIdentityProviderConfig.getGroupMappings()) {
            final MemberStrategy memberStrategy = groupMappingConfig.getMemberStrategy();
            if( MemberStrategy.MEMBERS_BY_OU.equals(memberStrategy) ) {
                try {
                    final LdapName userDN = new LdapName(user.getId());
                    final LdapName groupDN = new LdapName(group.getDn());
                    if( userDN.startsWith(groupDN) ) {
                        return true;
                    }
                } catch(NamingException e) {
                    String msg = "failed to check group membership";
                    logger.log(Level.WARNING, "LDAP error: " + msg, e);
                    throw new FindException(msg, e);
                }
            } else {
                Attributes attributes = group.getAttributes();
                Attribute membersAttribute = attributes==null ? null : attributes.get(groupMappingConfig.getMemberAttrName());
                if(membersAttribute == null) {
                    continue;
                }

                for(int i = 0;i < membersAttribute.size();i++) {
                    try {
                        String member = (String)membersAttribute.get(i);
                        if( MemberStrategy.MEMBERS_ARE_LOGIN.equals(memberStrategy) ) {
                            if ( member.equals(user.getLogin()) ) {
                                return true;
                            }
                        } else if ( MemberStrategy.MEMBERS_ARE_DN.equals(memberStrategy) ) {
                            if ( member.equals(user.getId()) ) {
                                return true;
                            }
                        } else if ( MemberStrategy.MEMBERS_ARE_NVPAIR.equals(memberStrategy) ) {
                            for ( String nameAttribute : getDistinctUserNameAttributeNames() ) {
                                if ( member.equals(nameAttribute + "=" + user.getName()) ) {
                                    return true;
                                }
                            }
                        }
                    } catch(NamingException ne) {
                        logger.log(Level.WARNING, "LDAP attribute read error: " + ne.getMessage(), ExceptionUtils.getDebugException(ne));
                    }
                }

                // The user is not a member of this group, need to check subgroups
                if ( groupNestingProcessNextDepth(depth) ) {
                    final List<String> nonGroups = new ArrayList<String>();
                    try {
                        for(int i = 0; i < membersAttribute.size(); i++) {
                            try {
                                final String memberString = (String)membersAttribute.get(i);

                                if ( isNonGroup(group, memberString) ) {
                                    continue;
                                }

                                LdapGroup subgroup = null;
                                if( MemberStrategy.MEMBERS_ARE_LOGIN.equals(memberStrategy) ) {
                                    subgroup = findByName((String)membersAttribute.get(i));
                                } else if ( MemberStrategy.MEMBERS_ARE_DN.equals(memberStrategy) ) {
                                    subgroup = findByPrimaryKey(memberString);
                                } else if( MemberStrategy.MEMBERS_ARE_NVPAIR.equals(memberStrategy) ) {
                                    String memberName = memberString;
                                    int index = memberName.indexOf( '=' );
                                    if ( index > 0 ) {
                                        memberName = memberName.substring( index+1 );
                                    }
                                    subgroup = findByName(memberName);
                                }

                                if( subgroup != null ) {
                                    if ( isMember(user, subgroup, depth + 1) ) {
                                        return true;
                                    }
                                } else {
                                    nonGroups.add( memberString );
                                }
                            } catch(NamingException e) {
                                // skip to the next member
                            }
                        }
                    } finally {
                        cacheNonGroups( group, nonGroups );
                    }
                }
            }
        }

        return false;
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void addUsers(LdapGroup group, Set users) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void removeUsers(LdapGroup group, Set users) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void addUser(LdapUser user, Set groups) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void removeUser(LdapUser user, Set groups) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void addUser(LdapUser user, LdapGroup group) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void removeUser(LdapUser user, LdapGroup group) {
        throw new UnsupportedOperationException();
    }

    /**
     * searches for the groups to which the passed user belong to
     *
     * @return a collection containing EntityHeader objects
     */
    @Override
    public Set<IdentityHeader> getGroupHeaders( final LdapUser user ) throws FindException {
        final LdapIdentityProvider identityProvider = getIdentityProvider();
        final LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();

        GroupMappingConfig[] groupTypes = ldapIdentityProviderConfig.getGroupMappings();
        boolean checkOuStrategyToo = false;
        LdapSearchFilter filter = new LdapSearchFilter();
        filter.or();
        for (GroupMappingConfig groupType : groupTypes) {
            final String grpclass = groupType.getObjClass();
            final String mbmAttrName = groupType.getMemberAttrName();
            final MemberStrategy memberstrategy = groupType.getMemberStrategy();
            if (MemberStrategy.MEMBERS_BY_OU.equals(memberstrategy)) {
                checkOuStrategyToo = true;
            } else if (MemberStrategy.MEMBERS_ARE_LOGIN.equals(memberstrategy)) {
                filter.and();
                  filter.objectClass(grpclass);
                  filter.attrEquals(mbmAttrName, user.getLogin());
                filter.end();
            } else if (MemberStrategy.MEMBERS_ARE_DN.equals(memberstrategy)) {
                filter.and();
                  filter.objectClass(grpclass);
                  filter.attrEquals( mbmAttrName, user.getId() );
                filter.end();
            } else if (MemberStrategy.MEMBERS_ARE_NVPAIR.equals(memberstrategy)) {
                filter.and();
                  filter.objectClass(grpclass);
                  Set<String> names = getDistinctUserNameAttributeNames();
                  if (names.size()>1) { filter.or(); }
                  for ( String name : names ) {
                      filter.attrEquals( mbmAttrName, name + "=" + user.getName() );
                  }
                  if (names.size()>1) { filter.end(); }
                filter.end();
            }
        }
        filter.end();

        final EntityHeaderSet<IdentityHeader> output = new EntityHeaderSet<IdentityHeader>();
        if ( !filter.isEmpty() ) {
            DirContext context = null;
            try {
                try {
                    context = identityProvider.getBrowseContext();
                } catch (AuthenticationException ae) {
                    String msg = "cannot get context";
                    logger.log(Level.WARNING, "LDAP authentication error: " + ae.getMessage(), ExceptionUtils.getDebugException(ae));
                    throw new FindException(msg, ae);
                } catch (NamingException e) {
                    String msg = "cannot get context";
                    logger.log(Level.WARNING, "LDAP error: " + msg, e);
                    throw new FindException(msg, e);
                }

                try {
                    final DirContext searchContext = context;
                    ldapTemplate.search( context, filter.buildFilter(), ldapRuntimeConfig.getMaxSearchResultSize(), null, new LdapUtils.LdapListener(){
                        @Override
                        boolean searchResult( final SearchResult sr ) throws NamingException {
                            IdentityHeader header = identityProvider.searchResultToHeader(sr);
                            if ( header != null ) {
                                output.add(header);
                                // Groups within groups.
                                // check if this group is a member of other groups. if so, then user belongs to those
                                // groups too.
                                if ( groupNestingEnabled()  ) {
                                    output.addAll(getSubGroups(searchContext, sr.getNameInNamespace(), 2));
                                }
                            }

                            return true;
                        }
                    } );
                } catch (SizeLimitExceededException e) {
                    // add something to the result that indicates the fact that the search criteria is too wide
                    logger.log(Level.FINE, "the search results exceeded the maximum. adding a " +
                                           "EntityType.MAXED_OUT_SEARCH_RESULT to the results",
                                           e);
                    output.setMaxExceeded(ldapRuntimeConfig.getMaxSearchResultSize());
                    // dont throw here, we still want to return what we got
                } catch (NamingException e) {
                    String msg = "error getting answer";
                    logger.log(Level.WARNING, msg, e);
                    throw new FindException(msg, e);
                }
            } finally {
                ResourceUtils.closeQuietly( context );
            }
        }

        if ( checkOuStrategyToo ) {
            // look for OU memberships
            addOUGroups( user.getId(), output );
        }

        return output;
    }

    /**
     * If the given DN has OUs, then add as groups.
     */
    private void addOUGroups( final String dn, final EntityHeaderSet<IdentityHeader> output ) {
        final String dnLower = dn.toLowerCase();
        int pos = 0;
        int res = dnLower.indexOf("ou=", pos);
        while (res >= 0) {
            // is there a valid organizational unit there?
            try {
                LdapGroup group = findByPrimaryKey( dn.substring(res) );
                if ( group != null ) {
                    output.add( groupToHeader(group) );
                }
            } catch (FindException e) {
                logger.finest("could not resolve this group " + e.getMessage());
            }
            pos = res + 2;
            res = dnLower.indexOf("ou=", pos);
        }
    }

    /**
     * practical equivalent to getGroupHeaders(LdapUserManager.findByPrimaryKey(userId));
     */
    @Override
    public Set<IdentityHeader> getGroupHeaders( final String userId ) throws FindException {
        return getGroupHeaders(getUserManager().findByPrimaryKey(userId));
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void setGroupHeaders(LdapUser user, Set groupHeaders) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
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
    private Set<IdentityHeader> getSubGroups( final DirContext context,
                                              final String dn,
                                              final int depth ) {
        final LdapIdentityProvider identityProvider = getIdentityProvider();
        final EntityHeaderSet<IdentityHeader> output = new EntityHeaderSet<IdentityHeader>();
        String filter = subGroupSearchString(dn);
        if (filter != null) {
            try {
                ldapTemplate.search( context, filter, ldapRuntimeConfig.getMaxSearchResultSize(), null, new LdapUtils.LdapListener(){
                    @Override
                    boolean searchResult( final SearchResult sr ) throws NamingException {
                        IdentityHeader header = identityProvider.searchResultToHeader(sr);
                        if (header != null && header.getType().equals(EntityType.GROUP)) {
                            output.add(header);
                            if ( groupNestingProcessNextDepth(depth) ) {
                                output.addAll(getSubGroups(context, sr.getNameInNamespace(), depth + 1));
                            }
                        }
                        return true;
                    }
                } );
            } catch (SizeLimitExceededException e) {
                // add something to the result that indicates the fact that the search criteria is too wide
                logger.log(Level.FINE, "the search results exceeded the maximum.", e);
                output.setMaxExceeded(ldapRuntimeConfig.getMaxSearchResultSize());
                // dont throw here, we still want to return what we got
            } catch (NamingException e) {
                logger.log(Level.WARNING, "naming exception with filter " + filter, e);
            }
        }

        // look for sub-OU memberships
        addOUGroups( dn, output );

        return output;
    }

    private IdentityHeader groupToHeader( final LdapGroup maybegrp ) {
        return new IdentityHeader(
                    getProviderOid(),
                    maybegrp.getDn(),
                    EntityType.GROUP,
                    maybegrp.getCn(),
                    maybegrp.getDescription(),
                    null,
                    null);
    }

    /**
     * filter string for subgroup search
     *
     * @return returns a search string or null if the group object classes dont allow for groups within groups
     */
    private String subGroupSearchString( final String dnOfChildGroup ) {
        LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();

        GroupMappingConfig[] groupTypes = ldapIdentityProviderConfig.getGroupMappings();
        LdapSearchFilter searchFilter = new LdapSearchFilter();
        searchFilter.or();
        for (GroupMappingConfig groupType : groupTypes) {
            if (groupType.getMemberStrategy().equals(MemberStrategy.MEMBERS_ARE_DN) ||
                groupType.getMemberStrategy().equals(MemberStrategy.MEMBERS_ARE_NVPAIR))
            {
                searchFilter.and();
                  searchFilter.objectClass(groupType.getObjClass());
                  searchFilter.attrEquals( groupType.getMemberAttrName(), dnOfChildGroup );
                searchFilter.end();
            }
        }
        searchFilter.end();
        if (searchFilter.isEmpty()) return null;

        return searchFilter.buildFilter();
    }

    /**
     * get members for a group
     *
     * @return a collection containing EntityHeader objects
     */
    @Override
    public EntityHeaderSet<IdentityHeader> getUserHeaders( final LdapGroup group ) throws FindException {
        return getUserHeaders( group, 1 );
    }

    private EntityHeaderSet<IdentityHeader> getUserHeaders( final LdapGroup group,
                                                            final int depth ) throws FindException {
        final LdapIdentityProvider identityProvider = getIdentityProvider();
        final LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();
        final GroupMappingConfig[] groupTypes = ldapIdentityProviderConfig.getGroupMappings();
        final EntityHeaderSet<IdentityHeader> headers = new EntityHeaderSet<IdentityHeader>();

        DirContext context = null;
        try {
            String dn = group.getDn();
            context = identityProvider.getBrowseContext();
            final DirContext searchContext = context;
            ldapTemplate.attributes( context, dn, new LdapUtils.LdapListener(){
                @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
                @Override
                void attributes( final String dn, final Attributes attributes ) throws NamingException {
                    final Attribute objectclasses = attributes.get(LdapIdentityProvider.OBJECTCLASS_ATTRIBUTE_NAME);
                    for ( GroupMappingConfig groupType : groupTypes ) {
                        final String grpclass = groupType.getObjClass();
                        if (LdapUtils.attrContainsCaseIndependent(objectclasses, grpclass)) {
                            final MemberStrategy memberStrategy = groupType.getMemberStrategy();
                            if ( MemberStrategy.MEMBERS_BY_OU.equals(memberStrategy) ) {
                                collectOUGroupMembers(searchContext, dn, headers, depth);
                            } else if ( MemberStrategy.MEMBERS_ARE_LOGIN.equals(memberStrategy) ) {
                                Attribute memberAttribute = attributes.get(groupType.getMemberAttrName());
                                if (memberAttribute != null) {
                                    for (int ii = 0; ii < memberAttribute.size(); ii++) {
                                        Object val = memberAttribute.get(ii);
                                        if (val != null) {
                                            String memberlogin = val.toString();
                                            LdapUser u;
                                            try {
                                                u = getUserManager().findByLogin(memberlogin);
                                            } catch (FindException e) {
                                                throw (NamingException) new NamingException().initCause( e );
                                            }
                                            if (u != null) {
                                                IdentityHeader h = new IdentityHeader(getProviderOid(), u.getDn(), EntityType.USER, u.getLogin(), null, u.getName(), null);
                                                headers.add(h);
                                            }
                                            //TODO [steve] should process nested groups for MEMBERS_ARE_LOGIN here
                                        }
                                    }
                                }
                            } else if ( MemberStrategy.MEMBERS_ARE_DN.equals(memberStrategy) ||
                                        MemberStrategy.MEMBERS_ARE_NVPAIR.equals(memberStrategy) ) {
                                final Attribute memberAttribute = attributes.get(groupType.getMemberAttrName());
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
                                                    IdentityHeader newheader = new IdentityHeader(getProviderOid(), u.getDn(),
                                                                                                  EntityType.USER,
                                                                                                  u.getLogin(), null, u.getName(), null);
                                                    if (!headers.contains(newheader)) {
                                                        headers.add(newheader);
                                                    }
                                                    done = true;
                                                } else if ( groupNestingProcessNextDepth(depth) ) {
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
                                                            subgroup = findByName(memberhint); //TODO [steve] need to handle NV_PAIR for groups?
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
                                                        Set<IdentityHeader> subgroupmembers = getUserHeaders(subgroup, depth + 1);
                                                        headers.addAll(subgroupmembers);
                                                    }
                                                }
                                            } catch (FindException e) {
                                                logger.log(Level.FINEST, "cannot resolve user through dn, try nv" +
                                                        "pair method", e);
                                            }
                                            // if that dont work, try search assuming nv pair
                                            if (!done) {
                                                nvSearchForUser(searchContext, memberhint, headers);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } );

            return headers;
        } catch (AuthenticationException ae) {
            logger.log(Level.WARNING, "LDAP authentication error: " + ae.getMessage(), ExceptionUtils.getDebugException(ae));
            throw new FindException(ae.getMessage(), ae);
        } catch (NamingException ne) {
            if ( ExceptionUtils.causedBy( ne, FindException.class ) ) {
                throw ExceptionUtils.getCauseIfCausedBy( ne, FindException.class );
            }
            logger.log(Level.SEVERE, null, ne);
            throw new FindException(ne.getMessage(), ne);
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    /**
     * equivalent to getUserHeaders(findByPrimaryKey(groupId));
     */
    @Override
    public Set<IdentityHeader> getUserHeaders(String groupId) throws FindException {
        return getUserHeaders(findByPrimaryKey(groupId));
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void setUserHeaders(LdapGroup group, Set groupHeaders) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws an UnsupportedOperationException
     */
    @Override
    public void setUserHeaders(String groupId, Set groupHeaders) {
        throw new UnsupportedOperationException();
    }

    /**
     * practical equivalent to LdapIdentityProvider.search(new EntityType[] {EntityType.GROUP}, "*");
     */
    @Override
    public EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException {
        LdapIdentityProvider identityProvider = getIdentityProvider();
        return identityProvider.search(new EntityType[]{EntityType.GROUP}, "*");
    }

    /**
     * like findAllHeaders but contains actual LdapUser objects instead of EntityHeader objects
     */
    @Override
    public Collection findAll() throws FindException {
        Collection<IdentityHeader> headers = findAllHeaders();
        Collection<LdapGroup> output = new ArrayList<LdapGroup>();
        for (IdentityHeader header : headers) {
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    protected LdapUserManager getUserManager() {
        LdapIdentityProvider identityProvider = getIdentityProvider();
        return identityProvider.getUserManager();
    }

    private void collectOUGroupMembers( final DirContext context,
                                        final String dn,
                                        final EntityHeaderSet<IdentityHeader> memberHeaders,
                                        final int depth ) throws NamingException {
        final LdapIdentityProvider identityProvider = getIdentityProvider();
        final long maxSize = ldapRuntimeConfig.getMaxSearchResultSize();
        if (memberHeaders.size() >= maxSize) return;

        // build group memberships
        String filter = identityProvider.userSearchFilterWithParam("*");

        // use dn of group as base
        try {
            ldapTemplate.search( context, dn, filter, maxSize, null, new LdapUtils.LdapListener(){
                @Override
                boolean searchResult( final SearchResult sr ) throws NamingException {
                    IdentityHeader header = identityProvider.searchResultToHeader(sr);
                    if (header == null) {
                        logger.info("The user " + sr.getNameInNamespace() + " is not valid according to template and will not " +
                                    "be considered as a member");
                    } else {
                        memberHeaders.add(header);
                    }
                    return true;
                }
            } );
        } catch (SizeLimitExceededException e) {
            // add something to the result that indicates the fact that the search criteria is too wide
            logger.log(Level.FINE, "the search results exceeded the maximum.", e);
            memberHeaders.setMaxExceeded(ldapRuntimeConfig.getMaxSearchResultSize());
            // dont throw here, we still want to return what we got
        }

        // sub group search
        if ( memberHeaders.size() < maxSize &&
             groupNestingProcessNextDepth(depth) ) {
            filter = identityProvider.groupSearchFilterWithParam("*");
            // use dn of group as base
            try {
                ldapTemplate.search( context, dn, filter, maxSize, null, new LdapUtils.LdapListener(){
                    @Override
                    boolean searchResult( final SearchResult sr ) throws NamingException {
                        if (sr != null && sr.getName() != null && sr.getName().length() > 0) {
                            String entitydn = sr.getNameInNamespace();
                            if (!entitydn.equals(dn)) { // avoid recursion on this group
                                try {
                                    LdapGroup subGroup = findByPrimaryKey(entitydn);
                                    if (subGroup != null) {
                                        Set<IdentityHeader> subGroupMembers = getUserHeaders(subGroup);
                                        memberHeaders.addAll(subGroupMembers);
                                    }
                                } catch (FindException e) {
                                    logger.log(Level.FINE, "error looking for sub-group" + entitydn, e);
                                }
                            }
                        }
                        return true;
                    }
                });
            } catch (SizeLimitExceededException e) {
                // add something to the result that indicates the fact that the search criteria is too wide
                logger.log(Level.FINE, "the search results exceeded the maximum.", e);
                memberHeaders.setMaxExceeded(ldapRuntimeConfig.getMaxSearchResultSize());
                // dont throw here, we still want to return what we got
            }
        }
    }

    private void nvSearchForUser( final DirContext context,
                                  final String nvhint,
                                  final Set<IdentityHeader> memberHeaders ) throws NamingException {
        final LdapIdentityProvider identityProvider = getIdentityProvider();
        final LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();
        final long maxSize = ldapRuntimeConfig.getMaxSearchResultSize();
        if (memberHeaders.size() >= maxSize) return;

        LdapSearchFilter filter = new LdapSearchFilter();
        filter.or();
        UserMappingConfig[] userTypes = ldapIdentityProviderConfig.getUserMappings();
        for (UserMappingConfig userType : userTypes) {
            filter.and();
              filter.objectClass( userType.getObjClass() );
              filter.expression(nvhint);
            filter.end();
        }
        filter.end();

        try {
            ldapTemplate.search( context, filter.buildFilter(), maxSize, null, new LdapUtils.LdapListener(){
                @Override
                boolean searchResult( final SearchResult sr ) throws NamingException {
                    IdentityHeader newheader = identityProvider.searchResultToHeader(sr);
                    if (newheader == null) {
                        logger.info("User " + sr.getNameInNamespace() + " is not valid according to the template and will not " +
                                    "be considered as a member");
                    } else if (!memberHeaders.contains(newheader)) {
                        memberHeaders.add(newheader);
                    }
                    return true;
                }
            } );
        } catch (OperationNotSupportedException e) {
            // this gets thrown by oid for some weird groups
            logger.log(Level.FINE, ldapIdentityProviderConfig.getName() + "directory cannot search on" + nvhint, e);
        }
    }

    private String[] getReturningAttributes() {
        String[] returningAttributes = null;

        Collection<String> attributes = identityProvider.getReturningAttributes();
        if ( attributes != null ) {
            returningAttributes = attributes.toArray(new String[attributes.size()]);
        }

        return returningAttributes;
    }

    private Set<String> getDistinctUserNameAttributeNames() {
        Set<String> nameNames;

        if ( useSingleMemberNvPair ) {
            nameNames = Collections.singleton( memberNvPairAttribute );
        } else {
            nameNames = new HashSet<String>();

            for ( UserMappingConfig config : identityProviderConfig.getUserMappings() ) {
                nameNames.add( config.getNameAttrName() );
            }            
        }

        return nameNames;
    }

    private int getConfigValue( final Integer configValue, final int defaultValue ) {
        int result = defaultValue;

        if ( configValue != null ) {
            result = configValue;
        }

        return result;
    }

    private boolean groupNestingEnabled() {
        return groupMaxNesting != 1;
    }

    private boolean groupNestingProcessNextDepth( final int depth ) {
        return groupMaxNesting == 0 || depth < groupMaxNesting;
    }

    private LdapIdentityProvider getIdentityProvider() {
        LdapIdentityProvider provider =  identityProvider;
        if ( provider == null ) {
            throw new IllegalStateException("Not configured!");
        }
        return provider;
    }

    private long getProviderOid() {
        long oid = providerOid;

        if ( oid == 0 ) {
            oid = identityProviderConfig.getOid();
            providerOid = oid;
        }

        return oid;
    }

    private String getId() {
        StringBuilder builder = new StringBuilder();
        builder.append( getProviderOid() );
        builder.append( ',' );
        builder.append( identityProviderConfig.getVersion() );
        return builder.toString();
    }

    private LdapIdentityProviderConfig getIdentityProviderConfig() {
        LdapIdentityProviderConfig config = identityProviderConfig;
        if ( config == null ) {
            throw new IllegalStateException("Not configured!");
        }
        return config;
    }

    private LdapGroup getCachedGroupByDn( final String dn ) {
        return getCachedGroup( GroupCacheKey.buildDnKey(dn) );
    }

    private LdapGroup getCachedGroupByCn( final String cn ) {
        return getCachedGroup( GroupCacheKey.buildCnKey(cn) );
    }

    private LdapGroup getCachedGroup( final GroupCacheKey key ) {
        LdapGroup ldapGroup = null;

        if ( groupCache != null ) {
            GroupCacheEntry entry = (GroupCacheEntry) groupCache.retrieve( key );
            if ( !isExpired(entry) ) {
                ldapGroup = entry.asLdapGroup();
            }
        }

        return ldapGroup;
    }

    private void cacheGroup( final LdapGroup group ) {
        if ( groupCache != null && group != null ) {
            GroupCacheEntry entry = new GroupCacheEntry(group);
            groupCache.store( GroupCacheKey.buildDnKey( group.getDn() ), entry, entry.timestamp );
            groupCache.store( GroupCacheKey.buildCnKey( group.getCn() ), entry, entry.timestamp );           
        }
    }

    private boolean isNonGroup( final LdapGroup group, final String member ) {
        boolean nonGroup = false;

        if ( groupCache != null ) {
            GroupCacheEntry entry = (GroupCacheEntry) groupCache.retrieve( GroupCacheKey.buildDnKey(group.getDn()) );
            if ( !isExpired(entry) ) {
                nonGroup = entry.isNonGroup( member );
            }
        }

        return nonGroup;
    }

    private void cacheNonGroups( final LdapGroup group, final Collection<String> nonGroupMembers ) {
        if ( groupCache != null && !nonGroupMembers.isEmpty() ) {
            GroupCacheEntry entry = (GroupCacheEntry) groupCache.retrieve( GroupCacheKey.buildDnKey(group.getDn()) );
            if ( !isExpired(entry) ) {
                entry.addNonGroups( nonGroupMembers );
            }
        }
    }

    private LdapGroup buildGroup( final String dn, final Attributes attributes ) throws NamingException {
        LdapGroup group = null;

        final LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();
        final GroupMappingConfig[] groupTypes = ldapIdentityProviderConfig.getGroupMappings();
        final Attribute objectclasses = attributes.get( LdapIdentityProvider.OBJECTCLASS_ATTRIBUTE_NAME);
        for (GroupMappingConfig groupType : groupTypes) {
            final String grpclass = groupType.getObjClass();
            if ( LdapUtils.attrContainsCaseIndependent(objectclasses, grpclass)) {
                Object tmp = LdapUtils.extractOneAttributeValue(attributes,
                        groupType.getNameAttrName());
                group = buildGroup( getProviderOid(), dn, tmp!=null ? tmp.toString() : null, attributes );
                break;
            }
        }

        return group;
    }

    private static LdapGroup buildGroup( final long providerOid, final String dn, final String cn, final Attributes attributes ) {
        LdapGroup ldapGroup = new LdapGroup();
        ldapGroup.setProviderId(providerOid);
        ldapGroup.setDn(dn);
        ldapGroup.setAttributes(attributes);
        if ( cn != null ) {
            ldapGroup.setCn( cn );
        }
        return ldapGroup;
    }

    private boolean isExpired( final GroupCacheEntry entry ) {
        boolean expired = true;

        if ( entry != null ) {
            long timestamp = entry.timestamp;
            expired = ( timestamp + cacheMaxAge ) < System.currentTimeMillis();
        }

        return expired;
    }

    private static final class GroupCacheKey extends Pair<String,String> {

        private GroupCacheKey( final String attribute,
                               final String value ) {
            super( attribute, value );
        }

        private static GroupCacheKey buildDnKey( final String dn ) {
            return new GroupCacheKey( "dn", dn );
        }

        private static GroupCacheKey buildCnKey( final String cn ) {
            return new GroupCacheKey( "cn", cn );    
        }
    }

    private static final class GroupCacheEntry {
        private final long providerOid;
        private final String dn;
        private final String cn;
        private final Attributes attributes;
        private final long timestamp = System.currentTimeMillis();
        private final Set<String> nonGroupMembers = Collections.synchronizedSet( new HashSet<String>() );

        private GroupCacheEntry( final LdapGroup ldapGroup ) {
            this( ldapGroup.getProviderId(),
                  ldapGroup.getDn(),
                  ldapGroup.getCn(),
                  ldapGroup.getAttributes() );
        }

        private GroupCacheEntry( final long providerOid,
                                 final String dn,
                                 final String cn,
                                 final Attributes attributes ) {
            this.providerOid = providerOid;
            this.dn = dn;
            this.cn = cn;
            this.attributes = attributes;
        }

        private LdapGroup asLdapGroup() {
            return buildGroup( providerOid, dn, cn, attributes );
        }

        public boolean isNonGroup( final String member ) {
            return nonGroupMembers.contains( member );
        }

        public void addNonGroups( final Collection<String> members ) {
            nonGroupMembers.addAll( members );
        }
    }

    private static final Logger logger = Logger.getLogger(LdapGroupManagerImpl.class.getName());

    private static final String memberNvPairAttribute = SyspropUtil.getString( "com.l7tech.server.identity.ldap.memberAttrName", "cn" );
    private static final boolean useSingleMemberNvPair = SyspropUtil.getBoolean( "com.l7tech.server.identity.ldap.useMemberAttrName", false );

    private static final int DEFAULT_GROUP_CACHE_SIZE = 100;
    private static final int DEFAULT_GROUP_CACHE_HIERARCHY_MAXAGE = 60000;
    private static final int DEFAULT_GROUP_MAX_NESTING = 0;

    private LdapIdentityProviderConfig identityProviderConfig;
    private LdapIdentityProvider identityProvider;
    private LdapRuntimeConfig ldapRuntimeConfig;
    private long providerOid;
    private LdapUtils.LdapTemplate ldapTemplate;
    private Cache groupCache;
    private int cacheSize = DEFAULT_GROUP_CACHE_SIZE;
    private int cacheMaxAge = DEFAULT_GROUP_CACHE_HIERARCHY_MAXAGE;
    private int groupMaxNesting = DEFAULT_GROUP_MAX_NESTING;
}
