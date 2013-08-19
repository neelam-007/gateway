package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateGoidEntityManager;
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
public class SsgKeyMetadataManagerImpl extends HibernateGoidEntityManager<SsgKeyMetadata, EntityHeader> implements SsgKeyMetadataManager {
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
        map.put("keystoreGoid", entity.getKeystoreGoid());
        map.put("alias", entity.getAlias());
        return Arrays.asList(map);
    }

    @Override
    @Transactional(readOnly=true)
    public SsgKeyMetadata findMetadata(Goid keystoreGoid, @NotNull String alias) throws FindException {
        final Map<String,Object> criteria = new HashMap<>();
        criteria.put( "keystoreGoid", keystoreGoid );
        criteria.put( "alias", alias );
        return findUnique( criteria );
    }

    private Goid checkAndSave(Goid keystoreGoid, String alias, SsgKeyMetadata newMeta) throws SaveException {
        if (!Goid.equals(keystoreGoid, newMeta.getKeystoreGoid()))
            throw new IllegalArgumentException("New metadata keystore GOID does not match");
        if (!alias.equals(newMeta.getAlias()))
            throw new IllegalArgumentException("New metadata key alias does not match");
        return save(newMeta);
    }

    @Override
    public Goid updateMetadataForKey(Goid keystoreGoid, @NotNull String alias, @Nullable SsgKeyMetadata newMeta) throws FindException, SaveException, UpdateException {
        SsgKeyMetadata existing = findMetadata(keystoreGoid, alias);
        if (existing != null) {
            if (newMeta != null) {
                existing.setSecurityZone(newMeta.getSecurityZone());
            } else {
                existing.setSecurityZone(null);
            }
            update(existing);
            return existing.getGoid();
        } else {
            // existing == null
            if (newMeta == null)
                return SsgKeyMetadata.DEFAULT_GOID;

            return checkAndSave(keystoreGoid, alias, newMeta);
        }
    }

    @Override
    public void deleteMetadataForKey(Goid keystoreGoid, @NotNull String alias) throws DeleteException {
        try {
            final SsgKeyMetadata found = findMetadata(keystoreGoid, alias);
            if (found != null) {
                delete(found);
            }
        } catch (final FindException e) {
            throw new DeleteException("Error looking up SsgKeyMetadata to delete", e);
        }
    }
}
