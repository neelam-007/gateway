package com.l7tech.server.tomcat;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;
import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

/**
 * Valve that listens for new Socket connections and stamps the thread with
 * an Id.
 *
 * <p>It it important to note that this behaviour is dependent on Tomcats
 * connection strategy. The default strategy avoids context switches by
 * processing connections in the same Thread that accepts the connection. If
 * this strategy is changed this valve will break.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class ConnectionIdValve extends ValveBase {

    //- PUBLIC

    /**
     * Create the valve instance.
     *
     * <p>This causes the valve to register itself for connection notifications
     * with the SsgServerSocketFactory</p>
     *
     * @see SsgServerSocketFactory
     * @see SsgServerSocketFactory.Listener
     */
    public ConnectionIdValve() {
        connectionSequenceLock = new Object();
        connectionSequence = 0;
        connectionId = new ThreadLocal();
        SsgServerSocketFactory.addListener(new SsgServerSocketFactory.Listener(){
            public void onAccept(Socket accepted) {
                long id = 0;
                synchronized(connectionSequenceLock) {
                    id = ++connectionSequence;
                }
                logger.info("Setting id for connection '"+id+"'.");
                connectionId.set(Long.valueOf(id));
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
        req.setAttribute(ATTRIBUTE_CONNECTION_ID, connectionId.get());

        // Let servlet do it's thing
        getNext().invoke(req, res);
    }

    //- PRIVATE

    private static final String ATTRIBUTE_CONNECTION_ID = "com.l7tech.server.connectionId";
    private static final Logger logger = Logger.getLogger(ConnectionIdValve.class.getName());

    private ThreadLocal connectionId;
    private Object connectionSequenceLock;
    private long connectionSequence;
}
