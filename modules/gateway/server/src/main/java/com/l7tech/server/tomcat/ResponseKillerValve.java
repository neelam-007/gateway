/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 11, 2005<br/>
 */
package com.l7tech.server.tomcat;

import org.apache.catalina.valves.ValveBase;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.coyote.http11.InternalOutputBuffer;
import org.apache.coyote.OutputBuffer;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>
 * An org.apache.catalina.Valve implementation that allows servlets to tell the container to
 * kill a connection without sending standard response including http status, etc.
 * <p>
 * A servlet tells this valve to kill the response connection by adding a special attribute to teh request object:
 * HttpServletRequest.setAttribute(ResponseKillerValve.ATTRIBUTE_FLAG_NAME, "foo");
 * <p>
 * This package should be jared and dropped in $TOMCAT_HOME/server/lib
 * Server.xml should refer to this valve by adding the following element as a child of Host:
 * &lt;Valve className="com.l7tech.server.tomcat.ResponseKillerValve"/&gt;
 *
 * @author flascelles@layer7-tech.com, acruise@layer7-tech.com
 */
public class ResponseKillerValve extends ValveBase {
    public static final String ATTRIBUTE_FLAG_NAME = "killmenow";
    private final Logger logger = Logger.getLogger(ResponseKillerValve.class.getName());

    public void invoke(Request req, Response res) throws IOException, ServletException {
        InternalOutputBuffer iob = null;
        try {
            OutputBuffer ob = res.getCoyoteResponse().getOutputBuffer();
            if (ob instanceof InternalOutputBuffer)
                iob = (InternalOutputBuffer)ob;
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                       "Unexpected type returned by res.getCoyoteResponse().getOutputBuffer()",
                       e);
        }

        // Let servlet do it's thing
        getNext().invoke(req, res);

        // Check for special flag
        if (req.getAttribute(ATTRIBUTE_FLAG_NAME) != null && iob != null) {
            logger.info("Killing response");
            try {
                iob.getOutputStream().close();
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                       "Error closing response output buffer",
                       e);
            }
        }
    }
}
