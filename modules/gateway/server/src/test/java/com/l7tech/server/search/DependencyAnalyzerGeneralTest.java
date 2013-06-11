package com.l7tech.server.search;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This was created: 5/30/13 as 3:47 PM
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
public class DependencyAnalyzerGeneralTest {

    Map<String, String> depth0SearchOptions = new HashMap<String, String>(DependencyAnalyzer.DefaultSearchOptions) {
        {
            put(DependencyAnalyzer.SearchDepthOptionKey, "0");
        }
    };
    Map<String, String> depth1SearchOptions = new HashMap<String, String>(DependencyAnalyzer.DefaultSearchOptions) {
        {
            put(DependencyAnalyzer.SearchDepthOptionKey, "1");
        }
    };
    Map<String, String> depth2SearchOptions = new HashMap<String, String>(DependencyAnalyzer.DefaultSearchOptions) {
        {
            put(DependencyAnalyzer.SearchDepthOptionKey, "2");
        }
    };

    @Mock
    private EntityCrud entityCrud;

    @InjectMocks
    GenericDependencyProcessor genericDependencyProcessor = new GenericDependencyProcessor();

    @Spy
    DependencyProcessorStore processorStore = new DependencyProcessorStore(CollectionUtils.MapBuilder.<Dependency.DependencyType, DependencyProcessor>builder().put(Dependency.DependencyType.GENERIC, genericDependencyProcessor).map());

    @InjectMocks
    DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzerImpl();

    private DependencyAnalyzerGeneralTest.MyEntityWithNoDependencies myEntityWithNoDependencies = new MyEntityWithNoDependencies("MyEntityWithNoDependencies");
    private final MyEntityWithOneDependency myEntityWithOneDependency = new MyEntityWithOneDependency("MyEntityWithOneDependency", myEntityWithNoDependencies);
    private final MyEntityWithDifferentGetter myEntityWithDifferentGetter = new MyEntityWithDifferentGetter("MyEntityWithDifferentGetter", myEntityWithNoDependencies);
    private final MyEntityWithInheritedDependency myEntityWithInheritedDependency = new MyEntityWithInheritedDependency("MyEntityWithInheritedDependency", myEntityWithNoDependencies);

    Set<Entity> entities = CollectionUtils.<Entity>set(myEntityWithNoDependencies, myEntityWithOneDependency, myEntityWithDifferentGetter, myEntityWithInheritedDependency);

    @Before
    public void beforeTests() throws FindException {
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
        MyEntityWithOneDependency rootEntity = new MyEntityWithOneDependency("MyEntityWithOneDependency", myEntityWithNoDependencies);
        MyEntityWithOneDependency entity = new MyEntityWithOneDependency("MyEntityWithOneDependency", myEntityWithNoDependencies);
        entity.setDependency(rootEntity);
        rootEntity.setDependency(entity);
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(rootEntity));

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
        MyEntityWithOneDependency rootEntity = new MyEntityWithOneDependency("MyEntityWithOneDependency", myEntityWithNoDependencies);
        MyEntityWithOneDependency entity = new MyEntityWithOneDependency("MyEntityWithOneDependency", myEntityWithNoDependencies);
        entity.setDependency(rootEntity);
        rootEntity.setDependency(entity);

        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(EntityHeaderUtils.fromEntity(rootEntity), depth1SearchOptions);

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
        public MyEntityWithInheritedDependency(String id, Entity dependency) {
            super(id, dependency);
        }
    }

    public class MyEntityWithOneDependency extends MyEntityWithNoDependencies {
        private Entity dependency;

        public MyEntityWithOneDependency(String id, Entity dependency) {
            super(id);
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

        public MyEntityWithDifferentGetter(String id, Entity dependency) {
            super(id);
            this.dependency = dependency;
        }

        @com.l7tech.search.Dependency
        public Entity myDependency() {
            return dependency;
        }
    }

    public class MyEntityWithNoDependencies implements Entity {
        private String id;

        public MyEntityWithNoDependencies(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }
    }
}
