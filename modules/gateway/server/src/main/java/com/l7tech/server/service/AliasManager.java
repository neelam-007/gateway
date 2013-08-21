package com.l7tech.server.service;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.FolderedEntityManager;

import java.util.Collection;

/**
 * This interface defines the methods available to an AliasManager and currently this interface is implemented
 * by {@link ServiceAliasManager} and {@link com.l7tech.server.policy.PolicyAliasManager}
 * However the implementations of both {@link ServiceAliasManager} and {@link com.l7tech.server.policy.PolicyAliasManager}
 * is handled by the abstract class {@link AliasManagerImpl} as the behaviour and implementation is identical for aliases
 * of Services and Policies.
 *
 * @param <AT> the actual Alias persisted object that we are finding. Currently this is either
 *             {@link com.l7tech.gateway.common.service.PublishedServiceAlias} or {@link com.l7tech.policy.PolicyAlias}.
 * @param <ET> the type of the entities that this manager's aliases target
 * @param <HT> the header type for the entities whose aliases this manager manages
 *
 * @author darmstrong
 */
public interface AliasManager<AT extends Alias<ET>, ET extends PersistentEntity, HT extends OrganizationHeader>
        extends FolderedEntityManager<AT, AliasHeader<ET>>
{

    /**
     * Find the alias entity by specifying the read entities oid and the folder the alias is related to
     * This is all that is needed to find an alias.
     * @param entityGoid The goid of the original entity
     * @param folderGoid The goid of the folder the alias is related to
     * @return The actual alias with correct type, or null if not found
     * @throws FindException
     */
    public AT findAliasByEntityAndFolder(Goid entityGoid, Goid folderGoid) throws FindException;

    public Collection<AT> findAllAliasesForEntity(Goid entityGoid) throws FindException;

    /**
     * Both Services and Policies have the same requirement for a set of entities returned from a findAll()
     * => they need to be expanded to include all alises
     * @param originalHeaders
     * @return Collection<HT> The returned collection will have an extra OrganizationHeader for any aliases found
     * The extra headers are the same as the original expect that their folder property represents the alias and their
     * isAlias() method returns true
     */
    public Collection<HT> expandEntityWithAliases(Collection<HT> originalHeaders) throws FindException;
}
