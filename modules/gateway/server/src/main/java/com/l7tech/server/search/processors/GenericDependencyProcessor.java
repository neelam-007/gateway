package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.policy.UsesPrivateKeys;
import com.l7tech.search.Dependencies;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.search.DependencyAnalyzerException;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.Functions;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.persistence.Transient;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.KeyStoreException;
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
public class GenericDependencyProcessor<O> extends BaseDependencyProcessor<O> {
    protected static final Logger logger = Logger.getLogger(GenericDependencyProcessor.class.getName());

    @Inject
    private EntityCrud entityCrud;

    @Inject
    private IdentityProviderConfigManager identityProviderConfigManager;

    @Inject
    private SsgKeyStoreManager keyStoreManager;

    @Inject
    DefaultKey defaultKey;

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
                            final List<Entity> dependenciesFromMethodReturnValue = getDependenciesFromMethodReturnValue(annotation, getterMethodReturn, finder);
                            if (dependenciesFromMethodReturnValue != null) {
                                dependentEntities.addAll(dependenciesFromMethodReturnValue);
                            }
                        }
                    }
                } catch (Exception e) {
                    //if an exception is thrown attempting to retrieve the dependency then log the exception but continue processing.
                    logger.log(Level.FINE, "WARNING finding dependencies - error getting dependent entity from method " + (method != null ? "using method " + method.getName() : "") + " for entity " + object.getClass(), e);
                    throw new FindException("WARNING finding dependencies - error getting dependent entity from method " + (method != null ? "using method " + method.getName() : "") + " for entity " + object.getClass());
                }
                dependencies.addAll(finder.getDependenciesFromEntities(object, finder, dependentEntities));
            }
        }

        //if the object implements UsesPrivateKeys then add the used private keys as dependencies.
        if (object instanceof UsesPrivateKeys && ((UsesPrivateKeys) object).getPrivateKeysUsed() != null) {
            for (SsgKeyHeader keyHeader : ((UsesPrivateKeys) object).getPrivateKeysUsed()) {
                final Entity keyEntry = loadEntity(keyHeader);
                if (keyEntry != null) {
                    Dependency dependency = finder.getDependency(keyEntry);
                    if (dependency != null && !dependencies.contains(dependency))
                        dependencies.add(dependency);
                }
            }
        }
        return dependencies;
    }

    private Object retrieveDependencyFromMethod(O object, Method method, com.l7tech.search.Dependency annotation) throws CannotRetrieveDependenciesException {
        //calls the getter method and retrieves the dependency.
        try {
            Object getterMethodReturn;
            if (method.getParameterTypes().length == 0) {
                getterMethodReturn = method.invoke(object);
            } else if (method.getParameterTypes().length == 1 && annotation != null && !annotation.key().isEmpty()) {
                getterMethodReturn = method.invoke(object, annotation.key());
            } else {
                throw new CannotRetrieveDependenciesException(method.getName().substring(3), annotation != null ? annotation.type().getEntityType().getEntityClass() : method.getReturnType(), object.getClass(), "Cannot retrieve dependency from method with more then one parameters.");
            }
            return getterMethodReturn;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new CannotRetrieveDependenciesException(method.getName().substring(3), annotation != null ? annotation.type().getEntityType().getEntityClass() : method.getReturnType(), object.getClass(), "Could not invoke setter method", e);
        }
    }

    private List<Entity> getDependenciesFromMethodReturnValue(com.l7tech.search.Dependency annotation, Object methodReturnValue, DependencyFinder finder) throws DependencyAnalyzerException, FindException {
        List<Entity> dependentEntities;
        if (annotation != null) {
            if (methodReturnValue instanceof Map) {
                if (annotation.key().isEmpty())
                    throw new IllegalStateException("When an entity method returns a map the map key must be specified in order to retrieve the correct search value.");
                methodReturnValue = ((Map) methodReturnValue).get(annotation.key());
            }
            //If the dependency annotation is specified us the finder to retrieve the entity.
            dependentEntities = methodReturnValue == null ? null : finder.retrieveEntities(methodReturnValue, annotation.type(), annotation.methodReturnType());
        } else {
            dependentEntities = methodReturnValue == null ? null : Arrays.asList((Entity) methodReturnValue);
        }
        return dependentEntities;
    }

    /**
     * This is a helper method to create a list of dependent entities from the given info
     *
     * @param annotation        The annotation describing the method return value
     * @param methodReturnValue The method return value to create the dependent entity from
     * @param finder            The dependency finder to use to create the entity
     * @return The list of dependent entities created from the method return value.
     */
    private List<DependentEntity> getDependentEntitiesFromMethodReturnValue(com.l7tech.search.Dependency annotation, Object methodReturnValue, DependencyFinder finder) {
        List<DependentEntity> dependentEntities;
        if (annotation != null) {
            if (methodReturnValue instanceof Map) {
                if (annotation.key().isEmpty())
                    throw new IllegalStateException("When an entity method returns a map the map key must be specified in order to retrieve the correct search value.");
                methodReturnValue = ((Map) methodReturnValue).get(annotation.key());
            }
            //If the dependency annotation is specified use the finder to retrieve the entity.
            dependentEntities = methodReturnValue == null ? null : finder.createDependentObject(methodReturnValue, annotation.type(), annotation.methodReturnType());
        } else {
            // in this case the methodReturnValue will be the dependent object
            dependentEntities = methodReturnValue == null ? null : Arrays.asList((DependentEntity) finder.createDependentObject(methodReturnValue));
        }
        return dependentEntities;
    }

    /**
     * Finds an entity given the search value and information about the search value
     *
     * @param searchValue     The search value that should uniquely identify the entity.
     * @param dependencyType  The type of dependency that is to be found
     * @param searchValueType The search value type.
     * @return The entity found
     * @throws FindException This is thrown if the entity could not be found.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<? extends Entity> find(@NotNull Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        switch (searchValueType) {
            case GOID: {
                //used the entity crud to find the entity using its GOID
                List<EntityHeader> headers;
                if (searchValue instanceof Goid)
                    headers = Arrays.asList(new EntityHeader((Goid) searchValue, dependencyType.getEntityType(), null, null));
                else if (searchValue instanceof String)
                    headers = Arrays.asList(new EntityHeader((String) searchValue, dependencyType.getEntityType(), null, null));
                else if (searchValue instanceof Goid[]) {
                    Goid[] goids = (Goid[]) searchValue;
                    headers = new ArrayList<>(goids.length);
                    for (Goid goid : goids) {
                        headers.add(new EntityHeader(goid, dependencyType.getEntityType(), null, null));
                    }
                } else
                    throw new IllegalArgumentException("Unsupported GOID value type: " + searchValue.getClass());
                //noinspection ConstantConditions
                return Functions.map(headers, new Functions.UnaryThrows<Entity, EntityHeader, FindException>() {
                    @Override
                    public Entity call(EntityHeader entityHeader) throws FindException {
                        return loadEntity(entityHeader);
                    }
                });
            }
            case ENTITY:
                if (searchValue instanceof Entity)
                    return Arrays.asList((Entity) searchValue);
                else
                    throw new IllegalStateException("Method return type is Entity but the returned object is not an entity: " + searchValue.getClass());
            case ENTITY_HEADER:
                return Arrays.asList(loadEntity((EntityHeader) searchValue));
            default:
                throw new IllegalArgumentException("Unsupported search method: " + searchValueType);
        }
    }

    @Override
    public DependentObject createDependentObject(O dependent) {
        EntityHeader entityHeader = EntityHeaderUtils.fromEntity((Entity) dependent);
        //Fix LDAP and Federated identity providers not being found properly
        if (dependent instanceof IdentityProviderConfig) {
            entityHeader.setType(EntityType.ID_PROVIDER_CONFIG);
        }
        return createDependentObject(entityHeader);
    }

    private DependentObject createDependentObject(EntityHeader entityHeader) {
        return new DependentEntity(entityHeader.getName(), entityHeader.getType(), entityHeader);
    }

    @Nullable
    @Override
    public List<DependentObject> createDependentObject(@NotNull Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) {
        switch (searchValueType) {
            case GOID: {
                //create an entity header using the giod
                List<EntityHeader> headers;
                if (searchValue instanceof Goid)
                    headers = Arrays.asList(new EntityHeader((Goid) searchValue, dependencyType.getEntityType(), null, null));
                else if (searchValue instanceof String)
                    headers = Arrays.asList(new EntityHeader((String) searchValue, dependencyType.getEntityType(), null, null));
                else if (searchValue instanceof Goid[]) {
                    Goid[] goids = (Goid[]) searchValue;
                    headers = new ArrayList<>(goids.length);
                    for (Goid goid : goids) {
                        headers.add(new EntityHeader(goid, dependencyType.getEntityType(), null, null));
                    }
                } else
                    throw new IllegalArgumentException("Unsupported GOID value type: " + searchValue.getClass());
                //convert the headers map to a list of dependent objects
                return Functions.map(headers, new Functions.Unary<DependentObject, EntityHeader>() {
                    @Override
                    public DependentObject call(EntityHeader entityHeader) {
                        return createDependentObject(entityHeader);
                    }
                });
            }
            case ENTITY:
                if (searchValue instanceof Entity)
                    //noinspection unchecked
                    return Arrays.asList(createDependentObject((O) searchValue));
                else
                    throw new IllegalStateException("Method return type is Entity but the returned object is not an entity: " + searchValue.getClass());
            case ENTITY_HEADER:
                return Arrays.asList(createDependentObject((EntityHeader) searchValue));
            default:
                throw new IllegalArgumentException("Unsupported search method: " + searchValueType);
        }
    }

    @Override
    public void replaceDependencies(@NotNull O object, @NotNull Map<EntityHeader, EntityHeader> replacementMap, DependencyFinder finder) throws CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
        //Get all the methods that this entity declares
        List<Method> entityMethods = getAllMethods(object.getClass());
        for (Method method : entityMethods) {
            //process each method that return an Entity or is annotated with @Dependency.
            if (isEntityMethod(method)) {
                Dependencies dependenciesAnnotation = method.getAnnotation(Dependencies.class);
                if (dependenciesAnnotation == null) {
                    //gets the Dependency annotation on the method if one is specified.
                    com.l7tech.search.Dependency annotation = method.getAnnotation(com.l7tech.search.Dependency.class);
                    if (annotation != null && annotation.searchObject()) {
                        Object getterMethodReturn = retrieveDependencyFromMethod(object, method, annotation);
                        //replace the dependencies in the search object
                        finder.replaceDependencies(getterMethodReturn, replacementMap);
                    } else {
                        //get the dependent entity referenced
                        Object getterMethodReturn = retrieveDependencyFromMethod(object, method, annotation);
                        List<DependentEntity> dependentEntities = getDependentEntitiesFromMethodReturnValue(annotation, getterMethodReturn, finder);
                        if (dependentEntities != null) {
                            for (DependentEntity dependentEntity : dependentEntities) {
                                //replace the dependent entity with one that is in the replacement map
                                EntityHeader header = findMappedHeader(replacementMap, dependentEntity.getEntityHeader());
                                if (header != null) {
                                    setDependencyForMethod(object, method, annotation, header, dependentEntity.getEntityHeader());
                                }
                            }
                        }
                    }
                } else {
                    for (com.l7tech.search.Dependency annotation : dependenciesAnnotation.value()) {
                        //calls the getter method and retrieves the dependency referenced
                        Object getterMethodReturn = retrieveDependencyFromMethod(object, method, annotation);
                        final List<DependentEntity> dependenciesFromMethodReturnValue = getDependentEntitiesFromMethodReturnValue(annotation, getterMethodReturn, finder);
                        if (dependenciesFromMethodReturnValue != null) {
                            for (DependentEntity dependentEntity : dependenciesFromMethodReturnValue) {
                                //replace the dependent entity with one that is in the replacement map
                                EntityHeader mappedHeader = findMappedHeader(replacementMap, dependentEntity.getEntityHeader());
                                if (mappedHeader != null) {
                                    setDependencyForMethod(object, method, annotation, mappedHeader, dependentEntity.getEntityHeader());
                                }
                            }
                        }
                    }
                }
            }
        }

        //if the object implements UsesPrivateKeys then add the used private keys as dependencies.
        if (object instanceof UsesPrivateKeys && ((UsesPrivateKeys) object).getPrivateKeysUsed() != null) {
            for (SsgKeyHeader keyHeader : ((UsesPrivateKeys) object).getPrivateKeysUsed()) {
                DependentObject dependentObject = finder.createDependentObject(keyHeader);
                EntityHeader mappedHeader = findMappedHeader(replacementMap, ((DependentEntity) dependentObject).getEntityHeader());
                if (mappedHeader != null) {
                    //TODO: need a way to set the private keys used.
                    throw new NotImplementedException();
                }
            }
        }
    }

    /**
     * This will find a mapped header in the given headers map it will first check by id, then by guid, then by name
     *
     * @param replacementMap  The map to search for a mapped header
     * @param dependentHeader The depended entity header to find a mapping for
     * @return The mapped entity header
     */
    @Nullable
    protected EntityHeader findMappedHeader(Map<EntityHeader, EntityHeader> replacementMap, final EntityHeader dependentHeader) {
        EntityHeader header = replacementMap.get(dependentHeader);

        // check by ID
        if (header == null ) {
            EntityHeader headerKey = Functions.grepFirst(replacementMap.keySet(), new Functions.Unary<Boolean, EntityHeader>() {
                @Override
                public Boolean call(EntityHeader entityHeader) {
                    return entityHeader.getType().equals(dependentHeader.getType())
                            && entityHeader.getGoid().equals(dependentHeader.getGoid());
                }
            });
            if (headerKey != null) {
                header = replacementMap.get(headerKey);
            }
        }

        // check by GUID
        if (header == null && dependentHeader instanceof GuidEntityHeader) {
            EntityHeader headerKey = Functions.grepFirst(replacementMap.keySet(), new Functions.Unary<Boolean, EntityHeader>() {
                @Override
                public Boolean call(EntityHeader entityHeader) {
                    return entityHeader instanceof GuidEntityHeader && entityHeader.getType().equals(dependentHeader.getType())
                            && StringUtils.equals(((GuidEntityHeader) entityHeader).getGuid(), ((GuidEntityHeader) dependentHeader).getGuid());
                }
            });
            if (headerKey != null) {
                header = replacementMap.get(headerKey);
            }
        }

        //check by name
        if (header == null && dependentHeader.getName()!=null) {
            EntityHeader headerKey = Functions.grepFirst(replacementMap.keySet(), new Functions.Unary<Boolean, EntityHeader>() {
                @Override
                public Boolean call(EntityHeader entityHeader) {
                    return entityHeader.getType().equals(dependentHeader.getType())
                            && StringUtils.equals(entityHeader.getName(), dependentHeader.getName());
                }
            });
            if (headerKey != null) {
                header = replacementMap.get(headerKey);
            }
        }
        return header;
    }

    /**
     * This will set the dependency for an object given the getter method
     *
     * @param object              The object to set the dependency on
     * @param getterMethod        The getter method
     * @param annotation          The annotation applied to the getter method
     * @param header              The entity header to map to.
     * @param currentEntityHeader The currently set dependency header
     * @throws CannotReplaceDependenciesException This is thrown if there is a problem replacing the dependency.
     */
    private void setDependencyForMethod(@NotNull O object, @NotNull Method getterMethod, @Nullable com.l7tech.search.Dependency annotation, @NotNull EntityHeader header, EntityHeader currentEntityHeader) throws CannotReplaceDependenciesException {
        if (annotation != null) {
            //finds the setter method
            final Method setterMethod = findMethod(object.getClass(), "set" + getterMethod.getName().substring(3), getterMethod.getReturnType());
            if (setterMethod == null) {
                throw new CannotReplaceDependenciesException(getterMethod.getName().substring(3), header.getStrId(), annotation.type().getEntityType().getEntityClass(), object.getClass(), "Cannot find setter method.");
            }
            try {
                switch (annotation.methodReturnType()) {
                    case GOID:
                        //Check if the goids are different. There is no point in replacing if the goids are the same
                        if (!header.getGoid().equals(currentEntityHeader.getGoid())) {
                            setterMethod.invoke(object, header.getGoid());
                        }
                        break;
                    case NAME:
                        //Check if the names are different. There is no point in replacing if the names are the same
                        if (!StringUtils.equals(header.getName(), currentEntityHeader.getName())) {
                            setterMethod.invoke(object, header.getName());
                        }
                        break;
                    case VARIABLE:
                        //Check if the names are different. There is no point in replacing if the names are the same
                        if (!StringUtils.equals(header.getName(), currentEntityHeader.getName())) {
                            throw new CannotReplaceDependenciesException(getterMethod.getName().substring(3), header.getStrId(), annotation.type().getEntityType().getEntityClass(), object.getClass(), "Replacing variable dependencies is not supported.");
                        }
                        break;
                    default:
                        throw new CannotReplaceDependenciesException(getterMethod.getName().substring(3), header.getStrId(), annotation.type().getEntityType().getEntityClass(), object.getClass(), "Unsupported dependency type: " + annotation.methodReturnType());
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new CannotReplaceDependenciesException(getterMethod.getName().substring(3), header.getStrId(), annotation.type().getEntityType().getEntityClass(), object.getClass(), "Could not invoke setter method", e);
            }
        } else {
            final Entity replacement;
            try {
                replacement = loadEntity(header);
            } catch (FindException e) {
                throw new CannotReplaceDependenciesException(getterMethod.getName().substring(3), header.getStrId(), header.getType().getEntityClass(), object.getClass(), "Could not find entity: " + header.toString(), e);
            }
            //find setting method
            final Method setterMethod = findMethod(object.getClass(), "set" + getterMethod.getName().substring(3), replacement.getClass());
            if (setterMethod == null) {
                throw new CannotReplaceDependenciesException(getterMethod.getName().substring(3), header.getStrId(), header.getType().getEntityClass(), object.getClass(), "Cannot find setter method.");
            }
            try {
                setterMethod.invoke(object, replacement);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new CannotReplaceDependenciesException(getterMethod.getName().substring(3), header.getStrId(), header.getType().getEntityClass(), object.getClass(), "Could not invoke setter method", e);
            }
        }
    }

    /**
     * Finds a method given a class method name and parameter type
     *
     * @param aClass        The class to search
     * @param methodName    The method name
     * @param parameterType The parameter type that the method can take
     * @return The method. Returns null if it cannot find a method.
     */
    @Nullable
    private Method findMethod(@NotNull Class<?> aClass, @NotNull final String methodName, @NotNull final Class<?> parameterType) {
        return Functions.grepFirst(Arrays.asList(aClass.getMethods()), new Functions.Unary<Boolean, Method>() {
            @Override
            public Boolean call(Method method) {
                return methodName.equals(method.getName()) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(parameterType);
            }
        });
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
        } else if (method.getAnnotation(Transient.class) == null) {
            return method.getName().startsWith("get") && Entity.class.isAssignableFrom(method.getReturnType()) && Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 0;
        }
        return false;
    }

    /**
     * Gets an entity given an entity header. This is needed to handle the special case where the entityCrud down-casts
     * identity provider entities. This will also handle SsgKeyEntry types, retireving the default type if needed.
     *
     * @param entityHeader The entity header for the entity to return
     * @return The entity.
     * @throws FindException This is thrown if the entity cannot be found
     */
    protected Entity loadEntity(EntityHeader entityHeader) throws FindException {
        if (EntityType.ID_PROVIDER_CONFIG.equals(entityHeader.getType())) {
            return identityProviderConfigManager.findByHeader(entityHeader);
        } else if (EntityType.SSG_KEY_ENTRY.equals(entityHeader.getType())) {
            final SsgKeyHeader keyHeader = (SsgKeyHeader) entityHeader;
            final SsgKeyEntry keyEntry;
            try {
                if (Goid.isDefault(keyHeader.getKeystoreId())) {
                    if (keyHeader.getAlias() == null) {
                        //if the keystore id is -1 and the alias is 0 then use the default key.
                        try {
                            keyEntry = defaultKey.getSslInfo();
                        } catch (IOException e) {
                            throw new FindException("Could got det Default ssl Key", e);
                        }
                    } else {
                        //the keystore is -1 but an alias is specified. Then find the key using the keyStoreManager
                        keyEntry = keyStoreManager.lookupKeyByKeyAlias(keyHeader.getAlias(), keyHeader.getKeystoreId());
                    }
                } else {
                    //This is when both the keyStore and alias as specified.
                    final SsgKeyFinder ssgKeyFinder = keyStoreManager.findByPrimaryKey(keyHeader.getKeystoreId());
                    keyEntry = ssgKeyFinder.getCertificateChain(keyHeader.getAlias());
                }
            } catch (KeyStoreException e) {
                throw new FindException("Exception finding SsgKeyEntry", e);
            }
            return keyEntry;
        } else
            return entityCrud.find(entityHeader);
    }
}
