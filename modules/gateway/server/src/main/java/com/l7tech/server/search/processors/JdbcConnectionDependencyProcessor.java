package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.search.Dependency;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * This is used to find JdbcConnection's by their name
 *
 * @author Victor Kazakov
 */
public class JdbcConnectionDependencyProcessor extends DefaultDependencyProcessor<JdbcConnection> implements DependencyProcessor<JdbcConnection> {

    @Inject
    private JdbcConnectionManager jdbcConnectionManager;

    @NotNull
    public List<DependencyFinder.FindResults<JdbcConnection>> find(@NotNull final Object searchValue, @NotNull final Dependency.DependencyType dependencyType, @NotNull final Dependency.MethodReturnType searchValueType) throws FindException {
        //handles finding jdbc connections by name
        switch (searchValueType) {
            case NAME:
                JdbcConnection connection = jdbcConnectionManager.getJdbcConnection((String) searchValue);
                return Arrays.<DependencyFinder.FindResults<JdbcConnection>>asList(DependencyFinder.FindResults.<JdbcConnection>create(connection, new EntityHeader(Goid.DEFAULT_GOID, EntityType.JDBC_CONNECTION,(String) searchValue,null)));
            default:
                //if a different search method is specified then search for the jdbc connection using the GenericDependency processor
                return super.find(searchValue, dependencyType, searchValueType);
        }
    }

    @NotNull
    @Override
    public List<DependentObject> createDependentObjects(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) throws CannotRetrieveDependenciesException {
        //handles creating a dependent jdbc connection from the name only.
        switch (searchValueType) {
            case NAME:
                JdbcConnection jdbcConnection = new JdbcConnection();
                jdbcConnection.setName((String) searchValue);
                return Arrays.asList(createDependentObject(jdbcConnection));
            default:
                //if a different search method is specified then create the jdbc connection using the GenericDependency processor
                return super.createDependentObjects(searchValue, dependencyType, searchValueType);
        }
    }
}
