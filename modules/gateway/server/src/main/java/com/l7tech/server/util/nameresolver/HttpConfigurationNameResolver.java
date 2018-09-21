package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import org.apache.commons.lang.StringUtils;
/**
 * Name resolver for Http Configuration  Entity
 */
public class HttpConfigurationNameResolver extends EntityNameResolver {
    private ResourceAdmin resourceAdmin;
    private static final String NO_PROTOCOL = "<no protocol>";
    private static final String NO_PORT = "<no port>";
    private static final String NO_HTTP_CONFIG_PATH = "<no path>";

    public HttpConfigurationNameResolver(ResourceAdmin resourceAdmin, FolderAdmin folderAdmin) {
        super(folderAdmin);
        this.resourceAdmin = resourceAdmin;
    }

    @Override
    protected boolean canResolveName(final EntityHeader entityHeader) {
        return (EntityType.HTTP_CONFIGURATION.equals(entityHeader.getType()));
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof HttpConfiguration;
    }

    @Override
    public String resolve(final EntityHeader entityHeader, final boolean includePath) throws FindException {
        final HttpConfiguration httpConfig = resourceAdmin.findHttpConfigurationByPrimaryKey(entityHeader.getGoid());
        validateFoundEntity(entityHeader, httpConfig);
        final String name = resolve(httpConfig, includePath);
        return buildName(name, null, null, true);
    }

    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        final HttpConfiguration httpConfig = (HttpConfiguration) entity;
        final String protocol = httpConfig.getProtocol() == null ? NO_PROTOCOL : httpConfig.getProtocol().toString();
        final String port = httpConfig.getPort() == 0 ? NO_PORT : String.valueOf(httpConfig.getPort());
        final String httpPath = httpConfig.getPath() == null ? NO_HTTP_CONFIG_PATH : httpConfig.getPath();
        String name = protocol + " " + httpConfig.getHost() + " " + port + " " + httpPath;
        String path = null;
        if (includePath && entity instanceof HasFolder) {
            path = getPath((HasFolder) entity);
        }
        return buildName(name, StringUtils.EMPTY, path, true);
    }
}
