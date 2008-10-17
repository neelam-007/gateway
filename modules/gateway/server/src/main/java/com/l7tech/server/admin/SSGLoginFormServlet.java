package com.l7tech.server.admin;

import com.l7tech.common.io.IOUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;

/**
 * @author: ghuang
 * Date: Oct 15, 2007
 */
public class SSGLoginFormServlet extends HttpServlet {
    private static final String NORMAL_MESSAGE = "Connecting to the SecureSpan Gateway";
    private static final String INCORRECT_USR_PWD_MESSAGE = "The user name or password is incorrect.  Try again.";
    private static final String LOCK_OUT_MESSAGE = "Maximum login attempts exceeded, please try again later.";
    private static final String CREDS_EXPIRED_MESSAGE = "Password expired, please change your password.";
    private static final String LOGIN_PAGE = "loginPage";

    protected void doGet(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        hresp.setContentType("text/html");
        hresp.setStatus(200);
        OutputStream os = hresp.getOutputStream();
        PrintStream ps = new PrintStream(os);
        // The page of Login form
        String page = new String( IOUtils.slurpUrl(SSGLoginFormServlet.class.getResource(getServletConfig().getInitParameter(LOGIN_PAGE))), "UTF-8");
        // The css style sheet for the login form
        String css = new String(IOUtils.slurpUrl(SSGLoginFormServlet.class.getResource("/com/l7tech/server/resources/ssglogin.css")), "UTF-8");
        try {
            String username = "";
            // There are two types of login messages depend
            String loginMessage = NORMAL_MESSAGE;
            if ( hreq.getAttribute(ManagerAppletFilter.RELOGIN) != null ) {
                String reLogin = (String) hreq.getAttribute(ManagerAppletFilter.RELOGIN);
                if ( reLogin.equalsIgnoreCase("YES") ) {
                    loginMessage = INCORRECT_USR_PWD_MESSAGE;
                } else {
                    loginMessage = LOCK_OUT_MESSAGE;
                }
            } else if (hreq.getAttribute(ManagerAppletFilter.CREDS_EXPIRED) != null ||
                    hreq.getAttribute(ManagerAppletFilter.INVALID_PASSWORD) != null) {
                //we need to handle for cases when the password has expired
                String credsExpired = (String) hreq.getAttribute(ManagerAppletFilter.CREDS_EXPIRED);    //expired password
                String invalidPassword = (String) hreq.getAttribute(ManagerAppletFilter.INVALID_PASSWORD);  //invalid password when attempt to change the password
                if (credsExpired != null && credsExpired.equalsIgnoreCase("YES")) {
                    loginMessage = CREDS_EXPIRED_MESSAGE;                                                                       
                } else if (invalidPassword != null && invalidPassword.equalsIgnoreCase("YES")) {
                    loginMessage = (String) hreq.getAttribute(ManagerAppletFilter.INVALID_PASSWORD_MESSAGE);
                }
                //fill in the username into the field
                username = (String) hreq.getAttribute(ManagerAppletFilter.USERNAME);
            }
            ps.print(MessageFormat.format(page, css, loginMessage, username));
        }
        finally {
           ps.close();
        }
    }

    protected void doPost(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        doGet(hreq, hresp);
    }
}