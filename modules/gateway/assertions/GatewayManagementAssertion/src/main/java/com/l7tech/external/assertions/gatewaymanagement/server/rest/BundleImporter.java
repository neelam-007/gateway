package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Item;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BundleImporter {

    @Inject
    private RestResourceLocator restResourceLocator;

    @NotNull
    public List<Mapping> importBundle(@NotNull Bundle bundle, @NotNull String importFolderID, @NotNull Map<String, Object> options) {
        final List<Mapping> mappingsRtn = new ArrayList<>(bundle.getMappings().size());
        Map<String, Mapping> mappings = Functions.toMap(bundle.getMappings(), new Functions.Unary<Pair<String, Mapping>, Mapping>() {
            @Override
            public Pair<String, Mapping> call(Mapping mapping) {
                Mapping mappingClone = ManagedObjectFactory.createMapping(mapping);
                mappingsRtn.add(mappingClone);
                return new Pair<>(mapping.getSrcId(), mappingClone);
            }
        });

        for (Item item : bundle.getReferences()) {
            Mapping mapping = mappings.get(item.getId());
            if (mapping == null) {
                throw new IllegalArgumentException("Cannot find mapping for " + item.getType() + " id: " + item.getId());
            }

            RestEntityResource restEntityResource = restResourceLocator.findByEntityType(EntityType.valueOf(mapping.getType()));

            switch (mapping.getAction()) {
                case NewOrExisting:
                    Item existingResourceItem = locateResource(mapping, restEntityResource);
                    if(existingResourceItem != null){
                        mapping.setActionTaken(Mapping.ActionTaken.UsedExisting);
                        mapping.setTargetId(existingResourceItem.getId());
                    } else {
                        boolean success = false;
                        try {
                            //noinspection unchecked
                            restEntityResource.updateResource(item.getContent(), item.getId());
                            success = true;
                        } catch (ResourceFactory.ResourceNotFoundException e) {
                            mapping.setErrorType(Mapping.ErrorType.TargetNotFound);
                        } catch (ResourceFactory.InvalidResourceException e) {
                            mapping.setErrorType(Mapping.ErrorType.UniqueKeyConflict);
                        }
                        if(success){
                            mapping.setActionTaken(Mapping.ActionTaken.CreatedNew);
                            mapping.setTargetId(item.getId());
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

        return mappingsRtn;
    }

    private Item locateResource(Mapping mapping, RestEntityResource restEntityResource) {
        try {
            return restEntityResource.getResource(mapping.getSrcId());
        } catch (ResourceFactory.ResourceNotFoundException e) {
            return null;
        }
    }
}
