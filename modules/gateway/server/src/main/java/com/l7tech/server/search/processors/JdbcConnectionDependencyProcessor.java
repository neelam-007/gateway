package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

/**
 * This is used to find JdbcConnection's by their name
 *
 * @author Victor Kazakov
 */
public class JdbcConnectionDependencyProcessor extends GenericDependencyProcessor<JdbcConnection> implements DependencyProcessor<JdbcConnection> {

    @Inject
    private JdbcConnectionManager jdbcConnectionManager;

    public JdbcConnection find(@NotNull Object searchValue, com.l7tech.search.Dependency dependency) throws FindException {
        switch (dependency.methodReturnType()) {
            case NAME:
                return jdbcConnectionManager.getJdbcConnection((String) searchValue);
            default:
                //if a different search method is specified then search for the jdbc connection using the GenericDependency processor
                return (JdbcConnection) super.find(searchValue, dependency);
        }
    }
}
