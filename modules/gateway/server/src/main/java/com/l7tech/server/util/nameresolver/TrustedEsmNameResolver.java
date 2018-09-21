package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import org.apache.commons.lang.StringUtils;

/**
 * Name resolver for Trusted Esm Entity
 */
public class TrustedEsmNameResolver extends EntityNameResolver {
    private ClusterStatusAdmin clusterStatusAdmin;

    public TrustedEsmNameResolver(ClusterStatusAdmin clusterStatusAdmin, FolderAdmin folderAdmin) {
        super(folderAdmin);
        this.clusterStatusAdmin = clusterStatusAdmin;
    }

    @Override
    protected boolean canResolveName(final EntityHeader entityHeader) {
        return (EntityType.TRUSTED_ESM.equals(entityHeader.getType()) || EntityType.TRUSTED_ESM_USER.equals(entityHeader.getType()));
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof TrustedEsm || entity instanceof TrustedEsmUser;
    }

    @Override
    public String resolve(final EntityHeader entityHeader, final boolean includePath) throws FindException {
        Entity entity = null;
        if (EntityType.TRUSTED_ESM.equals(entityHeader.getType())) {
            final TrustedEsm trustedEsm = clusterStatusAdmin.findTrustedEsm(entityHeader.getGoid());
            validateFoundEntity(entityHeader, trustedEsm);
            entity = trustedEsm;
        } else if (EntityType.TRUSTED_ESM_USER.equals(entityHeader.getType())) {
            final TrustedEsmUser trustedEsmUser = clusterStatusAdmin.findTrustedEsmUser(entityHeader.getGoid());
            validateFoundEntity(entityHeader, trustedEsmUser);
            entity = trustedEsmUser;
        }
        final String name = resolve(entity, includePath);
        return buildName(name, StringUtils.EMPTY, null, true);
    }

    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        String name = StringUtils.EMPTY;
        if (entity instanceof TrustedEsm) {
            final TrustedEsm trustedEsm = (TrustedEsm) entity;
            name = trustedEsm.getTrustedCert() != null ? trustedEsm.getTrustedCert().getSubjectDn() : trustedEsm.getName();
        } else if (entity instanceof TrustedEsmUser) {
            final TrustedEsmUser trustedEsmUser = (TrustedEsmUser) entity;
            name = trustedEsmUser.getEsmUserDisplayName() != null ? trustedEsmUser.getEsmUserDisplayName() : trustedEsmUser.getSsgUserId();
        }
        String path = null;
        if (includePath && entity instanceof HasFolder) {
            path = getPath((HasFolder) entity);
        }
        return buildName(name, null, path, true);
    }
}
