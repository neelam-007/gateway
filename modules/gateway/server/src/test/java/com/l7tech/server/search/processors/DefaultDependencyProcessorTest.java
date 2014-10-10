package com.l7tech.server.search.processors;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.search.Dependencies;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.*;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.internal.verification.Times;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
* This was created: 6/11/13 as 10:30 AM
*
* @author Victor Kazakov
*/
@SuppressWarnings({"unchecked", "UnusedDeclaration"})
@RunWith(MockitoJUnitRunner.class)
public class DefaultDependencyProcessorTest {
    private static final Goid GOID = new Goid(123,456);
    private static final String GOID_STR = Goid.toString(GOID);

    @Mock
    private EntityCrud entityCrud;

    @Mock
    private IdentityProviderConfigManager identityProviderConfigManager;

    @InjectMocks
    DefaultDependencyProcessor processor = new DefaultDependencyProcessor();

    @Spy
    private DependencyFinder dependencyFinder = new DependencyFinder(Collections.<String,Object>emptyMap(), new DependencyProcessorStore(CollectionUtils.MapBuilder.<com.l7tech.search.Dependency.DependencyType, InternalDependencyProcessor>builder().put(com.l7tech.search.Dependency.DependencyType.ANY, processor).map()));

    @Test
    public void testCreateDependentObjectFromPolicy() {
        final String policyName = "My Policy";
        Policy policy = new Policy(PolicyType.INTERNAL, policyName, null, false);
        final String guid = "GUID";
        policy.setGuid(guid);
        final Goid id = new Goid(0,123);
        policy.setGoid(id);

        DependentObject dependentObject = processor.createDependentObject(policy);
        Assert.assertEquals(DependentEntity.class, dependentObject.getClass());
        DependentEntity dependentEntity = (DependentEntity) dependentObject;

        Assert.assertEquals(policyName, dependentEntity.getName());
        Assert.assertEquals(id.toString(), dependentEntity.getEntityHeader().getStrId());
        Assert.assertEquals(EntityType.POLICY, dependentEntity.getDependencyType().getEntityType());
    }

    @Test
    public void testCreateDependentObjectFromEntity() {
        final Goid myID = new Goid(0,123);
        Entity entity = new Entity() {
            @Override
            public String getId() {
                return myID.toString();
            }
        };

        DependentObject dependentObject = processor.createDependentObject(entity);
        Assert.assertEquals(DependentEntity.class, dependentObject.getClass());
        DependentEntity dependentEntity = (DependentEntity) dependentObject;

        Assert.assertNull(dependentEntity.getName());
        Assert.assertEquals(myID.toString(), dependentEntity.getEntityHeader().getStrId());
        Assert.assertEquals(EntityType.ANY, dependentEntity.getDependencyType().getEntityType());
    }

    @Test
    public void testCreateDependentObjectFromNamedEntity() {
        final String myID = "myID";
        final String name = "myEntityName";
        Entity entity = new NamedEntity() {
            @Override
            public String getId() {
                return myID;
            }

            @Override
            public String getName() {
                return name;
            }
        };

        DependentObject dependentObject = processor.createDependentObject(entity);
        Assert.assertEquals(DependentEntity.class, dependentObject.getClass());
        DependentEntity dependentEntity = (DependentEntity) dependentObject;

        Assert.assertEquals(name, dependentEntity.getName());
        Assert.assertEquals(myID, dependentEntity.getEntityHeader().getStrId());
        Assert.assertEquals(EntityType.ANY, dependentEntity.getDependencyType().getEntityType());
    }

    @Test
    public void testFindUsingGOID() throws FindException {
        final EntityType type = EntityType.ANY;

        Mockito.when(entityCrud.find(Matchers.<EntityHeader>argThat(new BaseMatcher<EntityHeader>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof EntityHeader && ((EntityHeader) o).getGoid().equals(GOID) && ((EntityHeader) o).getType().equals(type);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("unexpected entity header. Expected Header with goid: " + GOID + " and Type: " + type);
            }
        }))).thenReturn(new Entity() {
            @Override
            public String getId() {
                return String.valueOf(GOID);
            }
        });

        List<DependencyFinder.FindResults> entities = processor.find(GOID, com.l7tech.search.Dependency.DependencyType.ANY, com.l7tech.search.Dependency.MethodReturnType.GOID);

        Mockito.verify(entityCrud).find(Matchers.any(EntityHeader.class));
        Assert.assertNotNull(entities);
        Assert.assertEquals(1, entities.size());
        Assert.assertEquals(GOID, Goid.parseGoid(((Entity)entities.get(0).getEntity()).getId()));
    }

    @Test
    public void testFindUsingStringGOID() throws FindException {
        final EntityType type = EntityType.ANY;

        Mockito.when(entityCrud.find(Matchers.<EntityHeader>argThat(new BaseMatcher<EntityHeader>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof EntityHeader && ((EntityHeader) o).getGoid().equals(GOID)&& ((EntityHeader) o).getType().equals(type);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("unexpected entity header. Expected Header with goid: " + GOID + " and Type: " + type);
            }
        }))).thenReturn(new Entity() {
            @Override
            public String getId() {
                return String.valueOf(GOID);
            }
        });

        List<DependencyFinder.FindResults> entities = processor.find(GOID_STR, com.l7tech.search.Dependency.DependencyType.ANY, com.l7tech.search.Dependency.MethodReturnType.GOID);

        Mockito.verify(entityCrud).find(Matchers.any(EntityHeader.class));
        Assert.assertNotNull(entities);
        Assert.assertEquals(1, entities.size());
        Assert.assertEquals(GOID_STR, ((Entity)entities.get(0).getEntity()).getId());
    }

    @Test
    public void testFindUsingLongOIDArray() throws FindException {
        final Goid[] goids = {new Goid(11,123), new Goid(11,456), new Goid(11,789)};
        final EntityType type = EntityType.ANY;

        for (int i = 0; i < goids.length; i++) {
            final int finalI = i;
            Mockito.when(entityCrud.find(Matchers.<EntityHeader>argThat(new BaseMatcher<EntityHeader>() {
                @Override
                public boolean matches(Object o) {
                    return o instanceof EntityHeader && ((EntityHeader) o).getGoid().equals(goids[finalI]) && ((EntityHeader) o).getType().equals(type);
                }

                @Override
                public void describeTo(Description description) {
                    description.appendText("unexpected entity header. Expected Header with goid: " + goids[finalI] + " and Type: " + type);
                }
            }))).thenReturn(new Entity() {
                @Override
                public String getId() {
                    return String.valueOf(goids[finalI]);
                }
            });
        }

        List<DependencyFinder.FindResults> entities = processor.find(goids, com.l7tech.search.Dependency.DependencyType.ANY, com.l7tech.search.Dependency.MethodReturnType.GOID);

        Mockito.verify(entityCrud, new Times(3)).find(Matchers.any(EntityHeader.class));
        Assert.assertNotNull(entities);
        Assert.assertEquals(3, entities.size());
        Assert.assertEquals(goids[0], Goid.parseGoid(((Entity)entities.get(0).getEntity()).getId()));
        Assert.assertEquals(goids[1], Goid.parseGoid(((Entity)entities.get(1).getEntity()).getId()));
        Assert.assertEquals(goids[2], Goid.parseGoid(((Entity)entities.get(2).getEntity()).getId()));
    }

    @Test
    public void testFindDependencies() throws FindException, CannotRetrieveDependenciesException {
        final String oid = "123";

        final Entity returnedEntity = new EmptyEntity("MyEntity");

        @SuppressWarnings("unchecked")
        List<Dependency> dependencies = processor.findDependencies(new Entity() {
            @Override
            public String getId() {
                return oid;
            }

            public Entity getMyEntity() {
                return returnedEntity;
            }

            public Entity entityMethod() {
                return new EmptyEntity("Will not be returned");
            }

            protected Entity getProtectedEntity() {
                return new EmptyEntity("protectedEntity");
            }

            private Entity getPrivateEntity() {
                return new EmptyEntity("privateEntity");
            }

            public Entity getThisEntity() {
                return this;
            }
        }, dependencyFinder);

        Mockito.verify(dependencyFinder).getDependency(Matchers.eq(DependencyFinder.FindResults.create(returnedEntity,null)));

        Mockito.verify(dependencyFinder, new Times(2)).getDependency(Matchers.any(DependencyFinder.FindResults.class));

        Assert.assertNotNull(dependencies);
        Assert.assertEquals(2, dependencies.size());
        Assert.assertNotNull(Functions.grepFirst(dependencies, new Functions.Unary<Boolean, Dependency>() {
            @Override
            public Boolean call(Dependency dependency) {
                return returnedEntity.getId().equals(((DependentEntity) dependency.getDependent()).getEntityHeader().getStrId());
            }
        }));

        Assert.assertNotNull(Functions.grepFirst(dependencies, new Functions.Unary<Boolean, Dependency>() {
            @Override
            public Boolean call(Dependency dependency) {
                return oid.equals(((DependentEntity) dependency.getDependent()).getEntityHeader().getStrId());
            }
        }));
    }

    @Test
    public void testFindDependenciesWithAnnotations() throws FindException, CannotRetrieveDependenciesException {
        final String oid = "123";

        final Entity returnedEntity = new EmptyEntity("MyEntity");

        @SuppressWarnings("unchecked")
        List<Dependency> dependencies = processor.findDependencies(new Entity() {
            @Override
            public String getId() {
                return oid;
            }

            @com.l7tech.search.Dependency(isDependency = false)
            public Entity getMyEntity() {
                return new EmptyEntity("Will not be returned");
            }

            @com.l7tech.search.Dependency
            public Entity entityMethod() {
                return returnedEntity;
            }
        }, dependencyFinder);

        Mockito.verify(dependencyFinder).getDependency(Matchers.eq(DependencyFinder.FindResults.create(returnedEntity,null)));

        Mockito.verify(dependencyFinder, new Times(1)).getDependency(Matchers.any(DependencyFinder.FindResults.class));

        Assert.assertNotNull(dependencies);
        Assert.assertEquals(1, dependencies.size());
        Assert.assertNotNull(dependencies.get(0));
        Assert.assertEquals(returnedEntity.getId(), ((DependentEntity) dependencies.get(0).getDependent()).getEntityHeader().getStrId());
    }

    @Test
    public void testFindDependenciesWithSubObjects() throws FindException, CannotRetrieveDependenciesException {
        final String oid = "123";

        final Entity returnedEntity = new EmptyEntity("MyEntity");
        final Object helper = new Object() {
            @com.l7tech.search.Dependency(isDependency = false)
            public Entity getMyEntity() {
                return new EmptyEntity("Will not be returned");
            }

            @com.l7tech.search.Dependency
            public Entity entityMethod() {
                return returnedEntity;
            }
        };

        List<DependencySearchResults> rtn = dependencyFinder.process(Arrays.<DependencyFinder.FindResults>asList(DependencyFinder.FindResults.create(new Entity() {
            @Override
            public String getId() {
                return oid;
            }

            @com.l7tech.search.Dependency(searchObject = true)
            public Object getHelper() {
                return helper;
            }
        },null)));
        List<Dependency> dependencies = rtn.get(0).getDependencies();

        Mockito.verify(dependencyFinder).getDependency(Matchers.eq(DependencyFinder.FindResults.create(returnedEntity,null)));

        Mockito.verify(dependencyFinder, new Times(2)).getDependency(Matchers.any(DependencyFinder.FindResults.class));

        Assert.assertNotNull(dependencies);
        Assert.assertEquals(1, dependencies.size());
        Assert.assertNotNull(dependencies.get(0));
        Assert.assertEquals(returnedEntity.getId(), ((DependentEntity) dependencies.get(0).getDependent()).getEntityHeader().getStrId());
    }

    @Test
    public void testFindDependenciesFromPropertyGetter() throws FindException, CannotRetrieveDependenciesException {
        final String oid = "123";

        final Entity returnedEntity = new EmptyEntity("MyEntity");

        List<DependencySearchResults> rtn = dependencyFinder.process(Arrays.<DependencyFinder.FindResults>asList(DependencyFinder.FindResults.create(new Entity() {
            public static final String PROPERTY_KEY = "My.Property.key";

            @Override
            public String getId() {
                return oid;
            }

            @com.l7tech.search.Dependency(key = PROPERTY_KEY)
            public Object getProperty(String key) {
                if (PROPERTY_KEY.equals(key))
                    return returnedEntity;
                return null;
            }
        },null)));
        List<Dependency> dependencies = rtn.get(0).getDependencies();

        Mockito.verify(dependencyFinder).getDependency(Matchers.eq(DependencyFinder.FindResults.create(returnedEntity,null)));

        Mockito.verify(dependencyFinder, new Times(2)).getDependency(Matchers.any(DependencyFinder.FindResults.class));

        Assert.assertNotNull(dependencies);
        Assert.assertEquals(1, dependencies.size());
        Assert.assertNotNull(dependencies.get(0));
        Assert.assertEquals(returnedEntity.getId(), ((DependentEntity) dependencies.get(0).getDependent()).getEntityHeader().getStrId());
    }

    @Test
    public void testFindDependenciesFromMap() throws FindException, CannotRetrieveDependenciesException {
        final String oid = "123";

        final Entity returnedEntity = new EmptyEntity("MyEntity");

        List<DependencySearchResults> rtn = dependencyFinder.process(Arrays.<DependencyFinder.FindResults>asList(DependencyFinder.FindResults.create(new Entity() {
            public static final String PROPERTY_KEY = "My.Property.key";

            @Override
            public String getId() {
                return oid;
            }

            @com.l7tech.search.Dependency(key = PROPERTY_KEY)
            public Map<String, Object> getProperties() {
                return CollectionUtils.MapBuilder.<String, Object>builder().put(PROPERTY_KEY, returnedEntity).map();
            }
        },null)));
        List<Dependency> dependencies = rtn.get(0).getDependencies();

        Mockito.verify(dependencyFinder).getDependency(Matchers.eq(DependencyFinder.FindResults.create(returnedEntity,null)));

        Mockito.verify(dependencyFinder, new Times(2)).getDependency(Matchers.any(DependencyFinder.FindResults.class));

        Assert.assertNotNull(dependencies);
        Assert.assertEquals(1, dependencies.size());
        Assert.assertNotNull(dependencies.get(0));
        Assert.assertEquals(returnedEntity.getId(), ((DependentEntity) dependencies.get(0).getDependent()).getEntityHeader().getStrId());
    }

    @Test
    public void testFindMultipleDependenciesFromMap() throws FindException, CannotRetrieveDependenciesException {
        final String oid = "123";

        List<String> names = Arrays.asList("MyEntity", "MyEntity2");
        final Entity returnedEntity = new EmptyEntity(names.get(0));
        final Entity returnedEntity2 = new EmptyEntity(names.get(1));

        List<DependencySearchResults> rtn = dependencyFinder.process(Arrays.<DependencyFinder.FindResults>asList(DependencyFinder.FindResults.create(new Entity() {
            public static final String PROPERTY_KEY = "My.Property.key";
            public static final String PROPERTY_KEY2 = "My.Property.key2";

            @Override
            public String getId() {
                return oid;
            }

            @Dependencies({
                    @com.l7tech.search.Dependency(key = PROPERTY_KEY),
                    @com.l7tech.search.Dependency(key = PROPERTY_KEY2)
            })
            public Map<String, Object> getProperties() {
                return CollectionUtils.MapBuilder.<String, Object>builder().put(PROPERTY_KEY, returnedEntity).put(PROPERTY_KEY2, returnedEntity2).map();
            }
        },null)));
        List<Dependency> dependencies = rtn.get(0).getDependencies();

        Mockito.verify(dependencyFinder).getDependency(Matchers.eq(DependencyFinder.FindResults.create(returnedEntity,null)));

        Mockito.verify(dependencyFinder, new Times(3)).getDependency(Matchers.any(DependencyFinder.FindResults.class));

        Assert.assertNotNull(dependencies);
        Assert.assertEquals(2, dependencies.size());
        Assert.assertNotNull(dependencies.get(0));
        Assert.assertTrue(names.contains(((DependentEntity) dependencies.get(0).getDependent()).getEntityHeader().getStrId()));
        Assert.assertNotNull(dependencies.get(1));
        Assert.assertTrue(names.contains(((DependentEntity) dependencies.get(1).getDependent()).getEntityHeader().getStrId()));
    }

    private class EmptyEntity implements Entity {

        private String id;

        private EmptyEntity(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EmptyEntity that = (EmptyEntity) o;

            return !(id != null ? !id.equals(that.id) : that.id != null);

        }
    }
}
