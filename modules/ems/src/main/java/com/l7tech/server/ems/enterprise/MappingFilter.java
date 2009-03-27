package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.ems.gateway.GatewayRegistrationEvent;
import com.l7tech.server.ems.ui.EsmSecurityManager;
import com.l7tech.server.ems.user.UserPropertyManager;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public class MappingFilter implements Filter {

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        ServletContext context = filterConfig.getServletContext();
        emsSecurityManager = (EsmSecurityManager) context.getAttribute("securityManager");
        ssgClusterManager = (SsgClusterManager) context.getAttribute("ssgClusterManager");
        userPropertyManager = (UserPropertyManager) context.getAttribute("userPropertyManager");
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
    }

    @Override
    public void doFilter( final ServletRequest servletRequest,
                          final ServletResponse servletResponse,
                          final FilterChain filterChain ) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        boolean handled = false;
        final String requestURI = httpServletRequest.getRequestURI();
        if ( ("/Configure.html".equals(requestURI) ||
              "/StandardReports.html".equals(requestURI) ||
              "/PolicyMigration.html".equals(requestURI)) &&
             httpServletRequest.getMethod().equalsIgnoreCase("get") ) {
            String username = httpServletRequest.getParameter("username");
            String clusterGuid = httpServletRequest.getParameter("clusterguid");

            if ( username!=null && !username.isEmpty() &&
                 clusterGuid !=null && !clusterGuid.isEmpty() ) {
                // attempt to add mapping
                final EsmSecurityManager.LoginInfo info = emsSecurityManager.getLoginInfo( httpServletRequest.getSession(true) );
                if ( info != null && info.getUser() != null ) {
                    try {
                        SsgCluster ssgCluster = ssgClusterManager.findByGuid(clusterGuid);
                        if ( ssgCluster != null ) {
                            Map<String,String> props = userPropertyManager.getUserProperties( info.getUser() );
                            props.put("cluster." +  ssgCluster.getGuid() + ".trusteduser", username);
                            userPropertyManager.saveUserProperties( info.getUser(), props );
                            if (applicationContext!=null) {
                                applicationContext.publishEvent(new GatewayRegistrationEvent(this));
                                try {
                                    Thread.sleep(200); // pause to allow for status update
                                } catch ( InterruptedException ie ) {
                                    Thread.currentThread().interrupt();
                                }
                            }

                        } else {
                            logger.warning("Cluster '"+clusterGuid+"' not found when adding user mapping.");
                        }
                    } catch (ObjectModelException ome) {
                        logger.log( Level.WARNING, "Error adding mapping for user.", ome );
                    }
                }
            }

            if ( clusterGuid != null ) {
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

    private EsmSecurityManager emsSecurityManager;
    private SsgClusterManager ssgClusterManager;
    private UserPropertyManager userPropertyManager;
    private ApplicationContext applicationContext;
}
