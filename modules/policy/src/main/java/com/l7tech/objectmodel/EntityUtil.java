package com.l7tech.objectmodel;

import javax.persistence.Column;
import java.lang.reflect.AccessibleObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    public static <ET extends PersistentEntity> Map<Long, ET> buildEntityMap(Iterable<ET> entities) {
        final Map<Long, ET> map = new HashMap<Long, ET>();
        for (ET entity : entities) {
            map.put(entity.getOid(), entity);
        }
        return map;
    }

    public static class CreatedUpdatedDeleted<T extends PersistentEntity> {
        public final Map<Long, T> created;
        public final Map<Long, T> updated;
        public final Set<Long> deleted;

        public CreatedUpdatedDeleted(Map<Long, T> created, Map<Long, T> updated, Set<Long> deleted) {
            this.created = created;
            this.updated = updated;
            this.deleted = deleted;
        }
    }

    /**
     * Given two Map&lt;Long, T&gt;, finds those that have been created, updated or deleted, and returns those in a
     * Map&lt;Long, T&gt;, a Map&lt;Long, T&gt; and a Set&lt;Long&gt;, respectively.
     * 
     * @param before the previous entities
     * @param after the new entities
     * @param <T> the type of entity
     * @return a Triple consisting of the created entities, the updated entities, and the OIDs of the deleted entities.
     */
    public static <T extends PersistentEntity> CreatedUpdatedDeleted<T> findCreatedUpdatedDeleted(Map<Long, T> before, Map<Long, T> after) {
        final Map<Long, T> creates = new HashMap<Long, T>();
        final Map<Long, T> updates = new HashMap<Long, T>();
        for (Map.Entry<Long, T> entry : after.entrySet()) {
            final Long oid = entry.getKey();
            final T newb = entry.getValue();
            if (!before.containsKey(oid)) {
                creates.put(oid, newb);
            } else {
                T old = before.get(oid);
                if (old.getVersion() != newb.getVersion()) {
                    updates.put(oid, newb);
                }
            }
        }

        final Set<Long> deletes = new HashSet<Long>();
        for (Long oid : before.keySet()) {
            if (!after.containsKey(oid)) deletes.add(oid);
        }

        return new CreatedUpdatedDeleted<T>(creates, updates, deletes);
    }
}
