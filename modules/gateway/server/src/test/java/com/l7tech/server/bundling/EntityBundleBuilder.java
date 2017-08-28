package com.l7tech.server.bundling;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.EntityHeaderUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Helps create EntityBundles for more readable unit tests.
 */
public class EntityBundleBuilder {
    private List<EntityContainer> entities = new ArrayList<>();
    private List<EntityMappingInstructions> instructions = new ArrayList<>();

    public EntityBundleBuilder expectExistingRootFolder() {
        instructions.add(new EntityMappingInstructions(
                EntityHeaderUtils.fromEntity(createRootFolder()), null,
                EntityMappingInstructions.MappingAction.NewOrExisting, true, false
        ));
        return this;
    }

    public EntityBundleBuilder updateServiceByPath(@NotNull final PublishedService publishedService) {
        entities.add(new EntityContainer(publishedService));
        final EntityMappingInstructions.TargetMapping serviceTargetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.PATH);
        final EntityMappingInstructions serviceMappingInstructions = new EntityMappingInstructions(
                EntityHeaderUtils.fromEntity(publishedService), serviceTargetMapping, EntityMappingInstructions.MappingAction.NewOrUpdate, false, false
        );
        instructions.add(serviceMappingInstructions);
        return this;
    }

    public EntityBundleBuilder updateFolderByPath(@NotNull Folder folder) {
        entities.add(new EntityContainer(folder));
        final EntityMappingInstructions.TargetMapping folderTargetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.PATH);
        instructions.add(new EntityMappingInstructions(
                EntityHeaderUtils.fromEntity(folder), folderTargetMapping, EntityMappingInstructions.MappingAction.NewOrUpdate, false, false));
        return this;
    }

    public EntityBundleBuilder updateServiceAliasByPath(@NotNull final PublishedServiceAlias alias, @NotNull final String aliasName) {
        entities.add(new EntityContainer(alias));
        final EntityMappingInstructions.TargetMapping aliasTargetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.PATH);
        final AliasHeader aliasHeader = (AliasHeader) EntityHeaderUtils.fromEntity(alias);
        aliasHeader.setName(aliasName);
        instructions.add(new EntityMappingInstructions(
                aliasHeader, aliasTargetMapping, EntityMappingInstructions.MappingAction.NewOrUpdate, false, false));
        return this;
    }

    @NotNull
    public EntityBundle create() {
        return new EntityBundle(entities, instructions, new ArrayList<>());
    }

    static Folder createRootFolder() {
        final Folder rootFolder = new Folder("Root", null);
        rootFolder.setGoid(Folder.ROOT_FOLDER_ID);

        return rootFolder;
    }
}
