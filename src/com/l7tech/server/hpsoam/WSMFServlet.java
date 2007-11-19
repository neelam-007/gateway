package com.l7tech.server.hpsoam;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.util.HexUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Servlet implementing the WSMF web service for the HP SOA Manager interop.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 6, 2007<br/>
 */
public class WSMFServlet extends HttpServlet {

    private static final String PLACEHOLDER = "^^^INSERT_METHOD_HOST_PORT_HERE^^^";
    private static final Logger logger = Logger.getLogger(WSMFServlet.class.getName());
    private WebApplicationContext applicationContext;
    private WSMFService service;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        service = (WSMFService)applicationContext.getBean("wsmfService");
    }

    public static String getFullURL(HttpServletRequest req) {
        StringBuffer buf = req.getRequestURL();
        String qs = req.getQueryString();
        if (qs != null) {
            buf.append(qs);
        }
        return buf.toString();
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!WSMFService.isEnabled()) {
            returnError(res, "Service disabled", HttpConstants.STATUS_SERVICE_UNAVAILABLE);
            return;
        }

        // See if this is a request for a Service MO WSDL
        String incomingURL = getFullURL(req);
        Matcher matcher = WSMFService.serviceoidPattern.matcher(incomingURL);
        String tmp;
        if (!matcher.matches()) {
            tmp = null;
        } else {
            tmp = matcher.group(1);
        }

        if (tmp != null && tmp.length() > 0) {
            String wsdl = service.handleMOSpecificGET(incomingURL, req);
            res.getOutputStream().write(wsdl.getBytes());
        } else {
            // GET are expected when HPSPAM NS is retrieving a WSDL or another related resource
            String name = resourceName(req);
            try {
                byte[] response = getResourceAndLocalise(req, name);
                if (name.endsWith(".xsd") || name.endsWith(".wsdl")) {
                    res.setContentType("text/xml");
                } else {
                    logger.info("unexpected extension in " + name);
                }
                res.getOutputStream().write(response);
            } catch (IOException e) {
                res.getOutputStream().write(("Resource " + name + " not found").getBytes());
                logger.log(Level.INFO, "Error outputing resource named " + name, e);
            }
        }
        res.getOutputStream().close();
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!WSMFService.isEnabled()) {
            returnError(res, "Service disabled", HttpConstants.STATUS_SERVICE_UNAVAILABLE);
            return;
        }

        String payload = new String(HexUtils.slurpStream(req.getInputStream()));
        String incomingURL = getFullURL(req);
        try {
            String response = service.respondTo(payload, req);
            if (response == null) {
                returnError(res, "not yet implemented", 500);
            } else {
                returnXML(res, response);
                // todo, remove this debugging info once this all works
                // logger.fine("POST handled : " + payload + " Returned: " + response);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Invalid request " + payload);
            returnError(res, "invalid request " + payload, 500);
        }
    }

    private void returnError(HttpServletResponse res, String error, int status) throws IOException {
        res.setStatus(status);
        res.setContentType("text/plain");
        res.getOutputStream().write(error.getBytes());
        res.getOutputStream().close();
    }

    private void returnXML(HttpServletResponse res, String response) throws IOException {
        res.setStatus(200);
        res.setContentType("text/xml");
        res.getOutputStream().write(response.getBytes());
        res.getOutputStream().close();
    }

    private String resourceName(HttpServletRequest req) {
        String resName = req.getRequestURI();
        return resName.substring(resName.lastIndexOf('/')+1);
    }

    private byte[] getResourceAndLocalise(HttpServletRequest req, String resName) throws IOException {
        InputStream is = getInputStreamFromCP(resName);
        String beforeEdits = new String(HexUtils.slurpStream(is));
        return beforeEdits.replace(PLACEHOLDER, getHostPort(req)).getBytes();
    }

    private String getHostPort(HttpServletRequest req) {
        String output = req.getRequestURL().toString();
        int indexofthing = output.indexOf("//");
        int end = output.indexOf("/", indexofthing+2);
        return output.substring(0, end);
    }

    private static InputStream getInputStreamFromCP(String resourceToRead) throws FileNotFoundException {
        ClassLoader cl = WSMFServlet.class.getClassLoader();
        String pathToLoad = "com/l7tech/server/hpsoam/resources/" + resourceToRead;
        InputStream i = cl.getResourceAsStream(pathToLoad);
        if (i == null) {
            throw new FileNotFoundException(pathToLoad);
        }
        return i;
    }
}