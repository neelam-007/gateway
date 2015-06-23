package com.l7tech.server.module;

import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * Entity manager interface for {@link ServerModuleFile} entities.
 */
public interface ServerModuleFileManager extends EntityManager<ServerModuleFile, EntityHeader> {
    /**
     * Update the {@link ServerModuleFile module} state of the current cluster node.<br/>
     * This method will set the module state and reset any error message, indicating that the module has
     * successfully transited into the specified state.
     *
     * @param moduleGoid     The OID of the server module file to update the state.  Required.
     * @param state          Updated state.  Required.
     * @throws UpdateException Failed to update the state.
     * @see ServerModuleFile#setStateForNode(String, com.l7tech.gateway.common.module.ModuleState)
     */
    void updateState(Goid moduleGoid, ModuleState state) throws UpdateException;

    /**
     * Update the {@link ServerModuleFile module} state, of the current cluster node, with an error message.<br/>
     * This method will set module error message and its state to {@link ModuleState#ERROR}, indicating that the module
     * failed to transit from current to the next state.
     *
     * @param moduleGoid       The OID of the server module file to update the state.  Required.
     * @param errorMessage     A {@code String} describing the error.  Required.
     * @throws UpdateException Failed to update the state.
     * @see ServerModuleFile#setStateErrorMessageForNode(String, String)
     */
    void updateState(Goid moduleGoid, String errorMessage) throws UpdateException;

    /**
     * Find the {@link ServerModuleFileState state} of the current node.
     * <p/>
     * {@link ServerModuleFile} contains a list os states from all nodes in the cluster.
     * This method is a convenience method for getting the module state, if any, for the current cluster node.
     * This method will loop through the {@link ServerModuleFile#states states} list finding the state belonging to this node,
     * this means that there are not going to be calls made into the DB.
     *
     * @param moduleFile    the module file, holding the states.  Required.
     * @return the {@link ServerModuleFileState state} for the current cluster node, or {@code null} is there is no state for this node.
     */
    ServerModuleFileState findStateForCurrentNode(ServerModuleFile moduleFile);

    /**
     * Determines whether Server Module File uploading via Policy Manager is enabled or not.
     *
     * @return the value of {@link com.l7tech.server.ServerConfigParams#PARAM_SERVER_MODULE_FILE_UPLOAD_ENABLE} cluster wide property.  Default is {@code false}.
     */
    boolean isModuleUploadEnabled();

    /**
     * Retrieve module content as a stream of uninterpreted bytes in a read-only transaction. The value can then be read in chunks from the stream. <br/>
     * Note: Current implementation of MySQL JDBC returns in-memory {@code java.io.ByteArrayInputStream}.<br/>
     * Limitation: Will not work with Derby, as current implementation of Derby JDBC throws IOException exception, with cause stream closed.
     *
     * @param goid    the module {@link Goid}.  Required and cannot be {@code null}.
     * @return A {@code InputStream} that delivers the module content as a stream of uninterpreted bytes
     * or {@code null} if module specified with the {@code goid} cannot be found.
     * @throws FindException if an SQL error occurs while extracting module content.
     */
    @Nullable
    InputStream getModuleBytesAsStream(@NotNull Goid goid) throws FindException;

    /**
     * Retrieve module content as a byte array, in a read-only transaction
     *
     * @param goid    the module {@link Goid}.  Required and cannot be {@code null}.
     * @return A byte array that delivers the module content or {@code null} if module specified with the {@code goid} cannot be found.
     * @throws FindException if an SQL error occurs while extracting module content.
     */
    @Nullable
    byte[] getModuleBytes(@NotNull Goid goid) throws FindException;


    /**
     * Locate a {@link ServerModuleFile} specified by it's {@code moduleSha256}.
     *
     * @param moduleSha256    the module data bytes digest (currently sha256).
     * @return The {@link ServerModuleFile} that has the specified content digest.
     * @throws FindException if there is no {@link ServerModuleFile} having the specified {@code moduleSha256}.
     */
    @Nullable
    ServerModuleFile findModuleWithSha256(@NotNull String moduleSha256) throws FindException;
}
