package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.FolderResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;


/**
 * This was created: 11/18/13 as 4:30 PM
 *
 * @author Victor Kazakov
 */
@Component
public class FolderAPIResourceFactory extends WsmanBaseResourceFactory<FolderMO, FolderResourceFactory> {

    @Inject
    private DependencyAnalyzer dependencyAnalyzer;

    @Inject
    private FolderManager folderManager;

    @Inject
    private EntityCrud entityCrud;

    @Inject
    private PlatformTransactionManager transactionManager;

    @Inject
    private RbacAccessService rbacAccessService;

    public FolderAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.FOLDER.toString();
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.FolderResourceFactory factory) {
        super.factory = factory;
    }

    public void deleteResource(@NotNull String id, boolean forceDelete) throws ResourceFactory.ResourceNotFoundException {
        if(forceDelete){
            deleteRecursively(id);
        }else {
            super.deleteResource(id);
        }
    }

    private void deleteRecursively(final String id) throws ResourceFactory.ResourceNotFoundException {

        RestResourceFactoryUtils.transactional(transactionManager, false, new Functions.NullaryVoidThrows<ResourceFactory.ResourceNotFoundException>() {
            @Override
            public void call() throws ResourceFactory.ResourceNotFoundException {
                try {
                    final Folder folderToDelete = folderManager.findByPrimaryKey(Goid.parseGoid(id));
                    if(folderToDelete== null){
                        throw new ResourceFactory.ResourceNotFoundException("Resource not found " + id);
                    }
                    rbacAccessService.validatePermitted(folderToDelete, OperationType.DELETE);

                    //find the dependencies for the folder
                    final List<DependencySearchResults> dependencySearchResults = dependencyAnalyzer.getDependencies(
                            CollectionUtils.list(EntityHeaderUtils.fromEntity(folderToDelete)),
                            CollectionUtils.<String, Object>mapBuilder().put(DependencyAnalyzer.SearchEntityTypeOptionKey, CollectionUtils.list(EntityType.FOLDER)).map());
                    final List<Dependency> dependentObjects = dependencyAnalyzer.flattenDependencySearchResults(dependencySearchResults, true);
                    Collections.reverse(dependentObjects);

                    for (final Dependency dependentObject : dependentObjects) {
                        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
                        tt.setReadOnly(false);
                        final ResourceFactory.ResourceNotFoundException exception = tt.execute(new TransactionCallback<ResourceFactory.ResourceNotFoundException>() {
                            @Override
                            public ResourceFactory.ResourceNotFoundException doInTransaction(final TransactionStatus transactionStatus) {
                                try {
                                    EntityHeader entityHeader = ((DependentEntity) dependentObject.getDependent()).getEntityHeader();
                                    final Entity entity = entityCrud.find(entityHeader);
                                    if (entity != null) {
                                        // todo validation
                                        rbacAccessService.validatePermitted(entity, OperationType.DELETE);
                                        try {
                                            entityCrud.delete(entity);
                                        } catch (DeleteException e) {
                                            return new ResourceFactory.ResourceNotFoundException(entityHeader.getType().toString() + " resource #" + entity.getId() + " could not be deleted", e);
                                        }
                                    }
                                    transactionStatus.flush();
                                    return null;
                                } catch (FindException e) {
                                    return new ResourceFactory.ResourceNotFoundException(ExceptionUtils.getMessage(e), e);
                                }
                            }});

                        if (exception != null) {
                            throw exception;
                        }
                    }
                } catch (FindException e) {
                    throw new ResourceFactory.ResourceNotFoundException(ExceptionUtils.getMessage(e),e);
                } catch (CannotRetrieveDependenciesException e) {
                    throw new ResourceFactory.ResourceNotFoundException(ExceptionUtils.getMessage(e),e);
                }
            }
        });

    }
}