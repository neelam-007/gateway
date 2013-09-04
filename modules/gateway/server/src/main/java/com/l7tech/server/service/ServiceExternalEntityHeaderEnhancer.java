package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.server.cluster.ExternalEntityHeaderEnhancer;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;
import com.l7tech.wsdl.Wsdl;

import javax.inject.Inject;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ExternalEntityHeaderEnhancer that adds operations for SOAP services.
 */
public class ServiceExternalEntityHeaderEnhancer implements ExternalEntityHeaderEnhancer {

    private static final Logger logger = Logger.getLogger( ServiceExternalEntityHeaderEnhancer.class.getName() );

    @Inject
    private ServiceCache serviceCache;

    public String[] getOperations( final ExternalEntityHeader header  ) {
        String[] operations = null;

        final PublishedService service;
        if ( header.getType() == EntityType.SERVICE ) {
            service = serviceCache.getCachedService(header.getGoid());
        } else if ( header.getType() == EntityType.SERVICE_ALIAS && ValidationUtils.isValidLong( header.getProperty( "Alias Of" ), false, Long.MIN_VALUE, Long.MAX_VALUE) ) {
            service = serviceCache.getCachedService(GoidUpgradeMapper.mapId(EntityType.SERVICE, header.getProperty("Alias Of")));
        } else {
            service = null;
        }

        if ( service != null ) {
            try {
                // Get operations
                final Wsdl wsdl = service.parsedWsdl();
                if ( wsdl != null ) {
                    final Set<String> wsdlOperations = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

                    wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
                    for ( final BindingOperation operation : wsdl.getBindingOperations() ) {
                        wsdlOperations.add(operation.getName());
                    }

                    if ( !wsdlOperations.isEmpty() ) {
                        operations = wsdlOperations.toArray( new String[ wsdlOperations.size() ] );
                    }
                }
            } catch ( WSDLException we ) {
                // ignore WSDL error, skip operations
                if ( logger.isLoggable( Level.FINE ) ) {
                    logger.log( Level.FINE, "Error processing WSDL for service '"+service.getId()+"', '"+ ExceptionUtils.getMessage( we )+"'..", ExceptionUtils.getDebugException(we) );
                }
            }
        }

        return operations;
    }

    @Override
    public void enhance( final ExternalEntityHeader header ) {
        String[] operations = getOperations( header );
        if ( operations != null ) {
            // Set operations in the published-service entityInfo
            header.setProperty( "WSDL Operations", TextUtils.join( ",", operations ).toString() );
        }
    }
}
