package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.EntityProtectionInfo;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Config;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
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

    private static final Map<String, EntityProtectionInfo> protectedEntityMap = new HashMap<>();

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
                protectedEntityMap.put( pair.right, new EntityProtectionInfo( pair.left, true ) );
            }
        }
    }

    @Nullable
    public EntityProtectionInfo getEntityProtection( @NotNull String entityId ) {
        synchronized ( protectedEntityMap ) {
            return protectedEntityMap.get( entityId );
        }
    }

    public boolean isReadOnlyEntity( @NotNull Entity e ) {
        final String id = e.getId();
        final EntityProtectionInfo perm = id == null ? null : getEntityProtection( id );
        return perm != null && perm.matchesTypeOf( e ) && perm.isReadOnly();
    }

    public boolean isReadOnlyEntity( @NotNull final EntityHeaderRef eh ) {
        final String id = eh.getStrId();
        final EntityProtectionInfo perm = id == null ? null : getEntityProtection( id );
        return perm != null && perm.matchesTypeOf( eh ) && perm.isReadOnly();

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

    /**
     * Returns a copy of {@link #protectedEntityMap}.
     */
    @NotNull
    public Map<String, EntityProtectionInfo> getProtectedEntities() {
        if (isEnabled()) {
            synchronized (protectedEntityMap) {
                // todo: for now its ok not to copy the EntityProtectionInfo's as there is no way to set individual EntityProtectionInfo (bulkUpdateReadOnlyEntitiesList will clear the map first).
                return new HashMap<>(protectedEntityMap);
            }
        }
        return Collections.emptyMap();
    }
}
