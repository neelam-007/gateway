package com.l7tech.server.module;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.admin.ServerModuleFileAdminEvent;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    @NotNull protected final String clusterNodeId;

    /**
     * Gateway's stash manager factory.
     */
    @NotNull protected final StashManagerFactory stashManagerFactory;

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
            @NotNull final String clusterNodeId,
            @NotNull final StashManagerFactory stashManagerFactory
    ) {
        this.config = config;
        this.stashManagerFactory = stashManagerFactory;

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
    public void save(@NotNull final Goid id, @NotNull final ServerModuleFile entity) throws SaveException {
        super.save(id, entity);
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(new ServerModuleFileAdminEvent(this, ServerModuleFileAdminEvent.Action.UPLOADED, entity));
        }
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
        return moduleFile.getStateForNode(clusterNodeId);
    }

    @Override
    public boolean isModuleUploadEnabled() {
        return config.getBooleanProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_ENABLE, false);
    }

    /**
     * Utility {@code InputStream} for using the {@code StashManager} to stash original stream and close the stash when closing this stream.
     * <p/>
     * It will stash the original {@code InputStream} (assuming that everything read until {@code originalStream} EOF) and
     * create a {@code FilterInputStream} out of the stashed {@code InputStream}.<br/>
     * {@link #close()} will un-stash the stream and close the {@code StashManager}
     */
    static class StashManagerBackedInputStream extends FilterInputStream {
        @NotNull private final StashManager stashManager;

        /**
         * Default Constructor.
         *
         * @param stashManager     the {@code StashManager} to use.  Required and cannot be {@code null}.
         * @param stashedStream    the stashed {@code InputStream}.  Required and cannot be {@code null}.
         */
        private StashManagerBackedInputStream(
                @NotNull final StashManager stashManager,
                @NotNull final InputStream stashedStream
        ) {
            super(stashedStream);
            this.stashManager = stashManager;
        }

        /**
         * Stash the original {@code InputStream} (assuming that everything read until {@code originalStream} EOF) and
         * create a {@code FilterInputStream} i.e. {@code StashManagerBackedInputStream} out of the stashed {@code InputStream}.
         *
         * @param stashManager      the {@code StashManager} to use.  Required and cannot be {@code null}.
         * @param originalStream    the original {@code InputStream}.  Required and cannot be {@code null}.
         * @return An {@code InputStream} holding the stashed version of the {@code originalStream}.  Never {@code null}.
         * @throws IOException if an IO error happens while reading the {@code originalStream}.
         */
        @NotNull
        public static InputStream stash(
                @NotNull final StashManager stashManager,
                @NotNull final InputStream originalStream
        ) throws IOException {
            // first stash the original InputStream
            stashManager.stash(0, originalStream);
            try {
                // recall the stashed InputStream
                return new StashManagerBackedInputStream(stashManager, stashManager.recall(0));
            } catch (final NoSuchPartException e) {
                // shouldn't happen so throw invalid state
                throw new IllegalStateException(e);
            } catch (final IOException e) {
                // stashed already and yet it throws IOException
                // nothing else to do but un-stash and close stashManager
                stashManager.unstash(0);
                stashManager.close();
                // finally rethrow
                throw e;
            }
        }

        @Override
        public void close() throws IOException {
            super.close();
            stashManager.unstash(0);
            stashManager.close();
        }
    }

    // SQL for getting module data and module signature
    private static final String SQL_GET_DATA_BYTES_WITH_SIGNATURE_FOR_MODULE_GOID = "SELECT data_bytes, signature_properties " +
            "FROM server_module_file module " +
            "INNER JOIN server_module_file_data data " +
            "ON module.data_goid = data.goid " +
            "WHERE module.goid = ?";

    @Nullable
    @Override
    public Pair<InputStream, String> getModuleBytesAsStreamWithSignature(@NotNull final Goid goid) throws FindException {
        try {
            return doReadOnlyWork(new Functions.UnaryThrows<Pair<InputStream, String>, Connection, SQLException>() {
                @Override
                public Pair<InputStream, String> call(final Connection connection) throws SQLException {
                    try (final PreparedStatement statement = connection.prepareStatement(SQL_GET_DATA_BYTES_WITH_SIGNATURE_FOR_MODULE_GOID)) {
                        statement.setBytes(1, goid.getBytes());
                        try (final ResultSet rs = statement.executeQuery()) {
                            if (rs.next()) {
                                final InputStream isData = rs.getBinaryStream(1);
                                if (isData != null) {
                                    return Pair.pair(
                                            StashManagerBackedInputStream.stash(stashManagerFactory.createStashManager(), isData),
                                            rs.getString(2)
                                    );
                                }
                            }
                        } catch (final IOException e) {
                            // re-throw as SQLException
                            throw new SQLException(e);
                        }
                    }
                    return null;
                }
            });
        } catch (final SQLException e) {
            throw new FindException(e.toString(), e);
        }
    }

    private final String HQL_FIND_BY_MODULE_SHA256 =
            "FROM " + getTableName() +
                    " IN CLASS " + getImpClass().getName() +
                    " WHERE " + getTableName() + "." + F_MODULE_SHA256 + " = ?";

    @Nullable
    @Override
    @Transactional(readOnly=true)
    public ServerModuleFile findModuleWithSha256(@NotNull final String moduleSha256) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<ServerModuleFile>() {
                @Override
                protected ServerModuleFile doInHibernateReadOnly(final Session session) throws HibernateException, SQLException {
                    final Query q = session.createQuery(HQL_FIND_BY_MODULE_SHA256);
                    q.setParameter(0, moduleSha256);
                    final ServerModuleFile et = (ServerModuleFile) q.uniqueResult();
                    initializeLazilyLoaded(et);
                    return et;
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
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
