package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.search.Dependency;
import com.l7tech.server.cassandra.CassandraConnectionEntityManager;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class CassandraConnectionDependencyProcessor extends DefaultDependencyProcessor<CassandraConnection> implements DependencyProcessor<CassandraConnection> {

    @Inject
    private CassandraConnectionEntityManager cassandraEntityManager;

    @NotNull
    public List<DependencyFinder.FindResults<CassandraConnection>> find(@NotNull final Object searchValue, @NotNull final Dependency.DependencyType dependencyType, @NotNull final Dependency.MethodReturnType searchValueType) throws FindException {
        //handles finding cassandra connections by name
        switch (searchValueType) {
            case NAME:
                CassandraConnection connection = cassandraEntityManager.getCassandraConnectionEntity((String) searchValue);
                return Arrays.<DependencyFinder.FindResults<CassandraConnection>>asList(DependencyFinder.FindResults.<CassandraConnection>create(connection, new EntityHeader(Goid.DEFAULT_GOID, EntityType.CASSANDRA_CONFIGURATION, (String) searchValue, null)));
            default:
                //if a different search method is specified then search for the Cassandra connection using the GenericDependency processor
                return super.find(searchValue, dependencyType, searchValueType);
        }
    }

    @NotNull
    @Override
    public List<DependentObject> createDependentObjects(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) throws CannotRetrieveDependenciesException {
        //handles creating a dependent Cassandra connection from the name only.
        switch (searchValueType) {
            case NAME:
                CassandraConnection cassandraConnection = new CassandraConnection();
                cassandraConnection.setName((String) searchValue);
                return Arrays.asList(createDependentObject(cassandraConnection));
            default:
                //if a different search method is specified then create the Cassandra connection using the GenericDependency processor
                return super.createDependentObjects(searchValue, dependencyType, searchValueType);
        }
    }
}
