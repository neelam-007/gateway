/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.EntityFinderStub;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyManagerStub;
import com.l7tech.server.service.PolicyAliasManagerStub;
import com.l7tech.server.service.ServiceAliasManagerStub;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceManagerStub;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.aopalliance.intercept.MethodInvocation;

import javax.security.auth.Subject;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/** @author alex */
public class RbacFolderTest extends TestCase {
    private static final Logger logger = Logger.getLogger(RbacFolderTest.class.getName());

    private final MockRoleManager roleManager = new MockRoleManager();
    private final PolicyManager policyManager = new PolicyManagerStub();
    private final FolderManager folderManager = new FolderManagerStub();
    private final PolicyAliasManagerStub policyAliasManager = new PolicyAliasManagerStub();
    private final ServiceAliasManagerStub serviceAliasManager = new ServiceAliasManagerStub();
    private final RbacServices rbacServices = new RbacServicesImpl(roleManager);

    private ServiceManager serviceManager;
    private EntityFinderStub entityFinder;
    private SecuredMethodInterceptor interceptor;

    private GenericMethodInvocation<Collection<PolicyHeader>> findAllPolicies;
    private GenericMethodInvocation<Collection<ServiceHeader>> findAllServices;
    private GenericMethodInvocation<Collection<Folder>> findAllFolders;
    private GenericMethodInvocation<Collection<PolicyAlias>> findAllPolicyAliases;
    private InternalUser jimbo;
    private Folder rootFolder;
    private Folder folder1;
    private Folder folder2;
    private Policy aPolicy;
    private Policy bPolicy;
    private PolicyAlias aliasToAPolicy;
    private PolicyAlias aliasToBPolicy;
    private PublishedService aService;

    public RbacFolderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(RbacFolderTest.class);
    }

    private static class GenericMethodInvocation<RT> implements MethodInvocation {
        private final Object thiss;
        private final Method method;
        private final Object[] args;

        public GenericMethodInvocation(Object thiss, String methodName, Object... args)
            throws NoSuchMethodException {
            this.thiss = thiss;
            List<Class<?>> argTypes = new ArrayList<Class<?>>();
            for (Object arg : args) {
                argTypes.add(arg.getClass());
            }
            this.method = thiss.getClass().getMethod(methodName, argTypes.toArray(new Class<?>[0]));
            this.args = args;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object[] getArguments() {
            return args;
        }

        @Override
        public RT proceed() throws Throwable {
            return (RT)method.invoke(thiss, args);
        }

        @Override
        public Object getThis() {
            return thiss;
        }

        @Override
        public AccessibleObject getStaticPart() {
            return method;
        }
    }

    /**
     * Sets up the following tree:
     * <pre>
     * root folder
     *   |
     *   |- folder1
     *   |   |- aPolicy
     *   |   \_ alias(bPolicy)
     *   |
     *   \_ folder2
     *       |- bPolicy
     *       \_ alias(aPolicy)
     * </pre>
     */
    @Override
    protected void setUp() throws Exception {
        final SecureMeImpl that = new SecureMeImpl();

        findAllPolicies = new GenericMethodInvocation<Collection<PolicyHeader>>(that, "findAllPolicies");
        findAllServices = new GenericMethodInvocation<Collection<ServiceHeader>>(that, "findAllServices");
        findAllFolders = new GenericMethodInvocation<Collection<Folder>>(that, "findAllFolders");
        findAllPolicyAliases = new GenericMethodInvocation<Collection<PolicyAlias>>(that, "findAllPolicyAliases");

        rootFolder = new Folder("root", null);
        folderManager.save(rootFolder);
        folder1 = new Folder("f1", rootFolder);
        folderManager.save(folder1);
        folder2 = new Folder("f2", rootFolder);
        folderManager.save(folder2);

        jimbo = new InternalUser("jimbo");
        jimbo.setOid(1);

        aPolicy = new Policy(PolicyType.INCLUDE_FRAGMENT, "A policy", WspWriter.getPolicyXml(new CommentAssertion("A policy")), true);
        aPolicy.setFolder(folder1);
        policyManager.save(aPolicy);

        bPolicy = new Policy(PolicyType.INCLUDE_FRAGMENT, "B policy", WspWriter.getPolicyXml(new CommentAssertion("B policy")), true);
        bPolicy.setFolder(folder2);
        policyManager.save(bPolicy);

        aliasToAPolicy = new PolicyAlias(aPolicy, folder2);
        policyAliasManager.save(aliasToAPolicy);

        aliasToBPolicy = new PolicyAlias(bPolicy, folder1);
        policyAliasManager.save(aliasToBPolicy);

        aService = new PublishedService();
        aService.setSoap(false);
        aService.setName("A service in the root folder");
        aService.setFolder(rootFolder);
        aService.setPolicy(new Policy(PolicyType.PRIVATE_SERVICE, "policy for a service", WspWriter.getPolicyXml(new CommentAssertion("A service policy")), false));
        serviceManager = new ServiceManagerStub(policyManager, aService);
        entityFinder = new EntityFinderStub(roleManager, policyManager, serviceManager, folderManager, policyAliasManager, serviceAliasManager);
        interceptor = new SecuredMethodInterceptor(rbacServices, entityFinder);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private static interface SecureMe {
        Collection<PolicyHeader> findAllPolicies() throws FindException;
        Collection<Folder> findAllFolders() throws FindException;
    }

    private class SecureMeImpl implements SecureMe {
        @Secured(types=EntityType.POLICY, stereotype=MethodStereotype.FIND_HEADERS)
        public Collection<PolicyHeader> findAllPolicies() throws FindException {
            return policyManager.findAllHeaders();
        }

        @Secured(types=EntityType.SERVICE, stereotype=MethodStereotype.FIND_HEADERS)
        public Collection<ServiceHeader> findAllServices() throws FindException {
            return serviceManager.findAllHeaders();
        }

        @Secured(types=EntityType.FOLDER, stereotype=MethodStereotype.FIND_ENTITIES)
        public Collection<Folder> findAllFolders() throws FindException {
            return folderManager.findAll();
        }

        @Secured(types=EntityType.POLICY_ALIAS, stereotype=MethodStereotype.FIND_ENTITIES)
        public Collection<PolicyAlias> findAllPolicyAliases() throws FindException {
            return policyAliasManager.findAll();
        }
    }

    public void testFilteredRead() throws Throwable {
        canReadSpecific(jimbo, EntityType.POLICY, aPolicy.getOid());
        assertTrue(rbacServices.isPermittedForEntity(jimbo, aPolicy, OperationType.READ, null));

        Collection<PolicyHeader> policies = runAs(jimbo, findAllPolicies);

        // Make sure we didn't read bPolicy
        assertEquals("Number of policies read", 1, policies.size());
        final PolicyHeader got = policies.iterator().next();
        assertEquals("aPolicy OID", aPolicy.getOid(), got.getOid());
        assertEquals("aPolicy GUID", aPolicy.getGuid(), got.getGuid());
    }

    public void testReadAll() throws Throwable {
        canReadAnyPolicy(jimbo);
        assertTrue(rbacServices.isPermittedForEntity(jimbo, aPolicy, OperationType.READ, null));
        assertTrue(rbacServices.isPermittedForEntity(jimbo, bPolicy, OperationType.READ, null));

        Collection<PolicyHeader> policies = runAs(jimbo, findAllPolicies);

        assertEquals("Number of policies read", 2, policies.size());
    }

    public void testFolderNonTransitiveNonRoot() throws Throwable {
        canReadStuffInFolder(jimbo, folder1, false, EntityType.POLICY_ALIAS, EntityType.POLICY);
        assertTrue(rbacServices.isPermittedForEntity(jimbo, aPolicy, OperationType.READ, null));
        assertTrue(rbacServices.isPermittedForEntity(jimbo, aliasToBPolicy, OperationType.READ, null));

        List all = new ArrayList();
        all.addAll(runAs(jimbo, findAllPolicies));
        all.addAll(runAs(jimbo, findAllPolicyAliases));

        assertEquals("Number of things read", 2, all.size());
    }

    public void testFolderNonTransitiveRoot() throws Throwable {
        canReadStuffInFolder(jimbo, folder1, false, EntityType.ANY);
        assertTrue(rbacServices.isPermittedForEntity(jimbo, aService, OperationType.READ, null));

        Collection<ServiceHeader> all = runAs(jimbo, findAllServices);

        assertEquals("Number of things read", 1, all.size());
    }

    public void testTransitiveFolderPredicate() throws Throwable {
        canReadStuffInFolder(jimbo, rootFolder, true, EntityType.POLICY_ALIAS, EntityType.POLICY);
        assertTrue("Can read APolicy", rbacServices.isPermittedForEntity(jimbo, aPolicy, OperationType.READ, null));
        assertTrue("Can read BPolicy", rbacServices.isPermittedForEntity(jimbo, bPolicy, OperationType.READ, null));
        assertTrue("Can read BAlias", rbacServices.isPermittedForEntity(jimbo, aliasToBPolicy, OperationType.READ, null));
        assertTrue("Can read AAlias", rbacServices.isPermittedForEntity(jimbo, aliasToAPolicy, OperationType.READ, null));

        List all = new ArrayList();
        all.addAll(runAs(jimbo, findAllPolicies));
        all.addAll(runAs(jimbo, findAllPolicyAliases));

        assertEquals("Number of things read", 4, all.size());
    }

    private <T> T runAs(final User user, final GenericMethodInvocation<T> invocation) throws PrivilegedActionException {
        return Subject.doAs(new Subject(true, Collections.singleton(user), Collections.emptySet(), Collections.emptySet()), new PrivilegedAction<T>() {
            @Override
            public T run() {
                try {
                    return (T)interceptor.invoke(invocation);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable); // Can't happen
                }
            }
        });
    }

    private long canReadSpecific(final User user, final EntityType etype, final long oid) throws SaveException {
        Role role = new Role();
        role.addAssignedUser(user);
        role.addEntityPermission(OperationType.READ, etype, Long.toString(oid));
        return roleManager.save(role);
    }

    private long canReadStuffInFolder(final User user, Folder folder, final boolean transitive, final EntityType... types) throws SaveException {
        Role role = new Role();
        role.addAssignedUser(user);
        for (EntityType type : types) {
            role.addFolderPermission(OperationType.READ, type, folder, transitive);
        }
        return roleManager.save(role);
    }

    private long canReadAnyPolicy(final User user) throws SaveException {
        Role role = new Role();
        role.addAssignedUser(user);
        role.addEntityPermission(OperationType.READ, EntityType.POLICY, null);
        return roleManager.save(role);
    }

}