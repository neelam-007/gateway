package com.l7tech.server.module;

import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;

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
}