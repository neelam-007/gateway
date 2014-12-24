package com.l7tech.server.polback;

import com.l7tech.gateway.common.admin.PolicyBackedServiceAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;

/**
 *
 */
public class PolicyBackedServiceAdminImpl implements PolicyBackedServiceAdmin {
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
}
