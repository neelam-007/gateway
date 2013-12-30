package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.objectmodel.EntityType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the implementation of the RestEntityResource. It listens to the ContextRefreshedEvent in order to load a list
 * of all the RestEntityResource's after the application context has been loaded.
 */
public class RestResourceLocatorImpl implements ApplicationListener<ContextRefreshedEvent>, RestResourceLocator {
    /**
     * The rest resource entity map
     */
    private final Map<EntityType, RestEntityResource> restEntityResourceMap = new HashMap<>();

    /**
     * This returns the RestEntityResource for the given entity type. Or null if there is no such RestEntityResource
     *
     * @param entityType The entity type to return the entity resource for.
     * @return The RestEntityResource for the given entityType or null if there is no such RestEntityResource
     */
    @Override
    public RestEntityResource findByEntityType(EntityType entityType) {
        return restEntityResourceMap.get(entityType);
    }

    /**
     * This will be triggered after the application context has been loaded. It will cause the restEntityResourceMap to be loaded.
     *
     * @param event The contextRefreshedEvent
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        restEntityResourceMap.clear();
        buildReferenceEntityCache(event.getApplicationContext());
    }

    /**
     * Add all RestEntityResource's to the restEntityResourceMap from the application context
     *
     * @param applicationContext The applicationContext to find the RestEntityResource's in
     */
    private void buildReferenceEntityCache(ApplicationContext applicationContext) {
        final Map<String, RestEntityResource> beans = applicationContext.getBeansOfType(RestEntityResource.class);
        for (RestEntityResource restEntityResource : beans.values()) {
            restEntityResourceMap.put(restEntityResource.getEntityType(), restEntityResource);
        }
    }
}
