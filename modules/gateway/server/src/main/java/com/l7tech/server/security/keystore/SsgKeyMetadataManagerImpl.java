package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity manager for persistent {@link SsgKeyMetadata} instances.
 */
public class SsgKeyMetadataManagerImpl extends HibernateEntityManager<SsgKeyMetadata, EntityHeader> implements SsgKeyMetadataManager {
    @Override
    @Transactional(readOnly=true)
    public Class<? extends Entity> getImpClass() {
        return SsgKeyMetadata.class;
    }

    @Override
    @Transactional(readOnly=true)
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(SsgKeyMetadata entity) {
        Map<String,Object> map = new HashMap<>();
        map.put("keystoreOid", entity.getKeystoreOid());
        map.put("alias", entity.getAlias());
        return Arrays.asList(map);
    }

    @Override
    @Transactional(readOnly=true)
    public SsgKeyMetadata findMetadata(long keystoreOid, @NotNull String alias) throws FindException {
        final Map<String,Object> criteria = new HashMap<>();
        criteria.put( "keystoreOid", keystoreOid );
        criteria.put( "alias", alias );
        return findUnique( criteria );
    }

    @Override
    @Transactional(readOnly=false)
    public long setMetadataForNewKey(long keystoreOid, @NotNull String alias, @Nullable SsgKeyMetadata newMeta) throws FindException, DeleteException, SaveException {

        SsgKeyMetadata existing = findMetadata(keystoreOid, alias);
        if (existing != null)
            delete(existing);

        if (newMeta != null) {
            return checkAndSave(keystoreOid, alias, newMeta);
        }
        return SsgKeyMetadata.DEFAULT_OID;
    }

    private long checkAndSave(long keystoreOid, String alias, SsgKeyMetadata newMeta) throws SaveException {
        if (keystoreOid != newMeta.getKeystoreOid())
            throw new IllegalArgumentException("New metadata keystore OID does not match");
        if (!alias.equals(newMeta.getAlias()))
            throw new IllegalArgumentException("New metadata key alias does not match");
        return save(newMeta);
    }

    @Override
    public long updateMetadataForKey(long keystoreOid, @NotNull String alias, @Nullable SsgKeyMetadata newMeta) throws FindException, SaveException, UpdateException {
        SsgKeyMetadata existing = findMetadata(keystoreOid, alias);
        if (existing != null) {
            if (newMeta != null) {
                existing.setSecurityZone(newMeta.getSecurityZone());
            } else {
                existing.setSecurityZone(null);
            }
            update(existing);
            return existing.getOid();
        } else {
            // existing == null
            if (newMeta == null)
                return -1;

            return checkAndSave(keystoreOid, alias, newMeta);
        }
    }
}
