package com.l7tech.search;

import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;

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
         NAME, GUID, VARIABLE, ENTITY_HEADER, GOID, ENTITY
    }

    /**
     * These are the different types of dependencies
     */
    public enum DependencyType {
        GENERIC(EntityType.GENERIC),
        ASSERTION(null),
        POLICY(EntityType.POLICY),
        FOLDER(EntityType.FOLDER),
        JDBC_CONNECTION(EntityType.JDBC_CONNECTION),
        SECURE_PASSWORD(EntityType.SECURE_PASSWORD),
        SERVICE(EntityType.SERVICE),
        TRUSTED_CERT(EntityType.TRUSTED_CERT),
        CLUSTER_PROPERTY(EntityType.CLUSTER_PROPERTY),
        ID_PROVIDER_CONFIG(EntityType.ID_PROVIDER_CONFIG),
        JMS_ENDPOINT(EntityType.JMS_ENDPOINT),
//        JMS_CONNECTION(EntityType.JMS_CONNECTION),
        SSG_ACTIVE_CONNECTOR(EntityType.SSG_ACTIVE_CONNECTOR),
        SSG_PRIVATE_KEY(EntityType.SSG_KEY_ENTRY),
        SSG_CONNECTOR(EntityType.SSG_CONNECTOR),
        ANY(EntityType.ANY),
        SECURITY_ZONE(EntityType.SECURITY_ZONE),
        ENCAPSULATED_ASSERTION(EntityType.ENCAPSULATED_ASSERTION),
        REVOCATION_CHECK_POLICY(EntityType.REVOCATION_CHECK_POLICY),
        POLICY_ALIAS(EntityType.POLICY_ALIAS),
        SERVICE_ALIAS(EntityType.SERVICE_ALIAS),
        USER(EntityType.USER),
        GROUP(EntityType.GROUP),
        RESOURCE_ENTRY(EntityType.RESOURCE_ENTRY),
        SITEMINDER_CONFIGURATION(EntityType.SITEMINDER_CONFIGURATION),
        CUSTOM_KEY_VALUE_STORE(EntityType.CUSTOM_KEY_VALUE_STORE),
        RBAC_ROLE(EntityType.RBAC_ROLE),
        EMAIL_LISTENER(EntityType.EMAIL_LISTENER),
        FIREWALL_RULE(EntityType.FIREWALL_RULE),
        SAMPLE_MESSAGE(EntityType.SAMPLE_MESSAGE),
        CASSANDRA_CONNECTION(EntityType.CASSANDRA_CONFIGURATION);
//        SERVICE_DOCUMENT(EntityType.SERVICE_DOCUMENT);

        private EntityType entityType;

        DependencyType(EntityType entityType) {
            this.entityType = entityType;
        }

        public EntityType getEntityType() {
            return entityType;
        }

        @NotNull
        public static DependencyType fromEntityType(@NotNull final EntityType entityType) {
            for(final DependencyType dependencyType : DependencyType.values()){
                if(entityType.equals(dependencyType.getEntityType())){
                    return dependencyType;
                }
            }
            throw new IllegalArgumentException("No known dependency type for entity type: " + entityType);
        }
    }

    /**
     * @return true if this method returns an entity or entity reference. False otherwise. The default is true
     */
    boolean isDependency() default true;

    /**
     * @return The Entity type for the entity that is returned or referenced by this method.
     */
    DependencyType type() default DependencyType.ANY;

    /**
     * @return The type of object that this method returned. This is either the entity itself or an identifier that can
     *         be used to retrieve the entity. The Default is {@link MethodReturnType#ENTITY}
     */
    MethodReturnType methodReturnType() default MethodReturnType.ENTITY;

    /**
     * This is used in the case where a method returns a map of properties where one of the properties is an entity or a
     * key for an entity. The key should be the key to retrieve the entity.
     *
     * @return The map key used to get the entity or the entity identifier.
     */
    String key() default "";

    /**
     * Set this to true to search the returned object for dependencies to add to this object. The returned object will
     * not be added as a dependency
     *
     * @return If true the method return object will be search for dependencies to add to this object. The returned
     *         object will not be considered a dependency
     */
    boolean searchObject() default false;
}
