package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.audit.AuditConfiguration;
import com.l7tech.objectmodel.*;
import com.l7tech.search.Dependency;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.bundling.EntityBundleExporterImpl;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.DependencyAnalyzerImpl;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.util.CollectionUtils;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;
import java.util.Set;

/**
 * This was created: 5/30/13 as 3:47 PM
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
public class DependencyAnalyzerGeneralTest {

    Map<String, Object> depth0SearchOptions = CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.SearchDepthOptionKey, 0).map();
    Map<String, Object> depth1SearchOptions = CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.SearchDepthOptionKey, 1).map();
    Map<String, Object> depth2SearchOptions = CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.SearchDepthOptionKey, 2).map();

    @Mock
    private EntityCrud entityCrud;

    @InjectMocks
    DefaultDependencyProcessor defaultDependencyProcessor = new DefaultDependencyProcessor();

    @Spy
    DependencyProcessorStore processorStore = new DependencyProcessorStore(CollectionUtils.MapBuilder.<Dependency.DependencyType, InternalDependencyProcessor>builder().put(Dependency.DependencyType.ANY, defaultDependencyProcessor).map());

    @InjectMocks
    DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzerImpl();

    private DependencyAnalyzerGeneralTest.MyEntityWithNoDependencies myEntityWithNoDependencies = new MyEntityWithNoDependencies("MyEntityWithNoDependencies", new Goid(0,1));
    private final MyEntityWithOneDependency myEntityWithOneDependency = new MyEntityWithOneDependency("MyEntityWithOneDependency", new Goid(0,2), myEntityWithNoDependencies);
    private final MyEntityWithDifferentGetter myEntityWithDifferentGetter = new MyEntityWithDifferentGetter("MyEntityWithDifferentGetter", new Goid(0,3), myEntityWithNoDependencies);
    private final MyEntityWithInheritedDependency myEntityWithInheritedDependency = new MyEntityWithInheritedDependency("MyEntityWithInheritedDependency", new Goid(0,4), myEntityWithNoDependencies);
    private final MyEntityWithOneDependency cycleRoot = new MyEntityWithOneDependency("MyEntityWithOneDependency", new Goid(0,5), myEntityWithNoDependencies);
    private final MyEntityWithOneDependency cycleStep = new MyEntityWithOneDependency("MyEntityWithOneDependency", new Goid(0,6), myEntityWithNoDependencies);

    Set<Entity> entities = CollectionUtils.<Entity>set(myEntityWithNoDependencies, myEntityWithOneDependency, myEntityWithDifferentGetter, myEntityWithInheritedDependency, cycleRoot, cycleStep);

    @Before
    public void beforeTests() throws FindException {
        cycleStep.setDependency(cycleRoot);
        cycleRoot.setDependency(cycleStep);
        for (Entity entity : entities) {
            Mockito.when(entityCrud.find(Matchers.eq(EntityHeaderUtils.fromEntity(entity)))).thenReturn(entity);
        }
    }

    @Test
    public void testFindEntities() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithNoDependencies));

        Assert.assertEquals(0, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithDependencies() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithDifferentGetter() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithDifferentGetter));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithCycle() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(cycleRoot));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithInheritedDependencies() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithInheritedDependency));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithDepth0() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency), depth0SearchOptions);

        Assert.assertNull(dependencies.getDependencies());
    }

    @Test
    public void testFindEntitiesWithDepth1() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency), depth1SearchOptions);

        Assert.assertEquals(1, dependencies.getDependencies().size());
        Assert.assertTrue(dependencies.getDependencies().iterator().next().getDependencies() == null);
    }

    @Test
    public void testFindEntitiesWithDepth1WithCycle() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(cycleRoot), depth1SearchOptions);

        Assert.assertEquals(1, dependencies.getDependencies().size());
        Assert.assertTrue(dependencies.getDependencies().iterator().next().getDependencies() == null);
    }

    @Test
    public void testFindEntitiesWithDepth2() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency), depth2SearchOptions);

        Assert.assertEquals(1, dependencies.getDependencies().size());
        Assert.assertTrue(dependencies.getDependencies().iterator().next().getDependencies() != null);
    }

    @Test
    public void testReplaceDependencies() throws CannotReplaceDependenciesException, CannotRetrieveDependenciesException {
        dependencyAnalyzer.replaceDependencies(myEntityWithOneDependency, CollectionUtils.MapBuilder.<EntityHeader, EntityHeader>builder()
                .put(
                        EntityHeaderUtils.fromEntity(myEntityWithNoDependencies),
                        EntityHeaderUtils.fromEntity(myEntityWithDifferentGetter)).map(), true);
        Assert.assertEquals(myEntityWithOneDependency.getDependency(), myEntityWithDifferentGetter);

    }

    public class MyEntityWithInheritedDependency extends MyEntityWithOneDependency {
        public MyEntityWithInheritedDependency(String name, Goid id, Entity dependency) {
            super(name, id, dependency);
        }
    }

    @Test
    public void testFindGatewayConfigurationEntities() throws FindException, CannotRetrieveDependenciesException {
        Mockito.when(entityCrud.find(AuditConfiguration.getDefaultEntityHeader())).thenReturn(myEntityWithNoDependencies);
        List<DependencySearchResults> dependencies = dependencyAnalyzer.getDependencies(CollectionUtils.list(EntityHeaderUtils.fromEntity(myEntityWithDifferentGetter)),CollectionUtils.MapBuilder.<String, Object>builder()
                .put(EntityBundleExporterImpl.IncludeGatewayConfigurationOption,true).map());

        Assert.assertEquals(2, dependencies.size());
        Assert.assertEquals(myEntityWithDifferentGetter.getName(),dependencies.get(0).getDependent().getName());
        Assert.assertEquals(myEntityWithNoDependencies.getName(),dependencies.get(1).getDependent().getName());
    }

    public class MyEntityWithOneDependency extends MyEntityWithNoDependencies {
        private Entity dependency;

        public MyEntityWithOneDependency(String name, Goid id, Entity dependency) {
            super(name, id);
            this.dependency = dependency;
        }

        public Entity getDependency() {
            return dependency;
        }

        public void setDependency(Entity dependency) {
            this.dependency = dependency;
        }
    }

    public class MyEntityWithDifferentGetter extends MyEntityWithNoDependencies {
        private Entity dependency;

        public MyEntityWithDifferentGetter(String name, Goid id, Entity dependency) {
            super(name, id);
            this.dependency = dependency;
        }

        @com.l7tech.search.Dependency
        public Entity myDependency() {
            return dependency;
        }
    }

    public class MyEntityWithNoDependencies implements NamedEntity {
        private Goid id;
        private String name;

        public MyEntityWithNoDependencies(String name, Goid id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String getId() {
            return id.toString();
        }

        @Override
        public String getName() {
            return name;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
