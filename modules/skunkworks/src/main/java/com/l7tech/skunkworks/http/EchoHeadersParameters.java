package com.l7tech.skunkworks.http;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Enumeration;

/**
 * a servlet which will return information about incoming requests
 * specifically, the http headers and parameters received.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 11, 2007<br/>
 */
public class EchoHeadersParameters extends HttpServlet {
    private void outputResponse(String method, HttpServletRequest req, HttpServletResponse res) throws IOException {

        // add a couple headers to response
        res.setHeader("rehr1", "" + System.currentTimeMillis());
        res.setHeader("rehr2", "" + System.currentTimeMillis());

        StringBuffer payload = new StringBuffer();
        payload.append("<RequestReceived>");
        payload.append("\n\t<Method>");
        payload.append(method);
        payload.append("</Method>");

        // output headers
        payload.append("\n\n\t<Headers>");
        for (Enumeration i = req.getHeaderNames(); i.hasMoreElements();) {
            String hrName = (String)i.nextElement();
            for (Enumeration j = req.getHeaders(hrName); j.hasMoreElements();) {
                String hrVal = (String)j.nextElement();
                payload.append("\n\t\t<Header>");
                payload.append(hrName);
                payload.append(" = ");
                payload.append(hrVal);
                payload.append("</Header>");
            }
        }
        payload.append("\n\t</Headers>");

        // output parameters
        payload.append("\n\n\t<Parameters>");
        for (Enumeration i = req.getParameterNames(); i.hasMoreElements();) {
            String paramName = (String)i.nextElement();
            String[] vals = req.getParameterValues(paramName);
            for (String val : vals) {
                payload.append("\n\t\t<Param>");
                payload.append(paramName);
                payload.append(" = ");
                payload.append(val);
                payload.append("</Param>");
            }
        }
        payload.append("\n\t</Parameters>");

        payload.append("\n</RequestReceived>");

        res.setContentType("text/xml");
        ServletOutputStream sos = res.getOutputStream();
        sos.write(payload.toString().getBytes());
        sos.close();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        outputResponse("GET", req, res);
    }
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        outputResponse("POST", req, res);
    }
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        outputResponse("PUT", req, res);
    }
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        outputResponse("DELETE", req, res);
    }
}
