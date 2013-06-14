package com.l7tech.server.search.processors;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.DependencyAnalyzerException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This was created: 6/11/13 as 10:30 AM
 *
 * @author Victor Kazakov
 */
@SuppressWarnings({"unchecked", "UnusedDeclaration"})
@RunWith(MockitoJUnitRunner.class)
public class GenericDependencyProcessorTest {

    @Mock
    private EntityCrud entityCrud;

    @Mock
    private IdentityProviderConfigManager identityProviderConfigManager;

    @Mock
    private DependencyFinder dependencyFinder;

    @InjectMocks
    GenericDependencyProcessor processor;

    @Test
    public void testCreateDependentObjectFromPolicy() {
        final String policyName = "My Policy";
        Policy policy = new Policy(PolicyType.INTERNAL, policyName, null, false);
        final String guid = "GUID";
        policy.setGuid(guid);

        DependentObject dependentObject = processor.createDependentObject(policy);
        Assert.assertEquals(DependentEntity.class, dependentObject.getClass());
        DependentEntity dependentEntity = (DependentEntity) dependentObject;

        Assert.assertEquals(policyName, dependentEntity.getName());
        Assert.assertEquals(guid, dependentEntity.getPublicID());
        Assert.assertEquals(EntityType.POLICY, dependentEntity.getEntityType());
    }

    @Test
    public void testCreateDependentObjectFromEntity() {
        final String myID = "myID";
        Entity entity = new Entity() {
            @Override
            public String getId() {
                return myID;
            }
        };

        DependentObject dependentObject = processor.createDependentObject(entity);
        Assert.assertEquals(DependentEntity.class, dependentObject.getClass());
        DependentEntity dependentEntity = (DependentEntity) dependentObject;

        Assert.assertNull(dependentEntity.getName());
        Assert.assertNull(dependentEntity.getPublicID());
        Assert.assertEquals(myID, dependentEntity.getInternalID());
        Assert.assertEquals(EntityType.ANY, dependentEntity.getEntityType());
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
        Assert.assertNull(dependentEntity.getPublicID());
        Assert.assertEquals(myID, dependentEntity.getInternalID());
        Assert.assertEquals(EntityType.ANY, dependentEntity.getEntityType());
    }

    @Test
    public void testFindUsingLongOID() throws FindException {
        final long oid = 123;
        final EntityType type = EntityType.ANY;

        Mockito.when(entityCrud.find(Matchers.<EntityHeader>argThat(new BaseMatcher<EntityHeader>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof EntityHeader && ((EntityHeader) o).getOid() == oid && ((EntityHeader) o).getType().equals(type);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("unexpected entity header. Expected Header with oid: " + oid + " and Type: " + type);
            }
        }))).thenReturn(new Entity() {
            @Override
            public String getId() {
                return String.valueOf(oid);
            }
        });

        List<Entity> entities = processor.find(oid, new DependencyAnnotation(com.l7tech.search.Dependency.DependencyType.GENERIC, com.l7tech.search.Dependency.MethodReturnType.OID));

        Mockito.verify(entityCrud).find(Matchers.any(EntityHeader.class));
        Assert.assertNotNull(entities);
        Assert.assertEquals(1, entities.size());
        Assert.assertEquals(oid, Long.parseLong(entities.get(0).getId()));
    }

    @Test
    public void testFindUsingStringOID() throws FindException {
        final String oid = "123";
        final EntityType type = EntityType.ANY;

        Mockito.when(entityCrud.find(Matchers.<EntityHeader>argThat(new BaseMatcher<EntityHeader>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof EntityHeader && ((EntityHeader) o).getOid() == Long.parseLong(oid) && ((EntityHeader) o).getType().equals(type);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("unexpected entity header. Expected Header with oid: " + oid + " and Type: " + type);
            }
        }))).thenReturn(new Entity() {
            @Override
            public String getId() {
                return String.valueOf(oid);
            }
        });

        List<Entity> entities = processor.find(oid, new DependencyAnnotation(com.l7tech.search.Dependency.DependencyType.GENERIC, com.l7tech.search.Dependency.MethodReturnType.OID));

        Mockito.verify(entityCrud).find(Matchers.any(EntityHeader.class));
        Assert.assertNotNull(entities);
        Assert.assertEquals(1, entities.size());
        Assert.assertEquals(oid, entities.get(0).getId());
    }

    @Test
    public void testFindUsingLongOIDArray() throws FindException {
        final long[] oids = {123, 456, 789};
        final EntityType type = EntityType.ANY;

        for (int i = 0; i < oids.length; i++) {
            final int finalI = i;
            Mockito.when(entityCrud.find(Matchers.<EntityHeader>argThat(new BaseMatcher<EntityHeader>() {
                @Override
                public boolean matches(Object o) {
                    return o instanceof EntityHeader && ((EntityHeader) o).getOid() == oids[finalI] && ((EntityHeader) o).getType().equals(type);
                }

                @Override
                public void describeTo(Description description) {
                    description.appendText("unexpected entity header. Expected Header with oid: " + oids[finalI] + " and Type: " + type);
                }
            }))).thenReturn(new Entity() {
                @Override
                public String getId() {
                    return String.valueOf(oids[finalI]);
                }
            });
        }

        List<Entity> entities = processor.find(oids, new DependencyAnnotation(com.l7tech.search.Dependency.DependencyType.GENERIC, com.l7tech.search.Dependency.MethodReturnType.OID));

        Mockito.verify(entityCrud, new Times(3)).find(Matchers.any(EntityHeader.class));
        Assert.assertNotNull(entities);
        Assert.assertEquals(3, entities.size());
        Assert.assertEquals(oids[0], Long.parseLong(entities.get(0).getId()));
        Assert.assertEquals(oids[1], Long.parseLong(entities.get(1).getId()));
        Assert.assertEquals(oids[2], Long.parseLong(entities.get(2).getId()));
    }

    @Test
    public void testFindDependencies() throws FindException {
        final String oid = "123";

        final Entity returnedEntity = new EmptyEntity("MyEntity");

        Mockito.when(dependencyFinder.getDependency(Matchers.eq(returnedEntity))).thenReturn(new Dependency(new DependentObject(returnedEntity.getId()) {
        }));

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

        Mockito.verify(dependencyFinder).getDependency(Matchers.eq(returnedEntity));

        Mockito.verify(dependencyFinder, new Times(1)).getDependency(Matchers.any(Entity.class));

        Assert.assertNotNull(dependencies);
        Assert.assertEquals(1, dependencies.size());
        Assert.assertNotNull(dependencies.get(0));
        Assert.assertEquals(returnedEntity.getId(), dependencies.get(0).getDependent().getName());
    }

    @Test
    public void testFindDependenciesWithAnnotations() throws FindException, DependencyAnalyzerException {
        final String oid = "123";
        final String assertionName = "Assertion Name";

        ArrayList<String> entityNames = new ArrayList<>(Arrays.asList("MyEntity", "MyEntity2"));
        final Entity returnedEntity = new EmptyEntity(entityNames.get(0));
        final Entity returnedEntity2 = new EmptyEntity(entityNames.get(1));

        Mockito.when(dependencyFinder.getDependency(Matchers.eq(returnedEntity))).thenReturn(new Dependency(new DependentObject(returnedEntity.getId()) {
        }));
        Mockito.when(dependencyFinder.getDependency(Matchers.eq(returnedEntity2))).thenReturn(new Dependency(new DependentObject(returnedEntity2.getId()) {
        }));

        Mockito.when(dependencyFinder.retrieveEntities(Matchers.eq(assertionName), Matchers.argThat(new BaseMatcher<com.l7tech.search.Dependency>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof com.l7tech.search.Dependency && ((com.l7tech.search.Dependency) o).type().equals(com.l7tech.search.Dependency.DependencyType.ASSERTION) && ((com.l7tech.search.Dependency) o).methodReturnType().equals(com.l7tech.search.Dependency.MethodReturnType.NAME);
            }

            @Override
            public void describeTo(Description description) {
            }
        }))).thenReturn(Arrays.asList(returnedEntity2));

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

            @com.l7tech.search.Dependency(type = com.l7tech.search.Dependency.DependencyType.ASSERTION, methodReturnType = com.l7tech.search.Dependency.MethodReturnType.NAME)
            protected String getAssertion() {
                return assertionName;
            }
        }, dependencyFinder);

        Mockito.verify(dependencyFinder).getDependency(Matchers.eq(returnedEntity));

        Mockito.verify(dependencyFinder, new Times(2)).getDependency(Matchers.any(Entity.class));

        Assert.assertNotNull(dependencies);
        Assert.assertEquals(2, dependencies.size());
        Assert.assertNotNull(dependencies.get(0));
        Assert.assertTrue(entityNames.contains(dependencies.get(0).getDependent().getName()));
        entityNames.remove(dependencies.get(0).getDependent().getName());
        Assert.assertNotNull(dependencies.get(1));
        Assert.assertTrue(entityNames.contains(dependencies.get(1).getDependent().getName()));
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

    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static class DependencyAnnotation implements com.l7tech.search.Dependency {
        boolean dependency = true;
        DependencyType type = DependencyType.GENERIC;
        MethodReturnType methodReturnType = MethodReturnType.OID;

        private DependencyAnnotation(DependencyType type, MethodReturnType methodReturnType) {
            this.type = type;
            this.methodReturnType = methodReturnType;
        }

        @Override
        public boolean isDependency() {
            return dependency;
        }

        @Override
        public DependencyType type() {
            return type;
        }

        @Override
        public MethodReturnType methodReturnType() {
            return methodReturnType;
        }

        @Override
        public String key() {
            return "";
        }

        @Override
        public boolean searchObject() {
            return false;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return com.l7tech.search.Dependency.class;
        }
    }
}
