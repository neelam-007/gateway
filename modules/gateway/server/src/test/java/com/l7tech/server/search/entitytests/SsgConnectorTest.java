package com.l7tech.server.search.entitytests;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.processors.BaseDependencyProcessor;
import com.l7tech.server.search.processors.DependencyFinder;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This was created: 6/12/13 as 4:55 PM
 *
 * @author Victor Kazakov
 */
public class SsgConnectorTest extends DependencyTestBaseClass {

    AtomicLong idCount = new AtomicLong(1);
    private String myCustomConnectorScheme = "MyCustomConnector";

    private Dependency myDependency = new Dependency(new DependentEntity("dependentName", new EntityHeader(new Goid(0,1),EntityType.ANY, idCount.getAndIncrement()+"",null)));

    @Before
    public void before() {
        //add the custom connector dependency processor
        ssgConnectorDependencyProcessorRegistry.register(myCustomConnectorScheme, new BaseDependencyProcessor<SsgConnector>() {
            @NotNull
            public List<Dependency> findDependencies(@NotNull SsgConnector object, @NotNull DependencyFinder finder) throws FindException {
                return Arrays.asList(myDependency);
            }
        });
    }

    @After
    public void after() {
        //remove all custom connector dependency processors.
        ssgConnectorDependencyProcessorRegistry.remove(myCustomConnectorScheme);
    }


    @Test
    public void test() throws FindException, CannotRetrieveDependenciesException {

        SsgConnector connector = new SsgConnector();
        long ssgConnectorOid = idCount.getAndIncrement();
        connector.setGoid(new Goid(0L,ssgConnectorOid));
        connector.setScheme(myCustomConnectorScheme);
        final EntityHeader entityHeader = new EntityHeader(new Goid(0L,ssgConnectorOid), EntityType.SSG_CONNECTOR, null, null);
        mockEntity(connector, entityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(entityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(ssgConnectorOid, ((DependentEntity) result.getDependent()).getEntityHeader().getGoid().getLow());
        Assert.assertEquals(EntityType.SSG_CONNECTOR, ((DependentEntity) result.getDependent()).getDependencyType().getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(myDependency, result.getDependencies().get(0));
    }
}
