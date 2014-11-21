package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.search.Dependency;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.DependencyAnalyzerImpl;
import com.l7tech.server.search.DependencyCacheImpl;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.util.ApplicationEventProxy;
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
public class DependencyCacheAnalyzerGeneralTest {

    Map<String, Object> depth0SearchOptions = CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.SearchDepthOptionKey, 0).map();
    Map<String, Object> depth1SearchOptions = CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.SearchDepthOptionKey, 1).map();
    Map<String, Object> depth2SearchOptions = CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.SearchDepthOptionKey, 2).map();

    @Mock
    private EntityCrud entityCrud;

    @Mock
    private FolderManager folderManager;

    @InjectMocks
    DefaultDependencyProcessor defaultDependencyProcessor = new DefaultDependencyProcessor();

    @Spy
    DependencyProcessorStore processorStore = new DependencyProcessorStore(CollectionUtils.MapBuilder.<Dependency.DependencyType, InternalDependencyProcessor>builder().put(Dependency.DependencyType.ANY, defaultDependencyProcessor).map());

    @Spy
    ApplicationEventProxy applicationEventProxy = new ApplicationEventProxy();

    @Spy
    ServerConfigStub config = new ServerConfigStub();

    @InjectMocks
    @Spy
    DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzerImpl();

    @InjectMocks
    DependencyCacheImpl dependencyCache = new DependencyCacheImpl();

    private DependencyCacheAnalyzerGeneralTest.MyEntityWithNoDependencies myEntityWithNoDependencies = new MyEntityWithNoDependencies("MyEntityWithNoDependencies", new Goid(0, 1));
    private final MyEntityWithOneDependency myEntityWithOneDependency = new MyEntityWithOneDependency("MyEntityWithOneDependency", new Goid(0, 2), myEntityWithNoDependencies);
    private final MyEntityWithDifferentGetter myEntityWithDifferentGetter = new MyEntityWithDifferentGetter("MyEntityWithDifferentGetter", new Goid(0, 3), myEntityWithNoDependencies);
    private final MyEntityWithInheritedDependency myEntityWithInheritedDependency = new MyEntityWithInheritedDependency("MyEntityWithInheritedDependency", new Goid(0, 4), myEntityWithNoDependencies);
    private final MyEntityWithOneDependency cycleRoot = new MyEntityWithOneDependency("MyEntityWithOneDependency", new Goid(0, 5), myEntityWithNoDependencies);
    private final MyEntityWithOneDependency cycleStep = new MyEntityWithOneDependency("MyEntityWithOneDependency", new Goid(0, 6), myEntityWithNoDependencies);
    private final Folder folder = new Folder("Root Folder", null);

    Set<Entity> entities = CollectionUtils.<Entity>set(myEntityWithNoDependencies, myEntityWithOneDependency, myEntityWithDifferentGetter, myEntityWithInheritedDependency, cycleRoot, cycleStep, folder);

    @Before
    public void beforeTests() throws FindException, InterruptedException {
        cycleStep.setDependency(cycleRoot);
        cycleRoot.setDependency(cycleStep);
        EntityHeaderSet entityHeaderSet = new EntityHeaderSet();
        for (Entity entity : entities) {
            Mockito.when(entityCrud.find(Matchers.eq(EntityHeaderUtils.fromEntity(entity)))).thenReturn(entity);
            entityHeaderSet.add(EntityHeaderUtils.fromEntity(entity));
        }

        Mockito.when(folderManager.findRootFolder()).thenReturn(new Folder(folder));

        Mockito.when(entityCrud.findAll(JdbcConnection.class)).thenReturn(entityHeaderSet);

        dependencyCache.init();
        applicationEventProxy.publishEvent(new Started(this, null, ""));
        //sleep to make sure that the cache build starts. Otherwise where is a chance that a test will execute before the cache build begins.
        Thread.sleep(500);
    }

    @Test
    public void testFindEntities() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyCache.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithNoDependencies));

        Assert.assertEquals(0, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithDependencies() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyCache.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithDifferentGetter() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyCache.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithDifferentGetter));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithCycle() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyCache.getDependencies(EntityHeaderUtils.fromEntity(cycleRoot));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithInheritedDependencies() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyCache.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithInheritedDependency));

        Assert.assertEquals(1, dependencies.getDependencies().size());
    }

    @Test
    public void testFindEntitiesWithDepth0() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyCache.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency), depth0SearchOptions);

        Assert.assertNull(dependencies.getDependencies());
    }

    @Test
    public void testFindEntitiesWithDepth1() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyCache.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency), depth1SearchOptions);

        Assert.assertEquals(1, dependencies.getDependencies().size());
        Assert.assertTrue(dependencies.getDependencies().iterator().next().getDependencies() == null);
    }

    @Test
    public void testFindEntitiesWithDepth1WithCycle() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyCache.getDependencies(EntityHeaderUtils.fromEntity(cycleRoot), depth1SearchOptions);

        Assert.assertEquals(1, dependencies.getDependencies().size());
        Assert.assertTrue(dependencies.getDependencies().iterator().next().getDependencies() == null);
    }

    @Test
    public void testFindEntitiesWithDepth2() throws FindException, CannotRetrieveDependenciesException {
        DependencySearchResults dependencies = dependencyCache.getDependencies(EntityHeaderUtils.fromEntity(myEntityWithOneDependency), depth2SearchOptions);

        Assert.assertEquals(1, dependencies.getDependencies().size());
        Assert.assertTrue(dependencies.getDependencies().iterator().next().getDependencies() != null);
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

        @Dependency
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
