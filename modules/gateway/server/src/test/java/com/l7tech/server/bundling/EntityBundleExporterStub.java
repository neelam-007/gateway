package com.l7tech.server.bundling;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.NotNull;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Properties;

public class EntityBundleExporterStub implements EntityBundleExporter {
    @NotNull
    @Override
    public EntityBundle exportBundle(@NotNull Properties bundleExportProperties, @NotNull EntityHeader... headers) throws FindException {
        throw new NotImplementedException();
    }
}
