package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.policy.variable.Syntax;

import java.util.logging.Logger;

/**
 * Variable selector that supports audit search criteria of the current audit lookup policy.
 */
public class JdbcConnectionSelector implements ExpandVariables.Selector<JdbcConnection> {

    private static final Logger logger = Logger.getLogger(JdbcConnectionSelector.class.getName());

    @Override
    public Selection select(String contextName, JdbcConnection connection, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if(name.equals("user")){
            return new Selection( connection.getUserName());
        }
        else if(name.equals("url")){
            return new Selection( connection.getJdbcUrl());
        }
        return null;

    }

    @Override
    public Class<JdbcConnection> getContextObjectClass() {
        return JdbcConnection.class;
    }
}
