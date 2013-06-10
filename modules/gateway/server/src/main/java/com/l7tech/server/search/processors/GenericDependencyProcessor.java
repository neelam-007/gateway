package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @Override
    @NotNull
    public List<Dependency> findDependencies(O object, DependencyFinder finder) throws FindException {
        ArrayList<Dependency> dependencies = new ArrayList<>();
        //Get all the methods that this entity declares
        List<Method> entityMethods = getAllMethods(object.getClass());
        for (Method method : entityMethods) {
            //process each method that return an Entity or is annotated with @Dependency.
            if (isEntityMethod(method)) {
                Entity dependentEntity = null;
                try {
                    //calls the getter method and retrieves the dependency.
                    Object getterMethodReturn = method.invoke(object);
                    //gets the Dependency annotation on the method if one is specified.
                    com.l7tech.search.Dependency annotation = method.getAnnotation(com.l7tech.search.Dependency.class);
                    if (annotation != null && !annotation.methodReturnType().equals(com.l7tech.search.Dependency.MethodReturnType.ENTITY)) {
                        //If the dependency annotation is specified us the finder to retrieve the entity.
                        dependentEntity = finder.retrieveEntity(getterMethodReturn, annotation);
                    } else {
                        dependentEntity = (Entity) getterMethodReturn;
                    }
                } catch (Exception e) {
                    //if an exception is thrown attempting to retrieve the dependency then log the exception but continue processing.
                    logger.log(Level.FINE, "WARNING finding dependencies - error getting dependent entity from method " + (method != null ? "using method " + method.getName() : "") + " for entity " + object.getClass(), e);
                }
                if (dependentEntity != null) {
                    //if a dependency if found then search for its dependencies and add it to the set of dependencies found
                    final Dependency dependency = finder.getDependencyHelper(dependentEntity);
                    dependencies.add(dependency);
                }
            }
        }
        return dependencies;
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
    public Entity find(@NotNull Object searchValue, com.l7tech.search.Dependency dependency) throws FindException {
        switch (dependency.methodReturnType()) {
            case OID:
                //used the entity crud to find the entity using its OID
                EntityHeader header;
                if (searchValue instanceof Long)
                    header = new EntityHeader((Long) searchValue, dependency.type().getEntityType(), null, null);
                else if (searchValue instanceof String)
                    header = new EntityHeader((String) searchValue, dependency.type().getEntityType(), null, null);
                else
                    throw new IllegalArgumentException("Unsupported OID value type: " + searchValue.getClass());
                //noinspection ConstantConditions
                return entityCrud.find(header);
            case ENTITY:
                if (searchValue instanceof Entity)
                    return (Entity) searchValue;
                else
                    throw new IllegalStateException("Method return type is Entity but the returned object is not an entity: " + searchValue.getClass());
            default:
                throw new IllegalArgumentException("Unsupported search method: " + dependency.methodReturnType());
        }
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
        if (annotation != null) {
            return annotation.isDependency();
        } else {
            return method.getName().startsWith("get") && Entity.class.isAssignableFrom(method.getReturnType());
        }
    }
}
