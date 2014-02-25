package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.APIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public class BundleImporter {
    private static final Logger logger = Logger.getLogger(BundleImporter.class.getName());

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
        if(!test) {
            logger.log(Level.INFO, "Importing bundle!");
        } else {
            logger.log(Level.FINE, "Test Importing bundle!");
        }
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
                Map<EntityHeader, EntityHeader> resourceMapping = new HashMap<>(bundle.getReferences().size());
                for (Item item : bundle.getReferences()) {
                    Mapping mapping = mappings.get(item.getId());
                    if (mapping == null) {
                        throw new IllegalArgumentException("Cannot find mapping for " + item.getType() + " id: " + item.getId());
                    }

                    URLAccessible urlAccessible = URLAccessibleLocator.findByEntityType(mapping.getType());

                    //Find an existing resource to map it to.
                    Item existingResourceItem = locateResource(mapping, item);
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

                                    //get transformer
                                    APITransformer transformer = apiUtilityLocator.findTransformerByResourceType(mapping.getType());
                                    // Adds the mapped headers to the resourceMapping map if they are different.
                                    try {
                                        EntityHeader existingHeader = transformer.convertToHeader(existingResourceItem.getContent());
                                        EntityHeader originalHeader = transformer.convertToHeader(item.getContent());
                                        if(!headersMatch(existingHeader, originalHeader)){
                                            resourceMapping.put(originalHeader, existingHeader);
                                        }
                                    } catch (ResourceFactory.InvalidResourceException e) {
                                        mapping.setErrorType(Mapping.ErrorType.InvalidResource);
                                        transactionStatus.setRollbackOnly();
                                    }
                                    break;
                                case NewOrUpdate:
                                    try {
                                        //update the existing entity
                                        createOrUpdateResource(item, existingResourceItem.getId(), mapping, resourceMapping, false);
                                    } catch (Exception e) {
                                        mapping.setErrorType(Mapping.ErrorType.UniqueKeyConflict);
                                        transactionStatus.setRollbackOnly();
                                    }
                                    break;
                                case AlwaysCreateNew:
                                    try {
                                        createOrUpdateResource(item, null, mapping, resourceMapping, true);
                                    } catch (Exception e) {
                                        mapping.setErrorType(Mapping.ErrorType.UniqueKeyConflict);
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
                                        createOrUpdateResource(item, item.getId(), mapping, resourceMapping, true);
                                    } catch (CannotReplaceDependenciesException e) {
                                        mapping.setErrorType(Mapping.ErrorType.CannotReplaceDependency);
                                        mapping.addProperty("ErrorMessage", e.getMessage());
                                        transactionStatus.setRollbackOnly();
                                    } catch (ResourceFactory.ResourceNotFoundException e) {
                                        mapping.setErrorType(Mapping.ErrorType.TargetNotFound);
                                        mapping.addProperty("ErrorMessage", e.getMessage());
                                        transactionStatus.setRollbackOnly();
                                    } catch (ResourceFactory.ResourceFactoryException e) {
                                        mapping.setErrorType(Mapping.ErrorType.UniqueKeyConflict);
                                        mapping.addProperty("ErrorMessage", e.getMessage());
                                        transactionStatus.setRollbackOnly();
                                    } catch (CannotRetrieveDependenciesException e) {
                                        mapping.setErrorType(Mapping.ErrorType.TargetNotFound);
                                        mapping.addProperty("ErrorMessage", e.getMessage());
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

    /**
     * Returns true if the headers match. They have id's, names, and guids the same
     * @param header1 The first header
     * @param header2 The second header
     * @return True if the headers match (represent the same entity)
     */
    private boolean headersMatch(EntityHeader header1, EntityHeader header2) {
        //checks if they are GUID headers.
        if(header1 instanceof GuidEntityHeader){
            if(!(header2 instanceof GuidEntityHeader) || !StringUtils.equals(((GuidEntityHeader)header1).getGuid(), ((GuidEntityHeader)header2).getGuid())){
                return false;
            }
        }
        return header1.equals(header2)
                && StringUtils.equals(header1.getName(), header2.getName());
    }

    private void createOrUpdateResource(@NotNull final Item item, @Nullable final String id, @NotNull final Mapping mapping, @NotNull final Map<EntityHeader, EntityHeader> resourceMapping, final boolean create) throws ResourceFactory.ResourceFactoryException, CannotReplaceDependenciesException, CannotRetrieveDependenciesException {
        //validate that the id is not null if create is false
        if (!create && id == null) {
            throw new IllegalArgumentException("Must specify an id when updating an existing entity.");
        }
        try {
            //get transformer
            final APITransformer transformer = apiUtilityLocator.findTransformerByResourceType(mapping.getType());
            //noinspection unchecked
            Entity entity = (Entity) transformer.convertFromMO(item.getContent(), false);
            //if it is a mapping by name and the mapped name is set it should be preserved here.
            if(mapping.getProperties() != null && "name".equals(mapping.getProperties().get("MapBy")) && entity instanceof NamedEntityImp){
                String mapTo = (String) mapping.getProperties().get("MapTo");
                if(mapTo != null) {
                    ((NamedEntityImp)entity).setName(mapTo);
                }
            }
            //replace dependencies in the entity
            dependencyAnalyzer.replaceDependencies(entity, resourceMapping);
            //TODO: is there a way to do this without converting back to an MO?
            //convert back to an mo
            //TODO: special cases like passwords, is there a better way to handle them
            final Object managedObject = transformer.convertToMO(entity);
            if(managedObject instanceof StoredPasswordMO) {
                ((StoredPasswordMO)managedObject).setPassword(((StoredPasswordMO)item.getContent()).getPassword());
            }
            final APIResourceFactory factory = apiUtilityLocator.findFactoryByResourceType(mapping.getType());

            //save the mo
            if(managedObject instanceof ManagedObject){
                ((ManagedObject)managedObject).setId(id);
            }
            // Create the managed object within a transaction so that it can be flushed after it is created.
            // Flushing allows it to be found later by the entity managers.
            final TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setReadOnly( false );
            Either<ResourceFactory.ResourceFactoryException, String> idOrException = tt.execute(new TransactionCallback<Either<ResourceFactory.ResourceFactoryException, String>>() {
                @Override
                public Either<ResourceFactory.ResourceFactoryException, String> doInTransaction(final TransactionStatus transactionStatus) {
                    final String importedID;
                    try {
                        if(create){
                            if (id == null) {
                                importedID = factory.createResource(managedObject);
                            } else {
                                factory.createResource(id, managedObject);
                                importedID = id;
                            }
                        } else {
                            factory.updateResource(id, managedObject);
                            importedID = id;
                        }
                    } catch (ResourceFactory.ResourceFactoryException e) {
                        return Either.left(e);
                    }
                    //flush the newly created object so that it can be found by the entity managers later.
                    transactionStatus.flush();
                    return Either.right(importedID);
                }
            });
            //throw the exception if there was one attempting to save the entity.
            final String importedID = Eithers.extract(idOrException);

            // Adds the mapped headers to the resourceMapping map if they are different.
            try {
                EntityHeader createdHeader = transformer.convertToHeader(managedObject);
                EntityHeader originalHeader = transformer.convertToHeader(item.getContent());
                if(!headersMatch(createdHeader, originalHeader)){
                    resourceMapping.put(originalHeader, createdHeader);
                }
            } catch (ResourceFactory.InvalidResourceException e) {
                mapping.setErrorType(Mapping.ErrorType.InvalidResource);
                throw e;
            }
            mapping.setActionTaken(create ? Mapping.ActionTaken.CreatedNew : Mapping.ActionTaken.UpdatedExisting);
            mapping.setTargetId(importedID);

            URLAccessible urlAccessible = URLAccessibleLocator.findByEntityType(mapping.getType());

            mapping.setTargetUri(urlAccessible.getUrl(managedObject));

        } catch (ResourceFactory.ResourceFactoryException e) {
            mapping.setErrorType(Mapping.ErrorType.InvalidResource);
            throw e;
        }
    }

    private Item locateResource(final Mapping mapping, final Item item) {
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
                        Object resource;
                        //check if should search by name
                        if(mapping.getProperties() != null && "name".equals(mapping.getProperties().get("MapBy"))){
                            String mapTo = (String) mapping.getProperties().get("MapTo");
                            if(mapTo == null){
                                //If the mapTo property is not set get the name form the item in the bundle
                                mapTo = transformer.convertToHeader(item.getContent()).getName();
                            }
                            //Find the item by its name
                            List list = factory.listResources(0, 1, null, null, CollectionUtils.MapBuilder.builder().put("name", Arrays.asList(mapTo)).map());
                            if(list.isEmpty()){
                                return null;
                            }
                            resource = list.get(0);
                        } else {
                            resource = factory.getResource(mapping.getTargetId() != null ? mapping.getTargetId() : mapping.getSrcId());
                        }
                        return transformer.convertToItem(resource);
                    } catch (ResourceFactory.ResourceNotFoundException | ResourceFactory.InvalidResourceException e) {
                        return null;
                    }
                }
            });
        } catch(UnexpectedRollbackException e){
            return null;
        }
    }
}
