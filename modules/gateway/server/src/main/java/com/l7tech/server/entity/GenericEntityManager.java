package com.l7tech.server.entity;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * A manager for GenericEntity entities, whose concrete types may not be known at compile time.
 * <p/>
 * Users should not use this manager directly except to call {@link #getEntityManager(Class)}
 * to get a more-specific entity manager for concrete generic entity class they are working with.
 */
public interface GenericEntityManager extends EntityManager<GenericEntity, GenericEntityHeader> {
    /**
     * Register a generic entity class with the generic entity manager subsystem.
     * <p/>
     * This method would typically be called on startup by a modular assertion or other extension in order
     * to mark the specified extension class as safe to use, and to register an instance of the class
     * loaded from the appropriate ClassLoader.
     *
     * @param entityClass a generic entity subclass to allow.
     * @throws IllegalArgumentException if the specified entity class is not a subclass of GenericEntity or if its classname is already registered
     */
    <ET extends GenericEntity>
    void registerClass(@NotNull Class<ET> entityClass) throws IllegalArgumentException;

    /**
     * Register a generic entity class with the generic entity manager subsystem, optionally including additional
     * metadata for the generic entity registration.
     * <p/>
     * This method would typically be called on startup by a modular assertion or other extension in order
     * to mark the specified extension class as safe to use, and to register an instance of the class
     * loaded from the appropriate ClassLoader.
     * <p/>
     * Metadata can be specified in case this generic entity has additional requirements such as storing
     * instances of third-party classes within its properties that cannot be annotated as @XmlSafe.
     *
     * @param entityClass a generic entity subclass to allow.
     * @param metadata additional metadata for this generic entity registration, or null.
     * @throws IllegalArgumentException if the specified entity class is not a subclass of GenericEntity or if its classname is already registered
     */
    <ET extends GenericEntity>
    void registerClass(@NotNull Class<ET> entityClass, @Nullable GenericEntityMetadata metadata) throws IllegalArgumentException;

    /**
     * Unregister the specified generic entity class.
     *
     * @param entityClassName name of the class to unregister.  Required.
     * @return true iff. a registered class with this name existed and was unregistered.
     */
    boolean unRegisterClass(String entityClassName);

    /**
     * Check if the generic entity class name is registered.
     *
     * @param entityClassName name of the class to check if registered. Required.
     * @return true iff. a registered class with this name was already registered.
     */
    boolean isRegistered(String entityClassName);

    /**
     * Get a concrete entity manager.
     *
     * @param entityClass the concrete generic entity class.  Required.
     * @param <ET> the concrete entity type
     * @return an EntityManager that provides access to the specified concrete entity type.
     */
    <ET extends GenericEntity>
    EntityManager<ET, GenericEntityHeader> getEntityManager(@NotNull Class<ET> entityClass);

    <ET extends GenericEntity>
    ET findByGenericClassAndPrimaryKey(@NotNull Class<ET> entityClass, Goid goid) throws FindException;

    <ET extends GenericEntity>
    Collection<ET> findAll(Class<ET> entityClass) throws FindException;

    Collection<GenericEntityHeader> findAllHeaders(@NotNull Class<? extends GenericEntity> entityClass) throws FindException;

    Collection<GenericEntityHeader> findAllHeaders(@NotNull Class<? extends GenericEntity> entityClass, int offset, int windowSize) throws FindException;

    <ET extends GenericEntity>
    Goid save(@NotNull Class<ET> entityClass, ET entity) throws SaveException;

    <ET extends GenericEntity>
    void save(@NotNull Goid id, @NotNull Class<ET> entityClass, ET entity) throws SaveException;

    <ET extends GenericEntity>
    Integer getVersion(@NotNull Class<ET> entityClass, Goid goid) throws FindException;

    <ET extends GenericEntity>
    Map<Goid,Integer> findVersionMap(@NotNull Class<ET> entityClass) throws FindException;

    <ET extends GenericEntity>
    void delete(@NotNull Class<ET> entityClass, ET entity) throws DeleteException;

    <ET extends GenericEntity>
    ET getCachedEntity(@NotNull Class<ET> entityClass, Goid goid, int maxAge) throws FindException;

    <ET extends GenericEntity>
    ET findByUniqueName(@NotNull Class<ET> entityClass, String name) throws FindException;

    GenericEntity findByUniqueName(@NotNull String entityClass, String name) throws FindException;

    <ET extends GenericEntity>
    void delete(@NotNull Class<ET> entityClass, Goid goid) throws DeleteException, FindException;

    <ET extends GenericEntity>
    void update(@NotNull Class<ET> entityClass, ET entity) throws UpdateException;

    <ET extends GenericEntity>
    ET findByHeader(@NotNull Class<ET> entityClass, EntityHeader header) throws FindException;
}
