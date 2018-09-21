package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfoHeader;
import com.l7tech.gateway.common.uddi.UDDIServiceControlHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import org.apache.commons.lang.StringUtils;
/**
 * Name resolver for Service  Entity
 */
public class ServiceEntityNameResolver extends EntityNameResolver {
    private ServiceAdmin serviceAdmin;

    public ServiceEntityNameResolver(ServiceAdmin serviceAdmin, FolderAdmin folderAdmin) {
        super(folderAdmin);
        this.serviceAdmin = serviceAdmin;
    }

    @Override
    protected boolean canResolveName(final EntityHeader entityHeader) {
        return (EntityType.SERVICE_USAGE.equals(entityHeader.getType()) ||
                EntityType.UDDI_SERVICE_CONTROL.equals(entityHeader.getType()) ||
                EntityType.UDDI_PROXIED_SERVICE_INFO.equals(entityHeader.getType()) ||
                EntityType.SERVICE.equals(entityHeader.getType()));
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof PublishedService;
    }

    @Override
    public String resolve(final EntityHeader header, final boolean includePath) throws FindException {
        String name = StringUtils.EMPTY;
        if (EntityType.SERVICE.equals(header.getType())) {
            PublishedService service = serviceAdmin.findServiceByID(header.getStrId());
            validateFoundEntity(header, service);
            name = resolve(service, includePath);
        } else if (header instanceof ServiceUsageHeader) {
            final ServiceUsageHeader usageHeader = (ServiceUsageHeader) header;
            final PublishedService usageService = serviceAdmin.findServiceByID(usageHeader.getServiceGoid().toHexString());
            validateFoundEntity(EntityType.SERVICE, usageHeader.getServiceGoid(), usageService);
            name = resolve(usageService, includePath) + " on node " + usageHeader.getNodeId();
        } else if (header instanceof UDDIServiceControlHeader) {
            Goid serviceGoid = ((UDDIServiceControlHeader) header).getPublishedServiceGoid();
            if (serviceGoid != null) {
                final PublishedService publishedService = serviceAdmin.findServiceByID(serviceGoid.toHexString());
                validateFoundEntity(EntityType.SERVICE, serviceGoid, publishedService);
                name = resolve(publishedService, includePath);
            }
        } else if (header instanceof UDDIProxiedServiceInfoHeader) {
            Goid serviceGoid = ((UDDIProxiedServiceInfoHeader) header).getPublishedServiceGoid();
            if (serviceGoid != null) {
                final PublishedService publishedService = serviceAdmin.findServiceByID(serviceGoid.toHexString());
                validateFoundEntity(EntityType.SERVICE, serviceGoid, publishedService);
                name = resolve(publishedService, includePath);
            }
        }
        String path = null;
        if (includePath && header instanceof HasFolderId) {
            path = getPath((HasFolderId) header);
        }
        String uniqueInfo = getUniqueInfo(header, null);
        return buildName(name, uniqueInfo, path, true);
    }

    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        final NamedEntityImp named = (NamedEntityImp) entity;
        final String name = named.getName();
        final String routingUri = ((PublishedService) entity).getRoutingUri();
        final String uniqueInfo = routingUri != null ? routingUri : StringUtils.EMPTY;
        String path = null;
        if (includePath && entity instanceof HasFolder) {
            path = getPath((HasFolder) entity);
        }
        return buildName(name, uniqueInfo, path, true);
    }
}
