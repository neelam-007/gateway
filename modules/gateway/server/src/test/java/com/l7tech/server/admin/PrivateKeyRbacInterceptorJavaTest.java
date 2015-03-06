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
import java.util.List;

import static com.l7tech.gateway.common.security.rbac.OperationType.*;
import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.and;
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
    private static Method deleteWithStrangeArgOrder;

    private final PrivateKeyRbacInterceptor interceptor = new PrivateKeyRbacInterceptor();

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
        deleteWithStrangeArgOrder = TEST_INTERFACE_CLASS.getMethod("deleteWithStrangeArgumentOrder", Object.class, String.class, Object.class, Goid.class, Object.class);
    }

    @Before
    public void setUp() {
        interceptor.setUser(user);
        interceptor.rbacServices = rbacServices;
        interceptor.ssgKeyStoreManager = ssgKeyStoreManager;

        when(user.getName()).thenReturn(KEY_ALIAS);

        keyEntry = makeMockKeyEntry(KEYSTORE_ID, KEY_ALIAS);

        when(keyMetadata.getAlias()).thenReturn(KEY_ALIAS);
        when(keyMetadata.getKeystoreGoid()).thenReturn(KEYSTORE_ID);
        when(keyMetadata.getSecurityZone()).thenReturn(new SecurityZone());
    }

    /**
     * fail if invoked without a user
     */
    @Test
    public void testUpdateKeyEntry_InterceptorInvokedWithoutUser_IllegalStateExceptionThrown() throws Throwable {
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
    public void testUpdateKeyEntry_InterceptorInvokedWithoutRbacServices_IllegalStateExceptionThrown() throws Throwable {
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
    public void testUpdateKeyEntry_InterceptorInvokedWithoutKeystoreManager_IllegalStateExceptionThrown() throws Throwable {
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
    public void testLacksAnnotation_MethodLacksPrivateKeySecuredAnnotation_IllegalStateExceptionThrown() throws Throwable {
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
    public void testUpdateKeyEntry_KeystoreGoidOperationArgumentInvalid_IllegalArgumentExceptionThrown() throws Throwable {
        mockOperationPermittedForAllNonMockedKeyEntries(UPDATE);

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
    public void testUpdateKeyEntry_KeystoreGoidSpecifiedDoesNotExist_ObjectNotFoundExceptionThrown() throws Throwable {
        mockOperationPermittedForAllNonMockedKeyEntries(UPDATE);

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
    public void testUpdateKeyEntry_KeystoreAliasDoesNotExistInKeystore_ObjectNotFoundThrown() throws Throwable {
        when(ssgKeyStoreManager.findByPrimaryKey(any(Goid.class))).thenReturn(keystore);
        when(keystore.getCertificateChain(anyString())).thenThrow(new ObjectNotFoundException("Keystore BLAH " +
                "does not contain any certificate chain entry with alias FOO"));

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(updateKeyEntry, KEYSTORE_ID, KEY_ALIAS));
            fail("Expected ObjectNotFoundException.");
        } catch (ObjectNotFoundException e) {
            assertEquals("Keystore BLAH does not contain any certificate chain entry with alias FOO", e.getMessage());
        }

        verify(keystore, only()).getCertificateChain(anyString());
        verifyZeroInteractions(rbacServices);
    }

    /**
     * pass CHECK_ARG_OPERATION precheck if permitted for user
     */
    @Test
    public void testUpdateKeyEntry_UpdateOperationArgumentsValidAndUserPermitted_UpdateSucceeds() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(UPDATE, keystore);
        mockOperationPermittedForEntity(UPDATE, keyEntry);
        mockOperationPermittedForAllNonMockedKeyEntries(UPDATE);

        when(testInterface.updateKeyEntry(any(Goid.class), anyString())).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(updateKeyEntry, KEYSTORE_ID, KEY_ALIAS));

        assertTrue((Boolean) result);

        verify(rbacServices, times(3)).isPermittedForEntity(any(User.class), any(Entity.class), any(OperationType.class), anyString());
    }

    /**
     * fail CHECK_ARG_OPERATION precheck if not permitted for user with keystore
     */
    @Test
    public void testUpdateKeyEntry_UserNotPermittedWithKeystore_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();

        when(rbacServices.isPermittedForEntity(eq(user), eq(keyEntry), eq(UPDATE), isNull(String.class))).thenReturn(true);

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
    public void testCreateKeyEntry_DummyEntityRequiredForPreChecksForCreateOperation_CreateSucceeds() throws Throwable {
        mockKeyLookupSuccess();

        when(rbacServices.isPermittedForEntity(any(User.class), any(SsgKeyEntry.class), any(OperationType.class),
                anyString())).thenReturn(true); // use wildcard match, so will match the created dummy entity
        when(testInterface.createKeyEntry(any(Goid.class), anyString())).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(createKeyEntry, KEYSTORE_ID, KEY_ALIAS));

        assertTrue((Boolean) result);

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
    public void testCreateKeyEntry_DummyRequiredAndGivenWildcardKeystoreIdAndNoKeystoreUpdatePermission_CreateSucceeds() throws Throwable {
        mockKeyLookupSuccess();

        when(rbacServices.isPermittedForEntity(any(User.class), any(SsgKeyEntry.class), eq(CREATE),
                anyString())).thenReturn(true); // use wildcard match, so will match the created dummy entity
        when(testInterface.createKeyEntry(any(Goid.class), anyString())).thenReturn(true);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(createKeyEntry, new Goid(0, -1), KEY_ALIAS));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update SSG_KEYSTORE", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(any(User.class), any(Entity.class), eq(CREATE), anyString());
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
    public void testCreateKeyEntry_DummyRequiredAndGivenWildcardKeystoreIdWithKeystoreUpdatePermission_CreateSucceeds() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntityType(UPDATE, EntityType.SSG_KEYSTORE);

        when(rbacServices.isPermittedForEntity(any(User.class), any(SsgKeyEntry.class), any(OperationType.class),
                anyString())).thenReturn(true); // use wildcard match, so will match the created dummy entity
        when(testInterface.createKeyEntry(any(Goid.class), anyString())).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(createKeyEntry, new Goid(0,-1), KEY_ALIAS));

        assertTrue((Boolean) result);

        verify(rbacServices).isPermittedForEntity(any(User.class), any(Entity.class), any(OperationType.class), anyString());
        verify(rbacServices).isPermittedForAnyEntityOfType(any(User.class), eq(UPDATE), eq(EntityType.SSG_KEYSTORE));
    }

    /**
     * pass CHECK_ARG_OPERATION precheck that requires an unusual argument order for locating the
     * keystore ID and key alias
     */
    @Test
    public void testDeleteWithStrangeArgumentOrder_GivenUnusualOrderForKeytoreIdAndKeyAlias_Success() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(UPDATE, keystore);
        mockOperationPermittedForEntity(DELETE, keyEntry);

        when(testInterface.deleteWithStrangeArgumentOrder(isNull(), eq(KEY_ALIAS), isNull(), eq(KEYSTORE_ID), isNull())).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(deleteWithStrangeArgOrder,
                null, KEY_ALIAS, null, KEYSTORE_ID, null));

        assertTrue((Boolean) result);

        verify(rbacServices).isPermittedForEntity(any(User.class), eq(keystore), eq(UPDATE), anyString());
        verify(rbacServices).isPermittedForEntity(any(User.class), eq(keyEntry), eq(DELETE), anyString());
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires an unusual argument order, if needed UPDATE SSG_KEYSTORE
     * permission not present
     */
    @Test
    public void testDeleteWithStrangeArgumentOrder_MissingUpdateKeystorePermission_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(DELETE, keyEntry);

        when(testInterface.deleteWithStrangeArgumentOrder(isNull(), eq(KEY_ALIAS), isNull(), eq(KEYSTORE_ID), isNull())).thenReturn(true);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(deleteWithStrangeArgOrder,
                    null, KEY_ALIAS, null, KEYSTORE_ID, null));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update SSG_KEYSTORE", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(any(User.class), eq(keystore), eq(UPDATE), anyString());
        verify(rbacServices).isPermittedForEntity(any(User.class), eq(keyEntry), eq(DELETE), anyString());
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires an unusual argument order, if needed DELETE SSG_KEY_ENTRY
     * permission not present
     */
    @Test
    public void testDeleteWithStrangeArgumentOrder_MissingDeleteKeyEntryPermission_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(UPDATE, keystore);

        when(testInterface.deleteWithStrangeArgumentOrder(isNull(), eq(KEY_ALIAS), isNull(), eq(KEYSTORE_ID), isNull())).thenReturn(true);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(deleteWithStrangeArgOrder,
                    null, KEY_ALIAS, null, KEYSTORE_ID, null));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Delete null", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(any(User.class), eq(keyEntry), eq(DELETE), anyString());
        verify(rbacServices, never()).isPermittedForEntity(any(User.class), eq(keystore), eq(UPDATE), anyString());
    }

    /**
     * pass CHECK_ARG_OPERATION precheck that requires create permission for key entry and update permission for
     * keystore but no return check
     */
    @Test
    public void testCheckArgOpCreateWithMetadata_PermissionsGranted_Success() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(UPDATE, keystore);
        mockOperationPermittedForAllNonMockedKeyEntries(CREATE);

        when(testInterface.checkArgOpCreateWithMetadata(eq(KEYSTORE_ID), eq(KEY_ALIAS), eq(keyMetadata))).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpCreateWithMetadata, KEYSTORE_ID, KEY_ALIAS, keyMetadata));

        assertTrue((Boolean) result);

        verify(rbacServices).isPermittedForEntity(eq(user), and(not(eq(keyEntry)), isA(SsgKeyEntry.class)), eq(CREATE), isNull(String.class));
        verify(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires create permission for key entry and update permission for
     * keystore if CREATE permission not present for key entry
     */
    @Test
    public void testCheckArgOpCreateWithMetadata_NoCreatePermissionForKeyEntry_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(UPDATE, keystore);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpCreateWithMetadata, KEYSTORE_ID, KEY_ALIAS, keyMetadata));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Create interceptor_test_key_alias", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(eq(user), and(not(eq(keyEntry)), isA(SsgKeyEntry.class)), eq(CREATE), isNull(String.class));
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires create permission for key entry and update permission for
     * keystore if UPDATE permission not present for keystore
     */
    @Test
    public void testCheckArgOpCreateWithMetadata_NoUpdatePermissionForKeystore_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForAllNonMockedKeyEntries(CREATE);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpCreateWithMetadata, KEYSTORE_ID, KEY_ALIAS, keyMetadata));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update SSG_KEYSTORE", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(eq(user), and(not(eq(keyEntry)), isA(SsgKeyEntry.class)), eq(CREATE), isNull(String.class));
        verify(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires create permission for key entry and update permission for
     * keystore if keystore lookup fails
     */
    @Test
    public void testCheckArgOpCreateWithMetadata_KeystoreLookupFails_ObjectNotFoundExceptionThrown() throws Throwable {
        mockOperationPermittedForEntity(UPDATE, keystore);
        mockOperationPermittedForAllNonMockedKeyEntries(CREATE);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpCreateWithMetadata, KEYSTORE_ID, KEY_ALIAS, keyMetadata));
            fail("Expected ObjectNotFoundException.");
        } catch (ObjectNotFoundException e) {
            assertEquals("No keystore found for goid 0000000000000000000000000001432e", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(eq(user), and(not(eq(keyEntry)), isA(SsgKeyEntry.class)), eq(CREATE), isNull(String.class));
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * pass CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata
     */
    @Test
    public void testCheckArgOpUpdateWithMetadata_PermissionsGranted_Success() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(UPDATE, keyEntry);
        mockOperationPermittedForEntity(UPDATE, keystore);
        mockOperationPermittedForAllNonMockedKeyEntries(UPDATE);

        when(testInterface.checkArgOpUpdateWithMetadata(eq(KEYSTORE_ID), eq(KEY_ALIAS), eq(keyMetadata))).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithMetadata, KEYSTORE_ID, KEY_ALIAS, keyMetadata));

        assertTrue((Boolean) result);

        verify(rbacServices).isPermittedForEntity(user, keyEntry, UPDATE, null);
        // for the next one, verify the argument was a new SsgKeyEntry (i.e. the dummy entity, not our mock). Need to
        // be specific about the class here so Mockito doesn't also match it to the subsequent call for the keystore
        verify(rbacServices).isPermittedForEntity(eq(user),
                and(not(eq(keyEntry)), isA(SsgKeyEntry.class)), eq(UPDATE), isNull(String.class));
        verify(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata
     * if permission to update existing key entry denied
     */
    @Test
    public void testCheckArgOpUpdateWithMetadata_NoPermissionsGranted_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithMetadata, KEYSTORE_ID, KEY_ALIAS, keyMetadata));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update null", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, keyEntry, UPDATE, null);

        // no further permission checks if permission denied for existing key entry
        verify(rbacServices, never()).isPermittedForEntity(eq(user),
                and(not(eq(keyEntry)), isA(SsgKeyEntry.class)), eq(UPDATE), isNull(String.class));
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata
     * if permission to update mutated key entry denied
     */
    @Test
    public void testCheckArgOpUpdateWithMetadata_NoPermissionToUpdateKeyEntry_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(UPDATE, keyEntry);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithMetadata, KEYSTORE_ID, KEY_ALIAS, keyMetadata));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update interceptor_test_key_alias", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, keyEntry, UPDATE, null);
        verify(rbacServices).isPermittedForEntity(eq(user),
                and(not(eq(keyEntry)), isA(SsgKeyEntry.class)), eq(UPDATE), isNull(String.class));
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata
     * if permission to update keystore denied
     */
    @Test
    public void testCheckArgOpUpdateWithMetadata_NoPermissionToUpdateKeystore_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(UPDATE, keyEntry);
        mockOperationPermittedForAllNonMockedKeyEntries(UPDATE);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithMetadata, KEYSTORE_ID, KEY_ALIAS, keyMetadata));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update SSG_KEYSTORE", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, keyEntry, UPDATE, null);
        verify(rbacServices).isPermittedForEntity(eq(user),
                and(not(eq(keyEntry)), isA(SsgKeyEntry.class)), eq(UPDATE), isNull(String.class));
        verify(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata
     * if key entry lookup fails
     */
    @Test
    public void testCheckArgOpUpdateWithMetadata_KeyEntryLookupFailure_ObjectNotFoundExceptionThrown() throws Throwable {
        when(ssgKeyStoreManager.findByPrimaryKey(any(Goid.class))).thenReturn(keystore);
        when(keystore.getCertificateChain(anyString())).thenThrow(new ObjectNotFoundException("key entry not found"));

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithMetadata, KEYSTORE_ID, KEY_ALIAS, keyMetadata));
            fail("Expected ObjectNotFoundException.");
        } catch (ObjectNotFoundException e) {
            assertEquals("key entry not found", e.getMessage());
        }

        verify(keystore, only()).getCertificateChain(anyString());
        verifyZeroInteractions(rbacServices);
    }

    /**
     * fail CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata
     * if keystore lookup fails
     */
    @Test
    public void testCheckArgOpUpdateWithMetadata_KeystoreLookupFailure_ObjectNotFoundExceptionThrown() throws Throwable {
        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithMetadata, KEYSTORE_ID, KEY_ALIAS, keyMetadata));
            fail("Expected ObjectNotFoundException.");
        } catch (ObjectNotFoundException e) {
            assertEquals("No keystore found for goid 0000000000000000000000000001432e", e.getMessage());
        }

        verify(ssgKeyStoreManager).findByPrimaryKey(KEYSTORE_ID);
        verifyZeroInteractions(rbacServices);
    }

    /**
     * pass CHECK_UPDATE_ALL_KEYSTORES_NONFATAL precheck if user can update all keystores
     */
    @Test
    public void testIsNonFatalPreCheckPassed_PermissionsGranted_Success() throws Throwable {
        mockOperationPermittedForEntityType(UPDATE, EntityType.SSG_KEYSTORE);

        when(testInterface.isNonFatalPreCheckPassed(anyObject())).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(isNonFatalPreCheckPassed, new Object()));

        assertTrue((Boolean) result);

        verify(rbacServices).isPermittedForAnyEntityOfType(user, UPDATE, EntityType.SSG_KEYSTORE);
    }

    /**
     * fail CHECK_UPDATE_ALL_KEYSTORES_NONFATAL nonfatally when permitted to do so
     */
    @Test
    public void testIsNonFatalPreCheckPassed_NoPermissionsGranted_ReturnsFalse() throws Throwable {
        when(testInterface.isNonFatalPreCheckPassed(anyObject())).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(isNonFatalPreCheckPassed, new Object()));

        assertFalse((Boolean) result);

        verify(rbacServices).isPermittedForAnyEntityOfType(user, UPDATE, EntityType.SSG_KEYSTORE);
    }

    /**
     * fail CHECK_UPDATE_ALL_KEYSTORES_NONFATAL with PermissionDeniedException when nonfatal failure not permitted
     */
    @Test
    public void isFatalNonFatalPreCheckPassed_NonFatalFailureNotPermitted_PermissionDeniedExceptionThrown() throws Throwable {
        Method isFatalNonFatalPreCheckPassed =
                TEST_INTERFACE_CLASS.getMethod("isFatalNonFatalPreCheckPassed", Object.class);

        when(testInterface.isFatalNonFatalPreCheckPassed(anyObject())).thenReturn(true);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(isFatalNonFatalPreCheckPassed, new Object()));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update SSG_KEYSTORE (assign special key purpose [update all])", e.getMessage());
        }

        verify(rbacServices).isPermittedForAnyEntityOfType(user, UPDATE, EntityType.SSG_KEYSTORE);
    }

    /**
     * pass NO_PRE_CHECK even if all permissions would be denied
     */
    @Test
    public void testNoPreChecks_NoPermissionsGranted_Success() throws Throwable {
        Method noPreChecks = TEST_INTERFACE_CLASS.getMethod("noPreChecks", Goid.class, String.class);

        when(testInterface.noPreChecks(KEYSTORE_ID, KEY_ALIAS)).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(noPreChecks, KEYSTORE_ID, KEY_ALIAS));

        assertTrue((Boolean) result);

        verifyZeroInteractions(rbacServices);
    }

    /**
     * fail empty precheck list as invalidly annotated
     */
    @Test
    public void testNoPreChecksEmptyList_NoPreChecksDefined_IllegalStateExceptionThrown() throws Throwable {
        when(testInterface.noPreChecksEmptyList()).thenReturn(true);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(noPreChecksEmptyList));
            fail("Expected IllegalStateException.");
        } catch (IllegalStateException e) {
            assertEquals("At least one PreCheck must be declared (can use NO_PRE_CHECK as no-op)", e.getMessage());
        }

        verifyZeroInteractions(rbacServices);
    }

    /**
     * pass CHECK_ARG_EXPORT_KEY pre check if user has DELETE ALL SSG_KEY_ENTRY permission
     */
    @Test
    public void testExportKey_PermissionGranted_Success() throws Throwable {
        mockOperationPermittedForEntityType(DELETE, EntityType.SSG_KEY_ENTRY);

        when(testInterface.exportKey(KEYSTORE_ID, KEY_ALIAS)).thenReturn("blah");

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(exportKey, KEYSTORE_ID, KEY_ALIAS));

        assertEquals("blah", result);

        verify(rbacServices, only()).isPermittedForAnyEntityOfType(user, DELETE, EntityType.SSG_KEY_ENTRY);
    }

    /**
     * fail CHECK_ARG_EXPORT_KEY pre check if user lacks DELETE ALL SSG_KEY_ENTRY permission
     */
    @Test
    public void testExportKey_NoPermissionsGranted_PermissionDeniedExceptionThrown() throws Throwable {
        when(testInterface.exportKey(KEYSTORE_ID, KEY_ALIAS)).thenReturn("blah");

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(exportKey, KEYSTORE_ID, KEY_ALIAS));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Delete SSG_KEY_ENTRY (export private key [delete all])", e.getMessage());
        }

        verify(rbacServices, only()).isPermittedForAnyEntityOfType(user, DELETE, EntityType.SSG_KEY_ENTRY);
    }

    /**
     * pass multiple prechecks if all succeed
     */
    @Test
    public void testReadAttributeFromKeyEntry_PermissionsGranted_Success() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(READ, keyEntry);
        mockOperationPermittedForEntity(READ, keystore);
        mockOperationPermittedForEntityType(UPDATE, EntityType.SSG_KEYSTORE);
        mockOperationPermittedForAllNonMockedKeyEntries(UPDATE);

        Method readAttributeFromKeyEntry =
                TEST_INTERFACE_CLASS.getMethod("readAttributeFromKeyEntry", Goid.class, String.class);

        when(testInterface.readAttributeFromKeyEntry(KEYSTORE_ID, KEY_ALIAS)).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(readAttributeFromKeyEntry, KEYSTORE_ID, KEY_ALIAS));

        assertTrue((Boolean) result);

        verify(rbacServices).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, READ, null);
        verify(rbacServices).isPermittedForAnyEntityOfType(user, UPDATE, EntityType.SSG_KEYSTORE);
    }

    /**
     * pass CHECK_ARG_OPERATION pre check for UPDATE and key entry in arg
     */
    @Test
    public void testCheckArgOpUpdateWithKeyEntry_PermissionsGranted_Success() throws Throwable {
        SsgKeyEntry toUpdate = makeMockKeyEntry(KEYSTORE_ID, KEY_ALIAS);
        SsgKeyEntry existing = keyEntry;

        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(UPDATE, keystore);
        mockOperationPermittedForEntity(UPDATE, toUpdate);
        mockOperationPermittedForEntity(UPDATE, existing);

        when(testInterface.checkArgOpUpdateWithKeyEntry(toUpdate)).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithKeyEntry, toUpdate));

        assertTrue((Boolean) result);

        verify(rbacServices).isPermittedForEntity(user, toUpdate, UPDATE, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null);
        verify(rbacServices).isPermittedForEntity(user, existing, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for UPDATE and key entry in arg if UPDATE permission not present for existing
     * key
     *
     * N.B. The original corresponding scala test was duplicated in a later test which was more explicitly commented.
     * I have assumed the developer had originally intended the test for a different case (for which there was
     * previously no test), so I have modified it to address that case.
     */
    @Test
    public void testCheckArgOpUpdateWithKeyEntry_NoUpdatePermissionForExistingKeyEntry_PermissionDeniedExceptionThrown() throws Throwable {
        SsgKeyEntry toUpdate = makeMockKeyEntry(KEYSTORE_ID, KEY_ALIAS);
        SsgKeyEntry existing = keyEntry;

        mockKeyLookupSuccess();

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithKeyEntry, toUpdate));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update null", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, existing, UPDATE, null);
        verify(rbacServices, never()).isPermittedForEntity(user, toUpdate, UPDATE, null);
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for UPDATE and key entry in arg if UPDATE keystore permission not present
     */
    @Test
    public void testCheckArgOpUpdateWithKeyEntry_NoUpdatePermissionForKeystore_PermissionDeniedExceptionThrown() throws Throwable {
        SsgKeyEntry toUpdate = makeMockKeyEntry(KEYSTORE_ID, KEY_ALIAS);
        SsgKeyEntry existing = keyEntry;

        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(UPDATE, existing);
        mockOperationPermittedForEntity(UPDATE, toUpdate);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithKeyEntry, toUpdate));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update SSG_KEYSTORE", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, toUpdate, UPDATE, null);
        verify(rbacServices).isPermittedForEntity(user, existing, UPDATE, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for UPDATE and key entry in arg if UPDATE permission not present
     * for updated key entry
     */
    @Test
    public void testCheckArgOpUpdateWithKeyEntry_NoUpdatePermissionForUpdatedKeyEntry_PermissionDeniedExceptionThrown() throws Throwable {
        SsgKeyEntry toUpdate = makeMockKeyEntry(KEYSTORE_ID, KEY_ALIAS);
        SsgKeyEntry existing = keyEntry;

        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(UPDATE, existing);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithKeyEntry, toUpdate));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update null", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, toUpdate, UPDATE, null);
        verify(rbacServices).isPermittedForEntity(user, existing, UPDATE, null);
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for UPDATE and key entry in arg if keystore lookup fails
     */
    @Test
    public void testCheckArgOpUpdateWithKeyEntry_KeystoreLookupFailure_ObjectNotFoundExceptionThrown() throws Throwable {
        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithKeyEntry, keyEntry));
            fail("Expected ObjectNotFoundException.");
        } catch (ObjectNotFoundException e) {
            assertEquals("No keystore found for goid 0000000000000000000000000001432e", e.getMessage());
        }

        verify(ssgKeyStoreManager).findByPrimaryKey(KEYSTORE_ID);
        verifyZeroInteractions(rbacServices);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for UPDATE and key entry in arg if key entry lookup fails
     */
    @Test
    public void testCheckArgOpUpdateWithKeyEntry_KeyEntryLookupFailure_ObjectNotFoundExceptionThrown() throws Throwable {
        when(ssgKeyStoreManager.findByPrimaryKey(any(Goid.class))).thenReturn(keystore);
        when(keystore.getCertificateChain(anyString())).thenThrow(new ObjectNotFoundException("key entry not found"));

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithKeyEntry, keyEntry));
            fail("Expected ObjectNotFoundException.");
        } catch (ObjectNotFoundException e) {
            assertEquals("key entry not found", e.getMessage());
        }

        verify(keystore, only()).getCertificateChain(anyString());
        verifyZeroInteractions(rbacServices);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for UPDATE and key entry in arg if key entry is null
     */
    @Test
    public void testCheckArgOpUpdateWithKeyEntry_NullKeyEntrySpecified_IllegalArgumentExceptionThrown() throws Throwable {
        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpUpdateWithKeyEntry, null));
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            assertEquals("Expected SsgKeyEntry argument with index 0", e.getMessage());
        }

        verifyZeroInteractions(rbacServices);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for UPDATE and key entry in arg if no args provided
     */
    @Test
    public void isCheckArgOpWithKeyEntryNoArg_NoArgumentsGiven_IllegalArgumentExceptionThrown() throws Throwable {
        Method checkArgOpWithKeyEntryNoArg = TEST_INTERFACE_CLASS.getMethod("checkArgOpWithKeyEntryNoArg");

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpWithKeyEntryNoArg));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Expected SsgKeyEntry argument with index 0", e.getMessage());
        }

        verifyZeroInteractions(rbacServices);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for UPDATE and key entry in arg if arg provided is not SsgKeyEntry
     */
    @Test
    public void isCheckArgOpWithKeyEntryInvalidArg_StringArgumentGiven_IllegalArgumentExceptionThrown() throws Throwable {
        Method checkArgOpWithKeyEntryInvalidArg =
                TEST_INTERFACE_CLASS.getMethod("checkArgOpWithKeyEntryInvalidArg", String.class);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpWithKeyEntryInvalidArg, "not a key entry"));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Expected SsgKeyEntry argument with index 0", e.getMessage());
        }

        verifyZeroInteractions(rbacServices);
    }

    /**
     * pass CHECK_ARG_OPERATION pre check for CREATE and key entry in arg
     */
    @Test
    public void testCheckArgOpCreateWithKeyEntry_PermissionsGranted_Success() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(CREATE, keyEntry);
        mockOperationPermittedForEntity(UPDATE, keystore);

        when(testInterface.checkArgOpCreateWithKeyEntry(keyEntry)).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpCreateWithKeyEntry, keyEntry));

        assertTrue((Boolean) result);

        verify(rbacServices).isPermittedForEntity(user, keyEntry, CREATE, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for CREATE and key entry in arg if CREATE permission denied
     */
    @Test
    public void testCheckArgOpCreateWithKeyEntry_NoCreatePermission_PermissionDeniedExceptionThrown() throws Throwable {
        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpCreateWithKeyEntry, keyEntry));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Create null", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, keyEntry, CREATE, null);
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for CREATE and key entry in arg if UPDATE permission denied for keystore
     */
    @Test
    public void testCheckArgOpCreateWithKeyEntry_NoUpdatePermissionForKeystore_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(CREATE, keyEntry);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpCreateWithKeyEntry, keyEntry));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update SSG_KEYSTORE", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, keyEntry, CREATE, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for CREATE and key entry in arg if keystore look up fails
     */
    @Test
    public void testCheckArgOpCreateWithKeyEntry_KeystoreLookupFailure_ObjectNotFoundExceptionThrown() throws Throwable {
        mockOperationPermittedForEntity(CREATE, keyEntry);

        when(ssgKeyStoreManager.findByPrimaryKey(KEYSTORE_ID)).thenThrow(new ObjectNotFoundException("key store not found"));

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpCreateWithKeyEntry, keyEntry));
            fail("Expected ObjectNotFoundException.");
        } catch (ObjectNotFoundException e) {
            assertEquals("key store not found", e.getMessage());
        }

        verify(ssgKeyStoreManager).findByPrimaryKey(KEYSTORE_ID);
        verify(rbacServices).isPermittedForEntity(user, keyEntry, CREATE, null);
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * pass CHECK_ARG_OPERATION pre check for DELETE and key entry in arg
     */
    @Test
    public void testCheckArgOpDeleteWithKeyEntry_PermissionsGranted_Success() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(DELETE, keyEntry);
        mockOperationPermittedForEntity(UPDATE, keystore);

        when(testInterface.checkArgOpDeleteWithKeyEntry(keyEntry)).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpDeleteWithKeyEntry, keyEntry));

        assertTrue((Boolean) result);

        verify(rbacServices).isPermittedForEntity(user, keyEntry, DELETE, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for DELETE and key entry in arg if DELETE permission denied
     */
    @Test
    public void testCheckArgOpDeleteWithKeyEntry_NoDeletePermission_PermissionDeniedExceptionThrown() throws Throwable {
        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpDeleteWithKeyEntry, keyEntry));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Delete null", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, keyEntry, DELETE, null);
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for DELETE and key entry in arg if UPDATE permission denied for keystore
     */
    @Test
    public void testCheckArgOpDeleteWithKeyEntry_NoUpdatePermissionForKeystore_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(DELETE, keyEntry);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpDeleteWithKeyEntry, keyEntry));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update SSG_KEYSTORE", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, keyEntry, DELETE, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for DELETE and key entry in arg if keystore lookup fails
     */
    @Test
    public void testCheckArgOpDeleteWithKeyEntry_KeystoreLookupFailure_ObjectNotFoundExceptionThrown() throws Throwable {
        mockOperationPermittedForEntity(DELETE, keyEntry);

        when(ssgKeyStoreManager.findByPrimaryKey(KEYSTORE_ID)).thenThrow(new ObjectNotFoundException("key store not found"));

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpDeleteWithKeyEntry, keyEntry));
            fail("Expected ObjectNotFoundException.");
        } catch (ObjectNotFoundException e) {
            assertEquals("key store not found", e.getMessage());
        }

        verify(ssgKeyStoreManager).findByPrimaryKey(KEYSTORE_ID);
        verify(rbacServices).isPermittedForEntity(user, keyEntry, DELETE, null);
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, UPDATE, null);
    }

    /**
     * pass CHECK_ARG_OPERATION pre check for READ and key entry in arg
     */
    @Test
    public void testCheckArgOpReadWithKeyEntry_PermissionsGranted_Success() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(READ, keyEntry);
        mockOperationPermittedForEntity(READ, keystore);

        when(testInterface.checkArgOpReadWithKeyEntry(keyEntry)).thenReturn(true);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpReadWithKeyEntry, keyEntry));

        assertTrue((Boolean) result);

        verify(rbacServices).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, READ, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for READ and key entry in arg if READ permission denied
     */
    @Test
    public void testCheckArgOpReadWithKeyEntry_NoReadPermission_PermissionDeniedExceptionThrown() throws Throwable {
        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpReadWithKeyEntry, keyEntry));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Read null", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, READ, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for READ and key entry in arg if UPDATE permission denied for keystore
     */
    @Test
    public void testCheckArgOpReadWithKeyEntry_NoUpdatePermissionForKeystore_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(READ, keyEntry);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpReadWithKeyEntry, keyEntry));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Read SSG_KEYSTORE", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, READ, null);
    }

    /**
     * fail CHECK_ARG_OPERATION pre check for READ and key entry in arg if keystore lookup fails
     */
    @Test
    public void testCheckArgOpReadWithKeyEntry_KeystoreLookupFailure_ObjectNotFoundExceptionThrown() throws Throwable {
        mockOperationPermittedForEntity(READ, keyEntry);

        when(ssgKeyStoreManager.findByPrimaryKey(KEYSTORE_ID)).thenThrow(new ObjectNotFoundException("key store not found"));

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(checkArgOpReadWithKeyEntry, keyEntry));
            fail("Expected ObjectNotFoundException.");
        } catch (ObjectNotFoundException e) {
            assertEquals("key store not found", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices, never()).isPermittedForEntity(user, keystore, READ, null);
    }

    /**
     * fail multiple prechecks if even one fails
     */
    @Test
    public void testReadAttributeFromKeyEntry_NoPermissionToUpdateKeystore_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(READ, keyEntry);
        mockOperationPermittedForEntity(READ, keystore);

        Method readAttributeFromKeyEntry = TEST_INTERFACE_CLASS.getMethod("readAttributeFromKeyEntry", Goid.class, String.class);

        when(testInterface.readAttributeFromKeyEntry(KEYSTORE_ID, KEY_ALIAS)).thenReturn(true);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(readAttributeFromKeyEntry, KEYSTORE_ID, KEY_ALIAS));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Update SSG_KEYSTORE (assign special key purpose [update all])", e.getMessage());
        }

        verify(rbacServices).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, READ, null);
        verify(rbacServices).isPermittedForAnyEntityOfType(user, UPDATE, EntityType.SSG_KEYSTORE);
    }

    /**
     * pass return check if permission is granted
     */
    @Test
    public void testFindAllKeyEntries_PermissionGranted_Success() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(READ, keyEntry);
        mockOperationPermittedForEntity(READ, keystore);

        Method findAllKeyEntries = TEST_INTERFACE_CLASS.getMethod("findAllKeyEntries");

        List<SsgKeyEntry> ssgKeyEntryList = Arrays.asList(keyEntry, keyEntry, keyEntry);

        when(testInterface.findAllKeyEntries()).thenReturn(ssgKeyEntryList);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(findAllKeyEntries));

        assertEquals(ssgKeyEntryList, result);

        verify(rbacServices, times(3)).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices, times(3)).isPermittedForEntity(user, keystore, READ, null);
    }

    /**
     * fail return check if permission is withheld for even a single element of a returned list
     */
    @Test
    public void testFindAllKeyEntries_NoPermissionForOneElementInReturnedList_PermissionDeniedExceptionThrown() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(READ, keyEntry);
        mockOperationPermittedForEntity(READ, keystore);

        Method findAllKeyEntries = TEST_INTERFACE_CLASS.getMethod("findAllKeyEntries");

        List<SsgKeyEntry> ssgKeyEntryList = Arrays.asList(keyEntry, keyEntry, makeMockKeyEntry(new Goid(0, 3453), "foo"));

        when(testInterface.findAllKeyEntries()).thenReturn(ssgKeyEntryList);

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(findAllKeyEntries));
            fail("Expected PermissionDeniedException.");
        } catch (PermissionDeniedException e) {
            assertEquals("Permission denied: Read SSG_KEY_ENTRY", e.getMessage());
        }

        verify(rbacServices, times(2)).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices, times(2)).isPermittedForEntity(user, keystore, READ, null);
    }

    /**
     * pass return filter if permission is granted
     */
    @Test
    public void testFindAllKeyEntriesFilter_PermissionGranted_Success() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(READ, keyEntry);
        mockOperationPermittedForEntity(READ, keystore);

        Method findAllKeyEntriesFilter = TEST_INTERFACE_CLASS.getMethod("findAllKeyEntriesFilter");

        List<SsgKeyEntry> ssgKeyEntryList = Arrays.asList(keyEntry, keyEntry, keyEntry);

        when(testInterface.findAllKeyEntriesFilter()).thenReturn(ssgKeyEntryList);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(findAllKeyEntriesFilter));

        assertEquals(ssgKeyEntryList, result);

        verify(rbacServices, times(3)).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices, times(3)).isPermittedForEntity(user, keystore, READ, null);
    }

    /**
     * pass return filter with filtered results if permission is withheld an element of the returned list
     */
    @Test
    public void testFindAllKeyEntriesFilter_NoPermissionForOneElementInReturnedList_KeyEntriesSubsetReturned() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(READ, keyEntry);
        mockOperationPermittedForEntity(READ, keystore);

        Method findAllKeyEntriesFilter = TEST_INTERFACE_CLASS.getMethod("findAllKeyEntriesFilter");

        List<SsgKeyEntry> ssgKeyEntryList = Arrays.asList(keyEntry, keyEntry, makeMockKeyEntry(KEYSTORE_ID, "foo"));

        when(testInterface.findAllKeyEntriesFilter()).thenReturn(ssgKeyEntryList);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(findAllKeyEntriesFilter));

        assertEquals(Arrays.asList(keyEntry, keyEntry), result);

        verify(rbacServices, times(2)).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices, times(3)).isPermittedForEntity(user, keystore, READ, null);
    }

    /**
     * pass return check (single entity) if permission is granted
     */
    @Test
    public void testFindKeyEntry_PermissionGranted_Success() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(READ, keyEntry);
        mockOperationPermittedForEntity(READ, keystore);

        Method findKeyEntry = TEST_INTERFACE_CLASS.getMethod("findKeyEntry", Goid.class, String.class);

        when(testInterface.findKeyEntry(KEYSTORE_ID, KEY_ALIAS)).thenReturn(keyEntry);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(findKeyEntry, KEYSTORE_ID, KEY_ALIAS));

        assertEquals(keyEntry, result);

        verify(rbacServices).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, READ, null);
    }

    /**
     * pass return filter (single entity) with null result if permission is withheld for the returned entity itself
     */
    @Test
    public void testFindKeyEntryFilter_NoReadPermissionForKeyEntry_NullReturned() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(READ, keystore); // only keystore permission granted

        Method findKeyEntryFilter = TEST_INTERFACE_CLASS.getMethod("findKeyEntryFilter", Goid.class, String.class);

        when(testInterface.findKeyEntryFilter(KEYSTORE_ID, KEY_ALIAS)).thenReturn(keyEntry);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(findKeyEntryFilter, KEYSTORE_ID, KEY_ALIAS));

        assertEquals(null, result);

        verify(rbacServices).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, READ, null);
    }

    /**
     * pass return filter (single entity) with null result if permission is withheld for the returned entity's keystore
     */
    @Test
    public void testFindKeyEntryFilter_NoReadPermissionForKeystore_NullReturned() throws Throwable {
        mockKeyLookupSuccess();
        mockOperationPermittedForEntity(READ, keyEntry); // only key entry permission granted

        Method findKeyEntryFilter = TEST_INTERFACE_CLASS.getMethod("findKeyEntryFilter", Goid.class, String.class);

        when(testInterface.findKeyEntryFilter(KEYSTORE_ID, KEY_ALIAS)).thenReturn(keyEntry);

        Object result = interceptor.invoke(new TestReflectiveMethodInvocation(findKeyEntryFilter, KEYSTORE_ID, KEY_ALIAS));

        assertEquals(null, result);

        verify(rbacServices).isPermittedForEntity(user, keyEntry, READ, null);
        verify(rbacServices).isPermittedForEntity(user, keystore, READ, null);
    }

    /**
     * return check for method that returns a list that contains at least one thing that isn't a key entry
     */
    @Test
    public void testFindUnrelatedStuff_AtLeastOneMethodReturnValueNotSsgKeyEntry_IllegalStateExceptionThrown() throws Throwable {
        Method findUnrelatedStuff = TEST_INTERFACE_CLASS.getMethod("findUnrelatedStuff");

        when(testInterface.findUnrelatedStuff()).thenReturn(Arrays.asList("foo", keyEntry, new Object()));

        try {
            interceptor.invoke(new TestReflectiveMethodInvocation(findUnrelatedStuff));
            fail("Expected IllegalStateException.");
        } catch (IllegalStateException e) {
            assertEquals("Unable to filter return type for method findUnrelatedStuff: List", e.getMessage());
        }

        verifyZeroInteractions(rbacServices);
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
     * N.B. The old Scala implementation of this worked by stipulating that instances not match "haveClass":
     * rbacServices.isPermittedForEntity(beEqualTo(user), haveClass[SsgKeyEntry], beEqualTo(op), org.mockito.Matchers.eq(null)) returns true
     *
     * Using only Mockito this type of matching seems to be unavailable, so we will specify that the permissions
     * is for all SsgKeyEntry instances except the only one we are actually mocking.
     */
    private void mockOperationPermittedForAllNonMockedKeyEntries(OperationType operation) throws FindException {
        when(rbacServices.isPermittedForEntity(eq(user), and(not(eq(keyEntry)), isA(SsgKeyEntry.class)),
                eq(operation), isNull(String.class))).thenReturn(true);
    }

    private SsgKeyEntry makeMockKeyEntry(Goid keystoreId, String keyAlias) {
        SsgKeyEntry keyEntry = mock(SsgKeyEntry.class);
        when(keyEntry.getAlias()).thenReturn(keyAlias);
        when(keyEntry.getKeystoreId()).thenReturn(keystoreId);
        return keyEntry;
    }

    private class TestReflectiveMethodInvocation extends ReflectiveMethodInvocation {

        protected TestReflectiveMethodInvocation(Method method, Object ... arguments) {
            super(null, testInterface, method, arguments, TEST_INTERFACE_CLASS, new ArrayList<>());
        }
    }
}
