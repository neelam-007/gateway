package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.policy.variable.Syntax;

import java.util.logging.Logger;

/**
 * Variable selector that supports audit search criteria of the current audit lookup policy.
 */
public class ConnectorSelector implements ExpandVariables.Selector<SsgConnector> {

    private static final Logger logger = Logger.getLogger(ConnectorSelector.class.getName());

    @Override
    public Selection select(String contextName, SsgConnector connection, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if(name.equals("port")){
            return new Selection( connection.getPort());
        }
        else if(name.equals("protocol")){
            return new Selection( connection.getScheme());
        }
        return null;

    }

    @Override
    public Class<SsgConnector> getContextObjectClass() {
        return SsgConnector.class;
    }
}
