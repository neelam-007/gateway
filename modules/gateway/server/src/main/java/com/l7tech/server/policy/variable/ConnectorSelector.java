package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.policy.variable.Syntax;

import java.util.logging.Logger;

/**
 * Variable selector for the SsgConnector
 */
public class ConnectorSelector implements ExpandVariables.Selector<SsgConnector> {

    private static final Logger logger = Logger.getLogger(ConnectorSelector.class.getName());

    @Override
    public Selection select(String contextName, SsgConnector connector, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if(name.equals("port")){
            return new Selection( connector.getPort());
        } else if(name.equals("name")){
            return new Selection( connector.getName());
        } else if(name.equals("interfaces")){
            String bindAddress = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
            return new Selection(bindAddress == null ? "(ALL)" : bindAddress);
        } else if(name.equals("enabled")){
            return new Selection( connector.isEnabled()?"Yes":"No");
        } else if(name.equals("protocol")){
            return new Selection( connector.getScheme());
        }
        return null;

    }

    @Override
    public Class<SsgConnector> getContextObjectClass() {
        return SsgConnector.class;
    }
}
