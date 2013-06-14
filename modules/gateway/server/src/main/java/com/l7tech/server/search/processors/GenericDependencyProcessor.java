package com.l7tech.server.search.processors;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.search.Dependencies;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.DependencyAnalyzerException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is used to find dependencies for a generic object. Dependencies are found by looking at the methods that the
 * object defines. If the methods return an Entity object then this is considered to be a dependency. Methods can also
 * be annotated with  {@link com.l7tech.search.Dependency} in order to find dependencies in other ways or to exclude
 * methods
 *
 * @author Victor Kazakov
 */
public class GenericDependencyProcessor<O> implements DependencyProcessor<O> {
    protected static final Logger logger = Logger.getLogger(GenericDependencyProcessor.class.getName());

    @Inject
    private EntityCrud entityCrud;

    @Inject
    private IdentityProviderConfigManager identityProviderConfigManager;

    @Override
    @NotNull
    public List<Dependency> findDependencies(O object, DependencyFinder finder) throws FindException {
        ArrayList<Dependency> dependencies = new ArrayList<>();
        //Get all the methods that this entity declares
        List<Method> entityMethods = getAllMethods(object.getClass());
        for (Method method : entityMethods) {
            //process each method that return an Entity or is annotated with @Dependency.
            if (isEntityMethod(method)) {
                List<Entity> dependentEntities = null;
                try {
                    Dependencies dependenciesAnnotation = method.getAnnotation(Dependencies.class);
                    if (dependenciesAnnotation == null) {
                        //gets the Dependency annotation on the method if one is specified.
                        com.l7tech.search.Dependency annotation = method.getAnnotation(com.l7tech.search.Dependency.class);
                        if (annotation != null && annotation.searchObject()) {
                            Object getterMethodReturn = retrieveDependencyFromMethod(object, method, annotation);
                            dependencies.addAll(finder.getDependencies(getterMethodReturn));
                        } else {
                            Object getterMethodReturn = retrieveDependencyFromMethod(object, method, annotation);
                            dependentEntities = getDependenciesFromMethodReturnValue(annotation, getterMethodReturn, finder);
                        }
                    } else {
                        dependentEntities = new ArrayList<>();
                        for (com.l7tech.search.Dependency annotation : dependenciesAnnotation.value()) {
                            //calls the getter method and retrieves the dependency.
                            Object getterMethodReturn = retrieveDependencyFromMethod(object, method, annotation);
                            dependentEntities.addAll(getDependenciesFromMethodReturnValue(annotation, getterMethodReturn, finder));
                        }
                    }
                } catch (Exception e) {
                    //if an exception is thrown attempting to retrieve the dependency then log the exception but continue processing.
                    logger.log(Level.FINE, "WARNING finding dependencies - error getting dependent entity from method " + (method != null ? "using method " + method.getName() : "") + " for entity " + object.getClass(), e);
                    throw new FindException("WARNING finding dependencies - error getting dependent entity from method " + (method != null ? "using method " + method.getName() : "") + " for entity " + object.getClass());
                }
                if (dependentEntities != null) {
                    //if a dependency if found then search for its dependencies and add it to the set of dependencies found
                    for (Entity entity : dependentEntities) {
                        if (entity != null) {
                            //Making sure an entity does not depend on itself
                            if (!object.equals(entity)) {
                                final Dependency dependency = finder.getDependency(entity);
                                dependencies.add(dependency);
                            }
                        }
                    }
                }
            }
        }
        return dependencies;
    }

    private Object retrieveDependencyFromMethod(O object, Method method, com.l7tech.search.Dependency annotation) throws IllegalAccessException, InvocationTargetException {
        //calls the getter method and retrieves the dependency.
        Object getterMethodReturn;
        if (method.getParameterTypes().length == 0) {
            getterMethodReturn = method.invoke(object);
        } else if (method.getParameterTypes().length == 1 && annotation != null && !annotation.key().isEmpty()) {
            getterMethodReturn = method.invoke(object, annotation.key());
        } else {
            throw new IllegalArgumentException("Cannot retrieve dependency from method with more then one parameters.");
        }
        return getterMethodReturn;
    }

    private List<Entity> getDependenciesFromMethodReturnValue(com.l7tech.search.Dependency annotation, Object methodReturnValue, DependencyFinder finder) throws DependencyAnalyzerException, FindException {
        List<Entity> dependentEntities;
        if (annotation != null && !annotation.methodReturnType().equals(com.l7tech.search.Dependency.MethodReturnType.ENTITY)) {
            if (methodReturnValue instanceof Map) {
                if (annotation.key().isEmpty())
                    throw new IllegalStateException("When an entity method returns a map the map key must be specified in order to retrieve the correct search value.");
                methodReturnValue = ((Map) methodReturnValue).get(annotation.key());
            }
            //If the dependency annotation is specified us the finder to retrieve the entity.
            dependentEntities = finder.retrieveEntities(methodReturnValue, annotation);
        } else {
            dependentEntities = methodReturnValue == null ? null : Arrays.asList((Entity) methodReturnValue);
        }
        return dependentEntities;
    }

    /**
     * Finds an entity given the search value and the dependency annotation info
     *
     * @param searchValue The search value that should uniquely identify the entity.
     * @param dependency  The dependency info that describes this dependency and search value
     * @return The entity found
     * @throws FindException This is thrown if the entity could not be found.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<? extends Entity> find(@NotNull Object searchValue, com.l7tech.search.Dependency dependency) throws FindException {
        switch (dependency.methodReturnType()) {
            case OID:
                //used the entity crud to find the entity using its OID
                List<EntityHeader> headers;
                if (searchValue instanceof Long)
                    headers = Arrays.asList(new EntityHeader((Long) searchValue, dependency.type().getEntityType(), null, null));
                else if (searchValue instanceof String)
                    headers = Arrays.asList(new EntityHeader((String) searchValue, dependency.type().getEntityType(), null, null));
                else if (searchValue instanceof long[]) {
                    long[] oids = (long[]) searchValue;
                    headers = new ArrayList<>(oids.length);
                    for (long oid : oids) {
                        headers.add(new EntityHeader(oid, dependency.type().getEntityType(), null, null));
                    }
                } else
                    throw new IllegalArgumentException("Unsupported OID value type: " + searchValue.getClass());
                //noinspection ConstantConditions
                return Functions.map(headers, new Functions.UnaryThrows<Entity, EntityHeader, FindException>() {
                    @Override
                    public Entity call(EntityHeader entityHeader) throws FindException {
                        return loadEntity(entityHeader);
                    }
                });
            case ENTITY:
                if (searchValue instanceof Entity)
                    return Arrays.asList((Entity) searchValue);
                else
                    throw new IllegalStateException("Method return type is Entity but the returned object is not an entity: " + searchValue.getClass());
            default:
                throw new IllegalArgumentException("Unsupported search method: " + dependency.methodReturnType());
        }
    }

    @Override
    public DependentObject createDependentObject(O dependent) {
        EntityHeader entityHeader = EntityHeaderUtils.fromEntity((Entity) dependent);
        //Fix LDAP and Federated identity providers not being found properly
        if (dependent instanceof IdentityProviderConfig) {
            entityHeader.setType(EntityType.ID_PROVIDER_CONFIG);
        }
        return new DependentEntity(entityHeader.getName(), entityHeader.getType(), entityHeader instanceof GuidEntityHeader ? ((GuidEntityHeader) entityHeader).getGuid() : null, entityHeader.getStrId());
    }

    /**
     * Returns all the fields for the given class, including the fields defined in it's superclasses
     *
     * @param entityClass The class to get the fields for
     * @return The fields defined in this class and its superclasses.
     */
    private static List<Method> getAllMethods(Class entityClass) {
        //Gets the classes defined fields
        ArrayList<Method> fields = new ArrayList<>(Arrays.asList(entityClass.getDeclaredMethods()));
        if (entityClass.getSuperclass() != null) {
            //if this class has a superclass then also retrieve the fields defined in the superclass.
            fields.addAll(getAllMethods(entityClass.getSuperclass()));
        }
        return fields;
    }

    /**
     * Finds if this field is usable as an entity field. Currently this always returns true unless the field has the
     *
     * @param method The field to check.
     * @return True if the field should be used to to find a dependency. False otherwise.
     */
    private static boolean isEntityMethod(Method method) {
        com.l7tech.search.Dependency annotation = method.getAnnotation(com.l7tech.search.Dependency.class);
        Dependencies dependenciesAnnotation = method.getAnnotation(Dependencies.class);
        if (annotation != null) {
            return annotation.isDependency();
        } else if (dependenciesAnnotation != null) {
            return dependenciesAnnotation.value().length > 0;
        } else {
            return method.getName().startsWith("get") && Entity.class.isAssignableFrom(method.getReturnType()) && Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 0;
        }
    }

    /**
     * Gets an entity given an entity header. This is needed to handle the special case where the entityCrud down-casts
     * identity provider entities
     *
     * @param entityHeader The entity header for the entity to return
     * @return The entity.
     * @throws FindException This is thrown if the entity cannot be found
     */
    protected Entity loadEntity(EntityHeader entityHeader) throws FindException {
        if (EntityType.ID_PROVIDER_CONFIG.equals(entityHeader.getType())) {
            return identityProviderConfigManager.findByHeader(entityHeader);
        } else
            return entityCrud.find(entityHeader);
    }
}
