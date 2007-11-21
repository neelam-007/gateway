package com.l7tech.server.admin;

import com.l7tech.common.util.HexUtils;

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

    protected void doGet(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        hresp.setContentType("text/html");
        hresp.setStatus(200);
        OutputStream os = hresp.getOutputStream();
        PrintStream ps = new PrintStream(os);
        // The page of Login form
        String page = new String(HexUtils.slurpUrl(SSGLoginFormServlet.class.getResource("/com/l7tech/server/resources/ssglogin.html")), "UTF-8");
        // The css style sheet for the login form
        String css = new String(HexUtils.slurpUrl(SSGLoginFormServlet.class.getResource("/com/l7tech/server/resources/ssglogin.css")), "UTF-8");
        try {
            // There are two types of login messages depend
            String loginMessage = (hreq.getAttribute(ManagerAppletFilter.RELOGIN) != null) ?
                    INCORRECT_USR_PWD_MESSAGE : NORMAL_MESSAGE;
            ps.print(MessageFormat.format(page, css, loginMessage));
        }
        finally {
           ps.close();
        }
    }

    protected void doPost(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        doGet(hreq, hresp);
    }
}