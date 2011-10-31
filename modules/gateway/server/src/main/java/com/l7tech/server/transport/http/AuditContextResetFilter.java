package com.l7tech.server.transport.http;

import com.l7tech.server.audit.AuditContext;
import static com.l7tech.util.ClassUtils.cast;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Clears the audit context on the way out
 */
public class AuditContextResetFilter implements Filter {

    @Override
    public void init( final FilterConfig filterConfig ) throws ServletException {
        final Option<Object> contextOption = optional( filterConfig.getServletContext().getAttribute( "auditContext" ) );
        auditContext = contextOption.map( cast( AuditContext.class ) ).some();
    }

    @Override
    public void doFilter( final ServletRequest servletRequest,
                          final ServletResponse servletResponse,
                          final FilterChain filterChain ) throws IOException, ServletException {
        try {
            filterChain.doFilter( servletRequest, servletResponse );
        } finally {
            auditContext.clear();
        }
    }

    @Override
    public void destroy() {
    }

    private AuditContext auditContext;
}
