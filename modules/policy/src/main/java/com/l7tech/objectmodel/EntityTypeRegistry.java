package com.l7tech.objectmodel;

import com.l7tech.util.ExceptionUtils;

import java.util.*;
import java.net.URL;
import java.io.IOException;

/**
 * Static access and initialization (from jar resources) of EntityType's implementation classes.
 *
 */
public class EntityTypeRegistry {

//    private static final String ENTITY_TYPES_PROPERTIES = "META-INF/com.l7tech.objectmodel.EntityTypes.properties";
    private static final String ENTITY_TYPES_PROPERTIES = "com/l7tech/EntityTypes.properties";
    private static final Map<EntityType, Class<? extends Entity>> REGISTRY = init();

    private static Map<EntityType, Class<? extends Entity>> init() {

        HashMap<EntityType, Class<? extends Entity>> registry = new HashMap<EntityType,Class<? extends Entity>>();

        try {
            Enumeration<URL> urls = EntityTypeRegistry.class.getClassLoader().getResources(ENTITY_TYPES_PROPERTIES);
            while (urls.hasMoreElements()) {
                Properties props = new Properties();
                props.load(urls.nextElement().openStream());
                Enumeration<String> propNames = (Enumeration<String>) props.propertyNames();
                while (propNames.hasMoreElements()) {
                    String name = propNames.nextElement();
                    Class clazz;
                    try {
                        clazz = Class.forName(props.getProperty(name));
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException("Implementation class not found for EntityType: " + name + " : " + ExceptionUtils.getMessage(e),
                                                           ExceptionUtils.getDebugException(e));
                    }

                    if (registry.keySet().contains(EntityType.valueOf(name)))
                        throw new IllegalStateException("Implementation class for entity type already initialized to: " + registry.get(EntityType.valueOf(name)));

                    if (! Entity.class.isAssignableFrom(clazz))
                        throw new IllegalArgumentException("Implementation class is not an Entity: " + clazz);

                    registry.put(EntityType.valueOf(name), clazz);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error initializing the EntityTypeRegistry: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }

        return registry;
    }

    private EntityTypeRegistry() {}

    public static Class<? extends Entity> getEntityClass(EntityType type) {
        return REGISTRY.get(type);
    }
}
