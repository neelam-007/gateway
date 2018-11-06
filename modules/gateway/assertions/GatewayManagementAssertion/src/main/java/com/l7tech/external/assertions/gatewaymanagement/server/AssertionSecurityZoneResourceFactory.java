package com.l7tech.external.assertions.gatewaymanagement.server;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.l7tech.gateway.api.AssertionSecurityZoneMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.server.policy.AssertionAccessManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.Either;
import com.l7tech.util.Eithers;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;

/**
 * Only GET, PUT and Enumerate operations are supported
 *
 * CREATE: use PUT operation to assign a security zone to an assertion (class name) (selector: name = class name)
 * READ: when querying by name, an assertion security zone will be returned even if it was not persisted
 * UPDATE: same
 * DELETE: use PUT operation to un-assign a security zone (remove securityZoneId attribute)
 * ENUMERATE: returns list of all instances of registered assertions even if it was not persisted.
 */
@ResourceFactory.ResourceType(type=AssertionSecurityZoneMO.class)
public class AssertionSecurityZoneResourceFactory extends SecurityZoneableEntityManagerResourceFactory<AssertionSecurityZoneMO, AssertionAccess, EntityHeader> {

    //- PUBLIC

    public AssertionSecurityZoneResourceFactory(final RbacServices rbacServices,
                                                final SecurityFilter securityFilter,
                                                final PlatformTransactionManager transactionManager,
                                                final AssertionAccessManager assertionAccessManager,
                                                final SecurityZoneManager securityZoneManager,
                                                final AssertionRegistry assertionRegistry) {
        super(false, true, rbacServices, securityFilter, transactionManager, assertionAccessManager, securityZoneManager);
        this.assertionAccessManager = assertionAccessManager;
        this.assertionRegistry = assertionRegistry;
    }

    @Override
    public boolean isCreateSupported() {
        return false;
    }

    @Override
    public AssertionSecurityZoneMO getResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        return Eithers.extract( transactional( new TransactionalCallback<Either<ResourceNotFoundException,AssertionSecurityZoneMO>>(){
            @Override
            public Either<ResourceNotFoundException,AssertionSecurityZoneMO> execute() throws ObjectModelException {
                try {
                    EntityBag<AssertionAccess> entityBag = selectEntityBag(selectorMap);
                    //check if this is a registered assertion.
                    if(!assertionRegistry.isAssertionRegistered(entityBag.getEntity().getName())){
                        return left(new ResourceNotFoundException("Assertion is not registered: " + entityBag.getEntity().getName()));
                    }
                    checkPermitted( OperationType.READ, null, entityBag.getEntity() );
                    return right( identify( asResource( entityBag ), entityBag.getEntity() ) );
                } catch ( ResourceNotFoundException e ) {
                    return left( e );
                }
            }
        }, true ) );
    }

    @Override
    public AssertionSecurityZoneMO putResource( final Map<String, String> selectorMap, final Object resource ) throws ResourceNotFoundException, InvalidResourceException {

        final String id = Eithers.extract2(transactional(new TransactionalCallback<Eithers.E2<ResourceNotFoundException, InvalidResourceException, String>>() {
            @SuppressWarnings({"unchecked"})
            @Override
            public Eithers.E2<ResourceNotFoundException, InvalidResourceException, String> execute() throws ObjectModelException {
                EntityBag<AssertionAccess> oldEntityBag;
                try {
                    oldEntityBag = selectEntityBag(selectorMap);
                } catch (ResourceNotFoundException e) {
                    return Eithers.left2_1(e);
                }

                try {
                    if (oldEntityBag == null || Goid.isDefault(oldEntityBag.getEntity().getGoid())) {   // create
                        final EntityBag<AssertionAccess> entityBag = fromResourceAsBag(resource);
                        for (PersistentEntity entity : entityBag) {
                            if (entity.getVersion() == VERSION_NOT_PRESENT) {
                                entity.setVersion(0);
                            }

                            if (!entity.getGoid().equals(PersistentEntity.DEFAULT_GOID) ||
                                    (entity.getVersion() != 0 && entity.getVersion() != 1)) { // some entities initialize the version to 1
                                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid identity or version");
                            }
                        }

                        checkPermitted(OperationType.UPDATE, null, entityBag.getEntity());

                        for (PersistentEntity entity : entityBag) {
                            validate(entity);
                        }

                        final Goid goid = assertionAccessManager.save(entityBag.getEntity());
                        EntityContext.setEntityInfo(getType(), goid.toString());
                        return Eithers.right2(goid.toString());

                    } else {  // update
                        final EntityBag<AssertionAccess> newEntityBag = fromResourceAsBag(resource);

                        if (resource instanceof AssertionSecurityZoneMO) {
                            final AssertionSecurityZoneMO managedResource = (AssertionSecurityZoneMO) resource;
                            setIdentifier(newEntityBag.getEntity(), managedResource.getId(), false);
                            setVersion(newEntityBag.getEntity(), managedResource.getVersion());
                        }

                        updateEntityBag(oldEntityBag, newEntityBag);

                        verifyIdentifier(oldEntityBag.getEntity().getGoid(),
                                newEntityBag.getEntity().getGoid());

                        verifyVersion(oldEntityBag.getEntity().getVersion(),
                                newEntityBag.getEntity().getVersion());

                        checkPermitted(OperationType.UPDATE, null, newEntityBag.getEntity());

                        for (PersistentEntity entity : oldEntityBag) {
                            validate(entity);
                        }

                        assertionAccessManager.update(oldEntityBag.getEntity());
                        afterUpdateEntity(oldEntityBag);
                        return Eithers.right2(oldEntityBag.getEntity().getId());
                    }
                } catch (InvalidResourceException e) {
                    return Eithers.left2_2(e);
                }
            }
        }, false));

        try{
            return getResource( Collections.singletonMap(IDENTITY_SELECTOR, id)); // re-select to get updated/created version#
        }catch ( PermissionDeniedException e1){
            // return nothing if have no read permission
            return null;
        }
    }

    @Override
    public Collection<Map<String, String>> getResources() {
        Collection<Map<String,String>> resources = Collections.emptyList();

        try {
            List<AssertionAccess> zones = new ArrayList<AssertionAccess>(assertionAccessManager.findAllRegistered());
            zones = accessFilter(zones, assertionAccessManager.getEntityType(), OperationType.READ, null);

            resources = new ArrayList<Map<String,String>>( zones.size() );

            for ( AssertionAccess header : zones ) {
                resources.add( Collections.singletonMap( NAME_SELECTOR, header.getName() ) );
            }
        } catch (FindException e) {
            handleObjectModelException(e);
        }

        return resources;
    }

    public Iterable<AssertionSecurityZoneMO> listResources(final String sortKey, final Boolean ascending, final Map<String, List<Object>> filtersMap) {
        try {
            List<AssertionAccess> entities = new ArrayList<AssertionAccess>(assertionAccessManager.findAllRegistered());

            entities = Lists.newArrayList(Iterables.filter(entities, new Predicate<AssertionAccess>() {
                @Override
                public boolean apply(@Nullable AssertionAccess assertionAccess) {
                    boolean match = true;
                    if (filtersMap.containsKey("name")) {
                        match = filtersMap.get("name").contains(assertionAccess.getName());
                    }
                    if (match && filtersMap.containsKey("id")) {
                        match = match &&  filtersMap.get("id").contains(assertionAccess.getGoid());
                    }
                    if (match && filtersMap.containsKey("securityZone.id") ) {
                        //if the search key contains the default security zone id, return all asertion accesses that do not have a security zone.
                        match = match && (assertionAccess.getSecurityZone() == null ? filtersMap.get("securityZone.id").contains(Goid.DEFAULT_GOID) : filtersMap.get("securityZone.id").contains(assertionAccess.getSecurityZone().getGoid()));
                    }
                    return match;
                }
            }));

            if (sortKey != null) {
                Collections.sort(entities, new Comparator<AssertionAccess>() {
                    @Override
                    public int compare(AssertionAccess o1, AssertionAccess o2) {
                        switch (sortKey) {
                            case "name":
                                return (ascending == null || ascending) ? o1.getName().compareTo(o2.getName()) : o2.getName().compareTo(o1.getName());
                            case "id":
                                return (ascending == null || ascending) ? o1.getId().compareTo(o2.getId()) : o2.getId().compareTo(o1.getId());
                            case "securityZone.id":
                                final String o1Zone = o1.getSecurityZone() == null ? Goid.DEFAULT_GOID.toString() : o1.getSecurityZone().getId();
                                final String o2Zone = o2.getSecurityZone() == null ? Goid.DEFAULT_GOID.toString() : o2.getSecurityZone().getId();
                                return (ascending == null || ascending) ? o1Zone.compareTo(o2Zone) : o2Zone.compareTo(o1Zone);
                            default:
                                throw new IllegalArgumentException("Cannot sort. Unknown sort key: " + sortKey);
                        }
                    }
                });
            }

            entities = accessFilter(entities, assertionAccessManager.getEntityType(), OperationType.READ, null);
            entities = filterEntities(entities);

            return Functions.map(entities, new Functions.UnaryThrows<AssertionSecurityZoneMO, AssertionAccess, ObjectModelException>() {
                @Override
                public AssertionSecurityZoneMO call(AssertionAccess e) throws ObjectModelException {
                    return identify(asResource(loadEntityBag(e)), e);
                }
            });
        }catch (ObjectModelException e) {
            handleObjectModelException(e);
        }

        return Collections.emptyList();
    }

    @Override
    public boolean isDeleteSupported() {
        return false;
    }

    //- PROTECTED

    @Override
    public AssertionSecurityZoneMO asResource(AssertionAccess entity) {
        AssertionSecurityZoneMO resource = ManagedObjectFactory.createAssertionAccess();

        resource.setName( entity.getName() );

        // handle SecurityZone
        doSecurityZoneAsResource( resource, entity );

        return resource;
    }

    @Override
    public AssertionAccess fromResource(Object resource, boolean strict) throws InvalidResourceException {

        if ( !(resource instanceof AssertionSecurityZoneMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected assertion access");

        final AssertionSecurityZoneMO assertionResource = (AssertionSecurityZoneMO) resource;

        final AssertionAccess accessEntity;
        accessEntity = new AssertionAccess();
        accessEntity.setName( assertionResource.getName() );

        // handle SecurityZone
        doSecurityZoneFromResource(assertionResource, accessEntity, strict);

        return accessEntity;
    }

    @Override
    protected void updateEntity(AssertionAccess oldEntity, AssertionAccess newEntity) throws InvalidResourceException {
        if(!oldEntity.getName().equals(newEntity.getName())){
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "cannot update an assertion access name");
        }
        oldEntity.setSecurityZone( newEntity.getSecurityZone() );
    }

    @Override
    protected AssertionAccess selectEntityCustom(Map<String, String> selectorMap) throws ResourceAccessException, InvalidResourceSelectors {
        String name = selectorMap.get(NAME_SELECTOR);
        if(name != null && assertionRegistry.isAssertionRegistered(name)){
            return new AssertionAccess(name);
        }
        return null;
    }

    //- PRIVATE

    private AssertionAccessManager assertionAccessManager;
    private AssertionRegistry assertionRegistry;
}
