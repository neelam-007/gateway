package com.l7tech.server.bundling;

import org.jetbrains.annotations.NotNull;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

public class EntityBundleImporterStub implements EntityBundleImporter {
    @NotNull
    @Override
    public List<EntityMappingResult> importBundle(@NotNull EntityBundle bundle, boolean test, final boolean active, final String versionComment) {
        throw new NotImplementedException();
    }
}
