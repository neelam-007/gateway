/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
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
import com.l7tech.server.folder.FolderManagerStub;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyManagerStub;
import com.l7tech.server.service.PolicyAliasManagerStub;
import com.l7tech.server.service.ServiceAliasManagerStub;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceManagerStub;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.Subject;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/** @author alex */
public class RbacFolderTest {
    private final PolicyManager policyManager = new PolicyManagerStub();
    private final FolderManager folderManager = new FolderManagerStub();
    private final PolicyAliasManagerStub policyAliasManager = new PolicyAliasManagerStub();
    private final ServiceAliasManagerStub serviceAliasManager = new ServiceAliasManagerStub();

    private ServiceManager serviceManager;
    private RbacServices rbacServices;
    private SecuredMethodInterceptor interceptor;
    private MockRoleManager roleManager;

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
            this.method = thiss.getClass().getMethod(methodName, argTypes.toArray(new Class<?>[argTypes.size()]));
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
            //noinspection unchecked
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
    @Before
    public void setUp() throws Exception {
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
        jimbo.setGoid(new Goid(0,1));

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
        EntityFinderStub entityFinder = new EntityFinderStub(policyManager, serviceManager, folderManager, policyAliasManager, serviceAliasManager);
        roleManager = new MockRoleManager(entityFinder);
        rbacServices = new RbacServicesImpl(roleManager, entityFinder);
        interceptor = new SecuredMethodInterceptor(rbacServices, entityFinder);
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

    @Test
    public void testFilteredRead() throws Throwable {
        canReadSpecific(jimbo, EntityType.POLICY, aPolicy.getGoid());
        assertTrue(rbacServices.isPermittedForEntity(jimbo, aPolicy, OperationType.READ, null));

        Collection<PolicyHeader> policies = runAs(jimbo, findAllPolicies);

        // Make sure we didn't read bPolicy
        assertEquals("Number of policies read", 1, policies.size());
        final PolicyHeader got = policies.iterator().next();
        assertEquals("aPolicy OID", aPolicy.getGoid(), got.getGoid());
        assertEquals("aPolicy GUID", aPolicy.getGuid(), got.getGuid());
    }

    @Test
    public void testReadAll() throws Throwable {
        canReadAnyPolicy(jimbo);
        assertTrue(rbacServices.isPermittedForEntity(jimbo, aPolicy, OperationType.READ, null));
        assertTrue(rbacServices.isPermittedForEntity(jimbo, bPolicy, OperationType.READ, null));

        Collection<PolicyHeader> policies = runAs(jimbo, findAllPolicies);

        assertEquals("Number of policies read", 2, policies.size());
    }

    @Test
    public void testFolderNonTransitiveNonRoot() throws Throwable {
        canReadStuffInFolder(jimbo, folder1, false, EntityType.POLICY_ALIAS, EntityType.POLICY);
        assertTrue(rbacServices.isPermittedForEntity(jimbo, aPolicy, OperationType.READ, null));
        assertTrue(rbacServices.isPermittedForEntity(jimbo, aliasToBPolicy, OperationType.READ, null));

        List<Object> all = new ArrayList<Object>();
        all.addAll(runAs(jimbo, findAllPolicies));
        all.addAll(runAs(jimbo, findAllPolicyAliases));

        assertEquals("Number of things read", 2, all.size());
    }

    @Test
    public void testFolderNonTransitiveRoot() throws Throwable {
        canReadStuffInFolder(jimbo, rootFolder, false, EntityType.ANY);
        assertTrue(rbacServices.isPermittedForEntity(jimbo, aService, OperationType.READ, null));

        Collection<ServiceHeader> all = runAs(jimbo, findAllServices);

        assertEquals("Number of things read", 1, all.size());
    }

    @Test
    public void testTransitiveFolderPredicate() throws Throwable {
        canReadStuffInFolder(jimbo, rootFolder, true, EntityType.POLICY_ALIAS, EntityType.POLICY);
        assertTrue("Can read APolicy", rbacServices.isPermittedForEntity(jimbo, aPolicy, OperationType.READ, null));
        assertTrue("Can read BPolicy", rbacServices.isPermittedForEntity(jimbo, bPolicy, OperationType.READ, null));
        assertTrue("Can read BAlias", rbacServices.isPermittedForEntity(jimbo, aliasToBPolicy, OperationType.READ, null));
        assertTrue("Can read AAlias", rbacServices.isPermittedForEntity(jimbo, aliasToAPolicy, OperationType.READ, null));

        List<Object> all = new ArrayList<Object>();
        all.addAll(runAs(jimbo, findAllPolicies));
        all.addAll(runAs(jimbo, findAllPolicyAliases));

        assertEquals("Number of things read", 4, all.size());
    }

    @Test
    public void testFolderAncestry() throws Throwable {
        Role role = new Role();
        role.addAssignedUser(jimbo);
        {
            Permission perm = new Permission(role, OperationType.READ, EntityType.FOLDER);
            perm.getScope().add(new EntityFolderAncestryPredicate(perm, EntityType.POLICY, aPolicy.getGoid()));
            role.getPermissions().add(perm);
        }

        roleManager.save(role);

        assertTrue("Can read folder1 due to EntityFolderAncestryPredicate",
                rbacServices.isPermittedForEntity(jimbo, folder1, OperationType.READ, null));

        assertFalse("Can't read aPolicy (a folder ancestry permission does not extend to the entity itself)", 
                rbacServices.isPermittedForEntity(jimbo, aPolicy, OperationType.READ, null));

        Collection<Folder> folders = runAs(jimbo, findAllFolders);
        assertEquals(2, folders.size());
        assertTrue("found folder1", folders.contains(folder1));
        assertTrue("Found root", folders.contains(rootFolder));
        assertFalse("Shouldn't find folder2", folders.contains(folder2));
    }

    @Test
    public void testBug6230MultiplePermissions() throws Exception {
        canReadSpecific(jimbo, EntityType.POLICY, aPolicy.getGoid(), bPolicy.getGoid());
        assertTrue("Can read A policy", rbacServices.isPermittedForEntity(jimbo, aPolicy, OperationType.READ, null));
        assertTrue("Can read B policy", rbacServices.isPermittedForEntity(jimbo, bPolicy, OperationType.READ, null));
        Collection<PolicyHeader> headers = runAs(jimbo, findAllPolicies);
        assertEquals("Found 2 policies", 2, headers.size());
    }

    private <T> T runAs(final User user, final GenericMethodInvocation<T> invocation) throws PrivilegedActionException {
        return Subject.doAs(new Subject(true, Collections.singleton(user), Collections.emptySet(), Collections.emptySet()), new PrivilegedAction<T>() {
            @Override
            public T run() {
                try {
                    //noinspection unchecked
                    return (T)interceptor.invoke(invocation);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable); // Can't happen
                }
            }
        });
    }

    private long canReadSpecific(final User user, final EntityType etype, final Goid... goids) throws SaveException {
        Role role = new Role();
        role.addAssignedUser(user);
        for (Goid goid : goids) {
            role.addEntityPermission(OperationType.READ, etype, Goid.toString(goid));
        }
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