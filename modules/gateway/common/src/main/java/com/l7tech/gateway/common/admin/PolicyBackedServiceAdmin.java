package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.POLICY_BACKED_SERVICE;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 *
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=POLICY_BACKED_SERVICE)
@Administrative
public interface PolicyBackedServiceAdmin {

    /**
     * Find all registered policy-backed services (but NOT templates).
     *
     * @return a collection of policy-backed service instances.  Never null but may be empty.
     * @throws FindException if there was a problem reading configs from the database.
     */
    @NotNull
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<PolicyBackedService> findAll() throws FindException;

    /**
     * Find all registered policy-backed services (NOT templates) that implement the named annotated interface class.
     *
     * @param interfaceClassName class name of @PolicyBacked annotated interface.  Required.
     * @return a list of policy backed service instances implementing the specified interface.  May be empty but never null.
     * @throws FindException if no policy backed interface with that name is registered, or there is some other problem finding the values.
     */
    @NotNull
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<PolicyBackedService> findAllForInterface( @NotNull String interfaceClassName ) throws FindException;

    /**
     * Find all registered policy-backed service templates.
     *
     * @return a collection of PolicyBackedService templates.  Never null but may be empty.
     */
    @NotNull
    @Transactional(readOnly=true)
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    Collection<String> findAllTemplateInterfaceNames();

    /**
     * Get a description of the
     * @param interfaceName name of the service template interface to inspect.
     * @return a description of all operations defined by this interface, expressed as EncapsulatedAssertionConfig
     *         instances. These are transient instances without a real Goid and with no backing policy.
     *         They contain a description of each method name, its input context variables, and its output context variables.
     * @throws ObjectNotFoundException if no service interface by that name is currently registered.
     */
    @NotNull
    @Transactional(readOnly=true)
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    Collection<EncapsulatedAssertionConfig> getInterfaceDescription( String interfaceName ) throws ObjectNotFoundException;

    /**
     * Locate a policy backed service config by its id.
     *
     * @param goid ID to look up.  Required.
     * @return the requested policy backed service, or null if not found.
     * @throws FindException if there is a problem accessing the sought-after information.
     */
    @Nullable
    @Secured(stereotype=FIND_ENTITY)
    PolicyBackedService findByPrimaryKey( Goid goid ) throws FindException;

    /**
     * Save a new or updated service config.
     *
     * @param config the new or updated config to save.  Required.
     * @return the ID assigned to the new config, or the existing ID of the updated config.  Never null.
     * @throws SaveException if there is a problem saving the entity.
     * @throws UpdateException if there is a problem updating the entity.
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid save( PolicyBackedService config ) throws SaveException, UpdateException;

    /**
     * Delete the specified policy-backed service or policy-backed service template.
     *
     * @param goid id of service to delete.  Required.
     * @throws FindException if there was a problem reading config from the database.
     * @throws DeleteException if the service could not be deleted.
     * @throws ConstraintViolationException if the service could not be deleted because it is in use.
     */
    @Secured(stereotype=DELETE_BY_ID)
    void deletePolicyBackedService( @NotNull Goid goid ) throws FindException, DeleteException, ConstraintViolationException;
}
