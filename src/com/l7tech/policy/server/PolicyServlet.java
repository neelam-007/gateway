package com.l7tech.policy.server;

import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.service.PublishedService;
import com.l7tech.util.Locator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        try {
            return getServiceManagerAndBeginTransaction().findByPrimaryKey(oid);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
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
            response.setContentType("text/xml; charset=utf-8");
            response.getOutputStream().println(service.getPolicyXml());
        }
    }

    private com.l7tech.service.ServiceManager getServiceManagerAndBeginTransaction() throws java.sql.SQLException, TransactionException {
        if (serviceManagerInstance == null){
            initialiseServiceManager();
        }
        PersistenceContext.getCurrent().beginTransaction();
        return serviceManagerInstance;
    }

    private void endTransaction() throws java.sql.SQLException, TransactionException {
        PersistenceContext.getCurrent().commitTransaction();
    }

    private synchronized void initialiseServiceManager() throws ClassCastException, RuntimeException {
        serviceManagerInstance = (com.l7tech.service.ServiceManager)Locator.getDefault().lookup(com.l7tech.service.ServiceManager.class);
        if (serviceManagerInstance == null) throw new RuntimeException("Cannot instantiate the ServiceManager");
    }

    private com.l7tech.service.ServiceManager serviceManagerInstance = null;
}
