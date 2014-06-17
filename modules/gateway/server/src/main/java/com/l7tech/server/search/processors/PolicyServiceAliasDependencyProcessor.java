package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.Alias;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * This is a custom dependency processor for policy and service aliases. It is needed to replace parent folder
 * dependencies
 */
public class PolicyServiceAliasDependencyProcessor extends DefaultDependencyProcessor<Alias> implements DependencyProcessor<Alias> {

    @Override
    public void replaceDependencies(@NotNull final Alias alias, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies ) throws CannotReplaceDependenciesException {
        super.replaceDependencies(alias, replacementMap, finder, replaceAssertionsDependencies);
        //replace the alias parent folder.
        if(alias.getFolder() != null){
            final EntityHeader parentFolderHeaderToUse = replacementMap.get(EntityHeaderUtils.fromEntity(alias.getFolder()));
            if(parentFolderHeaderToUse != null) {
                try {
                    alias.setFolder((Folder) loadEntity(parentFolderHeaderToUse));
                } catch (FindException e) {
                    throw new CannotReplaceDependenciesException(parentFolderHeaderToUse.getName(), parentFolderHeaderToUse.getStrId(), Folder.class, alias.getClass(), "Cannot find parent folder to use", e);
                }
            }
        }
    }
}
