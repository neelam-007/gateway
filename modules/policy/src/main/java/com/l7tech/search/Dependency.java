package com.l7tech.search;

import com.l7tech.objectmodel.EntityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to specify a dependency on an entity or assertion. By default all getter method that return an
 * entity object are considered to reference dependencies. In order to prevent a method from referencing a dependency
 * annotate it like this:
 * <pre><code>
 *     &#64;Dependency(isDependency = false)
 *     public Folder getFolder() { ... }
 * </code></pre>
 * <p/>
 * In order to specify that another method references a dependency annotate it like this:
 * <pre><code>
 *     &#64;Dependency(type = EntityType.JDBC_CONNECTION, methodReturnType = Dependency.MethodReturnType.NAME)
 *     public String getConnectionName() { ... }
 * </code></pre>
 *
 * @author Victor Kazakov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Dependency {
    /**
     * These are the different possible method return types for entities.
     */
    public enum MethodReturnType {
        OID, NAME, GUID, ENTITY
    }

    /**
     * @return true if this method returns an entity or entity reference. False otherwise. The default is true
     */
    boolean isDependency() default true;

    /**
     * @return The Entity type for the entity that is returned or referenced by this method.
     */
    EntityType type() default EntityType.ANY;

    /**
     * @return The type of object that this method returned. This is either the entity itself or an identifier that can
     *         be used to retrieve the entity. The Default is {@link MethodReturnType#ENTITY}
     */
    MethodReturnType methodReturnType() default MethodReturnType.ENTITY;
}
