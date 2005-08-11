package com.l7tech.skunkworks;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.coyote.http11.InternalOutputBuffer;

import javax.servlet.ServletException;
import java.io.IOException;

public class FooValve extends ValveBase {
    public void invoke(Request request, Response response) throws IOException, ServletException {
        InternalOutputBuffer iob = (InternalOutputBuffer)response.getCoyoteResponse().getOutputBuffer();
        iob.getOutputStream().close();
    }
}
