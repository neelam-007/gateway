package com.l7tech.server.ems;

import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.util.ExceptionUtils;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.security.auth.Subject;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

/**
 *
 */
public class EmsSecurityFilter implements Filter {

    //- PUBLIC

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        ServletContext context = filterConfig.getServletContext();
        securityManager = (EmsSecurityManager) context.getAttribute("securityManager");
    }

    @Override
    public void destroy() {        
    }

    @Override
    public void doFilter( final ServletRequest servletRequest,
                          final ServletResponse servletResponse,
                          final FilterChain filterChain ) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        final IOException[] ioeHolder = new IOException[1];
        final ServletException[] seHolder = new ServletException[1];
        RemoteUtils.runWithConnectionInfo(servletRequest.getRemoteAddr(), httpServletRequest, new Runnable(){
            @Override
            public void run() {
                try {
                    if ( securityManager.canAccess( httpServletRequest.getSession(true), httpServletRequest ) ) {
                        if ( logger.isLoggable(Level.FINER) )
                            logger.finer("Allowing access to resource '" + httpServletRequest.getRequestURI() + "'.");
                        Subject subject = new Subject();
                        EmsSecurityManager.LoginInfo info = securityManager.getLoginInfo(httpServletRequest.getSession(true));
                        if ( info != null ) {
                            subject.getPrincipals().add( info.getUser() );
                        }
                        Subject.doAs(subject, new PrivilegedExceptionAction<Object>(){
                            @Override
                            public Object run() throws Exception {
                                filterChain.doFilter( servletRequest, servletResponse );
                                return null;
                            }
                        });
                    } else {
                        logger.info("Forbid access to resource : '" + httpServletRequest.getRequestURI() + "'." );
                        httpServletResponse.sendRedirect("/Login.html");
                    }
                } catch(IOException ioe) {
                    ioeHolder[0] = ioe;
                } catch (PrivilegedActionException pae) {
                    Throwable exception = pae.getCause();
                    if (exception instanceof IOException) {
                        ioeHolder[0] = (IOException) exception;
                    } else if (exception instanceof ServletException) {
                        seHolder[0] = (ServletException) exception;
                    } else {
                        throw ExceptionUtils.wrap(exception);
                    }
                }
            }
        });

        // rethrow exceptions
        if (ioeHolder[0] != null) throw ioeHolder[0];
        if (seHolder[0] != null) throw seHolder[0];
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(EmsSecurityFilter.class.getName());

    private EmsSecurityManager securityManager;
    
}
