package com.l7tech.external.assertions.kerberosmapping.server;

import com.l7tech.server.identity.ldap.LdapGroupManager;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.common.util.WhirlycacheFactory;
import com.whirlycott.cache.Cache;

import java.util.Set;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * LDAP Group Manager that adds caching for lookups. 
 *
 * @author steve
 */
public class CachingLdapGroupManager implements LdapGroupManager {

    //- PUBLIC

    public CachingLdapGroupManager( final IdentityProvider ip, final LdapGroupManager delegate ) {
        this.providerOid = ip.getConfig().getOid();
        this.delegateGroupManager = delegate;        
    }

    public void configure(LdapIdentityProvider provider) {
        delegateGroupManager.configure(provider);    
    }

    public void addUser( LdapUser user, LdapGroup group ) throws FindException, UpdateException {
        delegateGroupManager.addUser( user, group );
    }

    public void addUser( LdapUser user, Set<LdapGroup> groups ) throws FindException, UpdateException {
        delegateGroupManager.addUser( user, groups );
    }

    public void addUsers( LdapGroup group, Set<LdapUser> users ) throws FindException, UpdateException {
        delegateGroupManager.addUsers( group, users );
    }

    public void delete( LdapGroup group ) throws DeleteException, ObjectNotFoundException {
        delegateGroupManager.delete( group );
    }

    public void delete( String identifier ) throws DeleteException, ObjectNotFoundException {
        delegateGroupManager.delete( identifier );
    }

    public void deleteAll( long ipoid ) throws DeleteException, ObjectNotFoundException {
        delegateGroupManager.deleteAll( ipoid );
    }

    public void deleteAllVirtual( long ipoid ) throws DeleteException, ObjectNotFoundException {
        delegateGroupManager.deleteAllVirtual( ipoid );
    }

    public Collection findAll() throws FindException {
        return delegateGroupManager.findAll();
    }

    public Collection<IdentityHeader> findAllHeaders() throws FindException {
        return delegateGroupManager.findAllHeaders();
    }

    public Group findByName( String groupName ) throws FindException {
        logger.fine( "Group lookup by name: " + groupName );

        return cachedGroupLookup( new GroupCacheKey("NAME", groupName), this.nameLoadStrategy );
    }

    public LdapGroup findByPrimaryKey( String identifier ) throws FindException {
        logger.fine( "Group lookup by key: " + identifier );

        return (LdapGroup) cachedGroupLookup( new GroupCacheKey("ID", identifier), this.primaryKeyLoadStrategy );
    }

    public Set<IdentityHeader> getGroupHeaders( LdapUser user ) throws FindException {
        return delegateGroupManager.getGroupHeaders( user );
    }

    public Set<IdentityHeader> getGroupHeaders( String userId ) throws FindException {
        return delegateGroupManager.getGroupHeaders( userId );
    }

    public Class getImpClass() {
        return delegateGroupManager.getImpClass();
    }

    public Set<IdentityHeader> getUserHeaders( LdapGroup group ) throws FindException {
        return getUserHeaders( group.getId() );
    }

    public Set<IdentityHeader> getUserHeaders( String groupId ) throws FindException {
        return cachedUserHeadersLookup( new GroupCacheKey("ID", groupId), this.primaryKeyLoadStrategy );
    }

    public boolean isMember( User user, LdapGroup group ) throws FindException {
        if (user.getProviderId() != providerOid ) {
            logger.log( Level.FINE, "User is not from this Identity Provider");
            return false;
        }

        Set<IdentityHeader> userHeaders = getUserHeaders(group);
        for (IdentityHeader header : userHeaders) {
            String dn = header.getStrId();
            if (dn == null || dn.length() == 0) continue;
            if (dn.equals(user.getId())) return true;
        }
        return false;
    }

    public LdapGroup reify( GroupBean bean ) {
        return delegateGroupManager.reify( bean );
    }

    public void removeUser( LdapUser user, LdapGroup group ) throws FindException, UpdateException {
        delegateGroupManager.removeUser( user, group );
    }

    public void removeUser( LdapUser user, Set<LdapGroup> groups ) throws FindException, UpdateException {
        delegateGroupManager.removeUser( user, groups );
    }

    public void removeUsers( LdapGroup group, Set<LdapUser> users ) throws FindException, UpdateException {
        delegateGroupManager.removeUsers( group, users );
    }

    public String save( LdapGroup group, Set<IdentityHeader> userHeaders ) throws SaveException {
        return delegateGroupManager.save( group, userHeaders );
    }

    public String saveGroup( LdapGroup group ) throws SaveException {
        return delegateGroupManager.saveGroup( group );
    }

    public Collection<IdentityHeader> search( String searchString ) throws FindException {
        return delegateGroupManager.search( searchString );
    }

    public void setGroupHeaders( LdapUser user, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException {
        delegateGroupManager.setGroupHeaders( user, groupHeaders );
    }

    public void setGroupHeaders( String userId, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException {
        delegateGroupManager.setGroupHeaders( userId, groupHeaders );
    }

    public void setUserHeaders( LdapGroup group, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException {
        delegateGroupManager.setUserHeaders( group, groupHeaders );
    }

    public void setUserHeaders( String groupId, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException {
        delegateGroupManager.setUserHeaders( groupId, groupHeaders );
    }

    public void update( LdapGroup group ) throws UpdateException, FindException {
        delegateGroupManager.update( group );
    }

    public void update( LdapGroup group, Set<IdentityHeader> userHeaders ) throws UpdateException, FindException {
        delegateGroupManager.update( group, userHeaders );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( CachingLdapGroupManager.class.getName() );

    private static final long MAX_GROUP_AGE = Long.getLong( CachingLdapGroupManager.class.getName() + ".maxAge", 300000L); // 5 mins

    private final long providerOid;
    private final LdapGroupManager delegateGroupManager;
    private final GroupLoadStrategy primaryKeyLoadStrategy = new GroupLoadStrategy() {
        public Group loadGroup( GroupCacheKey key ) throws FindException {
            return delegateGroupManager.findByPrimaryKey( key.identifier );
        }
    };
    private final GroupLoadStrategy nameLoadStrategy = new GroupLoadStrategy() {
        public Group loadGroup( GroupCacheKey key ) throws FindException {
            return delegateGroupManager.findByName( key.identifier );
        }
    };
    private final Cache cache = WhirlycacheFactory.createCache("ldapGroupCache", 200, 89, WhirlycacheFactory.POLICY_LRU);


    private GroupCacheEntry cachedLookup( final GroupCacheKey key, final GroupLoadStrategy loadStrategy ) throws FindException {
        GroupCacheEntry result = null;

        // lookup
        GroupCacheEntry gce = (GroupCacheEntry) cache.retrieve( key );
        if ( gce != null && key.equals( gce.key ) && !gce.isExpired() ) {
            result = gce;
        }

        // cache miss
        if ( result == null ) {
            Group group = loadStrategy.loadGroup( key );
            if ( group != null ) {
                // encache
                GroupCacheEntry entry = new GroupCacheEntry( key, group, delegateGroupManager.getUserHeaders((LdapGroup)group) );
                cache.store( key, entry, MAX_GROUP_AGE );
                result = entry;
            }
        }

        return result;
    }

    private Group cachedGroupLookup( final GroupCacheKey key, final GroupLoadStrategy loadStrategy ) throws FindException {
        Group group = null;

        // lookup
        GroupCacheEntry gce = cachedLookup( key, loadStrategy );
        if ( gce != null ) {
            group = gce.group;
        }

        return group;
    }

    private Set<IdentityHeader> cachedUserHeadersLookup( final GroupCacheKey key, final GroupLoadStrategy loadStrategy ) throws FindException {
        Set<IdentityHeader> headers = null;

        // lookup
        GroupCacheEntry gce = cachedLookup( key, loadStrategy );
        if ( gce != null ) {
            headers = gce.userHeaders;
        }

        return headers;

    }

    private interface GroupLoadStrategy {
        Group loadGroup( GroupCacheKey key ) throws FindException;
    }

    private static final class GroupCacheEntry {
        private final GroupCacheKey key;
        private final long loadTime;
        private final Group group;
        private final Set<IdentityHeader> userHeaders;

        private GroupCacheEntry( final GroupCacheKey key, final Group group, final Set<IdentityHeader> userHeaders ) {
            this.key = key;
            this.group = group;
            this.userHeaders = userHeaders;
            this.loadTime = System.currentTimeMillis();
        }

        private boolean isExpired() {
            return (loadTime + MAX_GROUP_AGE) < System.currentTimeMillis();
        }
    }

    private static final class GroupCacheKey {
        private final String type;
        private final String identifier;

        private GroupCacheKey( final String type, final String identifier ) {
            this.type = type;
            this.identifier = identifier;
        }

        @SuppressWarnings( { "RedundantIfStatement" } )
        @Override
        public boolean equals( Object o ) {
            if( this == o ) return true;
            if( o == null || getClass() != o.getClass() ) return false;

            GroupCacheKey that = (GroupCacheKey) o;

            if( !identifier.equals( that.identifier ) ) return false;
            if( !type.equals( that.type ) ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = type.hashCode();
            result = 31 * result + identifier.hashCode();
            return result;
        }
    }
}
