package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    @Override
    public List<DependentObject> createDependentObject(@NotNull Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) {
        switch (searchValueType) {
            case NAME:
                JdbcConnection jdbcConnection = new JdbcConnection();
                jdbcConnection.setName((String) searchValue);
                return Arrays.asList(createDependentObject(jdbcConnection));
            default:
                //if a different search method is specified then create the jdbc connection using the GenericDependency processor
                return super.createDependentObject(searchValue, dependencyType, searchValueType);
        }
    }
}
