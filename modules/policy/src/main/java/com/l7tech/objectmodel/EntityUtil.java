package com.l7tech.objectmodel;

import javax.persistence.Column;
import java.lang.reflect.AccessibleObject;

/**
 * Utility methods for working with entities.
 */
public class EntityUtil {

    /**
     * Attempt to read the declared maximum field length for a String property of an entity.
     * This will currently recognize the "length" property of the @Column annotation.
     * If no declared maximum field length can be obtained, the specified default value will
     * be returned instead.
     *
     * @param entityClass   the concrete class of the Entity whose field to examine.  Required.
     * @param propertyName  the name of the property whose annotations to search.  For example, "name" will check
     *                      for annotations on the "name" member field, and failing that, on the "getName" method.
     * @param defaultValue  a value to return if no relevant annotation could be found.
     * @return the maximum length of the specified property in characters.
     */
    public static int getMaxFieldLength(Class<? extends Entity> entityClass, String propertyName, int defaultValue) {
        Integer len = getLengthFromProperty(entityClass, propertyName);
        if (len == null) len = getLengthFromGetter(entityClass, propertyName);
        return len == null ? defaultValue : len;
    }

    private static Integer getLengthFromGetter(Class<? extends Entity> entityClass, String propertyName) {
        try {
            char[] nameChars = propertyName.toCharArray();
            nameChars[0] = Character.toUpperCase(nameChars[0]);
            String getterName = new StringBuilder(propertyName.length() + 3).append("get").append(nameChars).toString();
            return getLengthFromColumnAnnotation(entityClass.getMethod(getterName));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Integer getLengthFromProperty(Class<? extends Entity> entityClass, String propertyName) {
        try {
            return getLengthFromColumnAnnotation(entityClass.getField(propertyName));
        } catch (NoSuchFieldException e) {
            try {
                return getLengthFromColumnAnnotation(entityClass.getDeclaredField(propertyName));
            } catch (NoSuchFieldException e1) {
                return null;
            }
        }
    }

    private static Integer getLengthFromColumnAnnotation(AccessibleObject field) {
        Column column = field.getAnnotation(Column.class);
        if (column == null) return null;
        return column.length();
    }
}
