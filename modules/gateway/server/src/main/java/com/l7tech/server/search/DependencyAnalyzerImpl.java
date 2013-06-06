package com.l7tech.server.search;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.policy.PolicyAliasManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.service.ServiceAliasManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.EmptyIterator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This service is used to find dependencies of different entities.
 *
 * @author Victor Kazakov
 */
public class DependencyAnalyzerImpl implements DependencyAnalyzer {
    protected static final Logger logger = Logger.getLogger(DependencyAnalyzerImpl.class.getName());

    @Inject
    private EntityCrud entityCrud;

    @Inject
    private JdbcConnectionManager jdbcConnectionManager;

    @Inject
    private FolderManager folderManager;

    @Inject
    private PolicyManager policyManager;

    @Inject
    private ServiceManager serviceManager;

    @Inject
    private PolicyAliasManager policyAliasManager;

    @Inject
    private ServiceAliasManager serviceAliasManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public DependencySearchResults getDependencies(EntityHeader entityHeader) throws FindException {
        return getDependencies(entityHeader, DefaultSearchOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DependencySearchResults getDependencies(EntityHeader entityHeader, Map<String, String> searchOptions) throws FindException {
        Entity entity = entityCrud.find(entityHeader);
        int depth = getIntegerOption(searchOptions, SearchDepthOptionKey);
        //TODO: Create a utility class that will be used to perform the search. This class will be given searchOptions
        //TODO: and will track the found dependencies internally so that they do not need to be passed along to different method as parameters
        Dependency dependency = getDependencyHelper(entity, depth, new HashSet<Dependency>());
        return new DependencySearchResults(createDependencyEntity(entityHeader), dependency.getDependencies(), searchOptions);
    }

    /**
     * Returns the set of dependencies for the given entity.
     *
     * @param entity            The entity to find the dependencies for
     * @param depth             The depth to search till. If this is 0 the empty set is returned.
     * @param dependenciesFound This is the set of all dependencies already found. This is used to handle cyclical
     *                          dependencies.
     * @return The set of dependencies that this entity has.
     */
    private List<Dependency> getDependenciesHelper(final Object entity, int depth, Set<Dependency> dependenciesFound) throws FindException {
        //if the depth is 0 return the empty set. Base case.
        if (depth == 0) {
            return Collections.emptyList();
        }

        final List<Dependency> dependencies;
        // This checks for some special entities in order to handle them properly.
        //TODO: Use a factory to retrieve the correct dependency finder for the entity.
        if (entity instanceof Policy) {
            dependencies = findPolicyDependencies((Policy) entity, depth, dependenciesFound);
        } else if (entity instanceof Folder) {
            dependencies = findFolderDependencies((Folder) entity, depth, dependenciesFound);
        } else {
            dependencies = findGenericDependencies(entity, depth, dependenciesFound);
        }
        return dependencies;
    }

    /**
     * Find folder dependencies this will find all subfolders, policies, published services and aliases in the folder.
     * @throws FindException
     */
    private List<Dependency> findFolderDependencies(final Folder folder, int depth, Set<Dependency> dependenciesFound) throws FindException {
        Collection<Folder> folders = folderManager.findAll();
        Collection<Policy> policies = policyManager.findAll();
        Collection<PublishedService> services = serviceManager.findAll();
        Collection<PolicyAlias> policyAliases = policyAliasManager.findAll();
        Collection<PublishedServiceAlias> serviceAliases = serviceAliasManager.findAll();

        final ArrayList<Dependency> dependencies = new ArrayList<>();
        //subfolder dependencies
        for (Folder currentFolder : folders) {
            if (currentFolder.getFolder() != null && folder.getOid() == currentFolder.getFolder().getOid()) {
                Dependency dependency = getDependencyHelper(currentFolder, depth - 1, dependenciesFound);
                dependencies.add(dependency);
            }
        }
        //keep a list of the service policies found. These policies will not need to be added as dependencies because the services will already have them as dependencies.
        Set<Policy> servicePolicies = new HashSet<>();
        //service dependencies
        for (PublishedService service : services) {
            if (service.getFolder() != null && folder.getOid() == service.getFolder().getOid()) {
                Dependency dependency = getDependencyHelper(service, depth - 1, dependenciesFound);
                dependencies.add(dependency);
                servicePolicies.add(service.getPolicy());
            }
        }
        //policy dependencies
        for (Policy policy : policies) {
            if (policy.getFolder() != null && folder.getOid() == policy.getFolder().getOid() && !servicePolicies.contains(policy)) {
                Dependency dependency = getDependencyHelper(policy, depth - 1, dependenciesFound);
                dependencies.add(dependency);
            }
        }
        //policy alias dependencies
        for (PolicyAlias policyAlias : policyAliases) {
            if (policyAlias.getFolder() != null && folder.getOid() == policyAlias.getFolder().getOid()) {
                Dependency dependency = getDependencyHelper(policyAlias, depth - 1, dependenciesFound);
                dependencies.add(dependency);
            }
        }
        //service alias dependencies
        for (PublishedServiceAlias serviceAlias : serviceAliases) {
            if (serviceAlias.getFolder() != null && folder.getOid() == serviceAlias.getFolder().getOid()) {
                Dependency dependency = getDependencyHelper(serviceAlias, depth - 1, dependenciesFound);
                dependencies.add(dependency);
            }
        }
        return dependencies;
    }

    /**
     * Finds the dependencies in a policy by looking at the assertions contained in the policy
     * @throws FindException
     */
    private List<Dependency> findPolicyDependencies(Policy policy, int depth, Set<Dependency> dependenciesFound) throws FindException {
        final Assertion assertion;
        try {
            assertion = policy.getAssertion();
        } catch (IOException e) {
            throw new RuntimeException("Invalid policy with id " + policy.getGuid() + ": " + ExceptionUtils.getMessage(e), e);
        }

        final ArrayList<Dependency> dependencies = new ArrayList<>();
        final Iterator assit = assertion != null ? assertion.preorderIterator() : new EmptyIterator();
        //iterate for each assertion.
        while (assit.hasNext()) {
            final Assertion currentAssertion = (Assertion) assit.next();
            //for all the dependencies in the assertion if the dependency is not alread found add it to the list of dependencies.
            Functions.forall(getDependenciesHelper(currentAssertion, depth, dependenciesFound), new Functions.Unary<Boolean, Dependency>() {
                @Override
                public Boolean call(Dependency dependency) {
                    if (!dependencies.contains(dependency))
                        dependencies.add(dependency);
                    return true;
                }
            });
            //is the assertion implements UsesEntities then use the getEntitiesUsed method to find the entities used by the assertion.
            if (currentAssertion instanceof UsesEntities) {
                for (EntityHeader header : ((UsesEntities) currentAssertion).getEntitiesUsed()) {
                    final Entity entity = entityCrud.find(header);
                    if (entity != null) {
                        Dependency dependency = getDependencyHelper(entity, depth, dependenciesFound);
                        if (!dependencies.contains(dependency))
                            dependencies.add(dependency);
                    }
                }
            }
        }
        return dependencies;
    }

    /**
     * This finds dependencies for a generic entity. It will search the entity fields in order to find dependent
     * entities.
     *
     * @param entity            The entity to find dependencies for.
     * @param depth             The depth to search till
     * @param dependenciesFound The set of already found dependencies. This is used to handle cyclical dependencies.
     * @return The set of dependencies that this entity has.
     */
    private List<Dependency> findGenericDependencies(Object entity, int depth, Set<Dependency> dependenciesFound) throws FindException {
        ArrayList<Dependency> dependencies = new ArrayList<>();
        //Get all the fields that this entity declares
        List<Method> entityMethods = getAllMethods(entity.getClass());
        for (Method method : entityMethods) {
            //for each field that is an Entity and is usable. retrieve the field value by calling the appropriate getter.
            if (isEntityMethod(method)) {
                Entity dependentEntity = null;
                try {
                    //calls the getter method and retrieves the dependency.
                    Object getterMethodReturn = method.invoke(entity);
                    com.l7tech.search.Dependency annotation = method.getAnnotation(com.l7tech.search.Dependency.class);
                    if (annotation != null && !annotation.methodReturnType().equals(com.l7tech.search.Dependency.MethodReturnType.ENTITY)) {
                        dependentEntity = retrieveEntity(getterMethodReturn, annotation);
                    } else {
                        dependentEntity = (Entity) getterMethodReturn;
                    }
                } catch (Exception e) {
                    //if an exception is thrown attempting to retrieve the dependency then log the exception but continue processing.
                    logger.log(Level.FINE, "WARNING finding dependencies - error getting dependent entity from method " + (method != null ? "using method " + method.getName() : "") + " for entity " + entity.getClass(), e);
                }
                if (dependentEntity != null) {
                    //if a dependency if found then search for its dependencies and add it to the set of dependencies found
                    dependencies.add(getDependencyHelper(dependentEntity, depth - 1, dependenciesFound));
                }
            }
        }
        return dependencies;
    }

    /**
     * Retrieves an entity given a search value and the Dependency annotation. TODO: use a factory to retrieve the
     * correct entity based on its type
     */
    private Entity retrieveEntity(Object searchValue, com.l7tech.search.Dependency dependency) throws DependencyAnalyzerException, FindException {
        switch (dependency.methodReturnType()) {
            case GUID:
                //Not yet implemented
                break;
            case NAME: {
                if (searchValue instanceof String) {
                    switch (dependency.type()) {
                        case JDBC_CONNECTION:
                            return jdbcConnectionManager.getJdbcConnection((String) searchValue);
                        default:
                            throw new DependencyAnalyzerException("Named entity search is not supported for entities of type: " + dependency.type());
                    }
                } else
                    throw new DependencyAnalyzerException("Unsupported name search value type, must be String: " + searchValue.getClass());
            }
            case OID: {
                EntityHeader header;
                if (searchValue instanceof Long)
                    header = new EntityHeader((Long) searchValue, dependency.type(), dependency.type().getName(), null);
                else if (searchValue instanceof String)
                    header = new EntityHeader((String) searchValue, dependency.type(), dependency.type().getName(), null);
                else
                    throw new DependencyAnalyzerException("Unsupported OID value type: " + searchValue.getClass());
                return entityCrud.find(header);
            }
            case ENTITY:
                return (Entity) searchValue;
            default:
                throw new DependencyAnalyzerException("Unsupported search method: " + dependency.methodReturnType());
        }
        return null;
    }

    /**
     * Return the entity as a dependency with its dependencies populated if the depth is not 0.
     *
     * @param entity            The entity to search dependencies for.
     * @param depth             The depth to search till. If 0 dependencies will not be searched and a Depencency object
     *                          will be returned with areDependenciesSet() = false
     * @param dependenciesFound The set of already found dependencies, this is used to handle cyclical dependencies.
     * @return The dependency object representing this entity.
     */
    private Dependency getDependencyHelper(Entity entity, int depth, Set<Dependency> dependenciesFound) throws FindException {
        //Checks if the dependency for this entity has already been found.
        Dependency dependencyFound = findDependencyForEntity(entity, dependenciesFound);
        //If it has already been found return it.
        if (dependencyFound != null)
            return dependencyFound;
        //Creates a dependency for this entity.
        Dependency dependency = new Dependency(createDependencyEntity(EntityHeaderUtils.fromEntity(entity)));
        //Adds the dependency to the dependencies found set. This needs to be done before calling the
        // getDependenciesHelper() method in order to handle the cyclical case
        dependenciesFound.add(dependency);
        if (depth != 0)
            //If the depth is non 0 then find the dependencies for the given entity.
            dependency.setDependencies(getDependenciesHelper(entity, depth, dependenciesFound));
        return dependency;
    }

    private static DependencyEntity createDependencyEntity(EntityHeader entity) {
        return new DependencyEntity(entity.getName(), entity.getType(), entity instanceof GuidEntityHeader ? ((GuidEntityHeader) entity).getGuid() : null, entity.getStrId());
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
     * This will search through the given set of dependencies to see if the entity given has already been found as a
     * dependency. If it has that dependency is returned. Otherwise null is returned.
     *
     * @param entity            The entity to search for
     * @param dependenciesFound The set of already found dependencies.
     * @return A dependency for this entity if one has already been found, Null otherwise.
     */
    private static Dependency findDependencyForEntity(final Entity entity, Set<Dependency> dependenciesFound) {
        return Functions.grepFirst(dependenciesFound, new Functions.Unary<Boolean, Dependency>() {
            @Override
            public Boolean call(Dependency dependency) {
                //return true if the dependency is for the same entity as the one we are searching for.
                return dependency.getEntity().equals(createDependencyEntity(EntityHeaderUtils.fromEntity(entity)));
            }
        });
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

    /**
     * Returns an integer value from the search options for the given key. If the option is not set or the value cannot
     * be converted to an integer {@link IllegalArgumentException} is thrown
     *
     * @param searchOptions The search options to search
     * @param optionKey     The key to use to look up the value
     * @return The integer value
     */
    private static int getIntegerOption(Map<String, String> searchOptions, String optionKey) {
        String optionValue = searchOptions.get(optionKey);
        if (optionValue == null)
            throw new IllegalArgumentException("Search option value for option '" + optionKey + "' was null.");
        final int value;
        try {
            value = Integer.parseInt(optionValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Search option value for option '" + optionKey + "' was not a valid integer. Value: " + optionValue, e);
        }
        return value;
    }
}
