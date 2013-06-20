package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * This is used to find JdbcConnection's by their name
 *
 * @author Victor Kazakov
 */
public class JdbcConnectionDependencyProcessor extends GenericDependencyProcessor<JdbcConnection> implements DependencyProcessor<JdbcConnection> {

    @Inject
    private JdbcConnectionManager jdbcConnectionManager;

    @SuppressWarnings("unchecked")
    public List<JdbcConnection> find(@NotNull Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        switch (searchValueType) {
            case NAME:
                return Arrays.asList(jdbcConnectionManager.getJdbcConnection((String) searchValue));
            default:
                //if a different search method is specified then search for the jdbc connection using the GenericDependency processor
                return (List<JdbcConnection>) super.find(searchValue, dependencyType, searchValueType);
        }
    }
}
