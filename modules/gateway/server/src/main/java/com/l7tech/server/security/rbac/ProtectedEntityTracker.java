package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Config;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A bean that keeps an in-memory list of entities which are protected from modification.
 */
public class ProtectedEntityTracker {
    /**
     * Gateway Configuration bean
     */
    @NotNull protected final Config config;

    public static class EntityProtection {
        private final EntityType entityType;
        private final boolean readOnly;

        private EntityProtection( @NotNull EntityType entityType, boolean readOnly ) {
            this.entityType = entityType;
            this.readOnly = readOnly;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public boolean matchesTypeOf( Entity e ) {
            return e != null && entityType.getEntityClass().isAssignableFrom( e.getClass() );
        }
    }

    private static final Map<String, EntityProtection> protectedEntityMap = new HashMap<>();

    private static final ThreadLocal<Boolean> entityProtectionEnabled = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };

    public ProtectedEntityTracker(@NotNull final Config config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.getBooleanProperty(ServerConfigParams.PARAM_PROTECTED_ENTITY_TRACKER_ENABLE, true);
    }

    public void bulkUpdateReadOnlyEntitiesList( Collection< Pair< EntityType, String> > readOnlyEntities ) {
        synchronized ( protectedEntityMap ) {
            protectedEntityMap.clear();

            for ( Pair<EntityType, String> pair : readOnlyEntities ) {
                protectedEntityMap.put( pair.right, new EntityProtection( pair.left, true ) );
            }
        }
    }

    public void setEntityProtection( @NotNull EntityType entityType, @NotNull String entityId, boolean readOnly ) {
        synchronized ( protectedEntityMap ) {
            if ( readOnly ) {
                protectedEntityMap.put( entityId, new EntityProtection( entityType, true ) );
            } else {
                protectedEntityMap.remove( entityId );
            }
        }
    }

    @Nullable
    public EntityProtection getEntityProtection( @NotNull String entityId ) {
        synchronized ( protectedEntityMap ) {
            return protectedEntityMap.get( entityId );
        }
    }

    public boolean isReadOnlyEntity( @NotNull Entity e ) {
        String id = e.getId();
        EntityProtection perm = id == null ? null : getEntityProtection( id );
        return perm != null && perm.matchesTypeOf( e ) && perm.isReadOnly();
    }

    public boolean isEntityProtectionEnabled() {
        return isEnabled() && entityProtectionEnabled.get();
    }

    public <T> T doWithEntityProtectionDisabled( Callable<T> stuff ) throws Exception {
        final boolean oldState = entityProtectionEnabled.get();
        try {
            entityProtectionEnabled.set( false );
            return stuff.call();
        } finally {
            entityProtectionEnabled.set( oldState );
        }
    }
}
