package com.l7tech.server.ems.ui;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.IOException;
import com.l7tech.common.io.IOUtils;

/**
 * Servlet for handling logon.
 */
public class EsmSessionServlet extends HttpServlet {

    //- PUBLIC

    @Override
    public void init( final ServletConfig servletConfig ) throws ServletException {
        super.init(servletConfig);
        ServletContext context = servletConfig.getServletContext();
        securityManager = (EsmSecurityManager) context.getAttribute("securityManager");
    }

    //- PROTECTED

    @Override
    protected void doGet( final HttpServletRequest httpServletRequest,
                          final HttpServletResponse httpServletResponse ) throws ServletException, IOException {
        httpServletResponse.setContentType("text/html; charset=utf-8");
        httpServletResponse.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
        httpServletResponse.setHeader("Cache-Control", "no-cache, must-revalidate");
        httpServletResponse.setHeader("Pragma", "no-cache");
        IOUtils.copyStream( EsmSessionServlet.class.getResourceAsStream("pages/Login.html"), httpServletResponse.getOutputStream() );
    }

    @Override
    protected void doPost( final HttpServletRequest httpServletRequest,
                           final HttpServletResponse httpServletResponse ) throws ServletException, IOException {
        String username = httpServletRequest.getParameter("username");
        String password = httpServletRequest.getParameter("password");

        httpServletResponse.setContentType("text/plain; charset=utf-8");
        httpServletResponse.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
        httpServletResponse.setHeader("Cache-Control", "no-cache, must-revalidate");
        httpServletResponse.setHeader("Pragma", "no-cache");

        if ( username == null || username.isEmpty() ||
             password == null || password.isEmpty() ) {
            httpServletResponse.getWriter().print( "Incorrect username or password, please try again." );
        } else {
            try {
                if ( !login( httpServletRequest, username, password ) ) {
                    httpServletResponse.getWriter().print( "Incorrect username or password, please try again." );
                }
            } catch ( EsmSecurityManager.NotLicensedException nle ) {
                httpServletResponse.getWriter().print( "Enterprise Service Manager is not licensed." );
            }
        }
    }

    //- PRIVATE

    private EsmSecurityManager securityManager;

    /**
     * Perform login
     */
    private boolean login( final HttpServletRequest request,
                           final String username,
                           final String password ) {
        return securityManager.login( request.getSession(true), username, password );
    }

}
