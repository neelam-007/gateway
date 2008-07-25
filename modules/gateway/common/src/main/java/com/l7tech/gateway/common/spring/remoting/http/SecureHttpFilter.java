package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.spring.remoting.RemotingProvider;
//import com.l7tech.identity.UserBean;
//import org.springframework.context.ApplicationContext;
//import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.security.auth.Subject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Security filter for the manager remote interface.
 *
 * <p>This filter uses an id passed on the URL to run requests as a
 * particular subject (if authenticated).</p>
 *
 * <p>This filter does not enforce any security, that is done by the
 * {@link SecureRemoteInvocationExecutor} (for is authenticated checking) and
 * at various points in the code by
 * {@link <code>com.l7tech.server.security.rbac.SecuredMethodInterceptor</code>}.
 * </p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SecureHttpFilter implements Filter {

    //- PUBLIC

    /**
     *
     */
    public SecureHttpFilter() {
    }

    /**
     *
     */
    public void init( final FilterConfig filterConfig ) throws ServletException {
        String remotingProviderName = filterConfig.getInitParameter( PARAM_PROV_NAME );
        if ( remotingProviderName == null ) {
            remotingProviderName = DEFAULT_PROV_NAME;
        }
        remotingProvider = (RemotingProvider) filterConfig.getServletContext().getAttribute( remotingProviderName );
    }

    /**
     *
     */
    public void destroy() {
    }

    /**
     *
     */
    public void doFilter(final ServletRequest servletRequest,
                         final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {

        final HttpServletRequest hsr = (HttpServletRequest) servletRequest;

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Admin access for URI '" + hsr.getRequestURI() + "'.");
        }

        // Create the subject to run as
        Subject subject = new Subject();

        // Resume session if available
        String cookie = hsr.getHeader( SESSION_ID_HEADER );
        if (cookie == null) { // check for pre 3.6.5 URL parameter sessionId
            cookie = servletRequest.getParameter( SESSION_ID_PARAM );
        }
        if (cookie != null && remotingProvider != null) {
            remotingProvider.setPrincipalsForSubject(cookie, subject);
        }

        // Pass on down the chain with the auth'd user and remote host set(if any)
        try {
            Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    final IOException[] ioeHolder = new IOException[1];
                    final ServletException[] seHolder = new ServletException[1];
                    RemoteUtils.runWithConnectionInfo(servletRequest.getRemoteAddr(), hsr, new Runnable(){
                        public void run() {
                            try {
                                filterChain.doFilter(servletRequest, servletResponse);
                            } catch(IOException ioe) {
                                ioeHolder[0] = ioe;
                            } catch(ServletException se) {
                                seHolder[0] = se;
                            }
                        }
                    });

                    // rethrow exceptions 
                    if (ioeHolder[0] != null) throw ioeHolder[0];
                    if (seHolder[0] != null) throw seHolder[0];

                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof ServletException) {
                throw (ServletException) e;
            } else {
                throw new RuntimeException("Unexpected exception from doFilter", pae);
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SecureHttpFilter.class.getName());

    private static final String PARAM_PROV_NAME = "remotingProviderName";
    private static final String DEFAULT_PROV_NAME = "remotingProvider";

    private static final String SESSION_ID_HEADER = "X-Layer7-SessionId";
    private static final String SESSION_ID_PARAM = "sessionId";

    private RemotingProvider remotingProvider;
}
