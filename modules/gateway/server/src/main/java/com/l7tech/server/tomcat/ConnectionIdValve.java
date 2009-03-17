package com.l7tech.server.tomcat;

import com.l7tech.server.transport.http.HttpTransportModule;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Valve that listens for new Socket connections and stamps the thread with
 * an Id.
 *
 * <p>It it important to note that this behaviour is dependent on Tomcats
 * connection strategy. The default strategy avoids context switches by
 * processing connections in the same Thread that accepts the connection. If
 * this strategy is changed this valve will break.</p>
 */
public class ConnectionIdValve extends ValveBase {
    public static final String ATTRIBUTE_CONNECTION_ID = "com.l7tech.server.connectionId";
    public static final String ATTRIBUTE_TRANSPORT_MODULE_INSTANCE_ID = "com.l7tech.server.httpTransportModuleInstanceId";
    public static final String ATTRIBUTE_CONNECTOR_OID = "com.l7tech.server.ssgConnectorOid";

    //- PUBLIC

    /**
     * Create the valve instance.
     *
     * <p>This causes the valve to register itself for connection notifications
     * with the SsgServerSocketFactory</p>
     *
     * @param transportModule  the HttpTransportModule that owns this ConnectionIdValve.
     * @see SsgServerSocketFactory
     * @see SsgServerSocketFactory.Listener
     */
    public ConnectionIdValve(HttpTransportModule transportModule) {
        this.httpTransportModule = transportModule;
        final long transportId = httpTransportModule.getInstanceId();
        SsgServerSocketFactory.addListener(new SsgServerSocketFactory.Listener(){
            public void onGetInputStream(long transportModuleInstanceId, long connectorOid, Socket accepted) {
                if (transportModuleInstanceId != transportId)
                    // not for us
                    return;

                long id = connectionSequence.incrementAndGet();
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "Setting id for connection '{0}'", id);
                connectionId.set(id);
                ssgConnectorOid.set(connectorOid);
            }
        });
    }

    /**
     * Process a request.
     *
     * <p>This valve associates the connection id with the current thread and
     * then hands off to the next valve.</p>
     *
     * @param req The request
     * @param res The response
     * @throws IOException if an IOException occurs ...
     * @throws ServletException if a ServletException occurs ...
     */
    public void invoke(Request req, Response res) throws IOException, ServletException {
        // Set the connection id for the request
        final Long cid = connectionId.get();
        final Long connectorOid = ssgConnectorOid.get();
        req.setAttribute(ATTRIBUTE_CONNECTION_ID, cid);
        req.setAttribute(ATTRIBUTE_CONNECTOR_OID, connectorOid);
        req.setAttribute(ATTRIBUTE_TRANSPORT_MODULE_INSTANCE_ID, httpTransportModule.getInstanceId());

        // Let servlet do it's thing
        getNext().invoke(req, res);
    }

    /**
     * Get the current thread's connector OID, if known.
     *
     * @return the thread-local connector OID, or -1 if not known.
     */
    public static long getConnectorOid() {
        Long oid = ssgConnectorOid.get();
        return oid == null ? -1 : oid;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ConnectionIdValve.class.getName());
    private static final ThreadLocal<Long> ssgConnectorOid = new ThreadLocal<Long>();
    private final HttpTransportModule httpTransportModule;
    private final ThreadLocal<Long> connectionId = new ThreadLocal<Long>();
    private final AtomicLong connectionSequence = new AtomicLong(0);
}
