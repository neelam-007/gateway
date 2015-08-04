package com.l7tech.server.polback;

import com.l7tech.gateway.common.admin.PolicyBackedServiceAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class PolicyBackedServiceAdminImpl implements PolicyBackedServiceAdmin {
    private static final Logger logger = Logger.getLogger( PolicyBackedServiceAdminImpl.class.getName() );

    @Inject
    @Named( "policyBackedServiceManager" )
    private PolicyBackedServiceManager policyBackedServiceManager;

    @Inject
    @Named( "policyBackedServiceRegistry" )
    PolicyBackedServiceRegistry policyBackedServiceRegistry;

    @NotNull
    @Override
    public Collection<PolicyBackedService> findAll() throws FindException {
        return policyBackedServiceManager.findAll();
    }

    @NotNull
    @Override
    public Collection<PolicyBackedService> findAllForInterface( @NotNull final String interfaceClassName ) throws FindException {
        return Functions.grep( policyBackedServiceManager.findAll(), new Functions.Unary<Boolean, PolicyBackedService>() {
            @Override
            public Boolean call( PolicyBackedService policyBackedService ) {
                return policyBackedService.getServiceInterfaceName().equals( interfaceClassName );
            }
        } );
    }

    @NotNull
    @Override
    public Collection<String> findAllTemplateInterfaceNames() {
        return policyBackedServiceRegistry.getPolicyBackedServiceTemplates();
    }

    @NotNull
    @Override
    public Collection<EncapsulatedAssertionConfig> getInterfaceDescription( String interfaceName ) throws ObjectNotFoundException {
        return policyBackedServiceRegistry.getTemplateOperations( interfaceName );
    }

    @Nullable
    @Override
    public PolicyBackedService findByPrimaryKey( Goid goid ) throws FindException {
        return policyBackedServiceManager.findByPrimaryKey( goid );
    }

    @Override
    public Goid save( PolicyBackedService config ) throws SaveException, UpdateException {
        Goid goid;

        if ( config.isUnsaved() ) {
            goid = policyBackedServiceManager.save( config );
        } else {
            policyBackedServiceManager.update( config );
            goid = config.getGoid();
        }

        return goid;
    }

    @Override
    public void deletePolicyBackedService( @NotNull Goid goid ) throws FindException, DeleteException, ConstraintViolationException {
        policyBackedServiceManager.delete(goid);
    }

    @Override
    public boolean isAnyMultiMethodPolicyBackedServiceRegistered() {
        boolean sawMultiMethod = false;

        Set<String> templates = policyBackedServiceRegistry.getPolicyBackedServiceTemplates();
        for ( String template : templates ) {
            try {
                List<EncapsulatedAssertionConfig> methods = policyBackedServiceRegistry.getTemplateOperations( template );
                if ( methods.size() > 1 ) {
                    sawMultiMethod = true;
                    break;
                }
            } catch ( ObjectNotFoundException e ) {
                // Shouldn't happen
                logger.log( Level.WARNING, "Unable to find supposedly-registered policy backed service description: " + ExceptionUtils.getMessage( e ),
                        ExceptionUtils.getDebugException( e ) );
            }
        }
        return sawMultiMethod;
    }
}
