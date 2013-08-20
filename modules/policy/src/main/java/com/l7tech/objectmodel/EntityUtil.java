package com.l7tech.objectmodel;

import com.l7tech.util.Functions.Unary;

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
    public static int getMaxFieldLength(Class<?> entityClass, String propertyName, int defaultValue) {
        Integer len = getLengthFromProperty(entityClass, propertyName);
        if (len == null) len = getLengthFromGetter(entityClass, propertyName);
        return len == null ? defaultValue : len;
    }

    private static Integer getLengthFromGetter(Class<?> entityClass, String propertyName) {
        try {
            char[] nameChars = propertyName.toCharArray();
            nameChars[0] = Character.toUpperCase(nameChars[0]);
            String getterName = new StringBuilder(propertyName.length() + 3).append("get").append(nameChars).toString();
            return getLengthFromColumnAnnotation(entityClass.getMethod(getterName));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Integer getLengthFromProperty(Class<?> entityClass, String propertyName) {
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

    public static <ET extends GoidEntity> Map<Goid, ET> buildEntityMap(Iterable<ET> entities) {
        final Map<Goid, ET> map = new HashMap<Goid, ET>();
        for (ET entity : entities) {
            map.put(entity.getGoid(), entity);
        }
        return map;
    }

    public static class CreatedUpdatedDeleted<T extends GoidEntity> {
        public final Map<Goid, T> created;
        public final Map<Goid, T> updated;
        public final Set<Goid> deleted;

        public CreatedUpdatedDeleted(Map<Goid, T> created, Map<Goid, T> updated, Set<Goid> deleted) {
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
    public static <T extends GoidEntity> CreatedUpdatedDeleted<T> findCreatedUpdatedDeleted(Map<Goid, T> before, Map<Goid, T> after) {
        final Map<Goid, T> creates = new HashMap<Goid, T>();
        final Map<Goid, T> updates = new HashMap<Goid, T>();
        for (Map.Entry<Goid, T> entry : after.entrySet()) {
            final Goid goid = entry.getKey();
            final T newb = entry.getValue();
            if (!before.containsKey(goid)) {
                creates.put(goid, newb);
            } else {
                T old = before.get(goid);
                if (old.getVersion() != newb.getVersion()) {
                    updates.put(goid, newb);
                }
            }
        }

        final Set<Goid> deletes = new HashSet<Goid>();
        for (Goid goid : before.keySet()) {
            if (!after.containsKey(goid)) deletes.add(goid);
        }

        return new CreatedUpdatedDeleted<T>(creates, updates, deletes);
    }

    /**
     * First class function for identifier access.
     *
     * @return A function to access the identifier of an entity
     */
    public static Unary<String,Entity> id() {
        return new Unary<String,Entity>(){
            @Override
            public String call( final Entity entity ) {
                return entity == null ?  null : entity.getId();
            }
        };
    }

    /**
     * First class function for name access.
     *
     * @return A function to access the name of a named entity
     */
    public static Unary<String,NamedEntity> name() {
        return new Unary<String,NamedEntity>(){
            @Override
            public String call( final NamedEntity entity ) {
                return entity == null ?  null : entity.getName();
            }
        };
    }
}
