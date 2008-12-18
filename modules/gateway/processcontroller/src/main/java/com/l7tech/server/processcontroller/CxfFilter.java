package com.l7tech.server.processcontroller;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import java.io.IOException;

/**
 * See bug 6300 for why we have a global lock for CXF
 */
public class CxfFilter implements Filter {

    //- PUBLIC

    @Override
    public void init( final FilterConfig filterConfig ) throws ServletException {

    }

    @Override
    public void doFilter( final ServletRequest servletRequest,
                          final ServletResponse servletResponse,
                          final FilterChain filterChain ) throws IOException, ServletException {
        synchronized( sync ) {
            filterChain.doFilter( servletRequest, servletResponse );
        }
    }

    @Override
    public void destroy() {
    }

    //- PRIVATE

    private static final Object sync = new Object();

}