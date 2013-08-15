package com.l7tech.server.admin;

import com.l7tech.identity.*;
import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.whirlycott.cache.Cache;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 17, 2008
 * Time: 3:32:34 PM
 * Provide the cache implementation required by the {@link com.l7tech.server.admin.AdminLoginImpl}
 */
class GroupCache {

    private static final Logger logger = Logger.getLogger(GroupCache.class.getName());

    private final AtomicInteger cacheMaxTime = new AtomicInteger();
    private final AtomicInteger cacheMaxGroups = new AtomicInteger();
    private final Map<Goid,Long> providerInvalidation = new ConcurrentHashMap<Goid,Long>();
    private final Cache cache;
    
    GroupCache( final String name, final int cacheMaxSize, final int cacheMaxTime, final int cacheMaxGroups ){
        this.cacheMaxTime.set(cacheMaxTime);
        this.cacheMaxGroups.set(cacheMaxGroups);
        cache = cacheMaxSize < 1 ? null :
                WhirlycacheFactory.createCache(name, cacheMaxSize, 293, WhirlycacheFactory.POLICY_LFU);
    }

    /*
    * Look up the user in the cache and return it's Set<Principal> representing its
    * entire group membership
    *
    * @param u the User we want the set of Set<Principal> for
    * @param ip the IdentityProvider the user belongs to. This can be used to validate the user if required
    * @param skipAccountValidation <code>true</code> to skip checking for whether the user account has expired or been disabled
    * @return Set<Group> null if the user has no group membership, otherwise a Group for
    * each group the user is a member of
    * */
    public Set<IdentityHeader> getCachedValidatedGroups( final User u,
                                                         final IdentityProvider ip,
                                                         final boolean skipAccountValidation ) throws ValidationException {

        final Goid providerOid = ip.getConfig().getGoid();
        final CacheKey ckey = new CacheKey(providerOid, EntityType.USER, u.getId());
        final Set<IdentityHeader> cached = getCacheEntry(ckey, u.getLogin(), ip, cacheMaxTime.get());

        if ( cached != null ) {
            return cached;
        }

        return getAndCacheNewResult(u, ckey, ip, skipAccountValidation);
    }

    /**
     * Look up the group in the cache and return it's Set<IdentityHeader> representing its entire group membership
     * @param g the Group to look up.
     * @param ip the IdentityProvider the group belongs to.
     * @return null if the group has no group membership, otherwise a a set of Identity headers for each group the group is a member of
     * @throws FindException
     */
    public Set<IdentityHeader> getCachedGroups(final Group g, final IdentityProvider ip) throws FindException {
        final Goid providerOid = ip.getConfig().getGoid();
        final CacheKey ckey = new CacheKey(providerOid, EntityType.GROUP, g.getId());
        final Set<IdentityHeader> cached = getCacheEntry(ckey, g.getName(), ip, cacheMaxTime.get());
        if ( cached != null ) {
            return cached;
        }
        return getAndCacheNewResult(g, ckey, ip);
    }

    /**
     * Invalidate any cached data for the given provider.
     *
     * @param providerOid The provider identifier
     */
    public void invalidate( final Goid providerOid ) {
        providerInvalidation.put( providerOid, System.currentTimeMillis() );
    }

    void setCacheMaxTime( final int cacheMaxTime ) {
        this.cacheMaxTime.set( cacheMaxTime );        
    }

    /*
    * @return either Set<Group> or Long
    * */
    @SuppressWarnings({"unchecked"})
    private Set<IdentityHeader> getCacheEntry(CacheKey ckey, String identityId, IdentityProvider idp, int maxAge) {

        if (cache == null) return null; // fail fast if cache is disabled

        CacheEntry groups = null;

        final Object cachedObj = cache.retrieve(ckey);
        if( cachedObj != null && cachedObj instanceof CacheEntry ){
            groups = (CacheEntry) cachedObj;
        }

        if( groups == null ){
            return null;
        }

        final long cacheAddedTime = groups.getTimestamp();
        final Long invalidationTime = providerInvalidation.get( ckey.providerOid );
        if ( (cacheAddedTime + (long)maxAge > System.currentTimeMillis()) && (invalidationTime==null || invalidationTime < cacheAddedTime) ) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Cache hit for user/group {0} from IdP \"{1}\"", new String[] {identityId, idp.getConfig().getName()});
            return (Set<IdentityHeader>) groups.getCachedObject();
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Cache expiry for user/group {0} is stale on IdP \"{1}\"; will revalidate", new String[] { identityId, idp.getConfig().getName()});
            }
            return null;
        }
    }


    // If caller wants only one thread at a time to authenticate any given username,
    // caller is responsible for ensuring that only one thread at a time calls this per username,
    private <UT extends User, GT extends Group, GMT extends GroupManager<UT,GT>>
    Set<IdentityHeader> getAndCacheNewResult(final User uu, final CacheKey ckey, final IdentityProvider<UT,?,?,GMT> idp, final boolean skipAccountValidation) throws ValidationException {
        final UT u;
        if (uu instanceof UserBean) {
            u = idp.getUserManager().reify((UserBean) uu);
        } else {
            //noinspection unchecked
            u = (UT)uu;
        }

        if (!skipAccountValidation) idp.validate(u);
        //download group info and any other info to be added as a gP as and when required here..

        GMT gM = idp.getGroupManager();
        Set<IdentityHeader> gHeaders;
        try {
            gHeaders = gM.getGroupHeaders(u);
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error accessing groups for user '"+u.getId()+"', error message '"+ExceptionUtils.getMessage(fe)+"'.", fe );
            throw new ValidationException("Error accessing groups for user '"+u.getId()+"'.");
        }

        return capAndCacheGroups(ckey, idp, Either.<UT, GT>left(u), gHeaders);
    }

    private <UT extends User, GT extends Group, GMT extends GroupManager<UT,GT>>
        Set<IdentityHeader> getAndCacheNewResult(final Group group, final CacheKey ckey, final IdentityProvider<UT,?,?,GMT> idp) throws FindException {
        final GT g;
        if (group instanceof GroupBean) {
            g = idp.getGroupManager().reify((GroupBean) group);
        } else {
            //noinspection unchecked
            g = (GT)group;
        }

        GMT gM = idp.getGroupManager();
        Set<IdentityHeader> gHeaders = gM.getGroupHeadersForNestedGroup(group.getId());

        return capAndCacheGroups(ckey, idp, Either.<UT, GT>right(g), gHeaders);
    }

    /**
     * @return a subset of the given group identity headers which have been capped at the max number allowed and cached.
     */
    private <UT extends User, GT extends Group, GMT extends GroupManager<UT, ?>> Set<IdentityHeader> capAndCacheGroups(CacheKey ckey,
                                                                                                                       IdentityProvider<UT, ?, ?, GMT> idp,
                                                                                                                       Either<UT, GT> identity,
                                                                                                                       Set<IdentityHeader> gHeaders) {
        if( gHeaders != null ){
            int cacheMaxGroups = this.cacheMaxGroups.get();
            Set<IdentityHeader> groupSet = new HashSet<>();
            int count = 1;
            String id = null;
            if (identity.isLeft()) {
                id = identity.left().getLogin();
            } else {
                id = identity.right().getName();
            }
            for(IdentityHeader iH: gHeaders){
                if(count > cacheMaxGroups){
                   logger.log(Level.INFO, "Capping group membership for user/group '"+id+"' at " + cacheMaxGroups);
                   break;
                }
                groupSet.add(iH);
                count++;
            }
            CacheEntry<Set<IdentityHeader>> groupPrincipals = new CacheEntry<Set<IdentityHeader>>(groupSet);
            this.cache.store(ckey, groupPrincipals);
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           "Cached group membership principals for user/group {0} on IdP \"{1}\"",
                           new String[]{id, idp.getConfig().getName()});

            return groupSet;
        }else{
            return null;
        }
    }

    /*Only used by unit tests*/
    void dispose() {
        WhirlycacheFactory.shutdown(cache);
    }

    public static class CacheKey {
        private int cachedHashcode = -1;
        private final Goid providerOid;
        // user/group
        private final EntityType entityType;
        private final String identityId;

        public CacheKey(Goid providerOid, final EntityType entityType, String identityId) {
            this.providerOid = providerOid;
            this.entityType = entityType;
            this.identityId = identityId;
        }

        /** @noinspection RedundantIfStatement*/
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final CacheKey cacheKey = (CacheKey)o;

            if (providerOid != null ? !providerOid.equals(cacheKey.providerOid) : cacheKey.providerOid != null) return false;
            if (entityType != cacheKey.entityType) return false;
            if (identityId != null ? !identityId.equals(cacheKey.identityId) : cacheKey.identityId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            if (cachedHashcode == -1) {
                int result;
                result = (providerOid != null ? providerOid.hashCode() : 0);
                result = 31 * result + (entityType != null ? entityType.hashCode() : 0);
                result = 31 * result + (identityId != null ? identityId.hashCode() : 0);
                cachedHashcode = result;
            }
            return cachedHashcode;
        }
    }    
}
