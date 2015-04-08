package com.l7tech.server.module;

import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.EntityManagerStub;
import org.jetbrains.annotations.Nullable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.InputStream;

/**
 * A ServerModuleFileManager stub for testing {@link com.l7tech.gateway.common.cluster.ClusterStatusAdmin ClusterStatusAdmin}
 */
public class ServerModuleFileManagerStub extends EntityManagerStub<ServerModuleFile, EntityHeader> implements ServerModuleFileManager {

    /**
     * Construct with our sample modules
     */
    public ServerModuleFileManagerStub(final ServerModuleFile... moduleFiles) {
        super(moduleFiles);
    }

    @Override
    public void updateState(final Goid moduleGoid, final ModuleState state) throws UpdateException {
        // not needed for ClusterStatusAdmin
        throw new NotImplementedException();
    }

    @Override
    public void updateState(final Goid moduleGoid, final String errorMessage) throws UpdateException {
        // not needed for ClusterStatusAdmin
        throw new NotImplementedException();
    }

    @Override
    public ServerModuleFileState findStateForCurrentNode(final ServerModuleFile moduleFile) {
        // not needed for ClusterStatusAdmin
        throw new NotImplementedException();
    }

    @Override
    public boolean isModuleUploadEnabled() {
        // not needed for ClusterStatusAdmin
        throw new NotImplementedException();
    }

    @Nullable
    @Override
    public InputStream getModuleBytesAsStream(final Goid goid) throws FindException {
        // not needed for ClusterStatusAdmin
        throw new NotImplementedException();
    }
}
