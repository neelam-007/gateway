package com.l7tech.server.search.entitytests;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.processors.DependencyFinder;
import com.l7tech.server.search.processors.DependencyProcessor;
import com.l7tech.server.search.processors.DependencyTestBaseClass;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        ssgConnectorDependencyProcessorRegistry.register(myCustomConnectorScheme, new DependencyProcessor<SsgConnector>() {
            @NotNull
            public List<Dependency> findDependencies(@NotNull SsgConnector object, @NotNull DependencyFinder finder) throws FindException {
                return Arrays.asList(myDependency);
            }

            @Override
            public void replaceDependencies(@NotNull SsgConnector object, @NotNull Map<EntityHeader, EntityHeader> replacementMap, @NotNull DependencyFinder finder, boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {

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

    @Test
    public void hardWiredServiceId() throws FindException, CannotRetrieveDependenciesException {

        Policy policy = new Policy(PolicyType.PRIVATE_SERVICE, "HardwiredServiceIDPolicy",
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\"/>\n" +
                "    </wsp:Policy>", false);

        PublishedService publishedService = new PublishedService();
        publishedService.setName("HardwiredServiceID");
        publishedService.setGoid(new Goid(0L, idCount.getAndIncrement()));
        publishedService.setPolicy(policy);
        mockEntity(publishedService, EntityHeaderUtils.fromEntity(publishedService));

        SsgConnector connector = new SsgConnector();
        long ssgConnectorOid = idCount.getAndIncrement();
        connector.setGoid(new Goid(0L,ssgConnectorOid));
        connector.setScheme("SomeOtherScheme");
        connector.setHardwiredServiceId(publishedService.getGoid());
        final EntityHeader entityHeader = new EntityHeader(new Goid(0L,ssgConnectorOid), EntityType.SSG_CONNECTOR, null, null);
        mockEntity(connector, entityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(entityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(ssgConnectorOid, ((DependentEntity) result.getDependent()).getEntityHeader().getGoid().getLow());
        Assert.assertEquals(EntityType.SSG_CONNECTOR, ((DependentEntity) result.getDependent()).getDependencyType().getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(1, result.getDependencies().size());
        Assert.assertEquals(com.l7tech.search.Dependency.DependencyType.SERVICE, result.getDependencies().get(0).getDependent().getDependencyType());
        Assert.assertEquals(publishedService.getName(), result.getDependencies().get(0).getDependent().getName());
    }

    @Test
    public void hardWiredServiceIdReplace() throws FindException, CannotRetrieveDependenciesException, CannotReplaceDependenciesException {

        Policy policy = new Policy(PolicyType.PRIVATE_SERVICE, "HardwiredServiceIDPolicy",
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "        <wsp:All wsp:Usage=\"Required\"/>\n" +
                        "    </wsp:Policy>", false);

        PublishedService publishedService = new PublishedService();
        publishedService.setName("HardwiredServiceID");
        publishedService.setGoid(new Goid(0L, idCount.getAndIncrement()));
        publishedService.setPolicy(policy);
        EntityHeader publishedService1Header = EntityHeaderUtils.fromEntity(publishedService);
        mockEntity(publishedService, publishedService1Header);

        Policy policy2 = new Policy(PolicyType.PRIVATE_SERVICE, "HardwiredServiceIDPolicy",
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "        <wsp:All wsp:Usage=\"Required\"/>\n" +
                        "    </wsp:Policy>", false);

        PublishedService publishedService2 = new PublishedService();
        publishedService2.setName("HardwiredServiceID");
        publishedService2.setGoid(new Goid(0L, idCount.getAndIncrement()));
        publishedService2.setPolicy(policy2);
        EntityHeader publishedService2Header = EntityHeaderUtils.fromEntity(publishedService2);
        mockEntity(publishedService2, publishedService2Header);

        SsgConnector connector = new SsgConnector();
        long ssgConnectorOid = idCount.getAndIncrement();
        connector.setGoid(new Goid(0L,ssgConnectorOid));
        connector.setScheme("SomeOtherScheme");
        connector.setHardwiredServiceId(publishedService.getGoid());
        final EntityHeader entityHeader = new EntityHeader(new Goid(0L,ssgConnectorOid), EntityType.SSG_CONNECTOR, null, null);
        mockEntity(connector, entityHeader);

        dependencyAnalyzer.replaceDependencies(connector, CollectionUtils.MapBuilder.<EntityHeader, EntityHeader>builder()
                .put(publishedService1Header, publishedService2Header)
                .map(), false);

        Assert.assertEquals(publishedService2.getGoid(), connector.getHardwiredServiceId());
    }
}
