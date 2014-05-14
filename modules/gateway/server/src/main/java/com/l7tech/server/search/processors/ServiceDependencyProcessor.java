package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is used to remove policies as dependencies of services. The policies dependencies will be given to the service.
 *
 * @author Victor Kazakov
 */
public class ServiceDependencyProcessor extends GenericDependencyProcessor<PublishedService> implements DependencyProcessor<PublishedService> {

    @Inject
    private FolderManager folderManager;

    @Override
    @NotNull
    public List<Dependency> findDependencies(PublishedService object, DependencyFinder finder) throws FindException {
        List<Dependency> dependencies;
        if (!finder.getOption(DependencyAnalyzer.ReturnServicePoliciesAsDependencies, Boolean.class, false)) {
            List<Dependency> dependenciesFound = super.findDependencies(object, finder);
            dependencies = new ArrayList<>();
            for (Dependency dependency : dependenciesFound) {
                if (com.l7tech.search.Dependency.DependencyType.POLICY.equals(dependency.getDependent().getDependencyType()) && dependency.getDependencies() != null) {
                    for (Dependency policyDependency : dependency.getDependencies()) {
                        dependencies.add(policyDependency);
                    }
                } else {
                    dependencies.add(dependency);
                }
            }
        } else {
            dependencies = super.findDependencies(object, finder);
        }
        return dependencies;
    }

    @Override
    public void replaceDependencies(@NotNull PublishedService object, @NotNull Map<EntityHeader, EntityHeader> replacementMap, DependencyFinder finder) throws CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
        super.replaceDependencies(object, replacementMap, finder);

        //This will replace dependencies in the assertions that this service contains
        if(object.getPolicy() != null) {
            PolicyDependencyProcessor.replacePolicyAssertionDependencies(object.getPolicy(), replacementMap, finder);
        }

        // replace parent folder

        final EntityHeader srcFolderHeader = EntityHeaderUtils.fromEntity(object.getFolder());
        EntityHeader folderHeaderToUse = replacementMap.get(srcFolderHeader);
        if(folderHeaderToUse != null) {
            try {
                Folder folder = folderManager.findByHeader(folderHeaderToUse);
                object.setFolder(folder);
            } catch (FindException e) {
                throw new CannotRetrieveDependenciesException(folderHeaderToUse.getName(), Folder.class, object.getClass(), "Cannot find folder", e);
            }
        }
    }
}
