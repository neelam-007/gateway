package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;

/**
 * @author alex
 * @version $Revision$
 */
public interface JmsCrudListener {
    void connectionDeleted( JmsConnection connection );
    void connectionUpdated( JmsConnection connection );

    void endpointDeleted( JmsEndpoint endpoint );
    void endpointUpdated( JmsEndpoint endpoint );
}
