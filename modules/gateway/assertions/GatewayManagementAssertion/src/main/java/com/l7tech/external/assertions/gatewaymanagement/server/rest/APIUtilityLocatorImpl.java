package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.APIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the implementation of the APIUtilityLocator. It listens to the ContextRefreshedEvent in order to load a list
 * of all the APIResourceFactory's and APITransformer's after the application context has been loaded.
 */
public class APIUtilityLocatorImpl implements ApplicationListener<ContextRefreshedEvent>, APIUtilityLocator {
    /**
     * The factory map
     */
    private final Map<String, APIResourceFactory> factoryMap = new HashMap<>();

    /**
     * The transformer map
     */
    private final Map<String, EntityAPITransformer> transformerMap = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public APIResourceFactory findFactoryByResourceType(@NotNull String entityType) {
        return factoryMap.get(entityType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public EntityAPITransformer findTransformerByResourceType(@NotNull String entityType) {
        return transformerMap.get(entityType);
    }


    /**
     * This will be triggered after the application context has been loaded. It will cause the utility maps to be
     * loaded.
     *
     * @param event The contextRefreshedEvent
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        factoryMap.clear();
        transformerMap.clear();
        populateMaps(event.getApplicationContext());
    }

    /**
     * Add all APIResourceFactory's and APITransformer's to the maps from the application context
     *
     * @param applicationContext The applicationContext to find the APIResourceFactory's and APITransformer's in
     */
    private void populateMaps(ApplicationContext applicationContext) {
        final Map<String, APIResourceFactory> factoryBeans = applicationContext.getBeansOfType(APIResourceFactory.class);
        for (APIResourceFactory factory : factoryBeans.values()) {
            factoryMap.put(factory.getResourceType(), factory);
        }
        final Map<String, EntityAPITransformer> transformerBeans = applicationContext.getBeansOfType(EntityAPITransformer.class);
        for (EntityAPITransformer transformer : transformerBeans.values()) {
            transformerMap.put(transformer.getResourceType(), transformer);
        }
    }
}
