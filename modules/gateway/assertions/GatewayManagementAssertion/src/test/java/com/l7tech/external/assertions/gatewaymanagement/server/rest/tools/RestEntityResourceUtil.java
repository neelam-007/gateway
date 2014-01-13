package com.l7tech.external.assertions.gatewaymanagement.server.rest.tools;

import com.l7tech.external.assertions.gatewaymanagement.server.RestResponse;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.util.Functions;

import java.util.List;
import java.util.Map;

/**
 * These are the methods that a rest entity test must implement in order to have tests automatically run for it.
 */
public interface RestEntityResourceUtil<E extends PersistentEntity, M extends ManagedObject> {

    /**
     * This returns a list of entity ids that are retrievable using the rest api
     *
     * @return The entity ids that can be retrieved using the rest api
     */
    public List<String> getRetrievableEntityIDs();

    /**
     * This returns a list of managed objects that can be created using the api. The id's must be set on these.
     *
     * @return A list of managed objects that can be created using the api
     */
    public List<M> getCreatableManagedObjects();

    /**
     * This returns a list of managed objects that can be updated using the api
     *
     * @return A list of managed objects that can be updated using the api
     */
    public List<M> getUpdateableManagedObjects();

    /**
     * This is a map of managed object to response checker for objects that are expected to fail on create.
     *
     * @return A map of managed object to response checker for objects that are expected to fail on create.
     */
    public Map<M, Functions.BinaryVoid<M, RestResponse>> getUnCreatableManagedObjects();

    /**
     * This is a map of managed object to response checker for objects that are expected to fail on update.
     *
     * @return A map of managed object to response checker for objects that are expected to fail on update.
     */
    public Map<M, Functions.BinaryVoid<M, RestResponse>> getUnUpdateableManagedObjects();

    /**
     * This is a map of ids to response checker for objects that are expected to fail on get.
     *
     * @return A map of ids to response checker for objects that are expected to fail on get.
     */
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds();

    /**
     * This is a map of ids to response checker for objects that are expected to fail on delete.
     *
     * @return A map of ids to response checker for objects that are expected to fail on delete.
     */
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds();

    /**
     * This is a map of search query string to response checker for searches that are expected to fail.
     *
     * @return A map of search query string to response checker for searches that are expected to fail.
     */
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries();

    /**
     * This returns a list of id's of objects that can be deleted using the api.
     *
     * @return A list of id's that can be deleted using the api
     */
    public List<String> getDeleteableManagedObjectIDs();

    /**
     * The uri of the resource
     *
     * @return the uri of the resource
     */
    public String getResourceUri();

    /**
     * The type of the entity
     *
     * @return The entity type
     */
    public EntityType getType();

    /**
     * The expected reference title of the given entity.
     *
     * @param id The entity to get the title for
     * @return The title for the given entity
     */
    public String getExpectedTitle(String id) throws FindException;

    /**
     * This will verify that the returned links are all pressent and correct. Note that the self link has already been verified
     *
     * @param id    The entity to verify the links for
     * @param links The links to verify
     */
    public void verifyLinks(String id, List<Link> links) throws FindException;

    /**
     * This will chack that the managedObject is the correct managed object for the given entity.
     *
     * @param id            The entity
     * @param managedObject The managed object
     */
    public void verifyEntity(String id, M managedObject) throws FindException;

    /**
     * This returns a list of search queries and a list of the expected results of executing the search query
     *
     * @return A list of search queries and a list of the expected results of executing the search query
     */
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException;
}
