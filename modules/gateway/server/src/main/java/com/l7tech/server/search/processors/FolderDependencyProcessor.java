package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyAliasManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.service.ServiceAliasManager;
import com.l7tech.server.service.ServiceManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Finds dependencies that a folder has. The folder dependencies include sub-folders, policies, services, and aliases
 *
 * @author Victor Kazakov
 */
public class FolderDependencyProcessor extends GenericDependencyProcessor<Folder> implements DependencyProcessor<Folder> {

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
    public List<Dependency> findDependencies(Folder folder, DependencyFinder finder) throws FindException {
        Collection<Folder> folders = folderManager.findAll();
        Collection<Policy> policies = policyManager.findAll();
        Collection<PublishedService> services = serviceManager.findAll();
        Collection<PolicyAlias> policyAliases = policyAliasManager.findAll();
        Collection<PublishedServiceAlias> serviceAliases = serviceAliasManager.findAll();

        final ArrayList<Dependency> dependencies = new ArrayList<>();
        //sub-folder dependencies
        for (Folder currentFolder : folders) {
            if (currentFolder.getFolder() != null && Goid.equals(folder.getGoid(), currentFolder.getFolder().getGoid())) {
                Dependency dependency = finder.getDependency(currentFolder);
                if(dependency != null)
                    dependencies.add(dependency);
            }
        }
        //service dependencies
        for (PublishedService service : services) {
            if (service.getFolder() != null && Goid.equals(folder.getGoid(), service.getFolder().getGoid())) {
                Dependency dependency = finder.getDependency(service);
                if(dependency != null) {
                    dependencies.add(dependency);
                }
            }
        }
        //policy dependencies
        for (Policy policy : policies) {
            if (policy.getFolder() != null && Goid.equals(folder.getGoid(), policy.getFolder().getGoid()) && !PolicyType.PRIVATE_SERVICE.equals(policy.getType())) {
                Dependency dependency = finder.getDependency(policy);
                if(dependency != null) {
                    dependencies.add(dependency);
                }
            }
        }
        //policy alias dependencies
        for (PolicyAlias policyAlias : policyAliases) {
            if (policyAlias.getFolder() != null && Goid.equals(folder.getGoid(), policyAlias.getFolder().getGoid())) {
                Dependency dependency = finder.getDependency(policyAlias);
                if(dependency != null) {
                    dependencies.add(dependency);
                }
            }
        }
        //service alias dependencies
        for (PublishedServiceAlias serviceAlias : serviceAliases) {
            if (serviceAlias.getFolder() != null && Goid.equals(folder.getGoid(), serviceAlias.getFolder().getGoid())) {
                Dependency dependency = finder.getDependency(serviceAlias);
                if(dependency != null) {
                    dependencies.add(dependency);
                }
            }
        }

        SecurityZone securityZone = folder.getSecurityZone();
        if (securityZone != null) {
            Dependency securityZoneDependency = finder.getDependency(securityZone);
            if(securityZoneDependency != null) {
                dependencies.add(securityZoneDependency);
            }
        }

        return dependencies;
    }
}
