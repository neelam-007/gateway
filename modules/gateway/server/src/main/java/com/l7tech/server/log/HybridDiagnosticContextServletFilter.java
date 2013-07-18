package com.l7tech.server.log;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.util.ConfigFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Servlet filter that sets diagnostic information.
 */
public class HybridDiagnosticContextServletFilter implements Filter {

    //- PUBLIC

    @Override
    public void init( final FilterConfig filterConfig ) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter( final ServletRequest servletRequest,
                          final ServletResponse servletResponse,
                          final FilterChain filterChain ) throws IOException, ServletException {
        if ( enabled ) {
            // ensure clean
            HybridDiagnosticContext.reset();

            // populate connector / IP info
            final SsgConnector connector = HttpTransportModule.getConnector( servletRequest );
            if ( connector != null ) {
                HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connector.getGoid().toString());
            }
            HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.CLIENT_IP, servletRequest.getRemoteAddr() );
        }

        try {
            filterChain.doFilter( servletRequest, servletResponse );
        } finally {
            if ( enabled ) {
                // clean up on the way out
                HybridDiagnosticContext.reset();
            }
        }
    }

    //- PRIVATE

    private final boolean enabled = ConfigFactory.getBooleanProperty( "com.l7tech.server.log.servletFilterEnabled", true );
}
