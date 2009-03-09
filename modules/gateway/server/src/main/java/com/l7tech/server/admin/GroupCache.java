package com.l7tech.server.admin;

import com.l7tech.identity.*;
import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.whirlycott.cache.Cache;

import java.util.Set;
import java.util.HashSet;
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
    * @param u the User we want the set of Set<Principal> for
    * @param ip the IdentityProvider the user belongs to. This can be used to validate the user if required
    * @param skipAccountValidation <code>true</code> to skip checking for whether the user account has expired or been disabled
    * @return Set<Group> null if the user has no group membership, otherwise a Group for
    * each group the user is a member of
    * */
    public Set<IdentityHeader> getCachedValidatedGroups(User u, IdentityProvider ip, boolean skipAccountValidation) throws ValidationException {

        final long providerOid = ip.getConfig().getOid();
        final CacheKey ckey = new CacheKey(providerOid, u.getId());
        final Set<IdentityHeader> cached = getCacheEntry(ckey, u.getLogin(), ip, cacheMaxTime.get());

        if ( cached != null ) {
            return cached;
        }

        return getAndCacheNewResult(u, ckey, ip, skipAccountValidation);
    }

    void setCacheMaxTime( final int cacheMaxTime ) {
        this.cacheMaxTime.set( cacheMaxTime );        
    }

    /*
    * @return either Set<Group> or Long
    * */
    @SuppressWarnings({"unchecked"})
    private Set<IdentityHeader> getCacheEntry(CacheKey ckey, String login, IdentityProvider idp, int maxAge) {

        if (cache == null) return null; // fail fast if cache is disabled

        CacheEntry groups = null;

        Object cachedObj = cache.retrieve(ckey);
        if( cachedObj != null && cachedObj instanceof CacheEntry ){
            groups = (CacheEntry) cachedObj;
        }

        if( groups == null ){
            return null;
        }

        long cacheAddedTime = groups.getTimestamp();
        if ( cacheAddedTime + maxAge > System.currentTimeMillis() ) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Cache hit for user {1} from IdP \"{2}\"", new String[] {login, idp.getConfig().getName()});
            return (Set<IdentityHeader>) groups.getCachedObject();
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Cache expiry for user {1} is stale on IdP \"{2}\"; will revalidate", new String[] { login, idp.getConfig().getName()});
            }
            return null;
        }
    }


    // If caller wants only one thread at a time to authenticate any given username,
    // caller is responsible for ensuring that only one thread at a time calls this per username,
    @SuppressWarnings({"unchecked"})
    private Set<IdentityHeader> getAndCacheNewResult(User u, CacheKey ckey, IdentityProvider idp, boolean skipAccountValidation) throws ValidationException {
        int cacheMaxGroups = this.cacheMaxGroups.get();
        if (!skipAccountValidation) idp.validate(u);
        //download group info and any other info to be added as a gP as and when required here..

        GroupManager gM = idp.getGroupManager();
        Set<IdentityHeader> gHeaders;
        try {
            gHeaders = gM.getGroupHeaders(u);
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error accessing groups for user '"+u.getId()+"', error message '"+ExceptionUtils.getMessage(fe)+"'.", fe );
            throw new ValidationException("Error accessing groups for user '"+u.getId()+"'.");
        }

        if( gHeaders != null && gHeaders.size() > 0 ){
            Set<IdentityHeader> groupSet = new HashSet<IdentityHeader>();
            int count = 1;
            for(IdentityHeader iH: gHeaders){
                if(count > cacheMaxGroups){
                   logger.log(Level.INFO, "Capping group membership for user '"+u.getId()+"' at " + cacheMaxGroups);
                   break;
                }
                groupSet.add(iH);
                count++;
            }
            CacheEntry<Set<IdentityHeader>> groupPrincipals = new CacheEntry<Set<IdentityHeader>>(groupSet);
            this.cache.store(ckey, groupPrincipals);
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           "Cached group membership principals for user {n0} on IdP \"{1}\"",
                           new String[]{u.getLogin(), idp.getConfig().getName()});

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
        private final long providerOid;
        private final String userId;

        public CacheKey(long providerOid, String userId) {
            this.providerOid = providerOid;
            this.userId = userId;
        }

        /** @noinspection RedundantIfStatement*/
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final CacheKey cacheKey = (CacheKey)o;

            if (providerOid != cacheKey.providerOid) return false;
            if (userId != null ? !userId.equals(cacheKey.userId) : cacheKey.userId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            if (cachedHashcode == -1) {
                int result;
                result = (int)(providerOid ^ (providerOid >>> 32));
                result = 31 * result + (userId != null ? userId.hashCode() : 0);
                cachedHashcode = result;
            }
            return cachedHashcode;
        }
    }    
}
