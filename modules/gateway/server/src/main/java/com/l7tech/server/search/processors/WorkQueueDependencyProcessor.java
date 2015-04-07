package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.search.Dependency;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.server.workqueue.WorkQueueEntityManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class WorkQueueDependencyProcessor extends DefaultDependencyProcessor<WorkQueue> implements DependencyProcessor<WorkQueue> {

    @Inject
    private WorkQueueEntityManager workQueueEntityManager;

    @NotNull
    public List<DependencyFinder.FindResults<WorkQueue>> find(@NotNull final Object searchValue,
                                                              @NotNull final Dependency.DependencyType dependencyType,
                                                              @NotNull final Dependency.MethodReturnType searchValueType)
            throws FindException {
        //handles finding work queue by name
        switch (searchValueType) {
            case NAME:
                WorkQueue wq = workQueueEntityManager.getWorkQueueEntity((String) searchValue);
                return Arrays.<DependencyFinder.FindResults<WorkQueue>>asList(DependencyFinder.FindResults.create(
                        wq, new EntityHeader(Goid.DEFAULT_GOID, EntityType.WORK_QUEUE, (String) searchValue, null)));
            default:
                //if a different search method is specified then search for the work queue using the GenericDependency processor
                return super.find(searchValue, dependencyType, searchValueType);
        }
    }

    @NotNull
    @Override
    public List<DependentObject> createDependentObjects(@NotNull final Object searchValue,
                                                        @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType,
                                                        @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType)
            throws CannotRetrieveDependenciesException {
        //handles creating a dependent work queue from the name only.
        switch (searchValueType) {
            case NAME:
                WorkQueue wq = new WorkQueue();
                wq.setName((String) searchValue);
                return Arrays.asList(createDependentObject(wq));
            default:
                //if a different search method is specified then create the work queue using the GenericDependency processor
                return super.createDependentObjects(searchValue, dependencyType, searchValueType);
        }
    }
}
