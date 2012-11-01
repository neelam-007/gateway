package com.l7tech.server.admin;

import com.l7tech.gateway.common.security.PrivateKeySecured;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.CustomRbacInterceptor;
import com.l7tech.server.security.rbac.RbacServices;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.KeyStoreException;
import java.util.*;

import static com.l7tech.gateway.common.security.PrivateKeySecured.ReturnCheck;
import static com.l7tech.gateway.common.security.rbac.OperationType.*;

/**
 * Custom RBAC enforcement for methods that work with private keys.
 * Requires method to have the @PrivateKeySecured annotation as well.
 */
@SuppressWarnings("UnusedDeclaration") // Used via class name reference as @Secured customInterceptor
public class PrivateKeyRbacInterceptor implements CustomRbacInterceptor {
    User user;

    @SuppressWarnings("SpringJavaAutowiringInspection") // Idea warns about bean not implmenting RbacService because is actually a cache wrapper bean
    @Inject
    @Named("rbacServices")
    RbacServices rbacServices;

    @Inject
    @Named("ssgKeyStoreManager")
    SsgKeyStoreManager ssgKeyStoreManager;

    // Cache of keystores already looked up for the current invocation so we don't have to do something incredibly slow
    // like loop up the (same) keystore repeatedly for every returned key entry
    private Map<Long,SsgKeyFinder> keystoreCache = new HashMap<Long,SsgKeyFinder>();

    @SuppressWarnings("ThrowFromFinallyBlock")
    @Override
    public Object invoke( @NotNull MethodInvocation invocation ) throws Throwable {
        if (null == user)
            throw new IllegalStateException("Current user not set for PrivateKeyRbacInterceptor");
        if (rbacServices == null)
            throw new IllegalStateException("pkri-rs autowire failure");
        if (ssgKeyStoreManager == null)
            throw new IllegalStateException("pkri-sksm autowire failure");

        final String methodName = invocation.getMethod().getName();
        final PrivateKeySecured secured = invocation.getMethod().getAnnotation(PrivateKeySecured.class);
        if (null == secured)
            throw new IllegalStateException("@Secured method using PrivateKeyRbacInterceptor lacks @PrivateKeySecured");

        try {
            keystoreCache.clear();

            // Pre-checks
            if (!performPreChecks(invocation, secured))
                return Boolean.FALSE;

            // Invoke admin method
            Object ret = invocation.proceed();

            // Return checks
            ret = performReturnChecks(invocation, secured, ret);

            return ret;

        } finally {
            keystoreCache.clear();
        }
    }

    private Object performReturnChecks(MethodInvocation invocation, PrivateKeySecured secured, Object returnValue) throws FindException, KeyStoreException {
        // Perform return value checks as needed and configured
        final Class<?> returnType = invocation.getMethod().getReturnType();
        if ( !isUnfilterableReturnType( returnType ) && ReturnCheck.NO_RETURN_CHECK != secured.returnCheck() ) {
            boolean filterCheckFatal = ReturnCheck.CHECK_RETURN == secured.returnCheck();
            if ( isListOf( SsgKeyEntry.class, returnType, returnValue ) ) {
                //noinspection unchecked
                returnValue = filter( (Collection<SsgKeyEntry>) returnValue, filterCheckFatal );
            } else if ( SsgKeyEntry.class.isAssignableFrom( returnType ) ) {
                returnValue = filter( (SsgKeyEntry) returnValue, filterCheckFatal );
            } else {
                // Add additional filtering support here as needed by new admin methods (that return keystores, or maybe arrays, etc)
                throw new IllegalStateException( "Unable to filter return type for method " + invocation.getMethod().getName() + ": " + returnType.getSimpleName() );
            }
        }
        return returnValue;
    }

    private boolean performPreChecks(MethodInvocation invocation, PrivateKeySecured secured) throws FindException, KeyStoreException {
        // Perform pre-checks
        PrivateKeySecured.PreCheck[] preChecks = secured.preChecks();
        if (preChecks == null || preChecks.length < 1)
            throw new IllegalStateException("At least one PreCheck must be declared (can use NO_PRE_CHECK as no-op)");
        for (PrivateKeySecured.PreCheck preCheck : preChecks) {
            switch (preCheck) {
                case NO_PRE_CHECK:
                    // No action required.
                    break;

                case CHECK_ARG_OPERATION:
                    long keystoreId = (Long)invocation.getArguments()[secured.keystoreOidArg()];
                    String keyAlias = (String)invocation.getArguments()[secured.keyAliasArg()];
                    checkArgOperation(keystoreId, keyAlias, secured.argOp());
                    break;

                case CHECK_ARG_EXPORT_KEY:
                    checkArgExportKey();
                    break;

                case CHECK_UPDATE_ALL_KEYSTORES_NONFATAL:
                    if (!checkUpdateAllKeystoresNonfatal(secured))
                        return false;
                    break;

                default:
                    throw new IllegalStateException("Unrecognized PreCheck: " + preCheck);
            }
        }
        return true;
    }

    private void checkArgOperation( long keystoreId, String keyAlias, OperationType operation ) throws FindException, KeyStoreException {
        final SsgKeyEntry keyEntry;
        if (CREATE == operation) {
            keyEntry = createFakeKeyEntry( keystoreId, keyAlias );
        } else {
            keyEntry = findKeyEntry( keystoreId, keyAlias );
        }

        // Check entry permissions
        checkPermittedForEntity(keyEntry, operation);

        // Check owning keystore permissions
        OperationType ksOperation = READ == operation ? READ : UPDATE;
        checkPermittedForKeystore(keyEntry.getKeystoreId(), ksOperation);
    }

    private boolean checkUpdateAllKeystoresNonfatal(PrivateKeySecured secured) throws FindException {
        // TODO proper OTHER operation type names for "change audit/ssl/etc key" permissions.  for now require DELETE ALL
        if (!rbacServices.isPermittedForAnyEntityOfType(user, OperationType.UPDATE, EntityType.SSG_KEYSTORE)) {
            if (secured.allowNonFatalPreChecks())
                return false;
            throw new PermissionDeniedException(OperationType.UPDATE, EntityType.SSG_KEYSTORE, "assign special key purpose [update all]");
        }
        return true;
    }

    private void checkArgExportKey() throws FindException {
        // TODO proper OTHER operation type name for "export private key" permission.  for now require DELETE ALL
        if (!rbacServices.isPermittedForAnyEntityOfType(user, OperationType.DELETE, EntityType.SSG_KEY_ENTRY))
            throw new PermissionDeniedException(OperationType.DELETE, EntityType.SSG_KEY_ENTRY, "export private key [delete all]");
    }

    /**
     * Check if the specified return type should not be filtered, regardless of the filterReturnValue setting.
     *
     * @param c the class to check.  Required.
     * @return true if this class does not need to be filtered to remove entities that are not visible to this user (perhaps because it is primitive or void)
     */
    private static boolean isUnfilterableReturnType( @NotNull Class<?> c ) {
        return c.isPrimitive() ||
            void.class == c ||
            (c.isArray() && c.getComponentType().isPrimitive());
    }

    /**
     * Check if the specified maybeListClass is declared as a generic List < EC >  where EC is something
     * assignable to the elementClass.
     *
     * @param elementClass expected element class. Required.
     * @param maybeListClass class to test to see if it ia a List of these elements.  Required.
     * @param maybeList the actual (possible) List< elementClass > to examine.  May be null.
     * @return true if the maybeListClass is assignable to List and includes at least one generic type parameter assignable to elementClass.
     */
    private static boolean isListOf( @NotNull Class elementClass, @NotNull Class maybeListClass, @Nullable Object maybeList ) {
        boolean ret = false;

        if (maybeList != null && List.class.isAssignableFrom( maybeListClass ) && List.class.isInstance(maybeList) ) {
            List<?> list = (List<?>) maybeList;
            boolean sawDisqualifyingElement = false;

            for (Object o : list) {
                if (!elementClass.isInstance(o)) {
                    sawDisqualifyingElement = true;
                    break;
                }
            }

            if (!sawDisqualifyingElement) {
                // As best as we can tell, given that generic type information was erased at compile time,
                // nothing about this return value is inconsistent with its having been declared as List< elementClass >.
                return true;
            }
        }

        return ret;
    }

    public void setUser( @NotNull User user ) {
        this.user = user;
    }

    /**
     * Look up a keystore by OID, with caching for the current invocation.
     *
     * @param keystoreOid keystore OID.  Must not be -1.
     * @return matching keystore.  Never null.
     * @throws FindException on find error
     * @throws KeyStoreException on keystore error
     */
    @NotNull
    public SsgKeyFinder findKeystore( long keystoreOid ) throws FindException, KeyStoreException {
        if (keystoreOid == -1)
            throw new IllegalArgumentException("valid keystoreOid required");

        SsgKeyFinder ret = keystoreCache.get(keystoreOid);
        if (ret != null)
            return ret;

        ret = ssgKeyStoreManager.findByPrimaryKey(keystoreOid);
        if (ret == null) {
            // though this can't happen in production, for ease of unit testing we will treat null as "not found" so it plays well with unstubbed mocks
            throw new ObjectNotFoundException("No keystore found for oid " + keystoreOid);
        }
        keystoreCache.put(keystoreOid, ret);
        return ret;
    }


    @NotNull
    private SsgKeyEntry createFakeKeyEntry(long keystoreId, String alias) {
        return SsgKeyEntry.createDummyEntityForAuditing(keystoreId, alias);
    }

    @NotNull
    SsgKeyEntry findKeyEntry(@NotNull SsgKeyFinder finder, @NotNull String keyAlias) throws ObjectNotFoundException, KeyStoreException {
        return finder.getCertificateChain(keyAlias);
    }

    @NotNull
    SsgKeyEntry findKeyEntry(long keystoreOid, @NotNull String keyAlias) throws FindException, KeyStoreException {
        return findKeyEntry(findKeystore(keystoreOid), keyAlias);
    }


    /**
     * Filter the list to remove any keys for which the user does not have READ permission on both the key entry and its keystore.
     *
     * @param list list to filter.  May be null or empty.
     * @param fatal if true, an unreadable entry will trigger a PermissionDeniedException.
     *              if false, an unreadable entry will be filtered out of the returned List.
     * @return a filtered list.  Maybe empty.  May be null only if passed-in list was null.
     * @throws FindException
     * @throws KeyStoreException
     */
    @Nullable
    private Collection<SsgKeyEntry> filter(@Nullable Collection<?> list, boolean fatal) throws FindException, KeyStoreException {
        if (list == null)
            return null;

        List<SsgKeyEntry> filtered = new ArrayList<SsgKeyEntry>();

        for (Object entry : list) {
            if (entry instanceof SsgKeyEntry) {
                final SsgKeyEntry keyEntry = (SsgKeyEntry) entry;
                final SsgKeyEntry filteredEntry = doFilter(keyEntry, fatal);
                if (filteredEntry != null) {
                    filtered.add(filteredEntry);
                }
            }
        }

        return filtered;
    }


    /**
     * Filter the key entry to replace with null any entry for which the current user does not have READ permission on both the entry and its keystore.
     *
     * @param entry the entry to filter.  May be null.
     * @param fatal if true, an unreadable entry will trigger a PermissionDeniedException.
     *              if false, an unreadable entry will be filtered to a null return value.
     * @return a filtered return value.  May be null if the passed-in entry was null, or if it was filered for being unreadable by the current user.
     * @throws FindException
     * @throws KeyStoreException
     */
    @Nullable
    private SsgKeyEntry filter(final SsgKeyEntry entry, boolean fatal) throws FindException, KeyStoreException {
        return doFilter(entry, fatal);
    }

    @Nullable
    private SsgKeyEntry doFilter(SsgKeyEntry entry, boolean fatal) throws FindException, KeyStoreException {
        if (entry != null) {
            // Must be able to READ both the key entry and its keystore
            final boolean readKey = rbacServices.isPermittedForEntity(user, entry, READ, null);
            if (!readKey && fatal)
                throw new PermissionDeniedException(READ, EntityType.SSG_KEY_ENTRY);

            final boolean readKeystore = isPermittedForKeystore(entry.getKeystoreId(), READ);
            if (!readKeystore && fatal)
                throw new PermissionDeniedException(READ, EntityType.SSG_KEYSTORE);

            if (readKey && readKeystore)
                return entry;
        }
        return null;
    }

    private boolean isPermittedForKeystore(long keystoreOid, OperationType operation) throws FindException, KeyStoreException {
        if (keystoreOid == -1) {
            // Must be able to access ALL keystores
            return rbacServices.isPermittedForAnyEntityOfType(user, operation, EntityType.SSG_KEYSTORE);
        }

        // Must be able to access THIS keystore
        SsgKeyFinder keystore = findKeystore(keystoreOid);
        return rbacServices.isPermittedForEntity(user, keystore, operation, null);
    }

    private boolean isPermittedForEntity(Entity entity, OperationType operation) throws FindException {
        return rbacServices.isPermittedForEntity(user, entity, operation, null);
    }

    private void checkPermittedForEntity(Entity entity, OperationType operation) throws FindException {
        if (!isPermittedForEntity(entity, operation))
            permissionDenied(operation, entity, null);
    }

    private static void permissionDenied(OperationType operation, Entity entity, @Nullable String otherOperationName) {
        if (entity instanceof SsgKeyFinder) {
            // Can't serialize exception containing the entire keystore, so refer to the entity class generically
            throw new PermissionDeniedException(operation, EntityType.SSG_KEYSTORE, otherOperationName);
        } else {
            throw new PermissionDeniedException(operation, entity, otherOperationName);
        }
    }

    private void checkPermittedForKeystore(long keystoreOid, OperationType operation) throws FindException, KeyStoreException {
        if (keystoreOid != -1) {
            // Must be able to access THIS keystore
            SsgKeyFinder keystore = findKeystore(keystoreOid);
            if (!rbacServices.isPermittedForEntity(user, keystore, operation, null))
                throw new PermissionDeniedException(operation, EntityType.SSG_KEYSTORE);
        } else {
            // Must be able to access ALL keystores
            if (!rbacServices.isPermittedForAnyEntityOfType(user, operation, EntityType.SSG_KEYSTORE))
                throw new PermissionDeniedException(operation, EntityType.SSG_KEYSTORE);
        }
    }
}