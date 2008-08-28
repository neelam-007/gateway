/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.management.api.node.ProcessControllerApi;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.util.Background;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.context.ApplicationEvent;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class ProcessControllerEventProxyImpl implements ProcessControllerEventProxy {
    private static final Logger logger = Logger.getLogger(ProcessControllerEventProxyImpl.class.getName());

    private final SsgConnectorManager ssgConnectorManager;
    private final ServerConfig serverConfig;

    private ProcessControllerApi processControllerApi;

    public ProcessControllerEventProxyImpl(SsgConnectorManager ssgConnectorManager, ServerConfig config) {
        this.ssgConnectorManager = ssgConnectorManager;
        this.serverConfig = config;
        getProcessControllerApi(true); // Ping on startup to log as early as possible if PC is down
        Background.scheduleRepeated(new Background.SafeTimerTask(new TimerTask() {
            public void run() {
                getProcessControllerApi(false);
            }
        }), 15634, 5339);
    }

    private synchronized ProcessControllerApi getProcessControllerApi(boolean important) {
        if (processControllerApi == null) {
            int port = serverConfig.getIntPropertyCached("processControllerPort", 8765, 60000);
            String uri = serverConfig.getPropertyCached("processControllerUri", 60000);
            if (uri == null) uri = "/services/processControllerApi";

            ProcessControllerApi api;
            try {
                final JaxWsProxyFactoryBean pfb = new JaxWsProxyFactoryBean();
/*
                final CXFBusImpl bus = new CXFBusImpl();
                bus.setExtension(new DestinationFactoryManagerImpl(), DestinationFactoryManager.class);
                cfb.setBus(bus);
*/
                pfb.setServiceClass(ProcessControllerApi.class);
                pfb.setAddress("http://localhost:" + port + uri);
                api = (ProcessControllerApi)pfb.create();
                api.ping();
                processControllerApi = api;
                if (important) {
                    logger.info("Connected to Process Controller");
                } else {
                    logger.fine("Process Controller is still alive");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to connect to Process Controller", e);
                processControllerApi = null;
            }
        }
        return processControllerApi;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent)event;
            if (SsgConnector.class.isAssignableFrom(eie.getEntityClass())) {
                final char[] ops = eie.getEntityOperations();
                final long[] oids = eie.getEntityIds();
                for (int i = 0; i < oids.length; i++) {
                    long oid = oids[i];
                    switch (ops[i]) {
                        case EntityInvalidationEvent.CREATE:
                            try {
                                SsgConnector conn = ssgConnectorManager.findByPrimaryKey(oid);
                                getProcessControllerApi(false).connectorCreated(conn);
                            } catch (FindException e) {
                                logger.log(Level.WARNING, "Unable to find recently created SsgConnector #" + oid, e);
                                continue;
                            }
                        case EntityInvalidationEvent.UPDATE:
                        case EntityInvalidationEvent.DELETE:
                    }
                }
            }
        }
    }
}
