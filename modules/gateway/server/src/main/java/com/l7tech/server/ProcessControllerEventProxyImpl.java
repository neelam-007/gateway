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
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.springframework.context.ApplicationEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.Resource;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.TimerTask;
import java.util.List;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import java.net.ConnectException;

/** @author alex */
public class ProcessControllerEventProxyImpl implements ProcessControllerEventProxy, InitializingBean, DisposableBean {
    private static final Logger logger = Logger.getLogger(ProcessControllerEventProxyImpl.class.getName());
    private static final String DEFAULT_SSL_CIPHERS = "TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_256_CBC_SHA";
    private static final String PROP_SSL_CIPHERS = "com.l7tech.server.pc.sslciphers";

    @Resource
    private SsgConnectorManager ssgConnectorManager;

    @Resource
    private ServerConfig serverConfig;

    /** Access must be synchronized */
    private ProcessControllerApi processControllerApi;

    private final TimerTask task = new TimerTask() {
        public void run() {
            if (isProcessControllerPresent()) getProcessControllerApi(false);
        }
    };

    public void afterPropertiesSet() throws Exception {
        start();
    }

    public void destroy() throws Exception {
        stop();
    }

    private void start() {
        try {
            if (isProcessControllerPresent())
                getProcessControllerApi(true); // Ping on startup to log as early as possible if PC is down
        } catch ( Exception e ) {
            // CXF throws RuntimeException
            logger.log( Level.WARNING, "Error pinging process controller.", e);
        }

        // Note that timer task is spawned unconditionally so that if the PC becomes enabled at runtime we'll know
        Background.scheduleRepeated(new Background.SafeTimerTask(task), 15634, 5339);
    }

    private void stop() {
        task.cancel();
    }

    private boolean isProcessControllerPresent() {
        return serverConfig.getBooleanPropertyCached(ServerConfig.PARAM_PROCESS_CONTROLLER_PRESENT, false, 30000);
    }

    private synchronized ProcessControllerApi getProcessControllerApi(boolean important) {
        if (processControllerApi == null) {
            int port = serverConfig.getIntPropertyCached("processControllerPort", 8765, 60000);
            String uri = serverConfig.getPropertyCached("processControllerUri", 60000);
            if (uri == null) uri = "/services/processControllerApi";

            ProcessControllerApi api;
            try {
                final JaxWsProxyFactoryBean pfb = makeSslStub("https://localhost:" + port + uri, ProcessControllerApi.class);
                api = (ProcessControllerApi)pfb.create();
                api.ping();
                processControllerApi = api;
                if (important) {
                    logger.info("Connected to Process Controller");
                } else {
                    logger.fine("Process Controller is still alive");
                }
            } catch (Exception e) {
                if ( ExceptionUtils.causedBy( e, ConnectException.class ) ) {
                    logger.log(Level.WARNING, "Unable to connect to Process Controller");
                } else {
                    logger.log(Level.WARNING, "Error connecting to Process Controller", e);
                }
                processControllerApi = null;
            }
        }
        return processControllerApi;
    }

    // TODO make this a util method
    private static JaxWsProxyFactoryBean makeSslStub(String url, final Class<ProcessControllerApi> apiClass) {
        final JaxWsProxyFactoryBean pfb = new JaxWsProxyFactoryBean(new JaxWsClientFactoryBean());
        pfb.setServiceClass(apiClass);
        pfb.setAddress(url);
        final Client c = pfb.getClientFactoryBean().create();
        final HTTPConduit httpConduit = (HTTPConduit)c.getConduit();
        httpConduit.setTlsClientParameters(new TLSClientParameters() {
            @Override
            public List<String> getCipherSuites() {
                return Arrays.asList(SyspropUtil.getString(PROP_SSL_CIPHERS,DEFAULT_SSL_CIPHERS).split(","));
            }

            public boolean isDisableCNCheck() {
                return true;
            }

            // TODO should we explicitly trust the PC cert?
            public TrustManager[] getTrustManagers() {
                return new TrustManager[] { new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {}
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {}
                    public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                }};
            }
        });
        return pfb;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (!isProcessControllerPresent()) return;

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
                                ProcessControllerApi api = getProcessControllerApi(false);
                                if ( api != null ) {
                                    api.connectorCreated(conn);
                                }
                            } catch (FindException e) {
                                logger.log(Level.WARNING, "Unable to find recently created SsgConnector #" + oid, e);
                            } catch (SOAPFaultException sfe) {
                                if ( ExceptionUtils.causedBy( sfe, ConnectException.class ) ) {
                                    logger.log(Level.WARNING, "Connection error while notifiying process controller of new connector " + oid);
                                } else {
                                    logger.log(Level.WARNING, "Error while notifiying process controller of new connector " + oid, sfe);
                                }
                            }
                            break;
                        case EntityInvalidationEvent.UPDATE: // TODO
                            break;
                        case EntityInvalidationEvent.DELETE: // TODO
                            break;
                    }
                }
            }
        }
    }
}
