package com.l7tech.server.search.entitytests;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
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
    private String myCustomConnectorScheme = "MyCustomConnectory";

    private Dependency myDependency = new Dependency(new DependentEntity("dependentName", EntityType.ANY, "PublicID", String.valueOf(idCount.getAndIncrement())));

    @Before
    public void before() {
        //add the custom connector dependency processor
        dependencyProcessorRegistry.register(myCustomConnectorScheme, new BaseDependencyProcessor<SsgConnector>() {
            @NotNull
            public List<Dependency> findDependencies(SsgConnector object, DependencyFinder finder) throws FindException {
                return Arrays.asList(myDependency);
            }
        });
    }

    @After
    public void after() {
        //remove all custom connector dependency processors.
        dependencyProcessorRegistry.remove(myCustomConnectorScheme);
    }


    @Test
    public void test() throws FindException {

        SsgConnector connector = new SsgConnector();
        long ssgConnectorOid = idCount.getAndIncrement();
        connector.setOid(ssgConnectorOid);
        connector.setScheme(myCustomConnectorScheme);
        final EntityHeader entityHeader = new EntityHeader(ssgConnectorOid, EntityType.SSG_CONNECTOR, null, null);
        mockEntity(connector, entityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(entityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(ssgConnectorOid, Long.parseLong(((DependentEntity) result.getDependent()).getInternalID()));
        Assert.assertEquals(EntityType.SSG_CONNECTOR, ((DependentEntity) result.getDependent()).getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(myDependency, result.getDependencies().get(0));
    }
}
