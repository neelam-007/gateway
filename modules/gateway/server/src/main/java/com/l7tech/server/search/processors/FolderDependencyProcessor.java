package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyAliasManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.service.ServiceAliasManager;
import com.l7tech.server.service.ServiceManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.*;

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
            if (currentFolder.getFolder() != null && folder.getOid() == currentFolder.getFolder().getOid()) {
                Dependency dependency = finder.getDependency(currentFolder);
                dependencies.add(dependency);
            }
        }
        //keep a list of the service policies found. These policies will not need to be added as dependencies because the services will already have them as dependencies.
        Set<Policy> servicePolicies = new HashSet<>();
        //service dependencies
        for (PublishedService service : services) {
            if (service.getFolder() != null && folder.getOid() == service.getFolder().getOid()) {
                Dependency dependency = finder.getDependency(service);
                dependencies.add(dependency);
                servicePolicies.add(service.getPolicy());
            }
        }
        //policy dependencies
        for (Policy policy : policies) {
            if (policy.getFolder() != null && folder.getOid() == policy.getFolder().getOid() && !servicePolicies.contains(policy)) {
                Dependency dependency = finder.getDependency(policy);
                dependencies.add(dependency);
            }
        }
        //policy alias dependencies
        for (PolicyAlias policyAlias : policyAliases) {
            if (policyAlias.getFolder() != null && folder.getOid() == policyAlias.getFolder().getOid()) {
                Dependency dependency = finder.getDependency(policyAlias);
                dependencies.add(dependency);
            }
        }
        //service alias dependencies
        for (PublishedServiceAlias serviceAlias : serviceAliases) {
            if (serviceAlias.getFolder() != null && folder.getOid() == serviceAlias.getFolder().getOid()) {
                Dependency dependency = finder.getDependency(serviceAlias);
                dependencies.add(dependency);
            }
        }

        SecurityZone securityZone = folder.getSecurityZone();
        if (securityZone != null) {
            Dependency securityZoneDependency = finder.getDependency(securityZone);
            dependencies.add(securityZoneDependency);
        }

        return dependencies;
    }
}
