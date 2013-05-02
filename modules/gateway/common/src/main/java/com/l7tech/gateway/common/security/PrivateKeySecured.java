package com.l7tech.gateway.common.security;

import com.l7tech.gateway.common.security.rbac.OperationType;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static com.l7tech.gateway.common.security.PrivateKeySecured.ReturnCheck.CHECK_RETURN;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation carrying information to configure the behavior of the PrivateKeyRbacInterceptor.
 * <p/>
 * <b>CHECK</b> means to check for required permission and throw PermissionDeniedException if not found.
 * <p/>
 * <b>FILTER</b> means to check for READ permission and filter the value out from the return value if not found
 * (remove element from List, or replace key entry return with null).
 */
@Documented
@Retention(value = RUNTIME)
@Inherited
@Target({METHOD})
public @interface PrivateKeySecured {

    // Pre-checks

    enum PreCheck {
        /** Do not check any entities referred to by arguments, or check anything else prior to invoking the method. */
        NO_PRE_CHECK,

        /**
         * A {@link #keystoreOidArg} and {@link #keyAliasArg} identify a key entry for which
         * the calling user must possess {@link #argOp} permission on the key entry
         * as well as appropriate permission on its owning keystore (READ for READ, otherwise UPDATE).
         * <p/>
         * For CREATE/UPDATE operation, a {@link #metadataArg} identifies the SsgKeyMetadata (or null) the key
         * will be assigned when it is created/updated.
         */
        CHECK_ARG_OPERATION,

        /**
         * A {@link #keystoreOidArg} and {@link #keyAliasArg} identify a key entry for which
         * the calling user must posses permission to EXPORT THE PRIVATE KEY.
         * <p/>
         * Currently this is done by requiring permission to DELETE ALL SSG_KEY_ENTRY.
         */
        CHECK_ARG_EXPORT_KEY,

        /**
         * The calling user must possess permission to UPDATE the key entry provided by the first method argument.
         */
        CHECK_ARG_UPDATE_KEY_ENTRY,

        /**
         * The calling user must possess permission to UPDATE ALL SSG_KEYSTORE.
         * <p/>
         * Failure of this precheck will be handled nonfatally (ie, as a return value of false
         * form an annotated boolean method, rather than as a PermissionDeniedException)
         * if {@link com.l7tech.gateway.common.security.PrivateKeySecured#allowNonFatalPreChecks} is true.
         */
        CHECK_UPDATE_ALL_KEYSTORES_NONFATAL
    }

    /**
     * @return one or more checks to perform, possibly based on arguments, before invoking the method.
     *         If this is empty the interceptor will fail.  To check nothing, NONE must be explicitly designated
     *         (ie preChecks = {NO_PRE_CHECK})
     */
    PreCheck[] preChecks();

    /**
     * @return true if _NONFATAL pre checks are permitted to fail nonfatally (ie, by making a boolean method return false).
     */
    boolean allowNonFatalPreChecks() default false;

    /**
     * @return the operation permission to require of an identified key entry argument.
     *         An appropriate permission will also be required of its owning keystore (READ for READ, UPDATE otherwise).
     *         <p/>
     *         If this is left as NONE, CHECK_ARG_OPERATION will fail.
     */
    OperationType argOp() default OperationType.NONE;

    /**
     * @return index of keystore OID argument of type long.
     */
    int keystoreOidArg() default 0;

    /**
     * @return index of key entry alias argument of type String.
     */
    int keyAliasArg() default 1;

    /**
     * @return index of metadata argument of type SsgKeyMetadata, or -1 if there isn't one.
     */
    int metadataArg() default -1;


    // Post-checks

    /**
     * Expresses checking to be done on readability of return value, for methods returning
     * a key entry or list of key entries.
     */
    enum ReturnCheck {
        /** Do not check return values at all. */
        NO_RETURN_CHECK,

        /** Check return value(s) for readability, throw PermissionDeniedException if any not readable. */
        CHECK_RETURN,

        /** Filter return value(s) to remove any that aren't readable. */
        FILTER_RETURN
    }

    /**
     * Checking to perofrm on return value, if it is of type SsgKeyEntry or List< SsgKeyEntry >.
     *
     * @return processing to perform on return value before returning it.
     */
    ReturnCheck returnCheck() default CHECK_RETURN;

}
