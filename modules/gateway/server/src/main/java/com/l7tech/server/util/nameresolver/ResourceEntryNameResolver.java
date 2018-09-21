package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import org.apache.commons.lang.StringUtils;
/**
 * Name resolver for Resource Entry Entity
 */
public class ResourceEntryNameResolver extends EntityNameResolver {
    private ResourceAdmin resourceAdmin;

    public ResourceEntryNameResolver(ResourceAdmin resourceAdmin, FolderAdmin folderAdmin) {
        super(folderAdmin);
        this.resourceAdmin = resourceAdmin;
    }

    @Override
    protected boolean canResolveName(final EntityHeader entityHeader) {
        return (EntityType.RESOURCE_ENTRY.equals(entityHeader.getType()));
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof ResourceEntry;
    }

    @Override
    public String resolve(final EntityHeader entityHeader, final boolean includePath) throws FindException {
        final ResourceEntry resourceEntry = resourceAdmin.findResourceEntryByPrimaryKey(entityHeader.getGoid());
        validateFoundEntity(entityHeader, resourceEntry);
        String name = resolve(resourceEntry, includePath);
        return buildName(name, null, null, true);
    }

    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        ResourceEntry resource = (ResourceEntry) entity;
        String name = resource.getUri();
        String path = null;
        if (includePath && entity instanceof HasFolder) {
            path = getPath((HasFolder) entity);
        }
        return buildName(name, StringUtils.EMPTY, path, true);
    }
}
