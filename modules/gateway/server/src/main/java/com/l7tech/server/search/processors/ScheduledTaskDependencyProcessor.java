package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.task.ScheduledTaskManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is used to find ScheduledTask's dependencies
 *
 * @author wlui
 */
public class ScheduledTaskDependencyProcessor extends DefaultDependencyProcessor<ScheduledTask> implements DependencyProcessor<ScheduledTask> {

    @Inject
    private ScheduledTaskManager scheduledTaskManager;

    @Inject
    private IdentityProviderFactory identityProviderFactory;

    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull ScheduledTask scheduledTask, @NotNull DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        final ArrayList<Dependency> dependencies = new ArrayList<>();
        dependencies.addAll(super.findDependencies(scheduledTask, finder));

        if (scheduledTask.getIdProviderGoid() != null && scheduledTask.getUserId() != null) {
            //Get the user dependency
            final User user = identityProviderFactory.getProvider(scheduledTask.getIdProviderGoid()).getUserManager().findByPrimaryKey(scheduledTask.getUserId());
            final Dependency userDependency = finder.getDependency(DependencyFinder.FindResults.create(user, null));
            if (userDependency != null) {
                dependencies.add(userDependency);
            }
        }
        return dependencies;
    }

    @Override
    public void replaceDependencies(@NotNull ScheduledTask scheduledTask, @NotNull Map<EntityHeader, EntityHeader> replacementMap, @NotNull DependencyFinder finder, boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        super.replaceDependencies(scheduledTask, replacementMap, finder, replaceAssertionsDependencies);

        // replace the user dependency
        if (scheduledTask.getIdProviderGoid() != null && scheduledTask.getUserId() != null) {
            //Get the user dependency
            EntityHeader header = new IdentityHeader(scheduledTask.getIdProviderGoid(), scheduledTask.getUserId(), EntityType.USER, "", "", "", 0);
            EntityHeader replace = replacementMap.get(header);
            if (replace != null && replace instanceof IdentityHeader) {
                scheduledTask.setIdProviderGoid(((IdentityHeader) replace).getProviderGoid());
                scheduledTask.setUserId(replace.getStrId());
            }
        }
    }
}
