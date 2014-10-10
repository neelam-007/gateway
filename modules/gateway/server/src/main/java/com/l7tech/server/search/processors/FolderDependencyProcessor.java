package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyAliasManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.service.ServiceAliasManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds dependencies that a folder has. The folder dependencies include sub-folders, policies, services, and aliases
 *
 * @author Victor Kazakov
 */
public class FolderDependencyProcessor extends DefaultDependencyProcessor<Folder> implements DependencyProcessor<Folder> {

    @Inject
    private FolderManager folderManager;

    @Inject
    private PolicyManager policyManager;

    @Inject
    private ServiceManager serviceManager;

    @Inject
    private PolicyAliasManager policyAliasManager;

    @Inject
    private ServiceAliasManager serviceAliasManager;

    /**
     * Find the dependencies of a folder. The dependencies are returned in the following order: sub-folders, policies,
     * services, policy aliases, service aliases.
     *
     * @param folder The folder to find dependencies for
     * @param finder The finder that is performing the current dependency search
     * @return The list of dependencies that this folder has.
     * @throws FindException
     */
    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull final Folder folder, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        //the super.findDependencies does not need to be called here. All folder dependencies will be explicitly handled.

        //find all entities that a folder can contain
        final ArrayList<HasFolder> hasFolders = new ArrayList<>();
        hasFolders.addAll(folderManager.findByFolder(folder.getGoid()));
        hasFolders.addAll(Functions.reduce(policyManager.findByFolder(folder.getGoid()), new ArrayList<Policy>(), new Functions.Binary<ArrayList<Policy>, ArrayList<Policy>, Policy>() {
            @Override
            public ArrayList<Policy> call(ArrayList<Policy> policies, Policy policy) {
                //Do not include policies that are used for services.
                if (!PolicyType.PRIVATE_SERVICE.equals(policy.getType())) {
                    policies.add(policy);
                }
                return policies;
            }
        }));
        hasFolders.addAll(serviceManager.findByFolder(folder.getGoid()));
        hasFolders.addAll(policyAliasManager.findByFolder(folder.getGoid()));
        hasFolders.addAll(serviceAliasManager.findByFolder(folder.getGoid()));

        final ArrayList<Dependency> dependencies = new ArrayList<>();
        //finds children of this folder
        for (final HasFolder child : hasFolders) {
            if (child.getFolder() != null && Goid.equals(folder.getGoid(), child.getFolder().getGoid())) {
                final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(child,null));
                if (dependency != null)
                    dependencies.add(dependency);
            }
        }

        //Get the security zone dependency
        final SecurityZone securityZone = folder.getSecurityZone();
        final Dependency securityZoneDependency = finder.getDependency(DependencyFinder.FindResults.create(securityZone,null));
        if (securityZoneDependency != null) {
            dependencies.add(securityZoneDependency);
        }

        return dependencies;
    }
}
