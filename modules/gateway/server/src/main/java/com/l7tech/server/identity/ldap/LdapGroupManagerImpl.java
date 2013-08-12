package com.l7tech.server.identity.ldap;

import com.l7tech.identity.ldap.*;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.Pair;
import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.server.Lifecycle;
import com.l7tech.server.LifecycleException;
import com.whirlycott.cache.Cache;
import org.jetbrains.annotations.NotNull;

import javax.naming.*;
import javax.naming.ldap.LdapName;
import javax.naming.directory.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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

    // TODO should be configurable
    private static final int SUB_GROUP_DEPTH = 2;

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
        final GroupCacheKey groupCacheKeyByDN = GroupCacheKey.buildDnKey(dn);
        LdapGroup ldapGroup = getCachedGroup(groupCacheKeyByDN);
        if ( ldapGroup==null ) {
            synchronized ( groupCacheKeyByDN.getSync() ) {
                // Recheck in case someone else looked up the info
                ldapGroup = getCachedGroup(groupCacheKeyByDN);
                if ( ldapGroup==null ) {
                    final LdapGroup[] groupHolder = new LdapGroup[1];
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
                        logger.log(Level.WARNING, "LDAP error, while building group");
                        throw new FindException("naming exception", ExceptionUtils.getDebugException(e));
                    }

                    ldapGroup = groupHolder[0];
                }
            }
        }

        return ldapGroup;
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
        final GroupCacheKey groupCacheKeyByCn = GroupCacheKey.buildCnKey(name);
        LdapGroup ldapGroup = getCachedGroup(groupCacheKeyByCn);
        if ( ldapGroup == null ) {
            synchronized ( groupCacheKeyByCn.getSync() ) {
                // Recheck in case someone else looked up the info
                ldapGroup = getCachedGroup(groupCacheKeyByCn);
                if ( ldapGroup == null ) {
                    final LdapIdentityProvider identityProvider = getIdentityProvider();
                    final String filter = identityProvider.groupSearchFilterWithParam( name );
                    final LdapGroup[] groupHolder = new LdapGroup[1];
                    try {
                        ldapTemplate.search( filter, 2L, null, new LdapUtils.LdapListener(){
                            @Override
                            void searchResults( final NamingEnumeration<SearchResult> results ) throws NamingException {
                                try {
                                    if ( results.hasMore() ) {
                                        SearchResult result = results.next();
                                        groupHolder[0] = buildGroup( result.getNameInNamespace(), result.getAttributes() );
                                        cacheGroup( groupHolder[0] );
                                    }

                                    if ( results.hasMore() ) {
                                        logger.warning("search on group name returned more than one " +
                                                "result. returning 1st one");
                                    }
                                } catch (PartialResultException e) {
                                    LdapUtils.handlePartialResultException(e);
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
            }
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

        return isMember( user, group, true, 1 );
    }

    private boolean isMember( final User user,
                              final LdapGroup group,
                              final boolean processOuGroups,
                              final int depth ) throws FindException {
        LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();
        for(GroupMappingConfig groupMappingConfig : ldapIdentityProviderConfig.getGroupMappings()) {
            final MemberStrategy memberStrategy = groupMappingConfig.getMemberStrategy();
            if( MemberStrategy.MEMBERS_BY_OU.equals(memberStrategy) && processOuGroups ) {
                try {
                    final LdapName userDN = new LdapName(user.getId());
                    final LdapName groupDN = new LdapName(group.getDn());
                    if( userDN.startsWith(groupDN) ) {
                        return true;
                    }
                    // The user is not a member of this group, need to check subgroups
                    if ( groupNestingProcessNextDepth(depth) ) {
                        final List<LdapGroup> subgroups = new ArrayList<LdapGroup>();
                        if ( !getCachedSubgroups( group, subgroups ) ) {
                            synchronized( getSubGroupLookupSync( group ) ) {
                                // Recheck in case someone else looked up the info
                                if ( !getCachedSubgroups( group, subgroups ) ) {
                                    final String filter = identityProvider.groupSearchFilterWithParam("*");
                                    // use dn of group as base
                                    ldapTemplate.search( group.getDn(), filter, 0L, null, new LdapUtils.LdapListener(){
                                        @Override
                                        void attributes( final String dn, final Attributes attributes ) throws NamingException {
                                            if ( !group.getDn().equals(dn) ) { // avoid recursion on this group
                                                LdapGroup group = buildGroup(dn, attributes);
                                                if ( group != null ) {
                                                    subgroups.add( group );
                                                }
                                            }
                                        }
                                    });
                                    cacheSubgroups( group, subgroups );
                                }
                            }
                        }

                        // Process subgroups but skip any nested OU groups since if the user is a member
                        // of the nested OU group they would have been a member of this OU group                        
                        for( LdapGroup subgroup : subgroups ) {
                            if ( isMember(user, subgroup, false, depth + 1) ) {
                                return true;
                            }
                        }
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
                        final String member = (String)membersAttribute.get(i);
                        if( MemberStrategy.MEMBERS_ARE_LOGIN.equals(memberStrategy) ) {
                            if ( membershipEquality( member, user.getLogin() ) ) {
                                return true;
                            }
                        } else if ( MemberStrategy.MEMBERS_ARE_DN.equals(memberStrategy) ) {
                            if ( membershipEquality( member, user.getId() ) ) {
                                return true;
                            }
                        } else if ( MemberStrategy.MEMBERS_ARE_NVPAIR.equals(memberStrategy) ) {
                            for ( String nameAttribute : getDistinctUserNameAttributeNames() ) {
                                if ( membershipEquality( member, nameAttribute + "=" + user.getName() ) ) {
                                    return true;
                                }
                            }
                            if ( membershipEquality( member, user.getId() ) ) {
                                return true;
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
                                    String memberName = groupNameFromNVPair( memberString );
                                    subgroup = findByName(memberName);
                                }

                                if( subgroup != null ) {
                                    if ( isMember(user, subgroup, true, depth + 1) ) {
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

    private boolean membershipEquality( final String value1, final String value2 ) {
        boolean equal;

        // For backwards compatibility we'll support the old (global) system
        // property setting in addition to the new per provider setting.
        //
        // The global setting should be ignored if it has the default value.
        if ( getIdentityProviderConfig().isGroupMembershipCaseInsensitive() || !compareMembershipCaseSensitively ) {
            equal = value1.equalsIgnoreCase( value2 );
        } else {
            equal = value1.equals( value2 );
        }

        return equal;
    }

    private String groupNameFromNVPair( final String memberString ) {
        String memberName = memberString;
        int index = memberName.indexOf( '=' );
        if ( index > 0 ) {
            memberName = memberName.substring( index+1 );
        }
        return memberName;
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
                  filter.or();
                  for ( String name : names ) {
                      filter.attrEquals( mbmAttrName, name + "=" + user.getName() );
                  }
                  filter.attrEquals( mbmAttrName, user.getId() );
                  filter.end();
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

                final long maxSize = ldapRuntimeConfig.getMaxGroupSearchResultSize();
                try {
                    final DirContext searchContext = context;
                    ldapTemplate.search( context, filter.buildFilter(), maxSize, null, new LdapUtils.LdapListener(){
                        @Override
                        boolean searchResult( final SearchResult sr ) throws NamingException {
                            IdentityHeader header = identityProvider.searchResultToHeader(sr);
                            if ( header != null && output.add(header) ) {
                                // Groups within groups.
                                // check if this group is a member of other groups. if so, then user belongs to those
                                // groups too.
                                if ( groupNestingEnabled()  ) {
                                    getSubGroups(searchContext, output, header, SUB_GROUP_DEPTH);
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
                    output.setMaxExceeded(maxSize);
                    // dont throw here, we still want to return what we got
                } catch (NamingException e) {
                    String msg = "error getting answer";
                    logger.log(Level.WARNING, msg, e);
                    throw new FindException(msg, e);
                }

                if ( checkOuStrategyToo ) {
                    // look for OU memberships
                    addOUGroups( context, output, user.getId(), 1 );
                }
            } finally {
                ResourceUtils.closeQuietly( context );
            }
        }

        return output;
    }

    @Override
    public Set<IdentityHeader> getGroupHeadersForNestedGroup(@NotNull final String groupId) throws FindException {
        final EntityHeaderSet<IdentityHeader> output = new EntityHeaderSet<>();
        if (groupNestingEnabled()) {
            final IdentityHeader header = new IdentityHeader(getProviderOid(), groupId, EntityType.GROUP, null, null, null, null);
            try {
                final DirContext context = getIdentityProvider().getBrowseContext();
                getSubGroups(context, output, header, SUB_GROUP_DEPTH);
            } catch (final NamingException e) {
                throw new FindException("Unable to retrieve groups for group: " + groupId, e);
            }
        }
        return output;
    }

    /**
     * If the given DN has OUs, then add as groups.
     */
    private void addOUGroups( final DirContext context,
                              final EntityHeaderSet<IdentityHeader> output,
                              final String dn,
                              final int depth ) {
        final String dnLower = dn.toLowerCase();
        int pos = 1; //NOTE: 1 since the dn itself should not be tested as an OU group (should already have been added)
        int res = dnLower.indexOf("ou=", pos);
        while (res >= 0) {
            // is there a valid organizational unit there?
            try {
                LdapGroup group = findByPrimaryKey( dn.substring(res) );
                if ( group != null ) {
                    IdentityHeader header = groupToHeader(group);
                    if ( output.add( header ) && groupNestingProcessNextDepth(depth) ) {
                        getSubGroups(context, output, header, depth + 1);
                    }
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
     * @param context the context to use
     * @param groups The set of groups to add results to
     * @param groupHeader the header for the group to inspect
     * @param depth The current nesting depth
     */
    private void getSubGroups( final DirContext context,
                               final EntityHeaderSet<IdentityHeader> groups,
                               final IdentityHeader groupHeader,
                               final int depth ) {
        final LdapIdentityProvider identityProvider = getIdentityProvider();
        String filter = subGroupSearchString( groupHeader );
        if (filter != null) {
            final long maxSize = ldapRuntimeConfig.getMaxGroupSearchResultSize();
            try {
                ldapTemplate.search( context, filter, maxSize, null, new LdapUtils.LdapListener(){
                    @Override
                    boolean searchResult( final SearchResult sr ) throws NamingException {
                        IdentityHeader header = identityProvider.searchResultToHeader(sr);
                        if (header != null && header.getType().equals(EntityType.GROUP)) {
                            if ( groups.add(header) && groupNestingProcessNextDepth(depth) ) {
                                getSubGroups(context, groups, header, depth + 1);
                            }
                        }
                        return true;
                    }
                } );
            } catch (SizeLimitExceededException e) {
                // add something to the result that indicates the fact that the search criteria is too wide
                logger.log(Level.FINE, "the search results exceeded the maximum.", e);
                groups.setMaxExceeded(maxSize);
                // dont throw here, we still want to return what we got
            } catch (NamingException e) {
                logger.log(Level.WARNING, "naming exception with filter " + filter, e);
            }
        }

        // look for sub-OU memberships
        addOUGroups( context, groups, groupHeader.getStrId(), depth );
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
    private String subGroupSearchString( final IdentityHeader groupHeader ) {
        LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();

        GroupMappingConfig[] groupTypes = ldapIdentityProviderConfig.getGroupMappings();
        LdapSearchFilter searchFilter = new LdapSearchFilter();
        searchFilter.or();
        for (GroupMappingConfig groupType : groupTypes) {
            if (groupType.getMemberStrategy().equals(MemberStrategy.MEMBERS_ARE_DN) ) {
                searchFilter.and();
                  searchFilter.objectClass( groupType.getObjClass() );
                  searchFilter.attrEquals( groupType.getMemberAttrName(), groupHeader.getStrId() );
                searchFilter.end();
            } else if (groupType.getMemberStrategy().equals(MemberStrategy.MEMBERS_ARE_LOGIN) ) {
                searchFilter.and();
                  searchFilter.objectClass( groupType.getObjClass() );
                  searchFilter.attrEquals( groupType.getMemberAttrName(), groupHeader.getName() );
                searchFilter.end();
            } else if (groupType.getMemberStrategy().equals(MemberStrategy.MEMBERS_ARE_NVPAIR) ) {
                searchFilter.and();
                  searchFilter.objectClass( groupType.getObjClass() );
                  searchFilter.attrEqualsUnsafe( groupType.getMemberAttrName(), groupType.getNameAttrName() + "=" +LdapUtils.filterEscape(groupHeader.getName()) );
                searchFilter.end();

                // This seems wrong, but leaving in case it was here for a good reason
                searchFilter.and();
                  searchFilter.objectClass( groupType.getObjClass() );
                  searchFilter.attrEquals( groupType.getMemberAttrName(), groupHeader.getStrId() );
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
        return getUserHeaders( group, 1, new HashSet<String>(), new HashSet<String>() );
    }

    private EntityHeaderSet<IdentityHeader> getUserHeaders( final LdapGroup group,
                                                            final int depth,
                                                            final Set<String> processedGroupNames,
                                                            final Set<String> processedGroupIds ) throws FindException {
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
                                collectOUGroupMembers(searchContext, dn, headers, depth, processedGroupNames, processedGroupIds);
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
                                            } else if ( groupNestingProcessNextDepth(depth) && !processedGroupNames.contains(memberlogin) ) {
                                                try {
                                                    LdapGroup subgroup = findByName(memberlogin);
                                                    if ( subgroup != null ) {
                                                        Set<IdentityHeader> subgroupmembers = getUserHeaders(subgroup, depth + 1, processedGroupNames, processedGroupIds);
                                                        headers.addAll(subgroupmembers);
                                                        processedGroupNames.add( subgroup.getName() );
                                                        processedGroupIds.add( subgroup.getDn() );
                                                    }
                                                } catch (FindException e) {
                                                    logger.log( Level.WARNING, "Error checking for nested group '"+memberlogin+"'.", e );
                                                }
                                            }
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
                                                    LdapGroup subgroup = null;
                                                    if ( !processedGroupIds.contains(memberhint)  ) {
                                                        try {
                                                            subgroup = findByPrimaryKey(memberhint);
                                                        } catch (FindException e) {
                                                            // nothing on purpose
                                                            logger.finest("seems like " + memberhint +
                                                                    " is not a group dn" + e.getMessage());
                                                        }
                                                    }
                                                    if (subgroup == null && !processedGroupNames.contains(memberhint)) {
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
                                                        Set<IdentityHeader> subgroupmembers = getUserHeaders(subgroup, depth + 1, processedGroupNames, processedGroupIds);
                                                        headers.addAll(subgroupmembers);
                                                        processedGroupNames.add( subgroup.getName() );
                                                        processedGroupIds.add( subgroup.getDn() );
                                                        done = true;
                                                    }
                                                }
                                            } catch (FindException e) {
                                                logger.log(Level.FINEST, "cannot resolve user through dn, try nv" +
                                                        "pair method", e);
                                            }
                                            // if that dont work, try search assuming nv pair
                                            if (!done) {
                                                if ( !nvSearchForUser(searchContext, memberhint, headers) && groupNestingProcessNextDepth(depth) ) {
                                                    String groupName = groupNameFromNVPair( memberhint );
                                                    if ( !processedGroupNames.contains(groupName) ) {
                                                        try {
                                                            LdapGroup subgroup = findByName(groupName);
                                                            if ( subgroup != null ) { 
                                                                Set<IdentityHeader> subgroupmembers = getUserHeaders(subgroup, depth + 1, processedGroupNames, processedGroupIds);
                                                                headers.addAll(subgroupmembers);
                                                                processedGroupNames.add( subgroup.getName() );
                                                                processedGroupIds.add( subgroup.getDn() );
                                                            }
                                                        } catch (FindException e) {
                                                            logger.log( Level.WARNING, "Error checking for nested group '"+groupName+"'.", e );
                                                        }
                                                    }
                                                }
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
                                        final int depth,
                                        final Set<String> processedGroupNames,
                                        final Set<String> processedGroupIds ) throws NamingException {
        final LdapIdentityProvider identityProvider = getIdentityProvider();
        final long maxSize = ldapRuntimeConfig.getMaxGroupSearchResultSize();
        if ( (long) memberHeaders.size() >= maxSize) return;

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
            memberHeaders.setMaxExceeded(maxSize);
            // dont throw here, we still want to return what we got
        }

        // sub group search
        if ( (long) memberHeaders.size() < maxSize &&
             groupNestingProcessNextDepth(depth) ) {
            filter = identityProvider.groupSearchFilterWithParam("*");
            // use dn of group as base
            try {
                ldapTemplate.search( context, dn, filter, maxSize, null, new LdapUtils.LdapListener(){
                    @Override
                    boolean searchResult( final SearchResult sr ) throws NamingException {
                        if (sr != null && sr.getName() != null && sr.getName().length() > 0) {
                            String entitydn = sr.getNameInNamespace();
                            if (!entitydn.equals(dn) && !processedGroupIds.contains( entitydn )) { // avoid recursion on this group
                                try {
                                    LdapGroup subGroup = findByPrimaryKey(entitydn);
                                    if ( subGroup != null ) {
                                        Set<IdentityHeader> subGroupMembers = getUserHeaders(subGroup, depth + 1, processedGroupNames, processedGroupIds );
                                        memberHeaders.addAll(subGroupMembers);
                                        processedGroupNames.add( subGroup.getName() );
                                        processedGroupIds.add( subGroup.getDn() );
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
                memberHeaders.setMaxExceeded(maxSize);
                // dont throw here, we still want to return what we got
            }
        }
    }

    private boolean nvSearchForUser( final DirContext context,
                                     final String nvhint,
                                     final Set<IdentityHeader> memberHeaders ) throws NamingException {
        final LdapIdentityProvider identityProvider = getIdentityProvider();
        final LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();
        final long maxSize = ldapRuntimeConfig.getMaxGroupSearchResultSize();
        if ( (long) memberHeaders.size() >= maxSize) return true;

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

        final boolean[] foundUser = new boolean[1];
        try {
            ldapTemplate.search( context, filter.buildFilter(), maxSize, null, new LdapUtils.LdapListener(){
                @Override
                boolean searchResult( final SearchResult sr ) throws NamingException {
                    IdentityHeader newheader = identityProvider.searchResultToHeader(sr);
                    if (newheader == null) {
                        logger.info("User " + sr.getNameInNamespace() + " is not valid according to the template and will not " +
                                    "be considered as a member");
                    } else if (!memberHeaders.contains(newheader)) {
                        foundUser[0] = true;
                        memberHeaders.add(newheader);
                    } else {
                        foundUser[0] = true;
                    }
                    return true;
                }
            } );
        } catch (OperationNotSupportedException e) {
            // this gets thrown by oid for some weird groups
            logger.log(Level.FINE, ldapIdentityProviderConfig.getName() + "directory cannot search on" + nvhint, e);
        }

        return foundUser[0];
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

    private long getConfigValue( final Long configValue, final long defaultValue ) {
        long result = defaultValue;

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

        if ( oid == 0L ) {
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

    private LdapGroup getCachedGroup( final GroupCacheKey key ) {
        LdapGroup ldapGroup = null;

        final Cache groupCache = this.groupCache;
        if ( groupCache != null ) {
            GroupCacheEntry entry = (GroupCacheEntry) groupCache.retrieve( key );
            if ( !isExpired(entry) ) {
                ldapGroup = entry.asLdapGroup();
            }
        }

        return ldapGroup;
    }

    private void cacheGroup( final LdapGroup group ) {
        final Cache groupCache = this.groupCache;
        if ( groupCache != null && group != null ) {
            GroupCacheEntry entry = new GroupCacheEntry(group);
            groupCache.store( GroupCacheKey.buildDnKey( group.getDn() ), entry, entry.timestamp );
            groupCache.store( GroupCacheKey.buildCnKey( group.getCn() ), entry, entry.timestamp );           
        }
    }

    private boolean isNonGroup( final LdapGroup group, final String member ) {
        boolean nonGroup = false;

        final Cache groupCache = this.groupCache;
        if ( groupCache != null ) {
            GroupCacheEntry entry = (GroupCacheEntry) groupCache.retrieve( GroupCacheKey.buildDnKey(group.getDn()) );
            if ( !isExpired(entry) ) {
                nonGroup = entry.isNonGroup( member );
            }
        }

        return nonGroup;
    }

    private void cacheNonGroups( final LdapGroup group, final Collection<String> nonGroupMembers ) {
        final Cache groupCache = this.groupCache;
        if ( groupCache != null && !nonGroupMembers.isEmpty() ) {
            GroupCacheEntry entry = (GroupCacheEntry) groupCache.retrieve( GroupCacheKey.buildDnKey(group.getDn()) );
            if ( !isExpired(entry) ) {
                entry.addNonGroups( nonGroupMembers );
            }
        }
    }

    private Object getSubGroupLookupSync( final LdapGroup group ) {
        return (getClass().getName() + "-subgroups-for-" + group.getDn()).intern();
    }

    private boolean getCachedSubgroups( final LdapGroup group, final Collection<LdapGroup> subgroups ) {
        boolean foundSubgroups = false;

        final Cache groupCache = this.groupCache;
        if ( groupCache != null ) {
            GroupCacheEntry entry = (GroupCacheEntry) groupCache.retrieve( GroupCacheKey.buildDnKey(group.getDn()) );
            if ( !isExpired(entry) ) {
                Collection<LdapGroup> entrySubgroups = entry.getSubgroups();
                if ( entrySubgroups != null ) {
                    foundSubgroups = true;
                    subgroups.addAll( entrySubgroups );
                }
            }
        }

        return foundSubgroups;
    }

    private void cacheSubgroups( final LdapGroup group, final Collection<LdapGroup> subgroups ) {
        final Cache groupCache = this.groupCache;
        if ( groupCache != null ) {
            GroupCacheEntry entry = (GroupCacheEntry) groupCache.retrieve( GroupCacheKey.buildDnKey(group.getDn()) );
            if ( !isExpired(entry) ) {
                entry.setSubgroups( subgroups );
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

        private Object getSync() {
            return (getClass().getName() + "-group-for-(" + left + "," + right + ")").intern();
        }
    }

    private static final class GroupCacheEntry {
        private final long providerOid;
        private final String dn;
        private final String cn;
        private final Attributes attributes;
        private final long timestamp = System.currentTimeMillis();
        private final Set<String> nonGroupMembers = Collections.synchronizedSet( new HashSet<String>() );
        private final AtomicReference<Collection<LdapGroup>> subgroups = new AtomicReference<Collection<LdapGroup>>();

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

        public Collection<LdapGroup> getSubgroups() {
            return subgroups.get();    
        }

        public void setSubgroups( final Collection<LdapGroup> subgroups ) {
            this.subgroups.set( Collections.unmodifiableCollection(new ArrayList<LdapGroup>(subgroups)) );
        }
    }

    private static final Logger logger = Logger.getLogger(LdapGroupManagerImpl.class.getName());

    private static final String memberNvPairAttribute = ConfigFactory.getProperty( "com.l7tech.server.identity.ldap.memberAttrName", "cn" );
    private static final boolean useSingleMemberNvPair = ConfigFactory.getBooleanProperty( "com.l7tech.server.identity.ldap.useMemberAttrName", false );
    private static final boolean compareMembershipCaseSensitively = ConfigFactory.getBooleanProperty( "com.l7tech.server.identity.ldap.compareMembersCaseSensitively", true );

    private static final int DEFAULT_GROUP_CACHE_SIZE = 0; // Zero for backwards compatibility
    private static final long DEFAULT_GROUP_CACHE_HIERARCHY_MAXAGE = 60000L;
    private static final int DEFAULT_GROUP_MAX_NESTING = 0; // Unlimited for backwards compatibility

    private LdapIdentityProviderConfig identityProviderConfig;
    private LdapIdentityProvider identityProvider;
    private LdapRuntimeConfig ldapRuntimeConfig;
    private long providerOid;
    private LdapUtils.LdapTemplate ldapTemplate;
    private Cache groupCache;
    private int cacheSize = DEFAULT_GROUP_CACHE_SIZE;
    private long cacheMaxAge = DEFAULT_GROUP_CACHE_HIERARCHY_MAXAGE;
    private int groupMaxNesting = DEFAULT_GROUP_MAX_NESTING;
}
