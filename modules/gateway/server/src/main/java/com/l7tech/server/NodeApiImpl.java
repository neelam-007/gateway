package com.l7tech.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.boot.ShutdownWatcher;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.api.monitoring.NodeStatus;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.audit.AuditRecordManager;
import com.l7tech.util.IOUtils;
import com.l7tech.util.SyspropUtil;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Implementation of the Cluster Node API hosted by the Cluster Node and used by the Process Controller.
 *
 * @author alex
 */
public class NodeApiImpl implements NodeApi {

    //- PUBLIC

    @Override
    public void shutdown() {
        checkRequest();
        logger.warning("Node Shutdown requested");
        shutdowner.shutdownNow();
    }

    @Override
    public void ping() {
        checkRequest();
        logger.fine("ping");
    }

    @Override
    public NodeStatus getNodeStatus() {
        checkRequest();
        logger.fine("getNodeStatus");
        return new NodeStatus(NodeStateType.RUNNING, new Date(), new Date()); // TODO
    }

    @Override
    public String getProperty(String propertyId) throws UnsupportedPropertyException, FindException {
        checkRequest();
        logger.fine("getProperty");
        if (propertyId.equals(BuiltinMonitorables.AUDIT_SIZE.getName())) {
            return Long.toString(auditRecordManager.findCount(new AuditSearchCriteria.Builder().build()));
        } else if (propertyId.equals(BuiltinMonitorables.DATABASE_REPLICATION_DELAY.getName())) {
            return "41"; //TODO [steve] implement property for database replication delay
        } else {
            throw new UnsupportedPropertyException("propertyId");
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(NodeApiImpl.class.getName());

    @Inject
    private ServerConfig serverConfig;

    @Inject
    private SsgConnectorManager ssgConnectorManager;

    @Inject
    private ShutdownWatcher shutdowner;

    @Inject
    private AuditRecordManager auditRecordManager;

    @Resource
    private WebServiceContext wscontext; // Injected by CXF to get access to request metadata (e.g. HttpServletRequest)

    private X509Certificate processControllerCertificate;

    private void checkRequest() {
        final HttpServletRequest hsr = (HttpServletRequest)wscontext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        if (hsr == null) throw new IllegalStateException("Request received outside of expected servlet context");
        try {
            HttpTransportModule.requireEndpoint(hsr, SsgConnector.Endpoint.PC_NODE_API);

            final X509Certificate certificate = getRequestCertificate( hsr );
            if ( certificate == null ) {
                logger.fine( "Client certificate missing in request." );
                throw new IllegalStateException("Request denied (no certificate).");
            }

            if ( !InetAddress.getByName(hsr.getRemoteAddr()).isLoopbackAddress()) {
                throw new IllegalStateException("Request denied for non-local address.");
            }

            final X509Certificate processControllerCertificate = getProcessControllerCertificate();
            if ( processControllerCertificate != null ) {
                if ( !CertUtils.certsAreEqual( processControllerCertificate, certificate )) {
                    throw new IllegalStateException("Request denied (certificate mismatch).");
                }
            }
        } catch (ListenerException e) {
            throw new IllegalStateException(NODE_NOT_CONFIGURED_FOR_PC);
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException("Request denied for non-local address.", uhe);
        } catch ( CertificateException e ) {
            throw new IllegalStateException("Request denied (error processing certificate).", e);
        } catch ( IOException e ) {
            throw new IllegalStateException("Request denied (error processing certificate).", e);
        }
    }

    private X509Certificate getRequestCertificate( final HttpServletRequest hsr ) {
        final Object maybeCert = hsr.getAttribute("javax.servlet.request.X509Certificate");
        final X509Certificate certificate;
        if (maybeCert instanceof X509Certificate) {
            certificate = (X509Certificate)maybeCert;
        } else if (maybeCert instanceof X509Certificate[]) {
            X509Certificate[] certs = (X509Certificate[])maybeCert;
            certificate = certs.length>0 ? certs[0] : null;
        } else if (maybeCert != null) {
            logger.warning( "Client certificate was a " + maybeCert.getClass().getName() + ", not an X509Certificate" );
            certificate = null;
        } else {
            certificate = null;
        }
        return certificate;
    }

    private X509Certificate getProcessControllerCertificate() throws CertificateException, IOException {
        X509Certificate processControllerCertificate = this.processControllerCertificate;
        if ( processControllerCertificate == null ) {
            String pcCertFile = SyspropUtil.getProperty( "com.l7tech.server.processControllerCert" );
            if ( pcCertFile != null ) {
                processControllerCertificate = CertUtils.decodeCert( IOUtils.slurpFile( new File(pcCertFile) ) );
                this.processControllerCertificate = processControllerCertificate;
            }
        }
        return processControllerCertificate;
    }


}
