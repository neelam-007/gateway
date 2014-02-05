package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BundleImporter {

    @Inject
    private RestResourceLocator restResourceLocator;
    @Inject
    private PlatformTransactionManager transactionManager;

    private static final String FailOnNew = "FailOnNew";
    private static final String FailOnExisting = "FailOnExisting";

    @NotNull
    public List<Mapping> importBundle(@NotNull final Bundle bundle, final boolean test) {
        final List<Mapping> mappingsRtn = new ArrayList<>(bundle.getMappings().size());
        final Map<String, Mapping> mappings = Functions.toMap(bundle.getMappings(), new Functions.Unary<Pair<String, Mapping>, Mapping>() {
            @Override
            public Pair<String, Mapping> call(Mapping mapping) {
                Mapping mappingClone = ManagedObjectFactory.createMapping(mapping);
                mappingsRtn.add(mappingClone);
                return new Pair<>(mapping.getSrcId(), mappingClone);
            }
        });


        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly( false );
        tt.execute( new TransactionCallback(){
            @Override
            public Object doInTransaction( final TransactionStatus transactionStatus ) {
                for (Item item : bundle.getReferences()) {
                    Mapping mapping = mappings.get(item.getId());
                    if (mapping == null) {
                        throw new IllegalArgumentException("Cannot find mapping for " + item.getType() + " id: " + item.getId());
                    }

                    RestEntityResource restEntityResource = restResourceLocator.findByEntityType(EntityType.valueOf(mapping.getType()));

                    switch (mapping.getAction()) {
                        case NewOrExisting:
                            Item existingResourceItem = locateResource(mapping, restEntityResource);
                            if (existingResourceItem != null) {
                                Boolean isFailOnExisting = mapping.getProperty(FailOnExisting);
                                if(isFailOnExisting != null && isFailOnExisting){
                                    mapping.setErrorType(Mapping.ErrorType.TargetExists);
                                    transactionStatus.setRollbackOnly();
                                } else {
                                    mapping.setActionTaken(Mapping.ActionTaken.UsedExisting);
                                    mapping.setTargetId(existingResourceItem.getId());
                                    mapping.setTargetUri(restEntityResource.getUrl(existingResourceItem.getId()));
                                }
                            } else {
                                Boolean isFailOnNew = mapping.getProperty(FailOnNew);
                                if(isFailOnNew != null && isFailOnNew){
                                    mapping.setErrorType(Mapping.ErrorType.TargetNotFound);
                                    transactionStatus.setRollbackOnly();
                                } else {
                                    try {
                                        createResourse(item, mapping, restEntityResource);
                                    } catch (Exception e) {
                                        transactionStatus.setRollbackOnly();
                                    }
                                }
                            }
                            break;
                        case NewOrUpdate:
                            break;
                        case AlwaysCreateNew:
                            break;
                        case Ignore:
                            break;
                        default:
                            throw new IllegalStateException("Unknown mapping action: " + mapping.getAction());
                    }
                }
                if(test){
                    transactionStatus.setRollbackOnly();
                }
                return null;
            }

        });

        return mappingsRtn;
    }

    protected void createResourse(Item item, Mapping mapping, RestEntityResource restEntityResource) throws ResourceFactory.InvalidResourceException, ResourceFactory.ResourceNotFoundException {
        try {
            //noinspection unchecked
            restEntityResource.updateResource(item.getContent(), item.getId());
        } catch (ResourceFactory.ResourceNotFoundException e) {
            mapping.setErrorType(Mapping.ErrorType.TargetNotFound);
            throw e;
        } catch (ResourceFactory.InvalidResourceException e) {
            mapping.setErrorType(Mapping.ErrorType.UniqueKeyConflict);
            throw e;
        }
        mapping.setActionTaken(Mapping.ActionTaken.CreatedNew);
        mapping.setTargetId(item.getId());
        mapping.setTargetUri(restEntityResource.getUrl(item.getId()));

        //apply mappings
    }

    private Item locateResource(final Mapping mapping, final RestEntityResource restEntityResource) {
        //this needs to be wrapped in a transaction that ignores rollback. We don't need to rollback if a resource cannot be found.
        final TransactionTemplate tt = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        tt.setReadOnly( true );
        try{
            return tt.execute( new TransactionCallback<Item>(){
                @Override
                public Item doInTransaction( final TransactionStatus transactionStatus ) {
                    try {
                        return restEntityResource.getResource(mapping.getTargetId() != null ? mapping.getTargetId() : mapping.getSrcId());
                    } catch (ResourceFactory.ResourceNotFoundException e) {
                        return null;
                    }
                }
            });
        } catch(UnexpectedRollbackException e){
            return null;
        }
    }
}
