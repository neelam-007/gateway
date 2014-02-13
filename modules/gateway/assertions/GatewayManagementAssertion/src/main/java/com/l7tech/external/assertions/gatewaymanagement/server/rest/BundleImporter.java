package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.APIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

@SuppressWarnings("unchecked")
public class BundleImporter {

    @Inject
    private URLAccessibleLocator URLAccessibleLocator;
    @Inject
    private APIUtilityLocator apiUtilityLocator;
    @Inject
    private PlatformTransactionManager transactionManager;
    @Inject
    private DependencyAnalyzer dependencyAnalyzer;

    private static final String FailOnNew = "FailOnNew";
    private static final String FailOnExisting = "FailOnExisting";

    @NotNull
    public List<Mapping> importBundle(@NotNull final Bundle bundle, final boolean test) {
        final List<Mapping> mappingsRtn = new ArrayList<>(bundle.getMappings().size());
        final Map<String, Mapping> mappings = Functions.toMap(bundle.getMappings(), new Functions.Unary<Pair<String, Mapping>, Mapping>() {
            @Override
            public Pair<String, Mapping> call(Mapping mapping) {
                Mapping mappingClone = ManagedObjectFactory.createMapping(mapping);
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

                    URLAccessible urlAccessible = URLAccessibleLocator.findByEntityType(mapping.getType());

                    Item existingResourceItem = locateResource(mapping);
                    if (existingResourceItem != null) {
                        //Use the existing entity
                        Boolean isFailOnExisting = mapping.getProperty(FailOnExisting);
                        if(isFailOnExisting != null && isFailOnExisting){
                            mapping.setErrorType(Mapping.ErrorType.TargetExists);
                            transactionStatus.setRollbackOnly();
                        } else {
                            switch (mapping.getAction()) {
                                case NewOrExisting:
                                    //Use the existing entity
                                    mapping.setActionTaken(Mapping.ActionTaken.UsedExisting);
                                    mapping.setTargetId(existingResourceItem.getId());
                                    mapping.setTargetUri(urlAccessible.getUrl(existingResourceItem.getContent()));
                                    break;
                                case NewOrUpdate:
                                    //update the existing entity
                                    updateResource(item, existingResourceItem, mapping, mappingsRtn);
                                    break;
                                case AlwaysCreateNew:
                                    try {
                                        createResource(item, null, mapping, mappingsRtn);
                                    } catch (Exception e) {
                                        transactionStatus.setRollbackOnly();
                                    }
                                    break;
                                case Ignore:
                                    mapping.setActionTaken(Mapping.ActionTaken.Ignored);
                                    break;
                                default:
                                    throw new IllegalStateException("Unknown mapping action: " + mapping.getAction());
                            }
                        }
                    } else {
                        Boolean isFailOnNew = mapping.getProperty(FailOnNew);
                        if(isFailOnNew != null && isFailOnNew){
                            mapping.setErrorType(Mapping.ErrorType.TargetNotFound);
                            transactionStatus.setRollbackOnly();
                            mappingsRtn.add(mapping);
                            continue;
                        } else {
                            switch (mapping.getAction()) {
                                case NewOrExisting:
                                case NewOrUpdate:
                                case AlwaysCreateNew:
                                    //Create a new entity.
                                    try {
                                        createResource(item, item.getId(), mapping, mappingsRtn);
                                    } catch (Exception e) {
                                        transactionStatus.setRollbackOnly();
                                    }
                                    break;
                                case Ignore:
                                    mapping.setActionTaken(Mapping.ActionTaken.Ignored);
                                    break;
                                default:
                                    throw new IllegalStateException("Unknown mapping action: " + mapping.getAction());
                            }
                        }
                    }
                    mappingsRtn.add(mapping);
                }
                //TODO: handle any recursive dependencies (Encapsulated assertions?)
                if(test){
                    transactionStatus.setRollbackOnly();
                }
                return null;
            }

        });

        return mappingsRtn;
    }

    private void updateResource(Item item, Item existingResourceItem, Mapping mapping, List<Mapping> mappingsRtn) {

    }

    protected void createResource(@NotNull Item item, @Nullable String id, @NotNull Mapping mapping, @NotNull List<Mapping> resourcesProcessed) throws ResourceFactory.InvalidResourceException, ResourceFactory.ResourceNotFoundException, FindException {
        try {
            //get transformer
            APITransformer transformer = apiUtilityLocator.findTransformerByResourceType(mapping.getType());
            //noinspection unchecked
            Entity entity = (Entity) transformer.convertFromMO(item.getContent(), true);
            //dependencyAnalyzer.replaceDependencies(entity, null);
            //TODO: is there a way to do this without converting back to an MO?
            //convert back to an mo
            //TODO: special cases like passwords
            Object managedObject = transformer.convertToMO(entity);
            if(managedObject instanceof StoredPasswordMO) {
                ((StoredPasswordMO)managedObject).setPassword(((StoredPasswordMO)item.getContent()).getPassword());
            }
            final APIResourceFactory factory = apiUtilityLocator.findFactoryByResourceType(mapping.getType());

            //save the mo
            if(managedObject instanceof ManagedObject){
                ((ManagedObject)managedObject).setId(id);
            }
            if(id == null) {
                factory.createResource(managedObject);
            } else {
                factory.createResource(id, managedObject);
            }
        } catch (ResourceFactory.InvalidResourceException e) {
            mapping.setErrorType(Mapping.ErrorType.UniqueKeyConflict);
            throw e;
        }
        mapping.setActionTaken(Mapping.ActionTaken.CreatedNew);
        mapping.setTargetId(item.getId());

        URLAccessible urlAccessible = URLAccessibleLocator.findByEntityType(mapping.getType());

        mapping.setTargetUri(urlAccessible.getUrl(item.getContent()));
    }

    private Item locateResource(final Mapping mapping) {
        final APIResourceFactory factory = apiUtilityLocator.findFactoryByResourceType(mapping.getType());
        final APITransformer transformer = apiUtilityLocator.findTransformerByResourceType(mapping.getType());
        //this needs to be wrapped in a transaction that ignores rollback. We don't need to rollback if a resource cannot be found.
        final TransactionTemplate tt = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        tt.setReadOnly( true );
        try{
            return tt.execute( new TransactionCallback<Item>(){
                @Override
                public Item doInTransaction( final TransactionStatus transactionStatus ) {
                    try {
                        return transformer.convertToItem(factory.getResource(mapping.getTargetId() != null ? mapping.getTargetId() : mapping.getSrcId()));
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
