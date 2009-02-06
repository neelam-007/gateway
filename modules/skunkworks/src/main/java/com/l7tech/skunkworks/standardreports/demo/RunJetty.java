/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 15, 2008
 * Time: 4:18:48 PM
 */
package com.l7tech.skunkworks.standardreports.demo;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.bio.SocketConnector;

public class RunJetty {

    private static final int port = 8152;

    public static void main(String [] args) throws Exception{
        Server server = new Server();
        SocketConnector connector = new SocketConnector();
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});

        Handler fillHandler = new FillReportHandler();
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/demo");
        ReportOutputHandler reportOutputHandler = new ReportOutputHandler("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/build/demo_output");
        ResourceHandler outputHandlerStatic = new ResourceHandler();
        outputHandlerStatic.setResourceBase("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/build/demo_output");

        HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[]{fillHandler, resourceHandler, reportOutputHandler, outputHandlerStatic, new DefaultHandler()});
        server.addHandler(handlerList);
        try{
            server.start();
        }catch(Exception ex){
            ex.printStackTrace();
            throw new RuntimeException("Cannot start Jetty INSTANCE on port "+ port+": " + ex.getMessage());
        }

    }
}
