package com.l7tech.gateway.common.transport.http;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.SsgConnectorFinder;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Servlet filter that disables endpoints when not enabled.
 */
public class FeatureFilter implements Filter {

    //- PUBLIC

    @Override
    public void init( final FilterConfig filterConfig ) throws ServletException {
        final String names = filterConfig.getInitParameter( "endpoint-names" );

        this.endpoints = Collections.unmodifiableCollection( parseEndpoints( names ) );
        this.ssgConnectorFinder = (SsgConnectorFinder) filterConfig.getServletContext().getAttribute( "ssgConnectorFinder" );
    }

    /**
     * Subclasses that override this method should invoke the super method for feature filtering.
     */
    @Override
    public void doFilter( final ServletRequest servletRequest,
                          final ServletResponse servletResponse,
                          final FilterChain filterChain ) throws IOException, ServletException {
        boolean permitted = false;

        final SsgConnector connector = ssgConnectorFinder==null ? null :
                ssgConnectorFinder.findSsgConnector( servletRequest );
        
        if ( connector != null ) {
            if ( endpoints.isEmpty() ) {
                permitted = true;
            } else {
                for ( final SsgConnector.Endpoint endpoint : endpoints ) {
                    if ( connector.offersEndpoint(endpoint) ) {
                        permitted = true;
                        break;
                    }
                }
            }
        }

        if ( permitted ) {
            doFilterInternal( servletRequest, servletResponse, filterChain );
        } else {
            final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
            httpServletResponse.sendError(404);
        }
    }

    @Override
    public void destroy() {
    }

    //- PROTECTED

    /**
     * Subclasses can override this method to perform their own processing when permitted.
     */
    protected void doFilterInternal( final ServletRequest servletRequest,
                                     final ServletResponse servletResponse,
                                     final FilterChain filterChain ) throws IOException, ServletException {
        filterChain.doFilter( servletRequest, servletResponse );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( FeatureFilter.class.getName() );

    private SsgConnectorFinder ssgConnectorFinder;
    private Collection<SsgConnector.Endpoint> endpoints;

    private Collection<SsgConnector.Endpoint> parseEndpoints( final String endpointNames ) {
        final Collection<SsgConnector.Endpoint> endpoints = new HashSet<SsgConnector.Endpoint>();

        if ( endpointNames != null ) {
            for ( final String name : endpointNames.split(",") ) {
                if ( name != null ) {
                    try {
                        endpoints.add( SsgConnector.Endpoint.valueOf( name.trim() ) );
                    } catch ( IllegalArgumentException iae ) {
                        logger.warning("Ignoring invalid endpoint name '"+name+"'.");
                    }
                }
            }
        }

        return endpoints;
    }
}
