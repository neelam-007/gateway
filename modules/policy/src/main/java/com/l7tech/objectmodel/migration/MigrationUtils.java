package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.util.EmptyIterator;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;

/**
 * Utility methods for Migration related operations.
 *
 * @author jbufu
 */
public class MigrationUtils {

    private MigrationUtils() {}

    private static String stripPropertyName(String propName) {
        String name = propName;
        int sep = propName.indexOf(":");
        if ( sep > -1 )
            name = propName.substring(0, sep);
        return name;
    }

    public static PropertyResolver.Type getTargetType(Entity sourceEntity, String propName) throws PropertyResolverException {
        String name = stripPropertyName(propName);
        Method method = getterForPropertyName(sourceEntity, name);
        if (method == null)
            throw new PropertyResolverException("No getter found for the entity:property combination " + sourceEntity.getClass() + " : " + propName);
        return getTargetType(method);
    }

    public static PropertyResolver.Type getTargetType(Method method) {
        if (method.isAnnotationPresent(Migration.class))
            return method.getAnnotation(Migration.class).resolver();
        else
            return PropertyResolver.Type.DEFAULT;
    }

    private static Method methodForPropertyName(Object sourceEntity, String name) {
        return methodForPropertyName(sourceEntity, name, new Class<?>[]{});
    }

    private static Method methodForPropertyName(Object sourceEntity, String name, Class<?>... setterParam) {
        String prefix = setterParam == null || setterParam.length == 0 ? "get" : "set";
        try {
            // try with prefix first
            if (setterParam == null || setterParam.length == 0)
                return sourceEntity.getClass().getMethod(name.startsWith(prefix) ? name : prefix + name);
            else
                return sourceEntity.getClass().getMethod(name.startsWith(prefix) ? name : prefix + name, setterParam);
        } catch (NoSuchMethodException e) {
            // fall back to non-prefixed name
            if (! name.startsWith(prefix)) {
                try {
                    return sourceEntity.getClass().getMethod(name, setterParam);
                } catch (NoSuchMethodException ee) {
                    // no luck
                }
            }
            return null;
        }
    }

    public static Method getterForPropertyName(Object sourceEntity, String name) {
        return methodForPropertyName(sourceEntity, name);
    }

    public static Method setterForPropertyName(Object sourceEntity, String name, Class<?>... setterParam) {
        return methodForPropertyName(sourceEntity, name, setterParam);
    }

    /**
     * Checkes if the given method is marked as a dependency (using the {@link com.l7tech.objectmodel.migration.Migration}
     * annotation, or if it qualifies as a default dependency.
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
     * Checks if the given method is a dependency that can be handled by the default property resolver.
     *
     * This applies for methods that are not explicitly marked as non-dependencies and with the return type one of:
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

        if (property.isAnnotationPresent(Migration.class) && ! property.getAnnotation(Migration.class).dependency())
            return false;

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
     * Extracts the (name) mapping type from the given property method.
     */
    public static MigrationMappingSelection getMappingType(Method property) {
        if (! property.isAnnotationPresent(Migration.class)) return MigrationMappingSelection.OPTIONAL;
        return property.getAnnotation(Migration.class).mapName();
    }

    /**
     * Extracts the (name) mapping type from the given property method.
     */
    public static MigrationMappingSelection getValueMappingType(Method property) {
        if (! property.isAnnotationPresent(Migration.class)) return MigrationMappingSelection.NONE;
        return property.getAnnotation(Migration.class).mapValue();
    }

    public static ExternalEntityHeader.ValueType getValueType(Method property) {
        if (! property.isAnnotationPresent(Migration.class)) return ExternalEntityHeader.ValueType.TEXT;
        return property.getAnnotation(Migration.class).valueType();
    }

    public static String propertyNameFromGetter(String getterName) {
        return getterName == null ? null : getterName.startsWith("get") && getterName.length() > 3 ? getterName.substring(3, getterName.length()) : getterName;
    }

    public static boolean isExported(Method property) {
        return !property.isAnnotationPresent(Migration.class) || property.getAnnotation(Migration.class).export();
    }

    public static Assertion getAssertion(Policy policy, String compositePropName) throws PropertyResolverException {
        int targetOrdinal;
        try {
            String[] tokens = compositePropName.split(":");
            targetOrdinal = Integer.parseInt(tokens[1]);
        } catch (Exception e) {
            throw new PropertyResolverException("Error parsing property name: " + compositePropName, e);
        }

        Assertion rootAssertion;
        try {
            rootAssertion = policy.getAssertion();
        } catch (Exception e) {
            throw new PropertyResolverException("Error getting root assertion from policy.", e);
        }
        if (rootAssertion == null)
            throw new PropertyResolverException("Error getting root assertion from policy: policy contains no assertions");
        rootAssertion.ownerPolicyOid(policy.getOid());

        Assertion assertion = rootAssertion;
        Iterator iter = assertion.preorderIterator();
        while (iter.hasNext() && !(assertion.getOrdinal() == targetOrdinal)) {
            assertion = (Assertion) iter.next();
        }

        if (assertion != null && !(assertion.getOrdinal() == targetOrdinal))
            throw new PropertyResolverException("Assertion with ordinal " + targetOrdinal + " not found in poilcy.");

        return assertion;
    }
}
