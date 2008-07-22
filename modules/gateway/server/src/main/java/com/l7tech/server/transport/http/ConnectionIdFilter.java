package com.l7tech.server.transport.http;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Servlet filter that pulls a connection id out of the request and creates
 * a thread bound ConnectionId.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class ConnectionIdFilter implements Filter {

    //- PUBLIC

    public ConnectionIdFilter() {
        generation = hashCode();
    }

    public void init( FilterConfig filterConfig ) throws ServletException {
    }

    public void destroy() {
    }

    public void doFilter( ServletRequest srequest,
                          ServletResponse sresponse,
                          FilterChain chain ) throws ServletException, IOException
    {
        Object id = srequest.getAttribute(ATTRIBUTE_CONNECTION_ID);
        if (id instanceof Long) {
            ConnectionId connectionId = new ConnectionId(generation, ((Long)id).longValue());

            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Connection Id '"+connectionId+"'.");
            }

            ConnectionId.setConnectionId(connectionId);
            srequest.setAttribute(ATTRIBUTE_CONNECTION_ID_OBJ, connectionId);
        }
        else {
            logger.warning("Missing or invalid connectionId '"+id+"'.");
            ConnectionId.setConnectionId(null);
            srequest.removeAttribute(ATTRIBUTE_CONNECTION_ID_OBJ);
        }

        chain.doFilter(srequest, sresponse);
    }

    //- PRIVATE

    private static final String ATTRIBUTE_CONNECTION_ID = "com.l7tech.server.connectionId";
    private static final String ATTRIBUTE_CONNECTION_ID_OBJ = "com.l7tech.server.connectionIdentifierObject";
    private static final Logger logger = Logger.getLogger(ConnectionIdFilter.class.getName());
    private final long generation;
}
