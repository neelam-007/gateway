package com.l7tech.policy.server;

import com.l7tech.service.PublishedService;

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
 * The following parameters can be passed to resolve the PublishedService:
 * serviceoid : the internal object identifier of the PublishedService. if specified, this parameter is sufficient to
 *              retrieve the policy
 * urn : the urn of the service. if more than one service have the same urn, at least one more paramater will be
 *       necessary
 * soapaction : the soapaction of the PublishedService
 *
 * Pass the parameters as part of the url as in the samples below
 * http://localhost:8080/ssg/policy/disco.modulator?serviceoid=666
 * http://localhost:8080/ssg/policy/disco.modulator?urn=blah&soapaction=ugh
 *
 */
public class PolicyServlet extends HttpServlet {

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        // GET THE PARAMETERS PASSED
        String str_oid = httpServletRequest.getParameter("serviceoid");
        String urnParameter = httpServletRequest.getParameter("urn");
        String soapactionParamater = httpServletRequest.getParameter("soapaction");
        // RESOLVE THE SERVICE
        PublishedService targetService = null;
        if (str_oid != null && str_oid.length() > 0) targetService = resolveService(Long.parseLong(str_oid));
        else targetService = resolveService(urnParameter, soapactionParamater);
        // OUTPUT THE POLICY
        outputPublishedServicePolicy(targetService, httpServletResponse);
    }

    private PublishedService resolveService(long oid) {
        // todo
        return null;
    }

    private PublishedService resolveService(String urn, String soapAction) {
        // todo
        return null;
    }

    private void outputPublishedServicePolicy(PublishedService service, HttpServletResponse response) throws IOException {
        if (service == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "ERROR cannot resolve target service");
            return;
        } else {
            // todo
            response.getOutputStream().println("todo");
        }
    }
}
