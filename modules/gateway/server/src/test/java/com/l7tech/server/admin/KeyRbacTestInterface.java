package com.l7tech.server.admin;

import com.l7tech.gateway.common.security.PrivateKeySecured;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.security.rbac.OperationType;

import java.util.List;

import static com.l7tech.gateway.common.security.PrivateKeySecured.PreCheck;
import static com.l7tech.gateway.common.security.PrivateKeySecured.PreCheck.*;
import static com.l7tech.gateway.common.security.PrivateKeySecured.ReturnCheck;
import static com.l7tech.gateway.common.security.PrivateKeySecured.ReturnCheck.NO_RETURN_CHECK;

/**
 * A test interface, whose only implementations are mocks, for testing the {@link PrivateKeyRbacInterceptor}.
 */
public interface KeyRbacTestInterface {
    /** A test method that lacks a @PrivateKeySecured annotation */
    void lacksAnnotation();

    /** A test method that requires no permissions at all. */
    @PrivateKeySecured(preChecks={PreCheck.NO_PRE_CHECK})
    boolean noPreChecks(long keystoreId, String keyAlias);

    /** A test method that the interceptor will reject for failing to specify at least one precheck. */
    @PrivateKeySecured(preChecks={})
    boolean noPreChecksEmptyList();

    /** A test method that needs permission to read an attribute from the specified key entry, as well as update all keystores, requiring multiple prechecks */
    @PrivateKeySecured(preChecks={PreCheck.CHECK_ARG_OPERATION, PreCheck.NO_PRE_CHECK, PreCheck.CHECK_UPDATE_ALL_KEYSTORES_NONFATAL}, argOp=OperationType.READ)
    boolean readAttributeFromKeyEntry(long keystoreId, String keyAlias);

    /** A test method that needs permission to modify the specified key entry */
    @PrivateKeySecured(preChecks={PreCheck.CHECK_ARG_OPERATION}, argOp=OperationType.UPDATE)
    boolean updateKeyEntry(long keystoreId, String keyAlias);

    /** A test method that needs permission to create a new key entry */
    @PrivateKeySecured(preChecks={PreCheck.CHECK_ARG_OPERATION}, argOp=OperationType.CREATE)
    boolean createKeyEntry(long keystoreId, String keyAlias);

    /** A test method that nonfatally returns false if a nonfatal precheck is falsified */
    @PrivateKeySecured(preChecks={CHECK_UPDATE_ALL_KEYSTORES_NONFATAL}, allowNonFatalPreChecks=true)
    boolean isNonFatalPreCheckPassed(Object someArg);

    /** A test method that does not permit a nonfatal precheck to fail nonfatally */
    @PrivateKeySecured(preChecks={CHECK_UPDATE_ALL_KEYSTORES_NONFATAL})
    boolean isFatalNonFatalPreCheckPassed(Object someArg);

    @PrivateKeySecured(preChecks={CHECK_ARG_EXPORT_KEY}, returnCheck=ReturnCheck.NO_RETURN_CHECK)
    Object exportKey(long keystoreId, String keyAlias);

    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION}, argOp=OperationType.DELETE, keystoreOidArg=3, keyAliasArg=1)
    boolean deleteWithStrangeArgumentOrder(Object thing, String keyAlias, Object otherThing, long keystoreId, Object junk);

    @PrivateKeySecured(preChecks={NO_PRE_CHECK}, returnCheck=ReturnCheck.CHECK_RETURN)
    List<SsgKeyEntry> findAllKeyEntries();

    @PrivateKeySecured(preChecks={NO_PRE_CHECK}, returnCheck=ReturnCheck.CHECK_RETURN)
    SsgKeyEntry findKeyEntry(long keystoreId, String keyAlias);

    @PrivateKeySecured(preChecks={NO_PRE_CHECK}, returnCheck=ReturnCheck.FILTER_RETURN)
    List<SsgKeyEntry> findAllKeyEntriesFilter();

    @PrivateKeySecured(preChecks={NO_PRE_CHECK}, returnCheck=ReturnCheck.FILTER_RETURN)
    SsgKeyEntry findKeyEntryFilter(long keystoreId, String keyAlias);

    @PrivateKeySecured(preChecks={NO_PRE_CHECK}, returnCheck=ReturnCheck.FILTER_RETURN)
    List<Object> findUnrelatedStuff();

    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION}, argOp=OperationType.CREATE, metadataArg=2, returnCheck=NO_RETURN_CHECK)
    boolean checkArgOpCreateWithMetadata(long keystoreId, String alias, SsgKeyMetadata metadata);

    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION}, argOp=OperationType.UPDATE, metadataArg=2, returnCheck=NO_RETURN_CHECK)
    boolean checkArgOpUpdateWithMetadata(long keystoreId, String alias, SsgKeyMetadata metadata);

    @PrivateKeySecured(preChecks={CHECK_ARG_UPDATE_KEY_ENTRY}, argOp = OperationType.UPDATE)
    boolean checkArgUpdateKeyEntry(SsgKeyEntry keyEntry);

    @PrivateKeySecured(preChecks={CHECK_ARG_UPDATE_KEY_ENTRY}, argOp = OperationType.UPDATE)
    boolean checkArgUpdateKeyEntryNoArg();

    @PrivateKeySecured(preChecks={CHECK_ARG_UPDATE_KEY_ENTRY}, argOp = OperationType.UPDATE)
    boolean checkArgUpdateKeyEntryInvalidArg(String argNotKeyEntry);

    @PrivateKeySecured(preChecks={CHECK_ARG_UPDATE_KEY_ENTRY}, argOp = OperationType.CREATE)
    boolean checkArgUpdateKeyEntryInvalidOp(SsgKeyEntry keyEntry);
}
