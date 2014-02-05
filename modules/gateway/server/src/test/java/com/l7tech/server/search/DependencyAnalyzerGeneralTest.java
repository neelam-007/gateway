package com.l7tech.server.search;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.search.Dependency;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.processors.DependencyProcessor;
import com.l7tech.server.search.processors.GenericDependencyProcessor;
import com.l7tech.util.CollectionUtils;
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
    GenericDependencyProcessor genericDependencyProcessor = new GenericDependencyProcessor();

    @Spy
    DependencyProcessorStore processorStore = new DependencyProcessorStore(CollectionUtils.MapBuilder.<Dependency.DependencyType, DependencyProcessor>builder().put(Dependency.DependencyType.GENERIC, genericDependencyProcessor).map());

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
    public void testFindEntities() throws FindException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithNoDependencies));

        Assert.assertEquals(0, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithDependencies() throws FindException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithDifferentGetter() throws FindException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithDifferentGetter));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithCycle() throws FindException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(cycleRoot));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithInheritedDependencies() throws FindException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithInheritedDependency));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithDepth0() throws FindException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency), depth0SearchOptions);

        Assert.assertNull(dependencies.getDependencies());
    }

    @Test
    public void testFindEntitiesWithDepth1() throws FindException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency), depth1SearchOptions);

        Assert.assertEquals(1, dependencies.getDependencies().size());
        Assert.assertFalse(dependencies.getDependencies().iterator().next().areDependenciesSet());
    }

    @Test
    public void testFindEntitiesWithDepth1WithCycle() throws FindException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(cycleRoot), depth1SearchOptions);

        Assert.assertEquals(1, dependencies.getDependencies().size());
        Assert.assertFalse(dependencies.getDependencies().iterator().next().areDependenciesSet());
    }

    @Test
    public void testFindEntitiesWithDepth2() throws FindException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency), depth2SearchOptions);

        Assert.assertEquals(1, dependencies.getDependencies().size());
        Assert.assertTrue(dependencies.getDependencies().iterator().next().areDependenciesSet());
    }

    public class MyEntityWithInheritedDependency extends MyEntityWithOneDependency {
        public MyEntityWithInheritedDependency(String name, Goid id, Entity dependency) {
            super(name, id, dependency);
        }
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
