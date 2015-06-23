package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.ServerModuleFileResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ServerModuleFileTransformer;
import com.l7tech.gateway.api.ServerModuleFileMO;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.module.ServerModuleFileManager;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * {@code ServerModuleFile} resource factory.
 */
@Component
public class ServerModuleFileAPIResourceFactory extends EntityManagerAPIResourceFactory<ServerModuleFileMO, ServerModuleFile, EntityHeader> {

    @Inject
    private ServerModuleFileTransformer transformer;
    @Inject
    private ServerModuleFileManager manager;

    @Override
    protected EntityType getResourceEntityType() {
        return EntityType.SERVER_MODULE_FILE;
    }

    @Override
    protected ServerModuleFile convertFromMO(final ServerModuleFileMO resource) throws ResourceFactory.InvalidResourceException {
        return transformer.convertFromMO(resource, null).getEntity();
    }

    @NotNull
    @Override
    protected ServerModuleFileMO convertToMO(final ServerModuleFile entity) {
        return transformer.convertToMO(entity);
    }

    @Override
    protected ServerModuleFileManager getEntityManager() {
        return manager;
    }

    @Override
    public String createResource(@NotNull final ServerModuleFileMO resource) throws ResourceFactory.InvalidResourceException {
        if (!getEntityManager().isModuleUploadEnabled()) {
            throw new ServerModuleFileResource.DisabledException();
        }
        return super.createResource(resource);
    }

    @Override
    public void createResource(@NotNull final String id, @NotNull final ServerModuleFileMO resource) throws ResourceFactory.ResourceFactoryException {
        if (!getEntityManager().isModuleUploadEnabled()) {
            throw new ServerModuleFileResource.DisabledException();
        }
        super.createResource(id, resource);
    }

    @Override
    public void deleteResource(@NotNull final String id) throws ResourceFactory.ResourceNotFoundException {
        if (!getEntityManager().isModuleUploadEnabled()) {
            throw new ServerModuleFileResource.DisabledException();
        }
        super.deleteResource(id);
    }

    /**
     * Retrieves a {@code ServerModuleFile} resource with the given id and optionally include module data bytes.
     *
     * @param id             The id of the resource to retrieve
     * @param includeData    A flag indicating whether to include module data along with the meta info.
     * @return The resource Managed Object.
     * @throws ResourceFactory.ResourceNotFoundException if the resource with the specified id cannot be located.
     */
    @NotNull
    public ServerModuleFileMO getResource(
            @NotNull final String id,
            final boolean includeData
    ) throws ResourceFactory.ResourceNotFoundException {
        final ServerModuleFileMO mo = super.getResource(id);
        if (mo == null) {
            throw new ResourceFactory.ResourceNotFoundException("Unable to find entity.");
        }
        if (includeData) {
            // Note that this might fail with ResourceNotFoundException in case when the ServerModuleFile entity
            // have been deleted before downloading it's bytes.
            // Unfortunately there is nothing much that can be done in this case but throw runtime exception (i.e. ResourceAccessException)
            // as this entity cannot be skipped from here
            setModuleData(mo);
        }
        return mo;
    }

    /**
     * Returns a list of {@code ServerModuleFile} resources, optionally including their module data bytes.<br/>
     * It can optionally be sorted by the given sort key in either ascending or descending order.<br/>
     * The filters given are used to restrict the returned resources to only those entities that match the filters.
     *
     * @param sort           The attribute to sort the entities by. Null for no sorting.
     * @param ascending      The order to sort the entities.
     * @param filters        The collection of filters specifying which entities to include.
     * @param includeData    A flag indicating whether to include module data along with the meta info.
     * @return The list of {@code ServerModuleFile} resources matching the given parameters.
     */
    @NotNull
    public List<ServerModuleFileMO> listResources(
            @Nullable final String sort,
            @Nullable final Boolean ascending,
            @Nullable final Map<String, List<Object>> filters,
            final boolean includeData
    ) {
        try {
            List<ServerModuleFile> entities = getEntityManager().findPagedMatching(0, -1, sort, ascending, filters);
            entities = rbacAccessService.accessFilter(entities, getResourceEntityType(), OperationType.READ, null);

            return Functions.map(
                    entities,
                    new Functions.UnaryThrows<ServerModuleFileMO, ServerModuleFile, ObjectModelException>() {
                        @Override
                        public ServerModuleFileMO call(final ServerModuleFile e) throws ObjectModelException {
                            return transformer.convertToMO(e, includeData);
                        }
                    });
        } catch (ObjectModelException e) {
            throw new ResourceFactory.ResourceAccessException("Unable to list entities.", e);
        }
    }

    /**
     * Gather the module data bytes to the specified {@link ServerModuleFileMO managed object}.
     * <p/>
     * Note that this is executed within a read-only transaction.
     *
     * @param mo    the managed object to set module data to.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    public void setModuleData(@NotNull final ServerModuleFileMO mo) throws ResourceFactory.ResourceNotFoundException {
        final Goid goid = Goid.parseGoid(mo.getId());
        try {
            final InputStream stream = manager.getModuleBytesAsStream(goid);
            if (stream == null) {
                throw new ResourceFactory.ResourceNotFoundException("Unable to find ServerModuleFile with goid " + goid);
            }
            try {
                mo.setModuleData(IOUtils.slurpStream(stream));
            } finally {
                ResourceUtils.closeQuietly(stream);
            }
        } catch (ObjectModelException e) {
            throw new ResourceFactory.ResourceNotFoundException("Unable to find ServerModuleFile.", e);
        } catch (IOException e) {
            throw new ResourceFactory.ResourceAccessException("Unable to read ServerModuleFile data bytes.", e);
        }
    }
}
