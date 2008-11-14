package com.l7tech.server.ems.enterprise;

import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.objectmodel.ObjectModelException;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;

/**
 * 
 */
public class MappingFilter implements Filter {

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        ServletContext context = filterConfig.getServletContext();
        emsSecurityManager = (EmsSecurityManager) context.getAttribute("securityManager");
        ssgClusterManager = (SsgClusterManager) context.getAttribute("ssgClusterManager");
        userPropertyManager = (UserPropertyManager) context.getAttribute("userPropertyManager");
    }

    @Override
    public void doFilter( final ServletRequest servletRequest,
                          final ServletResponse servletResponse,
                          final FilterChain filterChain ) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        boolean handled = false;
        if ( "/Configure.html".equals(httpServletRequest.getRequestURI()) ) {
            String username = httpServletRequest.getParameter("username");
            String cluster = httpServletRequest.getParameter("cluster");

            if ( username!=null && !username.isEmpty() &&
                 cluster!=null && !cluster.isEmpty() ) {
                // attempt to add mapping
                final EmsSecurityManager.LoginInfo info = emsSecurityManager.getLoginInfo( httpServletRequest.getSession(true) );
                if ( info != null && info.getUser() != null ) {
                    try {
                        SsgCluster ssgCluster = ssgClusterManager.findByGuid( cluster );
                        if ( ssgCluster != null ) {
                            Map<String,String> props = userPropertyManager.getUserProperties( info.getUser() );
                            props.put("cluster." +  ssgCluster.getGuid() + ".trusteduser", username);
                            userPropertyManager.saveUserProperties( info.getUser(), props );
                        }
                    } catch (ObjectModelException ome) {
                        logger.log( Level.WARNING, "Error adding mapping for user.", ome );
                    }
                }

                // redirect even if mapping not performed so user does not see URL params
                handled = true;
                httpServletResponse.sendRedirect( httpServletRequest.getRequestURI() );
            }
        }

        if ( !handled ) {
            filterChain.doFilter( servletRequest, servletResponse );
        }
    }

    @Override
    public void destroy() {
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(MappingFilter.class.getName());

    private EmsSecurityManager emsSecurityManager;
    private SsgClusterManager ssgClusterManager;
    private UserPropertyManager userPropertyManager;
}
