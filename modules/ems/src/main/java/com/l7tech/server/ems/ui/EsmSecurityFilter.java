package com.l7tech.server.ems.ui;

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
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 */
public class EsmSecurityFilter implements Filter {

    //- PUBLIC

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        ServletContext context = filterConfig.getServletContext();
        servletContext = context;
        securityManager = (EsmSecurityManager) context.getAttribute("securityManager");
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
                    if ( "POST".equals(httpServletRequest.getMethod()) && httpServletRequest.getRequestURI().equals("/logout")) {
                        securityManager.logout( httpServletRequest.getSession(true) );
                        httpServletResponse.sendRedirect( getRedirectUrl( httpServletRequest, "/" ) );
                    } else if ( "POST".equals(httpServletRequest.getMethod()) && httpServletRequest.getRequestURI().equals("/login")) {
                        servletContext.getNamedDispatcher("sessionServlet").forward( servletRequest, servletResponse );
                    } else if ( securityManager.canAccess( httpServletRequest.getSession(true), httpServletRequest ) ) {
                        if ( logger.isLoggable(Level.FINER) )
                            logger.finer("Allowing access to resource '" + httpServletRequest.getRequestURI() + "'.");
                        Subject subject = new Subject();
                        EsmSecurityManager.LoginInfo info = securityManager.getLoginInfo(httpServletRequest.getSession(true));
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
                        if ( "GET".equals(httpServletRequest.getMethod()) && httpServletRequest.getRequestURI().equals("/")) {
                            servletContext.getNamedDispatcher("sessionServlet").forward( servletRequest, servletResponse );
                        } else if ("POST".equals(httpServletRequest.getMethod()) && httpServletRequest.getHeader("Wicket-Ajax") != null) {
                            sendAjaxSessionTimeout(httpServletResponse);
                        } else {
                            httpServletResponse.sendRedirect( getRedirectUrl( httpServletRequest, "/" ) );
                        }
                    }
                } catch(IOException ioe) {
                    ioeHolder[0] = ioe;
                } catch(ServletException se) {
                    seHolder[0] = se;
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

    private static final Logger logger = Logger.getLogger(EsmSecurityFilter.class.getName());

    private ServletContext servletContext;
    private EsmSecurityManager securityManager;
    
    private String getRedirectUrl( final HttpServletRequest httpServletRequest,
                                   final String path ) throws MalformedURLException {
        return new URL(new URL(httpServletRequest.getRequestURL().toString()), path).toString();
    }

    /**
     * Send a timeout response that is handled by Wicket.
     */
    private void sendAjaxSessionTimeout(HttpServletResponse httpServletResponse) throws IOException {
        String encoding = "UTF-8";
        httpServletResponse.setCharacterEncoding(encoding);
        httpServletResponse.setContentType("text/xml; charset=" + encoding);

        // Make sure it is not cached by a client
        httpServletResponse.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
        httpServletResponse.setHeader("Cache-Control", "no-cache, must-revalidate");
        httpServletResponse.setHeader("Pragma", "no-cache");

        Writer writer = httpServletResponse.getWriter();
        writer.write("<?xml version=\"1.0\" encoding=\"");
        writer.write(encoding);
        writer.write("\"?>");
        writer.write("<ajax-response>");
        writer.write("<evaluate");
        writer.write(">");
        writer.write("<![CDATA[");
        writer.write("l7.Dialog.showSessionExpiredDialog();");
        writer.write("]]>");
        writer.write("</evaluate>");
        writer.write("</ajax-response>");
    }
}
