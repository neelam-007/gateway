package com.l7tech.server.admin;

import com.l7tech.identity.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.admin.ValidationRuntimeException;
import com.whirlycott.cache.Cache;

import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 17, 2008
 * Time: 3:32:34 PM
 * Provide the cache implementation required by the {@link com.l7tech.server.admin.AdminLoginImpl}
 * implementation of the {@link SessionValidator} interface
 */
public class GroupPrincipalCache {

    private static final Logger logger = Logger.getLogger(GroupPrincipalCache.class.getName());
    
    private static int CACHE_SIZE = ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_PRINCIPAL_SESSION_CACHE_SIZE, 100);
    private static int CACHE_MAX_GROUPS = ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_PRINCIPAL_SESSION_CACHE_MAX_PRINCIPAL_GROUPS, 50);

    private final Cache principalCache;
    
    private GroupPrincipalCache(){
        String name = "PrincipalCache_unified";
        principalCache = CACHE_SIZE < 1 ? null :
                WhirlycacheFactory.createCache(name, CACHE_SIZE, 293, WhirlycacheFactory.POLICY_LFU);

    }


    /*
    * validate will look up the user in the cache and return it's Set<Principal> representing its
    * entire group membership
    * @param u the User we want the set of Set<Principal> for
    * @param ip the IdentityProvider the user belongs to. This can be used to validate the user if required
    * @return Set<GroupPrincipal> null if the user has no group membership, otherwise a GroupPrincipal for
    * each group the user is a member of
    * */
    public Set<GroupPrincipal> getCachedValidatedPrincipals(User u, IdentityProvider ip,
                                                       int maxAge) throws ValidationException{

        final long providerOid = ip.getConfig().getOid();
        final CacheKey ckey = new CacheKey(providerOid, u.getId());
        Object cached = getCacheEntry(ckey, u.getLogin(), ip, maxAge);

        if (cached instanceof Set) {
            return (Set<GroupPrincipal>)cached;
        } else if (cached != null) {
            return null;
        }        

        return getAndCacheNewResult(u, ckey, ip);
    }

    private static class InstanceHolder {
        private static final GroupPrincipalCache INSTANCE = new GroupPrincipalCache();
    }

    public static GroupPrincipalCache getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /*
    * @return either Set<GroupPrincipal> or Long
    * */
    private Object getCacheEntry(CacheKey ckey, String login, IdentityProvider idp, int maxAge) {

        if (principalCache == null) return null; // fail fast if cache is disabled

        CacheEntry<Set<GroupPrincipal>> groupPrincipals = null;

        Object cachedObj = principalCache.retrieve(ckey);
        if(cachedObj != null && cachedObj instanceof CacheEntry){
            groupPrincipals = (CacheEntry<Set<GroupPrincipal>>)cachedObj;
        }

        if(cachedObj == null){
            return null;
        }

        Object returnValue;
        long cacheAddedTime;
        cacheAddedTime = groupPrincipals.getTimestamp();
        returnValue = groupPrincipals.getCachedObject();

        if (cacheAddedTime + maxAge > System.currentTimeMillis()) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Cache hit for user {1} from IdP \"{2}\"", new String[] {login, idp.getConfig().getName()});
            return returnValue;
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Cache expiry for user {1} is stale on IdP \"{2}\"; will revalidate", new String[] { login, idp.getConfig().getName()});
            }
            return null;
        }
    }


    // If caller wants only one thread at a time to authenticate any given username,
    // caller is responsible for ensuring that only one thread at a time calls this per username,
    private Set<GroupPrincipal> getAndCacheNewResult(User u, CacheKey ckey, IdentityProvider idp) throws ValidationException{
        idp.validate(u);
        //download group info and any other info to be added as a gP as and when required here..

        GroupManager gM = idp.getGroupManager();
        Set<IdentityHeader> gHeaders = null;
        try{
            gHeaders = gM.getGroupHeaders(u);
        }catch(FindException fe){
            String msg = "Cannot find users groups";
            logger.log(Level.FINE, msg, fe);
            throw new ValidationException(msg);
        }

        if(gHeaders != null && gHeaders.size() > 0){
            Set<GroupPrincipal> gPs = new HashSet<GroupPrincipal>();
            int count = 1;
            for(IdentityHeader iH: gHeaders){
                if(count >= CACHE_MAX_GROUPS){
                   logger.log(Level.INFO, "Capping group membership for user at " + CACHE_MAX_GROUPS);
                   break;
                }
                GroupPrincipal gP = new GroupPrincipal(u.getLogin(), iH);
                gPs.add(gP);
                count++;
            }
            CacheEntry<Set<GroupPrincipal>> groupPrincipals = new CacheEntry<Set<GroupPrincipal>>(gPs);
            this.principalCache.store(ckey, groupPrincipals);
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           "Cached group membership principals for user {n0} on IdP \"{1}\"",
                           new String[]{u.getLogin(), idp.getConfig().getName()});

            return gPs;
        }else{
            return null;
        }
    }

    /*Only used by unit tests*/
    void dispose() {
        WhirlycacheFactory.shutdown(principalCache);
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final CacheKey cacheKey = (CacheKey)o;

            if (providerOid != cacheKey.providerOid) return false;
            if (userId != null ? !userId.equals(cacheKey.userId) : cacheKey.userId != null) return false;

            return true;
        }

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
