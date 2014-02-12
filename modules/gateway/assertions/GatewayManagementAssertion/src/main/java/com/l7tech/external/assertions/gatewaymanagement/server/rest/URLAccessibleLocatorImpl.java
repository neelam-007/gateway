package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.objectmodel.EntityType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the implementation of the URLAccessibleLocator. It listens to the ContextRefreshedEvent in order to load a list
 * of all the URLAccessibleLocator's after the application context has been loaded.
 */
public class URLAccessibleLocatorImpl implements ApplicationListener<ContextRefreshedEvent>, URLAccessibleLocator {
    /**
     * The URLAccessible entity map
     */
    private final Map<EntityType, URLAccessible> urlAccessibleMap = new HashMap<>();

    /**
     * This returns the URLAccessible for the given entity type. Or null if there is no such URLAccessible
     *
     * @param entityType The entity type to return the entity URLAccessible for.
     * @return The URLAccessible for the given entityType or null if there is no such URLAccessible
     */
    @Override
    public URLAccessible findByEntityType(EntityType entityType) {
        return urlAccessibleMap.get(entityType);
    }

    /**
     * This will be triggered after the application context has been loaded. It will cause the urlAccessibleMap to be loaded.
     *
     * @param event The contextRefreshedEvent
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        urlAccessibleMap.clear();
        buildURLAccessibleCache(event.getApplicationContext());
    }

    /**
     * Add all RestEntityResource's to the urlAccessibleMap from the application context
     *
     * @param applicationContext The applicationContext to find the RestEntityResource's in
     */
    private void buildURLAccessibleCache(ApplicationContext applicationContext) {
        final Map<String, URLAccessible> beans = applicationContext.getBeansOfType(URLAccessible.class);
        for (URLAccessible restEntityResource : beans.values()) {
            urlAccessibleMap.put(restEntityResource.getEntityType(), restEntityResource);
        }
    }
}
