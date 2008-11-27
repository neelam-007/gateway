package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.EntityType;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;

/**
 * Utility methods for Migration related operations.
 *
 * @author jbufu
 */
public class MigrationUtils {

    private MigrationUtils() {}

    /**
     * Retrieves the property resolver for the given property method.
     *
     * The method can specify a custom property resolver through the {@link Migration} annotations;
     * if a custom one is not specified, the {@link com.l7tech.objectmodel.migration.DefaultEntityPropertyResolver}
     * is returned.
     *
     * @param property the method for which a property resolver is retrieved
     * @return  the property resolver
     * @throws MigrationException if the resolver cannot be instantiated
     * @see com.l7tech.objectmodel.migration.Migration, PropertyResolver
     */
    public static PropertyResolver getResolver(Method property) throws MigrationException {
        Class<? extends PropertyResolver> resolverClass = property.isAnnotationPresent(Migration.class) ?
            property.getAnnotation(Migration.class).resolver() : DefaultEntityPropertyResolver.class;
        try {
            return resolverClass.newInstance();
        } catch (Exception e) {
            throw new MigrationException("Error getting property resolver for: " + property, e);
        }
    }

    /**
     * Checkes if the given method is marked as a dependency (using the {@link com.l7tech.objectmodel.migration.Migration}
     * annotation, or if qualifies as a default dependency.
     *
     * @param property the method being checked
     * @return true if the method is annotated as a dependency; if the annotation is not present, falls back to the default dependency check.
     * @see #isDefaultDependency(java.lang.reflect.Method)
     */
    public static boolean isDependency(Method property) {

        if (property == null) return false;

        // annotation says it's a dependency
        return property.isAnnotationPresent(Migration.class) && property.getAnnotation(Migration.class).dependency()
               || isDefaultDependency(property);
    }

    /**
     * Checks if the given method qualifies a dependency by default (i.e. without being annotated).
     *
     * This applies for methods with the return type one of:
     * <ul>
     * <li>Entity</li>
     * <li>EntityHeader</li>
     * <li>Collections and arrays of the above</li>
     * </ul>
     *
     * Properties that are default dependencies can be handled using the {@link com.l7tech.objectmodel.migration.DefaultEntityPropertyResolver}.
     *
     * @param property the method being checked
     * @return true if the property qualifies as a default dependency, false otherwise
     * @see com.l7tech.objectmodel.migration.DefaultEntityPropertyResolver
     */
    public static boolean isDefaultDependency(Method property) {

        if (property == null) return false;

        // returns an EntityHeader or Entity, or an array of either
        Class returnType = property.getReturnType();
        if (returnType.isArray()) returnType = returnType.getComponentType();
        if (isEntityOrHeader(returnType))
            return true;

        // Collection<EntityHeader || Entity>
        Type genericReturnType = property.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) genericReturnType).getRawType();
            Type[] typeArgs = ((ParameterizedType)genericReturnType).getActualTypeArguments();
            if ( rawType instanceof Class && Collection.class.isAssignableFrom((Class)rawType) &&
                 typeArgs.length == 1 && typeArgs[0] instanceof Class && isEntityOrHeader((Class)typeArgs[0]))
                return true;
        }

        return false;
    }

    /**
     * Returns true if the given class parameter is (sub)class of Entity or EntityHeader.
     */
    public static boolean isEntityOrHeader(Class clazz) {
        return EntityHeader.class.isAssignableFrom(clazz) || Entity.class.isAssignableFrom(clazz);
    }

    /**
     * Retrives an EntityHeader from the given Entity parameter.
     */
    public static EntityHeader getHeaderFromEntity(Entity entity) {
        final String name = entity instanceof NamedEntity ? ((NamedEntity)entity).getName() : null;
        final EntityType type = EntityType.findTypeByEntity(entity.getClass());
        return new EntityHeader(entity.getId(), type, name, "");
    }

    /**
     * Extracts the MappingType from the given property method.
     *
     * If not mapping types are specified through the {@link com.l7tech.objectmodel.migration.Migration} annotation,
     * {@link MigrationMappingType.defaultMappning()} is returned.
     */
    public static MigrationMappingType getMappingType(Method property) {

        if (! property.isAnnotationPresent(Migration.class)) return MigrationMappingType.defaultMappning();

        Migration annotation = property.getAnnotation(Migration.class);
        return new MigrationMappingType(annotation.mapName(), annotation.mapValue());
    }
}
