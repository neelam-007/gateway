package com.l7tech.policy.server;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 11, 2003
 *
 * This servlet returns policy documents (type xml).
 * Pass the parameters "urn" and "soapaction" to resolve the policy documents such as the sample below
 * http://localhost:8080/ssg/policy/disco.modulator?urn=blah&soapaction=ugh
 */
public class PolicyServlet extends HttpServlet {

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        String urnParameter = httpServletRequest.getParameter("urn");
        String soapactionParamater = httpServletRequest.getParameter("soapaction");

        // todo, remove this debug stuff
        httpServletResponse.getOutputStream().println("urnParameter = " + urnParameter);
        httpServletResponse.getOutputStream().println("soapactionParamater = " + soapactionParamater);
    }
}
