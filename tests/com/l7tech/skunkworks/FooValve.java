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
 */
public class FooValve extends ValveBase {
    public void invoke(Request request, Response response) throws IOException, ServletException {
        InternalOutputBuffer iob = (InternalOutputBuffer)response.getCoyoteResponse().getOutputBuffer();
        iob.getOutputStream().close();
    }
}
