package com.l7tech.objectmodel;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Static access and initialization (from jar resources) of EntityType's implementation classes.
 */
@SuppressWarnings({ "ThrowableResultOfMethodCallIgnored", "ThrowInsideCatchBlockWhichIgnoresCaughtException" })
public class EntityTypeRegistry {
    private static final String ENTITY_TYPES_PROPERTIES = "com/l7tech/EntityTypes.properties";
    private static final Map<EntityType, Class<? extends Entity>> REGISTRY;
    private static final Map<Class<? extends Entity>, EntityType> YRTSIGER;

    static {
        Pair<Map<EntityType, Class<? extends Entity>>, Map<Class<? extends Entity>, EntityType>> foo = init();
        REGISTRY = foo.left;
        YRTSIGER = foo.right;
    }

    private static Pair<Map<EntityType, Class<? extends Entity>>, Map<Class<? extends Entity>, EntityType>> init() {
        final Map<EntityType, Class<? extends Entity>> registry = new HashMap<EntityType,Class<? extends Entity>>();
        final Map<Class<? extends Entity>, EntityType> yrtsiger = new HashMap<Class<? extends Entity>, EntityType>();

        try {
            final Enumeration<URL> urls = EntityTypeRegistry.class.getClassLoader().getResources(ENTITY_TYPES_PROPERTIES);
            while (urls.hasMoreElements()) {
                final Properties props = new Properties();
                props.load(urls.nextElement().openStream());
                final Enumeration<String> propNames = (Enumeration<String>) props.propertyNames();
                while (propNames.hasMoreElements()) {
                    final String name = propNames.nextElement();
                    final Class clazz;
                    try {
                        clazz = Class.forName(props.getProperty(name));
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException(
                            String.format("Implementation class not found for EntityType: %s : %s", name, ExceptionUtils.getMessage(e)),
                                                           ExceptionUtils.getDebugException(e));
                    }

                    final EntityType type = EntityType.valueOf(name);
                    if (registry.containsKey(type))
                        throw new IllegalStateException(String.format("Implementation class for entity type %s already initialized to: %s", type, registry.get(type)));

                    if (! Entity.class.isAssignableFrom(clazz))
                        throw new IllegalArgumentException(String.format("Implementation class is not an Entity: %s", clazz));

                    registry.put(type, clazz);

                    if (yrtsiger.containsKey(clazz))
                        throw new IllegalStateException(String.format("Entity type for implementation class %s already initialized to: %s", clazz.getSimpleName(), yrtsiger.get(clazz)));
                    yrtsiger.put(clazz, type);
                }
            }
            return new Pair<Map<EntityType, Class<? extends Entity>>, Map<Class<? extends Entity>, EntityType>>(
                Collections.unmodifiableMap(registry),
                Collections.unmodifiableMap(yrtsiger)
            );
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Error initializing the EntityTypeRegistry: %s", ExceptionUtils.getMessage(e)), ExceptionUtils.getDebugException(e));
        }

    }

    private EntityTypeRegistry() {}

    public static Class<? extends Entity> getEntityClass(EntityType type) {
        return REGISTRY.get(type);
    }

    public static Collection<Class<? extends Entity>> getAllEntityClasses() {
        return REGISTRY.values();
    }

    public static EntityType getEntityType(Class<? extends Entity> clazz) {
        EntityType type = YRTSIGER.get(clazz);
        if ( type == null && clazz != null ) {
            type = EntityType.ANY;
            for ( Map.Entry<Class<? extends Entity>,EntityType> entry : YRTSIGER.entrySet() ) {
                final EntityType value = entry.getValue();
                Class<? extends Entity> entityClazz = entry.getKey();
                if ( entityClazz.isAssignableFrom(clazz) && entry.getValue() != EntityType.ANY) {
                    type = value;
                    break;
                }
            }
        }
        return type;
    }
}