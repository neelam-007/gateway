package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.UsesPrivateKeys;
import com.l7tech.search.Dependencies;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.persistence.Transient;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.security.KeyStoreException;
import java.util.*;
import java.util.logging.Logger;

/**
 * This is used to find dependencies for an object in the gateway. Dependencies are found by looking at the methods that
 * the object defines. If the methods return an Entity object then this is considered to be a dependency. Methods can
 * also be annotated with  {@link com.l7tech.search.Dependency} or {@link com.l7tech.search.Dependencies} in order to
 * find dependencies in other ways or to exclude methods.
 */
public class DefaultDependencyProcessor<O> extends BaseDependencyProcessor<O> {
    protected static final Logger logger = Logger.getLogger(DefaultDependencyProcessor.class.getName());

    @Inject
    private EntityCrud entityCrud;

    @Inject
    private IdentityProviderConfigManager identityProviderConfigManager;

    @Inject
    private SsgKeyStoreManager keyStoreManager;

    @Inject
    private CustomKeyValueStoreManager customKeyValueStoreManager;

    @Inject
    private TrustedCertManager trustedCertManager;

    @Inject
    private DefaultKey defaultKey;

    @Override
    @NotNull
    public List<Dependency> findDependencies(@NotNull final O object, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        final ArrayList<Dependency> dependencies = new ArrayList<>();

        processObjectDependencyMethods(object,
                new ProcessMethodDependencyReturn<FindException>() {
                    @Override
                    public void process(@NotNull final Object methodReturnValue, @NotNull final Method method, @Nullable final com.l7tech.search.Dependency annotation) throws CannotRetrieveDependenciesException, FindException {
                        //process the method return value and add the dependencies found to the list of dependencies.
                        dependencies.addAll(finder.getDependenciesFromObjects(object, finder, getDependenciesFromMethodReturnValue(annotation, methodReturnValue, finder)));
                    }
                }, new ProcessSsgKeyHeader<FindException>() {
                    @Override
                    public void process(@NotNull final UsesPrivateKeys usesPrivateKeys, @NotNull final SsgKeyHeader ssgKeyHeader) throws CannotRetrieveDependenciesException, FindException {
                        //find and process ssg key store dependencies.
                        final Entity keyEntry = loadEntity(ssgKeyHeader);
                        final Dependency dependency = finder.getDependency(keyEntry);
                        if (dependency != null && !dependencies.contains(dependency)) {
                            dependencies.add(dependency);
                        }
                    }
                });

        return dependencies;
    }

    /**
     * This will call the given method with the correct parameters according to the @Dependency annotation and returns
     * the method return value.
     *
     * @param object     The instance object to call this method on.
     * @param method     The method to call
     * @param annotation The @Dependency annotation describing how to call this method.
     * @return The method return value.
     * @throws CannotRetrieveDependenciesException This is thrown if there was an error calling the method.
     */
    @Nullable
    private static Object retrieveDependencyFromMethod(@NotNull final Object object, @NotNull final Method method, @Nullable final com.l7tech.search.Dependency annotation) throws CannotRetrieveDependenciesException {
        //calls the getter method and retrieves the dependency.
        try {
            final Object getterMethodReturn;
            if (method.getParameterTypes().length == 0) {
                //if the method takes not parameters just call the method.
                getterMethodReturn = method.invoke(object);
            } else if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(String.class) && annotation != null && !annotation.key().isEmpty()) {
                //the method takes a single string parameter and the @Dependency annotation is set with a specified key. So call the method with the specified key.
                getterMethodReturn = method.invoke(object, annotation.key());
            } else {
                //We don't know how to retrieve dependencies for this method.
                throw new CannotRetrieveDependenciesException(method.getName().substring(3), annotation != null ? annotation.type().getEntityType().getEntityClass() : method.getReturnType(), object.getClass(), "Cannot retrieve dependency from method with more then one parameters.");
            }
            return getterMethodReturn;
        } catch (InvocationTargetException | IllegalAccessException e) {
            //There was some error calling this method.
            throw new CannotRetrieveDependenciesException(method.getName().substring(3), annotation != null ? annotation.type().getEntityType().getEntityClass() : method.getReturnType(), object.getClass(), "Could not invoke getter method", e);
        }
    }

    /**
     * Returns a list of object dependencies given a dependency method return value. If the return value is a map and
     * the @Dependency annotation is specified then the appropriate value from the map is extracted.
     *
     * @param annotation        The @Dependency annotation describing how to handle the method return value.
     * @param methodReturnValue The method return value to reteive the dependency from.
     * @param finder            The dependency finder to use.
     * @return The list of dependencies that are represented by this method return value.
     * @throws FindException                       This is thrown if an entity could not be found.
     * @throws CannotRetrieveDependenciesException This is thrown if the {@link com.l7tech.search.Dependency} annotation
     *                                             is not properly used.
     */
    @NotNull
    private static List<Object> getDependenciesFromMethodReturnValue(@Nullable final com.l7tech.search.Dependency annotation, @NotNull final Object methodReturnValue, @NotNull final DependencyFinder finder) throws CannotRetrieveDependenciesException, FindException {
        if (annotation != null) {
            final Object returnObject = processMethodReturnValue(annotation, methodReturnValue);
            //If the dependency annotation is specified use the finder to retrieve the entity.
            return returnObject == null ? Collections.emptyList() : finder.retrieveObjects(returnObject, annotation.type(), annotation.methodReturnType());
        }
        return Arrays.asList(methodReturnValue);
    }

    /**
     * This is a helper method to create a list of dependent entities from the given info
     *
     * @param annotation        The annotation describing the method return value
     * @param methodReturnValue The method return value to create the dependent entity from
     * @param finder            The dependency finder to use to create the entity
     * @return The list of dependent entities created from the method return value.
     * @throws CannotRetrieveDependenciesException This is thrown if the {@link com.l7tech.search.Dependency} annotation
     *                                             is not properly used.
     */
    @NotNull
    private static List<DependentObject> getDependentEntitiesFromMethodReturnValue(@Nullable final com.l7tech.search.Dependency annotation, @NotNull final Object methodReturnValue, @NotNull final DependencyFinder finder) throws CannotRetrieveDependenciesException {
        if (annotation != null) {
            final Object returnObject = processMethodReturnValue(annotation, methodReturnValue);
            //If the dependency annotation is specified use the finder to retrieve the entity.
            return returnObject == null ? Collections.<DependentObject>emptyList() : finder.createDependentObject(returnObject, annotation.type(), annotation.methodReturnType());
        }
        // in this case the methodReturnValue will be the dependent object
        return Arrays.asList(finder.createDependentObject(methodReturnValue));
    }

    /**
     * This will process a method return value. If it is a map and an @Dependency annotation is specified then the
     * correct value from the map is returned, otherwise the methodReturnValue is returned.
     *
     * @param annotation        The annotation describing the method return value
     * @param methodReturnValue The method return value to process
     * @return The method return value or a value extracted from a map.
     * @throws CannotRetrieveDependenciesException This is thrown if the {@link com.l7tech.search.Dependency} annotation
     *                                             is not properly used.
     */
    @Nullable
    private static Object processMethodReturnValue(@NotNull final com.l7tech.search.Dependency annotation, @NotNull final Object methodReturnValue) throws CannotRetrieveDependenciesException {
        if (methodReturnValue instanceof Map) {
            if (annotation.key().isEmpty()) {
                throw new CannotRetrieveDependenciesException(annotation.methodReturnType().name(), annotation.type().getEntityType().getEntityClass(), "When an entity method returns a map the map key must be specified in order to retrieve the correct search value.");
            }
            return ((Map) methodReturnValue).get(annotation.key());
        } else {
            return methodReturnValue;
        }
    }

    /**
     * Finds a entities given the search value and information about the search value
     *
     * @param searchValue     The search value that should uniquely identify the entity.
     * @param dependencyType  The type of dependency that is to be found
     * @param searchValueType The search value type.
     * @return The entities found
     * @throws FindException This is thrown if the entity could not be found.
     */
    @NotNull
    @Override
    public List<O> find(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        switch (searchValueType) {
            case GOID: {
                final List<EntityHeader> headers;
                try {
                    headers = getEntityHeaders(searchValue, dependencyType.getEntityType());
                } catch (CannotRetrieveDependenciesException e) {
                    throw new FindException("Cannot find " + dependencyType.getEntityType(), e);
                }
                return Functions.map(headers, new Functions.UnaryThrows<O, EntityHeader, FindException>() {
                    @Override
                    public O call(final EntityHeader entityHeader) throws FindException {
                        try {
                            //noinspection unchecked
                            return (O) loadEntity(entityHeader);
                        } catch (ClassCastException e) {
                            throw new FindException("Cannot find " + dependencyType.getEntityType() + ". Entity cannot be cast to correct type: " + searchValue.getClass(), e);
                        }
                    }
                });
            }
            case ENTITY:
                if (searchValue instanceof Entity) {
                    try {
                        return Arrays.asList((O) searchValue);
                    } catch (ClassCastException e) {
                        throw new FindException("Cannot find " + dependencyType.getEntityType() + ". Method return type cannot be cast to correct type: " + searchValue.getClass(), e);
                    }
                } else {
                    throw new FindException("Cannot find " + dependencyType.getEntityType() + ". Method return type is Entity but the returned object is not an entity: " + searchValue.getClass());
                }
            case ENTITY_HEADER:
                if (searchValue instanceof EntityHeader) {
                    try {
                        return Arrays.asList((O) loadEntity((EntityHeader) searchValue));
                    } catch (ClassCastException e) {
                        throw new FindException("Cannot find " + dependencyType.getEntityType() + ". Entity cannot be cast to correct type: " + searchValue.getClass(), e);
                    }
                } else {
                    throw new FindException("Cannot find " + dependencyType.getEntityType() + ". Method return type is EntityHeader but the returned object is not an entity header: " + searchValue.getClass());
                }
            default:
                throw new FindException("Cannot find " + dependencyType.getEntityType() + ". Unsupported search method: " + searchValueType);
        }
    }

    /**
     * Returns a list of entity headers from a search value and entity type. The search value should either be a goid,
     * String, or an array of goids.
     *
     * @param searchValue The search value. Either a goid, string, or an array of goids
     * @param entityType  The entity type for the headers
     * @return The list of entity headers.
     * @throws CannotRetrieveDependenciesException This is thrown if the search value is an unsupported type
     */
    @NotNull
    private static List<EntityHeader> getEntityHeaders(@NotNull final Object searchValue, @NotNull final EntityType entityType) throws CannotRetrieveDependenciesException {
        //used the entity crud to find the entity using its GOID
        final List<EntityHeader> headers;
        if (searchValue instanceof Goid)
            headers = Arrays.asList(new EntityHeader((Goid) searchValue, entityType, null, null));
        else if (searchValue instanceof String)
            headers = Arrays.asList(new EntityHeader((String) searchValue, entityType, null, null));
        else if (searchValue instanceof Goid[]) {
            final Goid[] goids = (Goid[]) searchValue;
            headers = new ArrayList<>(goids.length);
            for (final Goid goid : goids) {
                headers.add(new EntityHeader(goid, entityType, null, null));
            }
        } else {
            throw new CannotRetrieveDependenciesException(entityType.getEntityClass(), "Unsupported GOID value type: " + searchValue.getClass() + " Expected with Goid, String or Goid[]");
        }
        return headers;
    }

    @NotNull
    @Override
    public DependentObject createDependentObject(@NotNull final O dependent) {
        if (dependent instanceof Entity) {
            final EntityHeader entityHeader = EntityHeaderUtils.fromEntity((Entity) dependent);
            //Fix LDAP and Federated identity providers not being found properly
            if (dependent instanceof IdentityProviderConfig) {
                entityHeader.setType(EntityType.ID_PROVIDER_CONFIG);
            }
            return createDependentEntity(entityHeader);
        } else {
            //Always return a dependent Object.
            return new DependentObject(null, com.l7tech.search.Dependency.DependencyType.ANY) {
            };
        }
    }

    /**
     * Creates a new dependent entity from an entity header.
     *
     * @param entityHeader The entity header to create the dependent entity from
     * @return The dependent entity.
     */
    @NotNull
    private static DependentEntity createDependentEntity(@NotNull final EntityHeader entityHeader) {
        return new DependentEntity(entityHeader.getName(), entityHeader);
    }

    @NotNull
    @Override
    public List<DependentObject> createDependentObjects(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) throws CannotRetrieveDependenciesException {
        switch (searchValueType) {
            case GOID: {
                final List<EntityHeader> headers = getEntityHeaders(searchValue, dependencyType.getEntityType());
                //convert the headers map to a list of dependent objects
                return Functions.map(headers, new Functions.Unary<DependentObject, EntityHeader>() {
                    @Override
                    public DependentObject call(EntityHeader entityHeader) {
                        return createDependentEntity(entityHeader);
                    }
                });
            }
            case ENTITY:
                if (searchValue instanceof Entity)
                    try {
                        return Arrays.asList(createDependentObject((O) searchValue));
                    } catch (ClassCastException e) {
                        throw new CannotRetrieveDependenciesException(dependencyType.getEntityType().getEntityClass(), "Method return type cannot be cast to correct type: " + searchValue.getClass(), e);
                    }
                else {
                    throw new CannotRetrieveDependenciesException(dependencyType.getEntityType().getEntityClass(), "Cannot find " + dependencyType.getEntityType() + ". Method return type is EntityHeader but the returned object is not an entity header: " + searchValue.getClass());
                }
            case ENTITY_HEADER:
                if (searchValue instanceof EntityHeader) {
                    return Arrays.<DependentObject>asList(createDependentEntity((EntityHeader) searchValue));
                } else {
                    throw new CannotRetrieveDependenciesException(dependencyType.getEntityType().getEntityClass(), "Method return type is EntityHeader but the returned object is not an entity header: " + searchValue.getClass());
                }
            default:
                throw new CannotRetrieveDependenciesException(dependencyType.getEntityType().getEntityClass(), "Unsupported search method: " + searchValueType);
        }
    }

    @Override
    public void replaceDependencies(@NotNull final O object, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        try {
            processObjectDependencyMethods(object,
                    new ProcessMethodDependencyReturn<CannotReplaceDependenciesException>() {
                        @Override
                        public void process(@NotNull final Object getterMethodReturn, @NotNull final Method method, @Nullable final com.l7tech.search.Dependency annotation) throws CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
                            //replace dependencies returned from the given method.
                            //get the list of dependencies returned.
                            final List<DependentObject> dependentEntities = getDependentEntitiesFromMethodReturnValue(annotation, getterMethodReturn, finder);
                            for (final DependentObject dependentObject : dependentEntities) {
                                if (dependentObject instanceof DependentEntity) {
                                    final DependentEntity dependentEntity = (DependentEntity) dependentObject;
                                    //replace the dependent entity with one that is in the replacement map
                                    final EntityHeader header = findMappedHeader(replacementMap, dependentEntity.getEntityHeader());
                                    if (header != null) {
                                        setDependencyForMethod(object, method, annotation, header, dependentEntity.getEntityHeader());
                                    }
                                } else {
                                    throw new CannotReplaceDependenciesException(method.getName().substring(3), null, annotation != null ? annotation.type().getEntityType().getEntityClass() : null, object.getClass(), "Cannot replace dependencies that are not DependentEntity.");
                                }
                            }
                        }
                    }, new ProcessSsgKeyHeader<CannotReplaceDependenciesException>() {
                        @Override
                        public void process(@NotNull final UsesPrivateKeys usesPrivateKeys, @NotNull final SsgKeyHeader ssgKeyHeader) throws CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
                            //replace private key dependencies
                            final EntityHeader mappedHeader = findMappedHeader(replacementMap, ssgKeyHeader);
                            if (mappedHeader != null) {
                                if(!(mappedHeader instanceof SsgKeyHeader)){
                                    throw new CannotReplaceDependenciesException(usesPrivateKeys.getClass(), "Attempting to replace ssg key but mapped header in not an SsgKeyHeader.");
                                }
                                usesPrivateKeys.replacePrivateKeyUsed(ssgKeyHeader, (SsgKeyHeader)mappedHeader);
                            }
                        }
                    });
        } catch (CannotRetrieveDependenciesException e) {
            throw new CannotReplaceDependenciesException(object.getClass(), e.getMessage(), e);
        }
    }

    /**
     * This will find all the methods in an object that are dependency methods and delegate to the caller to handle the
     * different types.
     *
     * @param object              The objects whose methods to search
     * @param processMethodReturn Delegate class to process a method return value.
     * @param processSsgKeyHeader Delegate class to process an ssg key header.
     * @param <T>                 The other type of exception that the processor methods can throw
     * @throws CannotRetrieveDependenciesException This is thrown if there is an error attempting to retrieve a
     *                                             dependency
     * @throws T                                   This is the other type of error that can be thrown by the processing
     *                                             methods.
     */
    private static <T extends Throwable> void processObjectDependencyMethods(
            @NotNull final Object object,
            @NotNull final ProcessMethodDependencyReturn<T> processMethodReturn,
            @NotNull final ProcessSsgKeyHeader<T> processSsgKeyHeader) throws CannotRetrieveDependenciesException, T {

        //Get all the methods that this entity declares
        final List<Method> entityMethods = getAllMethods(object.getClass());
        for (final Method method : entityMethods) {
            //process each method that returns an Entity or is annotated with @Dependency or @Dependencies.
            if (isDependencyGetterMethod(method)) {
                final Dependencies dependenciesAnnotation = method.getAnnotation(Dependencies.class);
                if (dependenciesAnnotation == null) {
                    //gets the Dependency annotation on the method if one is specified.
                    final com.l7tech.search.Dependency annotation = method.getAnnotation(com.l7tech.search.Dependency.class);
                    if (annotation != null && annotation.searchObject()) {
                        //In this case the method returned object should have its dependencies replaced.
                        //for example see {@link HttpConfiguration#getProxyConfiguration()}
                        final Object getterMethodReturn = retrieveDependencyFromMethod(object, method, annotation);
                        if (getterMethodReturn != null) {
                            //recourse to process the search object
                            processObjectDependencyMethods(getterMethodReturn, processMethodReturn, processSsgKeyHeader);
                        }
                    } else {
                        //get the dependent entity referenced
                        final Object getterMethodReturn = retrieveDependencyFromMethod(object, method, annotation);
                        if (getterMethodReturn != null) {
                            processMethodReturn.process(getterMethodReturn, method, annotation);
                        }
                    }
                } else {
                    for (final com.l7tech.search.Dependency annotation : dependenciesAnnotation.value()) {
                        //calls the getter method and retrieves the dependency referenced
                        final Object getterMethodReturn = retrieveDependencyFromMethod(object, method, annotation);
                        if (getterMethodReturn != null) {
                            processMethodReturn.process(getterMethodReturn, method, annotation);
                        }
                    }
                }
            }
        }

        //if the object implements UsesPrivateKeys then add the used private keys as dependencies.
        if (object instanceof UsesPrivateKeys && ((UsesPrivateKeys) object).getPrivateKeysUsed() != null) {
            for (final SsgKeyHeader keyHeader : ((UsesPrivateKeys) object).getPrivateKeysUsed()) {
                if (keyHeader != null) {
                   processSsgKeyHeader.process((UsesPrivateKeys) object, keyHeader);
                }
            }
        }
    }

    /**
     * This interface gets called to process a dependency object that is returned from a method.
     *
     * @param <T> A specific exception type that this can throw
     */
    private interface ProcessMethodDependencyReturn<T extends Throwable> {
        /**
         * process a method return value. Note the return value and the method will not be null. The @Dependency
         * annotation may be null.
         *
         * @param dependencyReturn The dependency value
         * @param dependencyMethod The method this dependency value came from
         * @param annotation       The @Dependency annotation describing how to call this method. May be null.
         * @throws CannotRetrieveDependenciesException
         * @throws T
         */
        void process(@NotNull Object dependencyReturn, @NotNull Method dependencyMethod, @Nullable com.l7tech.search.Dependency annotation) throws CannotRetrieveDependenciesException, T;
    }

    /**
     * This interface is used to process an ssgKeyHeader that is a dependency of the current object being inspected.
     *
     * @param <T> A specific exception type that this can throw
     */
    //TODO: this interface may not be needed. Or maybe could be made more generic.
    private interface ProcessSsgKeyHeader<T extends Throwable> {
        /**
         * process an ssg key header.
         *
         * @param usesPrivateKeys The object that uses the private keys.
         * @param ssgKeyHeader The ssg key header to process
         * @throws CannotRetrieveDependenciesException
         * @throws T
         */
        void process(UsesPrivateKeys usesPrivateKeys, @NotNull SsgKeyHeader ssgKeyHeader) throws CannotRetrieveDependenciesException, T;
    }

    /**
     * This will find a mapped header in the given headers map it will first check by id, then by guid, then by name
     *
     * @param replacementMap  The map to search for a mapped header
     * @param dependentHeader The depended entity header to find a mapping for
     * @return The mapped entity header, or null if it cant find one.
     */
    @Nullable
    protected static EntityHeader findMappedHeader(@NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final EntityHeader dependentHeader) {
        //try to find normally
        final EntityHeader header = replacementMap.get(dependentHeader);
        if (header != null) {
            return header;
        }
        // check by ID
        final EntityHeader idHeaderKey = Functions.grepFirst(replacementMap.keySet(), new Functions.Unary<Boolean, EntityHeader>() {
            @Override
            public Boolean call(@NotNull final EntityHeader entityHeader) {
                return entityHeader.getType().equals(dependentHeader.getType())
                        && entityHeader.getGoid().equals(dependentHeader.getGoid());
            }
        });
        if (idHeaderKey != null) {
            return replacementMap.get(idHeaderKey);
        }

        // check by GUID
        if (dependentHeader instanceof GuidEntityHeader) {
            final EntityHeader guidHeaderKey = Functions.grepFirst(replacementMap.keySet(), new Functions.Unary<Boolean, EntityHeader>() {
                @Override
                public Boolean call(@NotNull final EntityHeader entityHeader) {
                    return entityHeader instanceof GuidEntityHeader && entityHeader.getType().equals(dependentHeader.getType())
                            && StringUtils.equals(((GuidEntityHeader) entityHeader).getGuid(), ((GuidEntityHeader) dependentHeader).getGuid());
                }
            });
            if (guidHeaderKey != null) {
                return replacementMap.get(guidHeaderKey);
            }
        }

        //check by name
        if (dependentHeader.getName() != null) {
            final EntityHeader nameHeaderKey = Functions.grepFirst(replacementMap.keySet(), new Functions.Unary<Boolean, EntityHeader>() {
                @Override
                public Boolean call(@NotNull final EntityHeader entityHeader) {
                    return entityHeader.getType().equals(dependentHeader.getType())
                            && StringUtils.equals(entityHeader.getName(), dependentHeader.getName());
                }
            });
            if (nameHeaderKey != null) {
                return replacementMap.get(nameHeaderKey);
            }
        }
        return null;
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
    private void setDependencyForMethod(@NotNull final O object, @NotNull final Method getterMethod, @Nullable final com.l7tech.search.Dependency annotation, @NotNull final EntityHeader header, @NotNull final EntityHeader currentEntityHeader) throws CannotReplaceDependenciesException {
        if (annotation != null) {
            //finds the setter method
            final Method setterMethod;
            //this is the object that the setter method belongs to.
            final Object setterMethodObject;
            //Checks if the getter method returns a map and that the annotation key is not empty
            if (getterMethod.getParameterTypes().length == 0 &&
                    //the return type is either a Map
                    ((getterMethod.getGenericReturnType() instanceof Class && Map.class.isAssignableFrom((Class) getterMethod.getGenericReturnType())) ||
                            //Or it is a parameterized Map<>
                            (getterMethod.getGenericReturnType() instanceof ParameterizedType && ((ParameterizedType) getterMethod.getGenericReturnType()).getRawType() instanceof Class && Map.class.isAssignableFrom((Class) ((ParameterizedType) getterMethod.getGenericReturnType()).getRawType())))
                    && !annotation.key().isEmpty()) {
                //This is used to set the property on the map
                class MapSetter {
                    public void set(Object value) throws InvocationTargetException, IllegalAccessException {
                        final Map map = (Map) getterMethod.invoke(object);
                        //noinspection unchecked
                        map.put(annotation.key(), value);
                    }
                }
                setterMethodObject = new MapSetter();
                try {
                    //get the setter method from above
                    setterMethod = MapSetter.class.getMethod("set", Object.class);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("This should never be possible");
                }
            } else {
                //get the corresponding setter method from the getter method.
                setterMethod = findMethod(object.getClass(), "set" + getterMethod.getName().substring(3), getterMethod.getReturnType());
                setterMethodObject = object;
            }
            if (setterMethod == null) {
                throw new CannotReplaceDependenciesException(getterMethod.getName().substring(3), header.getStrId(), annotation.type().getEntityType().getEntityClass(), object.getClass(), "Cannot find setter method.");
            }
            try {
                switch (annotation.methodReturnType()) {
                    case GOID:
                        //Check if the goids are different. There is no point in replacing if the goids are the same
                        if (!Goid.equals(header.getGoid(), currentEntityHeader.getGoid())) {
                            setterMethod.invoke(setterMethodObject, header.getGoid());
                        }
                        break;
                    case NAME:
                        //Check if the names are different. There is no point in replacing if the names are the same
                        if (!StringUtils.equals(header.getName(), currentEntityHeader.getName())) {
                            setterMethod.invoke(setterMethodObject, header.getName());
                        }
                        break;
                    case VARIABLE:
                        //Check if the names are different. There is no point in replacing if the names are the same
                        if (!StringUtils.equals(header.getName(), currentEntityHeader.getName())) {
                            //This is not currently supported.
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
    private static Method findMethod(@NotNull final Class<?> aClass, @NotNull final String methodName, @NotNull final Class<?> parameterType) {
        return Functions.grepFirst(Arrays.asList(aClass.getMethods()), new Functions.Unary<Boolean, Method>() {
            @Override
            public Boolean call(@NotNull final Method method) {
                return methodName.equals(method.getName()) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(parameterType);
            }
        });
    }

    /**
     * Returns all the methods for the given class, including the methods defined in it's superclasses
     *
     * @param entityClass The class to get the methods for
     * @return The methods defined in this class and its superclasses.
     */
    @NotNull
    private static List<Method> getAllMethods(@NotNull final Class entityClass) {
        //Gets the classes defined methods
        final ArrayList<Method> fields = new ArrayList<>(Arrays.asList(entityClass.getDeclaredMethods()));
        if (entityClass.getSuperclass() != null) {
            //if this class has a superclass then also retrieve the fields defined in the superclass.
            fields.addAll(getAllMethods(entityClass.getSuperclass()));
        }
        return fields;
    }

    /**
     * Finds if this method is usable as an entity referencing method. An entity referencing method is one that is
     * annotated with {@link com.l7tech.search.Dependency} or {@link com.l7tech.search.Dependencies} or is a non
     * transient getter method that returns an {@link com.l7tech.objectmodel.Entity} object
     *
     * @param method The method to check.
     * @return True if the method should be used to to find a dependency. False otherwise.
     */
    private static boolean isDependencyGetterMethod(@NotNull final Method method) {
        final com.l7tech.search.Dependency annotation = method.getAnnotation(com.l7tech.search.Dependency.class);
        final Dependencies dependenciesAnnotation = method.getAnnotation(Dependencies.class);
        if (annotation != null) {
            return annotation.isDependency();
        } else if (dependenciesAnnotation != null) {
            return dependenciesAnnotation.value().length > 0;
        } else if (method.getAnnotation(Transient.class) == null) {
            //not transient, getter method, returns and Entity object, is public, and takes 0 parameters.
            return method.getName().startsWith("get") && Entity.class.isAssignableFrom(method.getReturnType()) && Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 0;
        }
        return false;
    }

    /**
     * Gets an entity given an entity header. This is needed to handle the special case where the entityCrud down-casts
     * identity provider entities. This will also handle SsgKeyEntry types, retrieving the default type if needed. Other
     * special cases handled are also custom key value stores and trusted certificates
     *
     * @param entityHeader The entity header for the entity to return.
     * @return The entity.
     * @throws FindException This is thrown if the entity cannot be found
     */
    //Should this be nullable or NotNull?
    protected Entity loadEntity(@NotNull final EntityHeader entityHeader) throws FindException {
        if (EntityType.ID_PROVIDER_CONFIG.equals(entityHeader.getType())) {
            return identityProviderConfigManager.findByHeader(entityHeader);
        } else if (EntityType.SSG_KEY_ENTRY.equals(entityHeader.getType()) && entityHeader instanceof SsgKeyHeader) {
            final SsgKeyHeader keyHeader = (SsgKeyHeader) entityHeader;
            final SsgKeyEntry keyEntry;
            try {
                if (Goid.isDefault(keyHeader.getKeystoreId())) {
                    if (keyHeader.getAlias() == null) {
                        //if the keystore id is default and the alias is null then use the default key.
                        try {
                            keyEntry = defaultKey.getSslInfo();
                        } catch (IOException e) {
                            throw new FindException("Could got det Default ssl Key", e);
                        }
                    } else {
                        //the keystore is default but an alias is specified. Then find the key using the keyStoreManager
                        keyEntry = keyStoreManager.lookupKeyByKeyAlias(keyHeader.getAlias(), keyHeader.getKeystoreId());
                    }
                } else {
                    //This is when both the keyStore and alias as specified. Doing it this way instead of 'keyStoreManager.lookupKeyByKeyAlias(keyHeader.getAlias(), keyHeader.getKeystoreId())' strictly enforces that the key is found in a specific keystore
                    final SsgKeyFinder ssgKeyFinder = keyStoreManager.findByPrimaryKey(keyHeader.getKeystoreId());
                    keyEntry = ssgKeyFinder.getCertificateChain(keyHeader.getAlias());
                }
            } catch (KeyStoreException e) {
                throw new FindException("Exception finding SsgKeyEntry", e);
            }
            return keyEntry;
        } else if (EntityType.CUSTOM_KEY_VALUE_STORE.equals(entityHeader.getType())) {
            final String entityId;
            final CustomKeyValueStore customKeyValueStore;
            if (entityHeader.getGoid() == null || Goid.DEFAULT_GOID.equals(entityHeader.getGoid())) {
                entityId = entityHeader.getName();
                customKeyValueStore = customKeyValueStoreManager.findByUniqueName(entityId);
            } else {
                entityId = entityHeader.getStrId();
                customKeyValueStore = customKeyValueStoreManager.findByPrimaryKey(entityHeader.getGoid());
            }
            if (customKeyValueStore == null)
                throw new FindException("Couldn't find custom key value with id: \"" + entityId + "\"");
            return customKeyValueStore;
        } else if (EntityType.TRUSTED_CERT.equals(entityHeader.getType())) {
            // may be referenced by name only
            if (Goid.isDefault(entityHeader.getGoid())) {
                return trustedCertManager.findByUniqueName(entityHeader.getName());
            } else {
                return trustedCertManager.findByPrimaryKey(entityHeader.getGoid());
            }
        } else {
            return entityCrud.find(entityHeader);
        }
    }
}
