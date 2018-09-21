package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.KeyMetadataHeaderWrapper;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import org.apache.commons.lang.StringUtils;

/**
 * Name resolver for Ssg key metadata Entity
 */
public class SsgKeyMetadataNameResolver extends EntityNameResolver {
    private final TrustedCertAdmin trustedCertAdmin;

    public SsgKeyMetadataNameResolver(TrustedCertAdmin trustedCertAdmin, FolderAdmin folderAdmin) {
        super(folderAdmin);
        this.trustedCertAdmin = trustedCertAdmin;
    }

    @Override
    protected boolean canResolveName(final EntityHeader entityHeader) {
        return (EntityType.SSG_KEY_METADATA.equals(entityHeader.getType()));
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof SsgKeyMetadata;
    }

    @Override
    public String resolve(final EntityHeader entityHeader, final boolean includePath) throws FindException {
        SsgKeyMetadata metadata = trustedCertAdmin.findKeyMetadata(entityHeader.getGoid());
        if (metadata == null && entityHeader instanceof KeyMetadataHeaderWrapper) {
            // may not have been persisted yet
            final KeyMetadataHeaderWrapper keyHeader = (KeyMetadataHeaderWrapper) entityHeader;
            metadata = new SsgKeyMetadata(keyHeader.getKeystoreOid(), keyHeader.getAlias(), null);
        }
        validateFoundEntity(entityHeader, metadata);
        String name = null;
        name = resolve(metadata, includePath);
        return buildName(name, null, null, true);
    }

    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        String name = null;
        final SsgKeyMetadata metadata = (SsgKeyMetadata) entity;
        name = metadata.getAlias();
        String path = null;
        if (includePath && entity instanceof HasFolder) {
            path = getPath((HasFolder) entity);
        }
        return buildName(name, StringUtils.EMPTY, path, true);
    }

}

