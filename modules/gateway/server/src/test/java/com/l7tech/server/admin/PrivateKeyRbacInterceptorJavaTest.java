package com.l7tech.server.admin;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.aop.framework.ReflectiveMethodInvocation;

import java.lang.reflect.Method;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.*;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@RunWith(MockitoJUnitRunner.class)
public class PrivateKeyRbacInterceptorJavaTest {

    private static final Goid KEYSTORE_ID = new Goid(0,82734);
    private static final String KEY_ALIAS = "interceptor_test_key_alias";
    private static final Class<KeyRbacTestInterface> TEST_INTERFACE_CLASS = KeyRbacTestInterface.class;

    // Methods that are used in multiple tests
    private static Method noPreChecksEmptyList;
    private static Method updateKeyEntry;
    private static Method createKeyEntry;
    private static Method isNonFatalPreCheckPassed;
    private static Method exportKey;
    private static Method checkArgOpCreateWithMetadata;
    private static Method checkArgOpUpdateWithMetadata;
    private static Method checkArgOpUpdateWithKeyEntry;
    private static Method checkArgOpCreateWithKeyEntry;
    private static Method checkArgOpReadWithKeyEntry;
    private static Method checkArgOpDeleteWithKeyEntry;

    private final PrivateKeyRbacInterceptor interceptor = new PrivateKeyRbacInterceptor();

    @Mock
    private SsgKeyEntry keyEntry;

    @Mock
    private SsgKeyMetadata keyMetadata;

    @Mock
    private KeyRbacTestInterface testInterface;

    @Mock
    private SsgKeyFinder keystore;

    @Mock
    private User user;

    @Mock
    private SsgKeyStoreManager ssgKeyStoreManager;

    @Mock
    private RbacServices rbacServices;

    @BeforeClass
    public static void beforeClass() throws NoSuchMethodException {
        noPreChecksEmptyList = TEST_INTERFACE_CLASS.getMethod("noPreChecksEmptyList");
        updateKeyEntry = TEST_INTERFACE_CLASS.getMethod("updateKeyEntry", Goid.class, String.class);
        createKeyEntry = TEST_INTERFACE_CLASS.getMethod("createKeyEntry", Goid.class, String.class);
        isNonFatalPreCheckPassed = TEST_INTERFACE_CLASS.getMethod("isNonFatalPreCheckPassed", Object.class);
        exportKey = TEST_INTERFACE_CLASS.getMethod("exportKey", Goid.class, String.class);
        checkArgOpCreateWithMetadata = TEST_INTERFACE_CLASS.getMethod("checkArgOpCreateWithMetadata", Goid.class, String.class, SsgKeyMetadata.class);
        checkArgOpUpdateWithMetadata = TEST_INTERFACE_CLASS.getMethod("checkArgOpUpdateWithMetadata", Goid.class, String.class, SsgKeyMetadata.class);
        checkArgOpUpdateWithKeyEntry = TEST_INTERFACE_CLASS.getMethod("checkArgOpUpdateWithKeyEntry", SsgKeyEntry.class);
        checkArgOpCreateWithKeyEntry = TEST_INTERFACE_CLASS.getMethod("checkArgOpCreateWithKeyEntry", SsgKeyEntry.class);
        checkArgOpReadWithKeyEntry = TEST_INTERFACE_CLASS.getMethod("checkArgOpReadWithKeyEntry", SsgKeyEntry.class);
        checkArgOpDeleteWithKeyEntry = TEST_INTERFACE_CLASS.getMethod("checkArgOpDeleteWithKeyEntry", SsgKeyEntry.class);
    }

    @Before
    public void setUp() {
        interceptor.setUser(user);
        interceptor.rbacServices = rbacServices;
        interceptor.ssgKeyStoreManager = ssgKeyStoreManager;

        when(user.getName()).thenReturn(KEY_ALIAS);

        when(keyEntry.getAlias()).thenReturn(KEY_ALIAS);
        when(keyEntry.getKeystoreId()).thenReturn(KEYSTORE_ID);

        when(keyMetadata.getAlias()).thenReturn(KEY_ALIAS);
        when(keyMetadata.getKeystoreGoid()).thenReturn(KEYSTORE_ID);
        when(keyMetadata.getSecurityZone()).thenReturn(new SecurityZone());
    }

    /**
     * fail if invoked without a user
     */
    @Test
    public void testInvoke_InterceptorInvokedWithoutUser_IllegalStateExceptionThrown() throws Throwable {
        interceptor.user = null;

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(updateKeyEntry));
            fail("Expected IllegalStateException.");
        } catch (IllegalStateException e) {
            assertEquals("Current user not set for PrivateKeyRbacInterceptor", e.getMessage());
        }
    }

    /**
     * fail if invoked without rbacServices
     */
    @Test
    public void testInvoke_InterceptorInvokedWithoutRbacServices_IllegalStateExceptionThrown() throws Throwable {
        interceptor.rbacServices = null;

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(updateKeyEntry));
            fail("Expected IllegalStateException.");
        } catch (IllegalStateException e) {
            assertEquals("pkri-rs autowire failure", e.getMessage());
        }
    }

    /**
     * fail if invoked without ssgKeyStoreManager
     */
    @Test
    public void testInvoke_InterceptorInvokedWithoutKeystoreManager_IllegalStateExceptionThrown() throws Throwable {
        interceptor.ssgKeyStoreManager = null;

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(updateKeyEntry));
            fail("Expected IllegalStateException.");
        } catch (IllegalStateException e) {
            assertEquals("pkri-sksm autowire failure", e.getMessage());
        }
    }

    /**
     * fail if invoked method lacks @PrivateKeySecured annotation
     */
    @Test
    public void testInvoke_MethodLacksPrivateKeySecuredAnnotation_IllegalStateExceptionThrown() throws Throwable {
        Method lacksAnnotation = TEST_INTERFACE_CLASS.getMethod("lacksAnnotation");

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(lacksAnnotation));
            fail("Expected IllegalStateException.");
        } catch (IllegalStateException e) {
            assertEquals("@Secured method using PrivateKeyRbacInterceptor lacks @PrivateKeySecured", e.getMessage());
        }
    }

    /**
     * Fail the operation arguments pre-check when given invalid keystore Goid.
     */
    @Test
    public void testInvoke_KeystoreGoidOperationArgumentInvalid_IllegalArgumentExceptionThrown() throws Throwable {
        mockOperationPermittedForAllNonMockedKeyEntries(OperationType.UPDATE);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(updateKeyEntry, new Goid(0, -1), KEY_ALIAS));
            fail("Expected IllegalStateException.");
        } catch (IllegalArgumentException e) {
            assertEquals("valid keystoreGoid required", e.getMessage());
        }

        verify(ssgKeyStoreManager, never()).findByPrimaryKey(any(Goid.class));
    }

    /**
     * Fail the operation arguments pre-check when the specified keystore does not exist.
     */
    @Test
    public void testInvoke_KeystoreGoidSpecifiedDoesNotExist_IllegalArgumentExceptionThrown() throws Throwable {
        mockOperationPermittedForAllNonMockedKeyEntries(OperationType.UPDATE);

        when(ssgKeyStoreManager.findByPrimaryKey(any(Goid.class))).thenThrow(new ObjectNotFoundException("No SsgKeyFinder available on this node with id=NNN"));

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(updateKeyEntry, KEYSTORE_ID, KEY_ALIAS));
            fail("Expected ObjectNotFoundException.");
        } catch (ObjectNotFoundException e) {
            assertEquals("No SsgKeyFinder available on this node with id=NNN", e.getMessage());
        }
    }

    /**
     * Fail the operation arguments pre-check when the specified key alias does not exist in specified keystore.
     */
    @Test
    public void testInvoke_KeystoreAliasDoesNotExistInKeystore_IllegalArgumentExceptionThrown() throws Throwable {
        when(ssgKeyStoreManager.findByPrimaryKey(any(Goid.class))).thenReturn(keystore);
        when(keystore.getCertificateChain(anyString())).thenThrow(new ObjectNotFoundException("Keystore BLAH " +
                "does not contain any certificate chain entry with alias FOO"));

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(updateKeyEntry, KEYSTORE_ID, KEY_ALIAS));
            fail("Expected ObjectNotFoundException.");
        } catch (ObjectNotFoundException e) {
            assertEquals("Keystore BLAH does not contain any certificate chain entry with alias FOO", e.getMessage());
        }
    }

    /**
     * pass CHECK_ARG_OPERATION precheck if permitted for user
     */
    @Test
    public void testInvoke_UpdateOperationArgumentsValidAndUserPermitted_UpdateSucceeds() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(OperationType.UPDATE, keystore);
        mockOperationPermittedForEntity(OperationType.UPDATE, keyEntry);
        mockOperationPermittedForAllNonMockedKeyEntries(OperationType.UPDATE);

        when(testInterface.updateKeyEntry(any(Goid.class), anyString())).thenReturn(true);

        Boolean result = (Boolean) interceptor.invoke(new TestReflectiveMethodInvocation(updateKeyEntry, KEYSTORE_ID, KEY_ALIAS));

        assertTrue(result);

        verify(rbacServices, times(3)).isPermittedForEntity(any(User.class), any(Entity.class), any(OperationType.class), anyString());
    }

    /**
     * fail CHECK_ARG_OPERATION precheck if not permitted for user with keystore
     */
    @Test
    public void testInvoke_UserNotPermittedWithKeystore_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();

        when(rbacServices.isPermittedForEntity(eq(user), eq(keyEntry), eq(OperationType.UPDATE), isNull(String.class))).thenReturn(true);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(updateKeyEntry, KEYSTORE_ID, KEY_ALIAS));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update interceptor_test_key_alias", e.getMessage());
        }

        verify(rbacServices, times(2)).isPermittedForEntity(any(User.class), any(Entity.class), any(OperationType.class), anyString());
    }

    /**
     * pass CHECK_ARG_OPERATION precheck that requires creating a dummy entity to check
     */
    @Test
    public void testInvoke_DummyEntityRequiredForPreChecksForCreateOperation_CreateSucceeds() throws Throwable {
        mockKeyLookupSuccess();

        when(rbacServices.isPermittedForEntity(any(User.class), any(SsgKeyEntry.class), any(OperationType.class),
                anyString())).thenReturn(true); // use wildcard match, so will match the created dummy entity
        when(testInterface.createKeyEntry(any(Goid.class), anyString())).thenReturn(true);

        Boolean result = (Boolean) interceptor.invoke(new TestReflectiveMethodInvocation(createKeyEntry, KEYSTORE_ID, KEY_ALIAS));

        assertTrue(result);

        verify(rbacServices, times(2)).isPermittedForEntity(any(User.class), any(Entity.class), any(OperationType.class), anyString());
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires creating a dummy entity to check, with a wildcard keystore id,
     * if lacking UPDATE ALL SSG_KEYSTORE
     *
     * This particular situation isn't useful (create key without specifying which keystore?!) but it provides
     * coverage of checkPermittedForKeystore() with wildcard keystore ID
     */
    @Test
    public void testInvoke_DummyRequiredAndGivenWildcardKeystoreIdAndNoKeystoreUpdatePermission_CreateSucceeds() throws Throwable {
        mockKeyLookupSuccess();

        when(rbacServices.isPermittedForEntity(any(User.class), any(SsgKeyEntry.class), eq(OperationType.CREATE),
                anyString())).thenReturn(true); // use wildcard match, so will match the created dummy entity
        when(testInterface.createKeyEntry(any(Goid.class), anyString())).thenReturn(true);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(createKeyEntry, new Goid(0, -1), KEY_ALIAS));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update SSG_KEYSTORE", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(any(User.class), any(Entity.class), eq(OperationType.CREATE), anyString());
        verify(rbacServices).isPermittedForAnyEntityOfType(any(User.class), any(OperationType.class), any(EntityType.class));
    }

    /**
     * pass CHECK_ARG_OPERATION precheck that requires creating a dummy entity to check, with a wildcard keystore id,
     * if have UPDATE ALL SSG_KEYSTORE
     *
     * This particular situation isn't useful (create key without specifying which keystore?!) but it provides
     * coverage of checkPermittedForKeystore() with wildcard keystore ID
     */
    @Test
    public void testInvoke_DummyRequiredAndGivenWildcardKeystoreIdWithKeystoreUpdatePermission_CreateSucceeds() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntityType(OperationType.UPDATE, EntityType.SSG_KEYSTORE);

        when(rbacServices.isPermittedForEntity(any(User.class), any(SsgKeyEntry.class), any(OperationType.class),
                anyString())).thenReturn(true); // use wildcard match, so will match the created dummy entity
        when(testInterface.createKeyEntry(any(Goid.class), anyString())).thenReturn(true);

        Boolean result = (Boolean) interceptor.invoke(new TestReflectiveMethodInvocation(createKeyEntry, new Goid(0,-1), KEY_ALIAS));

        assertTrue(result);

        verify(rbacServices).isPermittedForEntity(any(User.class), any(Entity.class), any(OperationType.class), anyString());
        verify(rbacServices).isPermittedForAnyEntityOfType(any(User.class), eq(OperationType.UPDATE), eq(EntityType.SSG_KEYSTORE));
    }

    /**
     * pass CHECK_ARG_OPERATION precheck that requires an unusual argument order for locating the
     * keystore ID and key alias
     */
    @Test
    public void testInvoke_GivenUnusualArgumentOrderForLocatingKeytoreIdAndKeyAlias_Success() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(OperationType.UPDATE, keystore);
        mockOperationPermittedForEntity(OperationType.DELETE, keyEntry);

        Method delWithStrangeArgOrder = TEST_INTERFACE_CLASS.getMethod("deleteWithStrangeArgumentOrder",
                Object.class, String.class, Object.class, Goid.class, Object.class);

        when(testInterface.deleteWithStrangeArgumentOrder(isNull(), eq(KEY_ALIAS), isNull(), eq(KEYSTORE_ID), isNull())).thenReturn(true);

        Boolean result = (Boolean) interceptor.invoke(new TestReflectiveMethodInvocation(delWithStrangeArgOrder,
                null, KEY_ALIAS, null, KEYSTORE_ID, null));

        assertTrue(result);

        verify(rbacServices).isPermittedForEntity(any(User.class), eq(keystore), eq(OperationType.UPDATE), anyString());
        verify(rbacServices).isPermittedForEntity(any(User.class), eq(keyEntry), eq(OperationType.DELETE), anyString());
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires an unusual argument order, if needed UPDATE SSG_KEYSTORE
     * permission not present
     */
    @Test
    public void testInvoke_GivenUnusualArgumentOrderMissingUpdateKeystorePermission_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(OperationType.DELETE, keyEntry);

        Method delWithStrangeArgOrder = TEST_INTERFACE_CLASS.getMethod("deleteWithStrangeArgumentOrder",
                Object.class, String.class, Object.class, Goid.class, Object.class);

        when(testInterface.deleteWithStrangeArgumentOrder(isNull(), eq(KEY_ALIAS), isNull(), eq(KEYSTORE_ID), isNull())).thenReturn(true);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(delWithStrangeArgOrder, null, KEY_ALIAS, null, KEYSTORE_ID, null));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update SSG_KEYSTORE", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(any(User.class), eq(keystore), eq(OperationType.UPDATE), anyString());
        verify(rbacServices).isPermittedForEntity(any(User.class), eq(keyEntry), eq(OperationType.DELETE), anyString());
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires an unusual argument order, if needed DELETE SSG_KEY_ENTRY
     * permission not present
     */
    @Test
    public void testInvoke_GivenUnusualArgumentOrderMissingDeleteKeyEntryPermission_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(OperationType.UPDATE, keystore);

        Method delWithStrangeArgOrder = TEST_INTERFACE_CLASS.getMethod("deleteWithStrangeArgumentOrder",
                Object.class, String.class, Object.class, Goid.class, Object.class);

        when(testInterface.deleteWithStrangeArgumentOrder(isNull(), eq(KEY_ALIAS), isNull(), eq(KEYSTORE_ID), isNull())).thenReturn(true);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(delWithStrangeArgOrder, null, KEY_ALIAS, null, KEYSTORE_ID, null));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Delete null", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(any(User.class), eq(keyEntry), eq(OperationType.DELETE), anyString());
        verify(rbacServices, never()).isPermittedForEntity(any(User.class), eq(keystore), eq(OperationType.UPDATE), anyString());
    }

    /**
     * return check for method that returns a list that contains at least one thing that isn't a key entry
     */
    @Test
    public void testInvoke_AtLeastOneMethodReturnValueNotSsgKeyEntry_IllegalStateExceptionThrown() throws Throwable {
        Method findUnrelatedStuff = TEST_INTERFACE_CLASS.getMethod("findUnrelatedStuff");

        when(testInterface.findUnrelatedStuff()).thenReturn(Arrays.asList("foo", keyEntry, new Object()));

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(findUnrelatedStuff));
            fail("Expected IllegalStateException.");
        } catch (IllegalStateException e) {
            assertEquals("Unable to filter return type for method findUnrelatedStuff: List", e.getMessage());
        }

        verify(rbacServices, never()).isPermittedForEntity(any(User.class), any(Entity.class), any(OperationType.class), anyString());
    }

    /**
     * Configure the finder mocks to do a successful lookup of our mock keystore and key entry
     */
    private void mockKeyLookupSuccess() throws FindException, KeyStoreException {
        when(ssgKeyStoreManager.findByPrimaryKey(any(Goid.class))).thenReturn(keystore);
        when(keystore.getCertificateChain(anyString())).thenReturn(keyEntry);
    }

    /**
     * Configure rbacServices mock to return true when queried about the specified permission on the specified entity
     */
    private void mockOperationPermittedForEntity(OperationType operation, Entity entity) throws FindException {
        when(rbacServices.isPermittedForEntity(eq(user), eq(entity), eq(operation), isNull(String.class)))
                .thenReturn(true);
    }

    /**
     * Configure rbacServices mock to return true when queried about the specified permission on any entity of the
     * specified type
     */
    private void mockOperationPermittedForEntityType(OperationType operation, EntityType type) throws FindException {
        when(rbacServices.isPermittedForAnyEntityOfType(eq(user), eq(operation), eq(type))).thenReturn(true);
    }

    /**
     * Configure rbacServices mock to return true when queried about the specified permission for all
     * non-mocked SsgKeyEntry instances.
     *
     * N.B. The old Scala implementation of this test relied on mocked instances not matching "haveClass":
     * rbacServices.isPermittedForEntity(beEqualTo(user), haveClass[SsgKeyEntry], beEqualTo(op), org.mockito.Matchers.eq(null)) returns true
     *
     * Using only Mockito this type of matching seems to be unavailable, so we will specify that the permissions
     * is for all SsgKeyEntry instances except the only one we are actually mocking.
     */
    private void mockOperationPermittedForAllNonMockedKeyEntries(OperationType operation) throws FindException {
        when(rbacServices.isPermittedForEntity(eq(user), not(eq(keyEntry)), eq(operation), isNull(String.class)))
                .thenReturn(true);
    }

    private class TestReflectiveMethodInvocation extends ReflectiveMethodInvocation {

        protected TestReflectiveMethodInvocation(Method method, Object ... arguments) {
            super(null, testInterface, method, arguments, TEST_INTERFACE_CLASS, new ArrayList<>());
        }
    }
}
