package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import org.apache.commons.lang.StringUtils;
/**
 * Name resolver for Service Alias Entity
 */
public class ServiceAliasNameResolver extends EntityNameResolver {
    private static final String ALIAS = " alias";
    private ServiceAdmin serviceAdmin;

    public ServiceAliasNameResolver(ServiceAdmin serviceAdmin, FolderAdmin folderAdmin) {
        super(folderAdmin);
        this.serviceAdmin = serviceAdmin;
    }
    @Override
    protected boolean canResolveName(final EntityHeader entityHeader) {
        return (EntityType.SERVICE_ALIAS.equals(entityHeader.getType()));
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof PublishedServiceAlias;
    }
    @Override
    public String resolve(final EntityHeader entityHeader, final boolean includePath) throws FindException {
        final PublishedService owningService = serviceAdmin.findByAlias(entityHeader.getGoid());
        validateFoundEntity(entityHeader, owningService);
        String name = owningService.getName() + ALIAS;
        Entity relatedEntity = owningService;
        String path = null;
        if (includePath && entityHeader instanceof HasFolderId) {
            path = getPath((HasFolderId) entityHeader);
        }
        String uniqueInfo = getUniqueInfo(relatedEntity);
        return buildName(name, uniqueInfo, path, true);
    }

    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        String name = StringUtils.EMPTY;
        Entity relatedEntity = null;
        if (entity instanceof PublishedServiceAlias) {
            final PublishedServiceAlias alias = (PublishedServiceAlias) entity;
            final PublishedService owningService = serviceAdmin.findServiceByID(String.valueOf(alias.getEntityGoid()));
            validateFoundEntity(EntityType.SERVICE, alias.getGoid(), owningService);
            name = owningService.getName() + ALIAS;
            relatedEntity = owningService;
        } else if (entity instanceof NamedEntityImp) {
            final NamedEntityImp named = (NamedEntityImp) entity;
            name = named.getName();
        }
        String path = null;
        if (includePath && entity instanceof HasFolder) {
            path = getPath((HasFolder) entity);
        }
        String uniqueInfo = getUniqueInfo(entity);
        if (StringUtils.isBlank(uniqueInfo) && relatedEntity != null) {
            uniqueInfo = getUniqueInfo((Entity) relatedEntity);
        }
        return buildName(name, uniqueInfo, path, true);
    }
}
