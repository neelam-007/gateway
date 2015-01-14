package com.l7tech.server.module;

import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.admin.ServerModuleFileAdminEvent;
import com.l7tech.util.Config;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Implementation of hibernate entity manager for {@link ServerModuleFile} entities.
 */
public class ServerModuleFileManagerImpl extends HibernateEntityManager<ServerModuleFile, EntityHeader> implements ApplicationEventPublisherAware, ServerModuleFileManager  {

    /**
     * module data {@link com.l7tech.gateway.common.module.ServerModuleFile#moduleSha256 sha256} field.
     */
    public static final String F_MODULE_SHA256 = "moduleSha256";

    /**
     * Gateway Configuration bean
     */
    @NotNull protected final Config config;

    /**
     * Current node-id in a cluster environment.
     */
    @NotNull protected String clusterNodeId;

    /**
     * Application event publisher
     */
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * Default constructor.
     *
     * @param config           Application config object.
     * @param clusterNodeId    Current cluster node id.
     */
    protected ServerModuleFileManagerImpl(
            @NotNull final Config config,
            @NotNull final String clusterNodeId
    ) {
        this.config = config;

        this.clusterNodeId = clusterNodeId;
        if (StringUtils.isBlank(clusterNodeId)) {
            throw new IllegalStateException("Current cluster node-id cannot be blank!");
        }
    }

    @Override
    public Goid save(final ServerModuleFile entity) throws SaveException {
        final Goid goid = super.save(entity);
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(new ServerModuleFileAdminEvent(this, ServerModuleFileAdminEvent.Action.UPLOADED, entity));
        }
        return goid;
    }

    @Override
    public void delete(final ServerModuleFile moduleFile) throws DeleteException {
        super.delete(moduleFile);
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(new ServerModuleFileAdminEvent(this, ServerModuleFileAdminEvent.Action.DELETED, moduleFile));
        }
    }

    @Override
    public void delete(final Goid goid) throws DeleteException, FindException {
        final ServerModuleFile moduleFile = findByPrimaryKey(goid);
        super.delete(goid);
        if (applicationEventPublisher != null && moduleFile != null) {
            applicationEventPublisher.publishEvent(new ServerModuleFileAdminEvent(this, ServerModuleFileAdminEvent.Action.DELETED, moduleFile));
        }
    }

    @Override
    public void updateState(@NotNull final Goid moduleGoid, @NotNull final ModuleState state) throws UpdateException {
        try {
            final ServerModuleFile moduleFile = findByPrimaryKey(moduleGoid);
            if (moduleFile == null) throw new UpdateException("Cannot find updating server module file from database.");
            moduleFile.setStateForNode(clusterNodeId, state);
            super.update(moduleFile);
        } catch (RuntimeException | FindException e) {
            throw new UpdateException("Unable to update server module file state for listener \"" + moduleGoid + "\"", e);
        }
    }

    @Override
    public void updateState(@NotNull final Goid moduleGoid, @NotNull final String errorMessage) throws UpdateException {
        try {
            final ServerModuleFile moduleFile = findByPrimaryKey(moduleGoid);
            if (moduleFile == null) throw new UpdateException("Cannot find updating server module file from database.");
            moduleFile.setStateErrorMessageForNode(clusterNodeId, errorMessage);
            super.update(moduleFile);
        } catch (RuntimeException | FindException e) {
            throw new UpdateException("Unable to update server module file state for listener \"" + moduleGoid + "\"", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ServerModuleFileState findStateForCurrentNode(@NotNull final ServerModuleFile moduleFile) {
        final Collection<ServerModuleFileState> states = moduleFile.getStates();
        if (states != null) {
            for (final ServerModuleFileState state : states) {
                if (clusterNodeId.equals(state.getNodeId())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isModuleUploadEnabled() {
        return config.getBooleanProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_ENABLE, false);
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return ServerModuleFile.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(@NotNull final ServerModuleFile entity) {
        return Arrays.asList(
                Collections.singletonMap(F_NAME, (Object) entity.getName()),
                Collections.singletonMap(F_MODULE_SHA256, (Object) entity.getModuleSha256())
        );
    }

    @Override
    public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
