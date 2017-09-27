package com.l7tech.server.bundling;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

public class EntityBundleImporterStub implements EntityBundleImporter {
    @NotNull
    @Override
    public List<List<EntityMappingResult>> importBundles(@NotNull List<EntityBundle> bundles, boolean test, boolean active, @Nullable String versionComment) {
        throw new NotImplementedException();
    }
}
