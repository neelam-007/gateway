package com.l7tech.skunkworks;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.coyote.http11.InternalOutputBuffer;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * This is a proof-of-concept Tomcat valve that kills every request without sending any response. :)
 * <p>
 * Valve classes have to be in $TOMCAT_HOME/server/classes or $TOMCAT_HOME/server/lib.
 * <p>
 * Install by adding &lt;Valve className="com.l7tech.skunkworks.FooValve"/&gt; as a child of //Host in $TOMCAT_HOME/conf/server.xml.
 *
 * Servlet tell this valve to kill the response connection by adding a special attribute to teh request object:
 * HttpServletRequest.setAttribute("killmenow", "please");
 *
 * In order for this to work properly, the HttpServletResponse object should be left untouched by the servlet.
 */
public class FooValve extends ValveBase {
    public void invoke(Request req, Response res) throws IOException, ServletException {
        InternalOutputBuffer iob = (InternalOutputBuffer)res.getCoyoteResponse().getOutputBuffer();
        // Passing to next valve
        getNext().invoke(req, res);
        // Checking for killmenow special attribute
        if (req.getAttribute("killmenow") != null) {
            System.out.println("Killing response");
            iob.getOutputStream().close();
        }
    }
}
