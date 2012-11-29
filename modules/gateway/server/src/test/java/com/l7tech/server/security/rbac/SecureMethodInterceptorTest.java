package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.util.Injector;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import javax.security.auth.Subject;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.*;

import static com.l7tech.objectmodel.EntityType.GENERIC;
import static com.l7tech.objectmodel.EntityType.USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: vkazakov
 * Date: 11/28/12
 * Time: 10:20 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Parameterized.class)
public class SecureMethodInterceptorTest {
    private SecuredMethodInterceptor interceptor;
    @Mock
    private RbacServices rbacServices;
    @Mock
    private EntityFinder entityFinder;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private MockAdmin mockAdmin;
    @Mock
    private Injector injector;

    //has privileges to everything
    private static InternalUser privilegedUser = new InternalUser("king");
    //has specific privileges but to global privileges
    private static InternalUser semiPrivilegedUser = new InternalUser("knight");
    //has no privileges
    private static InternalUser unprivilegedUser = new InternalUser("peasant");

    //The name of the method to run. The method must me a method in MockAdmin
    private String methodName;
    // the user to run the method as
    private InternalUser user;
    //The args to pass to the method
    private Object[] args;
    //The return value the method should return
    private Object rtn;
    //the expected return value from the SecuredMethodInterceptor
    private Object expectedRtn;
    //An expected exception if one should be thrown
    private Class<? extends Throwable> expectedException;

    //These are different types of entities and headers used by the tests
    private static MyEntity allowedEntityNew = new MyEntity();
    private static MyEntity deniedEntityNew = new MyEntity();
    private static MyEntity allowedEntityExisting = new MyEntity(123l);
    private static MyEntity deniedEntityExisting = new MyEntity(456l);
    private static EntityHeader allowedEntityHeader = new EntityHeader(allowedEntityExisting.getId(), GENERIC, "allowedTest", "test");
    private static EntityHeader deniedEntityHeader = new EntityHeader(deniedEntityExisting.getId(), GENERIC, "deniedTest", "test");
    private static IdentityHeader allowedIdentityHeaderUserType = new IdentityHeader(allowedEntityExisting.getOid(), allowedEntityExisting.getId(), USER, null, null, null, null);
    private static IdentityHeader deniedIdentityHeaderUserType = new IdentityHeader(deniedEntityExisting.getOid(), deniedEntityExisting.getId(), USER, null, null, null, null);
    public static String genericString = UUID.randomUUID().toString();

    /**
     * This is the list of tests to run. Each value in the array is a parameter to that is passed into the constructor.
     * 1) The name of the method to run. The method must me a method in MockAdmin
     * 2) the user to run the method as
     * 3) The args to pass to the method
     * 4) The return value the method will return
     * 5) The expected return value from the SecuredMethodInterceptor
     * 6) An expected exception if one should be thrown by the SecuredMethodInterceptor
     *
     * @return
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                //find headers Tests
                {"findHeaders", privilegedUser, null, Collections.singleton(allowedEntityHeader), Collections.singleton(allowedEntityHeader), null},
                {"findHeaders", privilegedUser, null, Collections.singleton(deniedEntityHeader), Collections.singleton(deniedEntityHeader), null},
                {"findHeaders", semiPrivilegedUser, null, Collections.singleton(deniedEntityHeader), Collections.emptySet(), null},
                {"findHeaders", unprivilegedUser, null, Collections.singleton(deniedEntityHeader), Collections.emptySet(), null},
                {"findHeaders", privilegedUser, null, Arrays.asList(allowedEntityHeader, deniedEntityHeader), Arrays.asList(allowedEntityHeader, deniedEntityHeader), null},
                {"findHeaders", semiPrivilegedUser, null, Arrays.asList(allowedEntityHeader, deniedEntityHeader), Collections.singleton(allowedEntityHeader), null},
                {"findHeaders", unprivilegedUser, null, Arrays.asList(allowedEntityHeader, deniedEntityHeader), Collections.emptySet(), null},
                //findHeader
                {"findHeader", privilegedUser, null, allowedEntityHeader, allowedEntityHeader, null},
                {"findHeader", privilegedUser, null, deniedEntityHeader, deniedEntityHeader, null},
                {"findHeader", semiPrivilegedUser, null, allowedEntityHeader, allowedEntityHeader, null},
                {"findHeader", semiPrivilegedUser, null, deniedEntityHeader, null, PermissionDeniedException.class},
                {"findHeader", unprivilegedUser, null, deniedEntityHeader, null, PermissionDeniedException.class},
                //find entities tests
                {"findEntities", privilegedUser, null, Arrays.asList(allowedEntityExisting), Collections.singleton(allowedEntityExisting), null},
                {"findEntities", privilegedUser, null, Arrays.asList(deniedEntityExisting), Arrays.asList(deniedEntityExisting), null},
                {"findEntities", semiPrivilegedUser, null, Arrays.asList(deniedEntityExisting), Collections.emptySet(), null},
                {"findEntities", privilegedUser, null, Arrays.asList(allowedEntityExisting, deniedEntityExisting), Arrays.asList(allowedEntityExisting, deniedEntityExisting), null},
                {"findEntities", semiPrivilegedUser, null, Arrays.asList(allowedEntityExisting, deniedEntityExisting), Collections.singleton(allowedEntityExisting), null},
                {"findEntities", unprivilegedUser, null, Arrays.asList(allowedEntityExisting, deniedEntityExisting), Collections.emptySet(), null},
                //find entity
                {"findEntity", privilegedUser, null, allowedEntityExisting, allowedEntityExisting, null},
                {"findEntity", privilegedUser, null, deniedEntityExisting, null, PermissionDeniedException.class},
                //save
                {"save", privilegedUser, new Object[]{allowedEntityNew}, null, null, null},
                {"save", privilegedUser, new Object[]{deniedEntityNew}, null, null, PermissionDeniedException.class},
                {"save", privilegedUser, new Object[]{allowedEntityExisting}, null, null, PermissionDeniedException.class},
                {"save", privilegedUser, new Object[0], null, null, null},
                {"save", unprivilegedUser, new Object[0], null, null, PermissionDeniedException.class},
                //Note that update throws an exception!
                //TODO: Should this be throwing an UnsupportedOperationException?
                {"update", privilegedUser, new Object[]{allowedEntityExisting}, null, null, UnsupportedOperationException.class},
                {"update", privilegedUser, new Object[]{deniedEntityExisting}, null, null, UnsupportedOperationException.class},
                //saveOrUpdate
                {"saveOrUpdate", privilegedUser, new Object[]{allowedEntityNew}, null, null, null},
                {"saveOrUpdate", privilegedUser, new Object[]{deniedEntityNew}, null, null, PermissionDeniedException.class},
                {"saveOrUpdate", privilegedUser, new Object[]{allowedEntityExisting}, null, null, null},
                {"saveOrUpdate", privilegedUser, new Object[]{deniedEntityExisting}, null, null, PermissionDeniedException.class},
                {"saveOrUpdate", privilegedUser, new Object[0], null, null, null},
                {"saveOrUpdate", unprivilegedUser, new Object[0], null, null, PermissionDeniedException.class},
                //deleteById
                {"deleteById", privilegedUser, new Object[]{allowedEntityNew.getId()}, null, null, ObjectNotFoundException.class},
                {"deleteById", privilegedUser, new Object[]{deniedEntityNew.getId()}, null, null, ObjectNotFoundException.class},
                {"deleteById", privilegedUser, new Object[]{allowedEntityExisting.getId()}, null, null, null},
                {"deleteById", privilegedUser, new Object[]{deniedEntityExisting.getId()}, null, null, PermissionDeniedException.class},
                //deleteIdentityById
                {"deleteIdentityById", privilegedUser, new Object[]{allowedEntityNew.getOid(), allowedEntityNew.getId()}, null, null, NullPointerException.class},
                {"deleteIdentityById", privilegedUser, new Object[]{deniedEntityNew.getOid(), deniedEntityNew.getId()}, null, null, NullPointerException.class},
                {"deleteIdentityById", privilegedUser, new Object[]{allowedEntityExisting.getOid(), allowedEntityExisting.getId()}, null, null, null},
                {"deleteIdentityById", privilegedUser, new Object[]{deniedEntityExisting.getOid(), deniedEntityExisting.getId()}, null, null, PermissionDeniedException.class},
                //deleteByUniqueAttribute
                {"deleteByUniqueAttribute", privilegedUser, null, null, null, null},
                {"deleteByUniqueAttribute", unprivilegedUser, null, null, null, PermissionDeniedException.class},
                //deleteEntity
                {"deleteEntity", privilegedUser, new Object[]{allowedEntityNew}, null, null, null},
                {"deleteEntity", privilegedUser, new Object[]{deniedEntityNew}, null, null, PermissionDeniedException.class},
                {"deleteEntity", privilegedUser, new Object[]{allowedEntityExisting}, null, null, null},
                {"deleteEntity", privilegedUser, new Object[]{deniedEntityExisting}, null, null, PermissionDeniedException.class},
                //deleteMulti
                {"deleteMulti", privilegedUser, null, null, null, null},
                {"deleteMulti", unprivilegedUser, null, null, null, PermissionDeniedException.class},
                //getPropertyOfEntity
                {"getPropertyOfEntity", privilegedUser, new Object[]{allowedEntityNew}, genericString, genericString, null},
                {"getPropertyOfEntity", privilegedUser, new Object[]{deniedEntityNew}, genericString, genericString, PermissionDeniedException.class},
                {"getPropertyOfEntity", privilegedUser, new Object[]{allowedEntityExisting}, genericString, genericString, null},
                {"getPropertyOfEntity", privilegedUser, new Object[]{deniedEntityExisting}, genericString, genericString, PermissionDeniedException.class},
                //getPropertyById
                {"getPropertyById", privilegedUser, new Object[]{allowedEntityNew.getId()}, genericString, genericString, ObjectNotFoundException.class},
                {"getPropertyById", privilegedUser, new Object[]{deniedEntityNew.getId()}, genericString, genericString, ObjectNotFoundException.class},
                {"getPropertyById", privilegedUser, new Object[]{allowedEntityExisting.getId()}, genericString, genericString, null},
                {"getPropertyById", privilegedUser, new Object[]{deniedEntityExisting.getId()}, genericString, genericString, PermissionDeniedException.class},
                //getIdentityPropertyById
                {"getIdentityPropertyById", privilegedUser, new Object[]{allowedEntityNew.getOid(), allowedEntityNew.getId()}, genericString, genericString, NullPointerException.class},
                {"getIdentityPropertyById", privilegedUser, new Object[]{deniedEntityNew.getOid(), deniedEntityNew.getId()}, genericString, genericString, NullPointerException.class},
                {"getIdentityPropertyById", privilegedUser, new Object[]{allowedEntityExisting.getOid(), allowedEntityExisting.getId()}, genericString, genericString, null},
                {"getIdentityPropertyById", privilegedUser, new Object[]{deniedEntityExisting.getOid(), deniedEntityExisting.getId()}, genericString, genericString, PermissionDeniedException.class},
                //setPropertyOfEntity
                {"setPropertyOfEntity", privilegedUser, new Object[]{genericString, allowedEntityNew}, null, null, null},
                {"setPropertyOfEntity", privilegedUser, new Object[]{genericString, deniedEntityNew}, null, null, PermissionDeniedException.class},
                {"setPropertyOfEntity", privilegedUser, new Object[]{genericString, allowedEntityExisting}, null, null, null},
                {"setPropertyOfEntity", privilegedUser, new Object[]{genericString, deniedEntityExisting}, null, null, PermissionDeniedException.class},
                //setPropertyById
                {"setPropertyById", privilegedUser, new Object[]{allowedEntityNew.getId()}, null, null, ObjectNotFoundException.class},
                {"setPropertyById", privilegedUser, new Object[]{deniedEntityNew.getId()}, null, null, ObjectNotFoundException.class},
                {"setPropertyById", privilegedUser, new Object[]{allowedEntityExisting.getId()}, null, null, null},
                {"setPropertyById", privilegedUser, new Object[]{deniedEntityExisting.getId()}, null, null, PermissionDeniedException.class},
                //setPropertyByUniqueAttribute
                {"setPropertyByUniqueAttribute", privilegedUser, null, null, null, null},
                {"setPropertyByUniqueAttribute", unprivilegedUser, null, null, null, PermissionDeniedException.class},
                //entityOperation
                {"entityOperation", privilegedUser, new Object[]{allowedEntityNew.getId()}, null, null, ObjectNotFoundException.class},
                {"entityOperation", privilegedUser, new Object[]{deniedEntityNew.getId()}, null, null, ObjectNotFoundException.class},
                {"entityOperation", privilegedUser, new Object[]{allowedEntityExisting.getId()}, null, null, null},
                {"entityOperation", privilegedUser, new Object[]{deniedEntityExisting.getId()}, null, null, PermissionDeniedException.class},
                //missingStereotype
                {"missingStereotype", privilegedUser, null, null, null, UnsupportedOperationException.class},
                //customInterceptor
                {"customInterceptor", privilegedUser, null, null, genericString, null},
        });
    }

    /**
     * The test constructor
     *
     * @param methodName        The name of the method to run. The method must me a method in MockAdmin
     * @param user              The user to run the method as
     * @param args              The args to pass to the method
     * @param rtn               The return value the method will return
     * @param expectedRtn       The expected return value from the SecuredMethodInterceptor
     * @param expectedException An expected exception if one should be thrown by the SecuredMethodInterceptor
     */
    public SecureMethodInterceptorTest(String methodName, InternalUser user, Object[] args, Object rtn, Object expectedRtn, Class<? extends Throwable> expectedException) {
        this.methodName = methodName;
        this.user = user;
        this.args = args;
        this.rtn = rtn;
        this.expectedRtn = expectedRtn;
        this.expectedException = expectedException;
    }

    @Before
    public void setup() throws Exception {
        //Process mockito annotations
        MockitoAnnotations.initMocks(this);
        //create the SecuredMethodInterceptor
        interceptor = new SecuredMethodInterceptor(rbacServices, entityFinder);
        interceptor.setApplicationContext(applicationContext);
        //initialize mocks
        initMocks();
    }

    private void initMocks() throws FindException {
        //This is for running with customInterceptor
        when(applicationContext.getBean("injector", Injector.class)).thenReturn(injector);

        //applies privileged user permissions on entities
        when(rbacServices.isPermittedForEntity(eq(privilegedUser), eq(allowedEntityNew), any(OperationType.class), (String) isNull())).thenReturn(true);
        when(rbacServices.isPermittedForEntity(eq(privilegedUser), eq(deniedEntityNew), any(OperationType.class), (String) isNull())).thenReturn(false);
        when(rbacServices.isPermittedForEntity(eq(privilegedUser), eq(allowedEntityExisting), any(OperationType.class), (String) isNull())).thenReturn(true);
        when(rbacServices.isPermittedForEntity(eq(privilegedUser), eq(deniedEntityExisting), any(OperationType.class), (String) isNull())).thenReturn(false);

        //applies semi privileged user permissions on entities
        when(rbacServices.isPermittedForEntity(eq(semiPrivilegedUser), eq(allowedEntityNew), any(OperationType.class), (String) isNull())).thenReturn(true);
        when(rbacServices.isPermittedForEntity(eq(semiPrivilegedUser), eq(deniedEntityNew), any(OperationType.class), (String) isNull())).thenReturn(false);
        when(rbacServices.isPermittedForEntity(eq(semiPrivilegedUser), eq(allowedEntityExisting), any(OperationType.class), (String) isNull())).thenReturn(true);
        when(rbacServices.isPermittedForEntity(eq(semiPrivilegedUser), eq(deniedEntityExisting), any(OperationType.class), (String) isNull())).thenReturn(false);

        //applies global privileges for users
        when(rbacServices.isPermittedForAnyEntityOfType(eq(privilegedUser), any(OperationType.class), any(EntityType.class))).thenReturn(true);
        when(rbacServices.isPermittedForAnyEntityOfType(eq(semiPrivilegedUser), any(OperationType.class), any(EntityType.class))).thenReturn(false);
        when(rbacServices.isPermittedForAnyEntityOfType(eq(unprivilegedUser), any(OperationType.class), any(EntityType.class))).thenReturn(false);

        //mocks the entity finder methods
        when(entityFinder.find(eq(allowedEntityHeader))).thenReturn(allowedEntityExisting);
        when(entityFinder.find(eq(deniedEntityHeader))).thenReturn(deniedEntityExisting);

        when(entityFinder.find(eq(allowedIdentityHeaderUserType))).thenReturn(allowedEntityExisting);
        when(entityFinder.find(eq(deniedIdentityHeaderUserType))).thenReturn(deniedEntityExisting);

        when(entityFinder.find(any(Class.class), eq(allowedEntityExisting.getId()))).thenReturn(allowedEntityExisting);
        when(entityFinder.find(any(Class.class), eq(deniedEntityExisting.getId()))).thenReturn(deniedEntityExisting);

        when(entityFinder.find(any(Class.class), eq(allowedEntityExisting.getId()))).thenReturn(allowedEntityExisting);
        when(entityFinder.find(any(Class.class), eq(deniedEntityExisting.getId()))).thenReturn(deniedEntityExisting);
    }

    /**
     * The test method. This is run once for every value in the parameterized array
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        //gets the appropriate method to run.
        Method method = MockAdmin.class.getMethod(methodName, getArgsClasses(args));
        //creates a method invocation
        StubMethodInvocation methodInvocation = new StubMethodInvocation(method, args, rtn, mockAdmin);

        Object result = null;
        try {
            //runs the method with the appropriate user
            result = runAs(user, methodInvocation);
        } catch (Throwable t) {
            //catches any exceptions. and checks if they were expected.
            if (expectedException == null) {
                fail("Threw an unexpected exception: " + t);
            }
            if (t.getClass().equals(expectedException)) {
                return;
            }
            //need to handle wrapping of throwable exceptions as runtime exceptions by the StubMethodInvocation
            else if (t.getClass().equals(RuntimeException.class) && t.getCause() != null && t.getCause().getClass().equals(expectedException)) {
                return;
            }
            fail("Threw an unexpected exception: " + t);
        }
        //if an exception was expected but not thrown fail
        if (expectedException != null) {
            fail("Expected exception but did not throw one: " + expectedException);
        }
        //validates the return value from the method run. Does an explicit ckeck for collections.
        if (expectedRtn instanceof Collection && result instanceof Collection) {
            Collection expectedRtnCollection = (Collection) expectedRtn;
            assertEquals("The returned collection size does not match the expected collection size.", expectedRtnCollection.size(), ((Collection) result).size());
            for (Object obj : expectedRtnCollection) {
                assertTrue("The results collection does not contain the expected element: " + obj, ((Collection) result).contains(obj));
            }
        } else {
            assertEquals(expectedRtn, result);
        }
    }

    /**
     * given an array of objects this will return an array of the classes of the objects
     *
     * @param args
     * @return
     */
    private Class[] getArgsClasses(Object[] args) {
        if (args == null || args.length == 0) {
            return new Class[0];
        }
        ArrayList<Class> clazzes = new ArrayList<Class>(args.length);
        for (Object arg : args) {
            clazzes.add(arg.getClass());
        }
        return clazzes.toArray(new Class[clazzes.size()]);
    }

    /**
     * Runs the method as the given user going through the SecuredMethodInterceptor
     */
    private <T> T runAs(final User user, final StubMethodInvocation invocation) throws PrivilegedActionException {
        return Subject.doAs(new Subject(true, Collections.singleton(user), Collections.emptySet(), Collections.emptySet()), new PrivilegedAction<T>() {
            @Override
            public T run() {
                try {
                    return (T) interceptor.invoke(invocation);
                } catch (final PermissionDeniedException throwable) {
                    throw throwable;
                } catch (final Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        });
    }

    /**
     * This is used to stub a method invocation
     */
    private class StubMethodInvocation implements MethodInvocation {
        private Method method;
        private Object[] arguments;
        private Object rtn;
        private Object t;

        private StubMethodInvocation(final Method method, final Object[] arguments, final Object rtn, final Object t) {
            this.method = method;
            this.arguments = arguments;
            this.rtn = rtn;
            this.t = t;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object[] getArguments() {
            return arguments;
        }

        @Override
        public Object proceed() throws Throwable {
            return rtn;
        }

        @Override
        public Object getThis() {
            return t;
        }

        @Override
        public AccessibleObject getStaticPart() {
            return null;
        }
    }

    /**
     * entity type shouldn't matter here they are all treated in a similar way. It is only used by rbasservices to find permissions
     * These are the different types of methods that will be tested
     */
    @Secured(types = EntityType.GENERIC)
    private interface MockAdmin {

        @Secured(stereotype = MethodStereotype.FIND_HEADERS)
        Collection<EntityHeader> findHeaders();

        @Secured(stereotype = MethodStereotype.FIND_HEADERS)
        EntityHeader findHeader();

        @Secured(stereotype = MethodStereotype.FIND_ENTITIES)
        Collection<Entity> findEntities();

        @Secured(stereotype = MethodStereotype.FIND_ENTITY)
        Entity findEntity();

        @Secured(stereotype = MethodStereotype.SAVE)
        void save(MyEntity entity);

        @Secured(stereotype = MethodStereotype.SAVE)
        void save();

        @Secured(stereotype = MethodStereotype.UPDATE)
        void update(MyEntity entity);

        @Secured(stereotype = MethodStereotype.SAVE_OR_UPDATE)
        void saveOrUpdate(MyEntity entity);

        @Secured(stereotype = MethodStereotype.SAVE_OR_UPDATE)
        void saveOrUpdate();

        @Secured(stereotype = MethodStereotype.DELETE_BY_ID)
        void deleteById(String id);

        @Secured(types = EntityType.USER, stereotype = MethodStereotype.DELETE_IDENTITY_BY_ID)
        void deleteIdentityById(Long oid, String id);

        @Secured(stereotype = MethodStereotype.DELETE_BY_UNIQUE_ATTRIBUTE)
        void deleteByUniqueAttribute();

        @Secured(stereotype = MethodStereotype.DELETE_ENTITY)
        void deleteEntity(MyEntity entity);

        @Secured(stereotype = MethodStereotype.DELETE_MULTI)
        void deleteMulti();

        @Secured(stereotype = MethodStereotype.GET_PROPERTY_OF_ENTITY)
        String getPropertyOfEntity(MyEntity entity);

        @Secured(stereotype = MethodStereotype.GET_PROPERTY_BY_ID)
        String getPropertyById(String id);

        @Secured(types = EntityType.USER, stereotype = MethodStereotype.GET_IDENTITY_PROPERTY_BY_ID)
        String getIdentityPropertyById(Long oid, String id);

        @Secured(stereotype = MethodStereotype.SET_PROPERTY_OF_ENTITY, relevantArg = 1)
        void setPropertyOfEntity(String value, MyEntity entity);

        @Secured(stereotype = MethodStereotype.SET_PROPERTY_BY_ID)
        void setPropertyById(String id);

        @Secured(stereotype = MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE)
        void setPropertyByUniqueAttribute();

        @Secured(stereotype = MethodStereotype.ENTITY_OPERATION)
        void entityOperation(String id);

        //no stereotype specified
        @Secured
        void missingStereotype();

        @Secured(customInterceptor = "com.l7tech.server.security.rbac.MockCustomRbacInterceptor")
        void customInterceptor();
    }

    /**
     * This is an entity that will be used for testing
     */
    private static class MyEntity implements PersistentEntity {
        private int version = 0;
        private long oid = PersistentEntity.DEFAULT_OID;

        public MyEntity() {
        }

        public MyEntity(long oid) {
            this.oid = oid;
        }

        @Override
        public String getId() {
            return String.valueOf(oid);
        }

        @Override
        public int getVersion() {
            return version;
        }

        @Override
        public void setVersion(int version) {
            this.version = version;
        }

        @Override
        public long getOid() {
            return oid;
        }

        @Override
        public void setOid(long oid) {
            this.oid = oid;
        }
    }
}
