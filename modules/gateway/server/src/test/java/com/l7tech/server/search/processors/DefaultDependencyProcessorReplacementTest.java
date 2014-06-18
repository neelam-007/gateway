package com.l7tech.server.search.processors;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.DependencyProcessorStore;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.util.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vkazakov on 6/17/2014.
 */
public class DefaultDependencyProcessorReplacementTest {

    @Mock
    private EntityCrud entityCrud;

    @Mock
    private IdentityProviderConfigManager identityProviderConfigManager;

    @InjectMocks
    DefaultDependencyProcessor processor = new DefaultDependencyProcessor();

    @Spy
    private DependencyFinder dependencyFinder = new DependencyFinder(Collections.<String,Object>emptyMap(), new DependencyProcessorStore(CollectionUtils.MapBuilder.<com.l7tech.search.Dependency.DependencyType, DependencyProcessor>builder().put(com.l7tech.search.Dependency.DependencyType.ANY, processor).map()));

    @Test
    public void testReplaceDependenciesFromGoid() throws FindException, CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
        final Goid id = new Goid(3,123);
        final Goid replacementID = new Goid(3,456);

        class MyEntity implements Entity {
            public Goid referencedId;

            @Override
            public String getId() {
                return id.toString();
            }

            @com.l7tech.search.Dependency(methodReturnType = com.l7tech.search.Dependency.MethodReturnType.GOID)
            public Goid getReferencedId(){
                return referencedId;
            }

            public void setReferencedId(Goid referencedId){
                this.referencedId = referencedId;
            }
        }
        MyEntity entity = new MyEntity();
        entity.setReferencedId(id);

        Map<EntityHeader,EntityHeader> replacementMap= new HashMap<>();
        replacementMap.put(new EntityHeader(id, EntityType.ANY, null, null), new EntityHeader(replacementID, EntityType.ANY, null, null));

        dependencyFinder.replaceDependencies(entity, replacementMap, false);

        Assert.assertEquals(replacementID, entity.getReferencedId());
    }

    @Test
    public void testReplaceDependenciesInMap() throws FindException, CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
        final Goid id = new Goid(3,123);
        final Goid replacementID = new Goid(3,456);

        class MyEntity implements Entity {
            public final static String referenceKey = "referenceKey";
            public Map<String, Object> propertiesMap = new HashMap<>();

            @Override
            public String getId() {
                return id.toString();
            }

            @com.l7tech.search.Dependency(methodReturnType = com.l7tech.search.Dependency.MethodReturnType.GOID, key = referenceKey)
            public Map<String, Object> getProperties(){
                return propertiesMap;
            }
        }
        MyEntity entity = new MyEntity();
        entity.getProperties().put(MyEntity.referenceKey, id);

        Map<EntityHeader,EntityHeader> replacementMap= new HashMap<>();
        replacementMap.put(new EntityHeader(id, EntityType.ANY, null, null), new EntityHeader(replacementID, EntityType.ANY, null, null));

        dependencyFinder.replaceDependencies(entity, replacementMap, false);

        Assert.assertEquals(replacementID, entity.getProperties().get(MyEntity.referenceKey));
    }
}
