package com.l7tech.server.admin

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import org.springframework.aop.framework.ReflectiveMethodInvocation
import org.aopalliance.intercept.MethodInvocation
import java.lang.reflect.Method
import com.l7tech.identity.User
import com.l7tech.server.security.rbac.RbacServices
import com.l7tech.server.security.keystore.{SsgKeyFinder, SsgKeyStoreManager}
import com.l7tech.gateway.common.security.keystore.{SsgKeyMetadata, SsgKeyEntry}
import com.l7tech.objectmodel.{SecurityZone, Entity, EntityType, ObjectNotFoundException}
import com.l7tech.objectmodel.EntityType._
import java.{lang, util}
import com.l7tech.gateway.common.security.rbac.{PermissionDeniedException, OperationType}
import com.l7tech.gateway.common.security.rbac.OperationType._

/**
 * Test case for [[com.l7tech.server.admin.PrivateKeyRbacInterceptor]].
 */
class PrivateKeyRbacInterceptorTest extends SpecificationWithJUnit with Mockito {

  "PrivateKeyRbacInterceptor" should {

    "fail if invoked without a user" in new DefaultScope {
      interceptor.user = null

      interceptor.invoke(methodInvocation(updateKeyEntry)) must throwA[IllegalStateException](message = "Current user not set")
    }

    "fail if invoked without rbacServices" in new DefaultScope {
      interceptor.rbacServices = null

      interceptor.invoke(methodInvocation(updateKeyEntry)) must throwA[IllegalStateException](message = "pkri-rs autowire failure")
    }

    "fail if invoked without ssgKeyStoreManager" in new DefaultScope {
      interceptor.ssgKeyStoreManager = null

      interceptor.invoke(methodInvocation(updateKeyEntry)) must throwA[IllegalStateException](message = "pkri-sksm autowire failure")
    }

    "fail if invoked method lacks @PrivateKeySecured annotation" in new DefaultScope {
      val lacksAnnotation = testInterfaceClass.getMethod("lacksAnnotation")
      interceptor.invoke(methodInvocation(lacksAnnotation)) must throwA[IllegalStateException](message = "lacks @PrivateKeySecured")
    }

    "fail CHECK_ARG_OPERATION precheck if attempt is made to specify wildcard keystore OID" in new DefaultScope {
      ssgKeyStoreManager.findByPrimaryKey(any) throws new ObjectNotFoundException("No SsgKeyFinder available on this node with id=NNN")

      interceptor.invoke(methodInvocation(updateKeyEntry, new lang.Long(-1), keyAlias)) must throwA[IllegalArgumentException](message = "valid keystoreOid required")
    }

    "fail CHECK_ARG_OPERATION precheck if specified keystore does not exist" in new DefaultScope {
      ssgKeyStoreManager.findByPrimaryKey(any) throws new ObjectNotFoundException("No SsgKeyFinder available on this node with id=NNN")

      interceptor.invoke(methodInvocation(updateKeyEntry, keystoreId, keyAlias)) must throwA[ObjectNotFoundException](message = "No SsgKeyFinder available on this node with id=")
    }

    "fail CHECK_ARG_OPERATION precheck if specified key alias does not exist in specified keystore" in new DefaultScope {
      ssgKeyStoreManager.findByPrimaryKey(any) returns keystore
      keystore.getCertificateChain(anyString) throws new ObjectNotFoundException("Keystore BLAH does not contain any certificate chain entry with alias FOO")

      interceptor.invoke(methodInvocation(updateKeyEntry, keystoreId, keyAlias)) must throwA[ObjectNotFoundException](message = "does not contain any certificate chain entry with alias")
    }

    "pass CHECK_ARG_OPERATION precheck if permitted for user" in new DefaultScope {
      keyLookupWillSucceed
      grant(UPDATE, keystore)
      grant(UPDATE, keyEntry)
      grantAllNonMockedKeyEntries(UPDATE)

      testInterface.updateKeyEntry(any, any) returns true
      interceptor.invoke(methodInvocation(updateKeyEntry, keystoreId, keyAlias)) must_== true

      there were three(rbacServices).isPermittedForEntity(any, any, any, any)
    }

    "fail CHECK_ARG_OPERATION precheck if not permitted for user with keystore" in new DefaultScope {
      keyLookupWillSucceed
      grant(UPDATE, keyEntry)

      testInterface.updateKeyEntry(any, any) returns true
      interceptor.invoke(methodInvocation(updateKeyEntry, keystoreId, keyAlias)) must throwA[PermissionDeniedException]

      there were two(rbacServices).isPermittedForEntity(any, any, any, any)
    }

    "pass CHECK_ARG_OPERATION precheck that requires creating a dummy entity to check" in new DefaultScope {
      keyLookupWillSucceed
      rbacServices.isPermittedForEntity(any, any, any, any) returns true  // use wildcard match, so will match the created dummy entity

      testInterface.createKeyEntry(any, any) returns true
      interceptor.invoke(methodInvocation(createKeyEntry, keystoreId, keyAlias)) must_== true

      there were two(rbacServices).isPermittedForEntity(any, any, any, any)
    }

    "fail CHECK_ARG_OPERATION precheck that requires creating a dummy entity to check, with a wildcard keystore id, if lacking UPDATE ALL SSG_KEYSTORE" in new DefaultScope {
      // This particular situation isn't useful (create key without specifying which keystore?!) but it provides coverage of checkPermittedForKeystore() with wildcard keystore ID
      keyLookupWillSucceed
      rbacServices.isPermittedForEntity(any, any, beEqualTo(CREATE), any) returns true  // use wildcard match, so will match the created dummy entity

      testInterface.createKeyEntry(any, any) returns true
      interceptor.invoke(methodInvocation(createKeyEntry, new lang.Long(-1), keyAlias)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(any, any, beEqualTo(CREATE), any)
      there was one(rbacServices).isPermittedForAnyEntityOfType(any, any, any)
    }

    "pass CHECK_ARG_OPERATION precheck that requires creating a dummy entity to check, with a wildcard keystore id, if have UPDATE ALL SSG_KEYSTORE" in new DefaultScope {
      // This particular situation isn't useful (create key without specifying which keystore?!) but it provides coverage of checkPermittedForKeystore() with wildcard keystore ID
      keyLookupWillSucceed
      rbacServices.isPermittedForEntity(any, any, any, any) returns true  // use wildcard match, so will match the created dummy entity
      grantAll(UPDATE, SSG_KEYSTORE)

      testInterface.createKeyEntry(any, any) returns true
      interceptor.invoke(methodInvocation(createKeyEntry, new lang.Long(-1), keyAlias)) must_== true

      there was one(rbacServices).isPermittedForEntity(any, any, any, any)
      there was one(rbacServices).isPermittedForAnyEntityOfType(user, OperationType.UPDATE, EntityType.SSG_KEYSTORE)
    }

    "pass CHECK_ARG_OPERATION precheck that requires an unusual argument order for locating the keystore ID and key alias" in new DefaultScope {
      keyLookupWillSucceed
      grant(UPDATE, keystore)
      grant(DELETE, keyEntry)

      val deleteWithStrangeArgumentOrder = testInterfaceClass.getMethod("deleteWithStrangeArgumentOrder", classOf[Object], classOf[String], classOf[Object], classOf[Long], classOf[Object])
      testInterface.deleteWithStrangeArgumentOrder(null, keyAlias, null, keystoreId, null) returns true
      interceptor.invoke(methodInvocation(deleteWithStrangeArgumentOrder, null, keyAlias, null, keystoreId, null)) must_== true

      there was one(rbacServices).isPermittedForEntity(user, keystore, OperationType.UPDATE, null)
      there was one(rbacServices).isPermittedForEntity(user, keyEntry, OperationType.DELETE, null)
    }

    "fail CHECK_ARG_OPERATION precheck that requires an unusual argument order, if needed UPDATE SSG_KEYSTORE permission not present" in new DefaultScope {
      keyLookupWillSucceed
      grant(DELETE, keyEntry)

      val deleteWithStrangeArgumentOrder = testInterfaceClass.getMethod("deleteWithStrangeArgumentOrder", classOf[Object], classOf[String], classOf[Object], classOf[Long], classOf[Object])
      testInterface.deleteWithStrangeArgumentOrder(null, keyAlias, null, keystoreId, null) returns true
      interceptor.invoke(methodInvocation(deleteWithStrangeArgumentOrder, null, keyAlias, null, keystoreId, null)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(user, keystore, OperationType.UPDATE, null)
      there was one(rbacServices).isPermittedForEntity(user, keyEntry, OperationType.DELETE, null)
    }

    "fail CHECK_ARG_OPERATION precheck that requires an unusual argument order, if needed DELETE SSG_KEY_ENTRY permission not present" in new DefaultScope {
      keyLookupWillSucceed
      grant(UPDATE, keystore)

      val deleteWithStrangeArgumentOrder = testInterfaceClass.getMethod("deleteWithStrangeArgumentOrder", classOf[Object], classOf[String], classOf[Object], classOf[Long], classOf[Object])
      testInterface.deleteWithStrangeArgumentOrder(null, keyAlias, null, keystoreId, null) returns true
      interceptor.invoke(methodInvocation(deleteWithStrangeArgumentOrder, null, keyAlias, null, keystoreId, null)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, OperationType.DELETE, null)
      there was no(rbacServices).isPermittedForEntity(user, keystore, OperationType.UPDATE, null) // key entry not checked if no keystore perm
    }

    "pass CHECK_ARG_OPERATION precheck that requires create permission for key entry and update permission for keystore but no return check" in new DefaultScope {
      keyLookupWillSucceed
      grant(UPDATE, keystore)
      grantAllNonMockedKeyEntries(CREATE)

      testInterface.checkArgOpCreateWithMetadata(keystoreId, keyAlias, keyMetadata) returns true
      interceptor.invoke(methodInvocation(checkArgOpCreateWithMetadata, keystoreId, keyAlias, keyMetadata)) must_== true

      there was one(rbacServices).isPermittedForEntity(beEqualTo(user), haveClass[SsgKeyEntry], beEqualTo(OperationType.CREATE), org.mockito.Matchers.eq(null))
      there was one(rbacServices).isPermittedForEntity(user, keystore, OperationType.UPDATE, null)
    }

    "fail CHECK_ARG_OPERATION precheck that requires create permission for key entry and update permission for keystore if CREATE permission not present for key entry" in new DefaultScope {
      keyLookupWillSucceed
      grant(UPDATE, keystore)

      interceptor.invoke(methodInvocation(checkArgOpCreateWithMetadata, keystoreId, keyAlias, keyMetadata)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(beEqualTo(user), haveClass[SsgKeyEntry], beEqualTo(OperationType.CREATE), org.mockito.Matchers.eq(null))
      there was no(rbacServices).isPermittedForEntity(user, keystore, OperationType.UPDATE, null)
    }

    "fail CHECK_ARG_OPERATION precheck that requires create permission for key entry and update permission for keystore if UPDATE permission not present for keystore" in new DefaultScope {
      keyLookupWillSucceed
      grantAllNonMockedKeyEntries(CREATE)

      interceptor.invoke(methodInvocation(checkArgOpCreateWithMetadata, keystoreId, keyAlias, keyMetadata)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(beEqualTo(user), haveClass[SsgKeyEntry], beEqualTo(OperationType.CREATE), org.mockito.Matchers.eq(null))
      there was one(rbacServices).isPermittedForEntity(user, keystore, OperationType.UPDATE, null)
    }

    "fail CHECK_ARG_OPERATION precheck that requires create permission for key entry and update permission for keystore if keystore lookup fails" in new DefaultScope {
      grant(UPDATE, keystore)
      grantAllNonMockedKeyEntries(CREATE)

      interceptor.invoke(methodInvocation(checkArgOpCreateWithMetadata, keystoreId, keyAlias, keyMetadata)) must throwA[ObjectNotFoundException]

      there was one(rbacServices).isPermittedForEntity(beEqualTo(user), haveClass[SsgKeyEntry], beEqualTo(OperationType.CREATE), org.mockito.Matchers.eq(null))
      there was no(rbacServices).isPermittedForEntity(user, keystore, OperationType.UPDATE, null)
    }

    "pass CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata" in new DefaultScope {
      keyLookupWillSucceed
      grant(UPDATE, keyEntry)
      grant(UPDATE, keystore)
      grantAllNonMockedKeyEntries(UPDATE)

      testInterface.checkArgOpUpdateWithMetadata(keystoreId, keyAlias, keyMetadata) returns true
      interceptor.invoke(methodInvocation(checkArgOpUpdateWithMetadata, keystoreId, keyAlias, keyMetadata)) must_== true

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, OperationType.UPDATE, null)
      there was one(rbacServices).isPermittedForEntity(beEqualTo(user), haveClass[SsgKeyEntry], beEqualTo(OperationType.UPDATE), org.mockito.Matchers.eq(null))
      there was one(rbacServices).isPermittedForEntity(user, keystore, OperationType.UPDATE, null)
    }

    "fail CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata if permission to update existing key entry denied" in new DefaultScope {
      keyLookupWillSucceed

      interceptor.invoke(methodInvocation(checkArgOpUpdateWithMetadata, keystoreId, keyAlias, keyMetadata)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, OperationType.UPDATE, null)
      // no permission check for updated key entry done if permission denied for existing key entry
      there was no(rbacServices).isPermittedForEntity(beEqualTo(user), haveClass[SsgKeyEntry], beEqualTo(OperationType.UPDATE), org.mockito.Matchers.eq(null))
      there was no(rbacServices).isPermittedForEntity(user, keystore, OperationType.UPDATE, null)
    }

    "fail CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata if permission to update mutated key entry denied" in new DefaultScope {
      keyLookupWillSucceed
      grant(UPDATE, keyEntry)

      interceptor.invoke(methodInvocation(checkArgOpUpdateWithMetadata, keystoreId, keyAlias, keyMetadata)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, OperationType.UPDATE, null)
      there was one(rbacServices).isPermittedForEntity(beEqualTo(user), haveClass[SsgKeyEntry], beEqualTo(OperationType.UPDATE), org.mockito.Matchers.eq(null))
      there was no(rbacServices).isPermittedForEntity(user, keystore, OperationType.UPDATE, null)
    }

    "fail CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata if permission to update keystore denied" in new DefaultScope {
      keyLookupWillSucceed
      grant(UPDATE, keyEntry)
      grantAllNonMockedKeyEntries(UPDATE)

      interceptor.invoke(methodInvocation(checkArgOpUpdateWithMetadata, keystoreId, keyAlias, keyMetadata)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, OperationType.UPDATE, null)
      there was one(rbacServices).isPermittedForEntity(beEqualTo(user), haveClass[SsgKeyEntry], beEqualTo(OperationType.UPDATE), org.mockito.Matchers.eq(null))
      there was one(rbacServices).isPermittedForEntity(user, keystore, OperationType.UPDATE, null)
    }

    "fail CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata if key entry lookup fails" in new DefaultScope {
      ssgKeyStoreManager.findByPrimaryKey(new lang.Long(keystoreId)) returns keystore
      keystore.getCertificateChain(keyAlias) throws new ObjectNotFoundException("key entry not found")

      interceptor.invoke(methodInvocation(checkArgOpUpdateWithMetadata, keystoreId, keyAlias, keyMetadata)) must throwA[ObjectNotFoundException]

      there was no(rbacServices).isPermittedForEntity(any, any, any, any)
    }

    "fail CHECK_ARG_OPERATION precheck that requires update permission on key entry and provides metadata if keystore lookup fails" in new DefaultScope {
      interceptor.invoke(methodInvocation(checkArgOpUpdateWithMetadata, keystoreId, keyAlias, keyMetadata)) must throwA[ObjectNotFoundException]

      there was one(ssgKeyStoreManager).findByPrimaryKey(new lang.Long(keystoreId))
      there was no(rbacServices).isPermittedForEntity(any, any, any, any)
    }

    "pass CHECK_UPDATE_ALL_KEYSTORES_NONFATAL precheck if user can update all keystores" in new DefaultScope {
      grantAll(UPDATE, SSG_KEYSTORE)

      testInterface.isNonFatalPreCheckPassed(any) returns true
      interceptor.invoke(methodInvocation(isNonFatalPreCheckPassed, new Object())) must_== true

      there was one(rbacServices).isPermittedForAnyEntityOfType(user, OperationType.UPDATE, EntityType.SSG_KEYSTORE)
    }

    "fail CHECK_UPDATE_ALL_KEYSTORES_NONFATAL nonfatally when permitted to do so" in new DefaultScope {
      testInterface.isNonFatalPreCheckPassed(any) returns true
      interceptor.invoke(methodInvocation(isNonFatalPreCheckPassed, new Object())) must_== false

      there was one(rbacServices).isPermittedForAnyEntityOfType(user, OperationType.UPDATE, EntityType.SSG_KEYSTORE)
    }

    "fail CHECK_UPDATE_ALL_KEYSTORES_NONFATAL with PermissionDeniedException when nonfatal failure not permitted" in new DefaultScope {
      val isFatalNonFatalPreCheckPassed = testInterfaceClass.getMethod("isFatalNonFatalPreCheckPassed", classOf[Object])
      testInterface.isFatalNonFatalPreCheckPassed(any) returns true
      interceptor.invoke(methodInvocation(isFatalNonFatalPreCheckPassed, new Object())) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForAnyEntityOfType(user, OperationType.UPDATE, EntityType.SSG_KEYSTORE)
    }

    "pass NO_PRE_CHECK even if all permissions would be denied" in new DefaultScope {
      val noPreChecks = testInterfaceClass.getMethod("noPreChecks", classOf[Long], classOf[String])
      testInterface.noPreChecks(any, any) returns true
      interceptor.invoke(methodInvocation(noPreChecks, keystoreId, keyAlias)) must_== true

      there was no(rbacServices).isPermittedForEntity(any, any, any, any)
      there was no(rbacServices).isPermittedForAnyEntityOfType(any, any, any)
    }

    "fail empty precheck list as invalidly annotated" in new DefaultScope {
      testInterface.noPreChecksEmptyList() returns true
      interceptor.invoke(methodInvocation(noPreChecksEmptyList)) must throwA[IllegalStateException](message = "At least one PreCheck")

      there was no(rbacServices).isPermittedForEntity(any, any, any, any)
      there was no(rbacServices).isPermittedForAnyEntityOfType(any, any, any)
    }

    "pass CHECK_ARG_EXPORT_KEY pre check if user has DELETE ALL SSG_KEY_ENTRY permission" in new DefaultScope {
      grantAll(DELETE, SSG_KEY_ENTRY)

      testInterface.exportKey(any, any) returns "blah"
      interceptor.invoke(methodInvocation(exportKey, keystoreId, keyAlias)) must be equalTo "blah"

      there was one(rbacServices).isPermittedForAnyEntityOfType(user, OperationType.DELETE, EntityType.SSG_KEY_ENTRY)
    }

    "fail CHECK_ARG_EXPORT_KEY pre check if user lacks DELETE ALL SSG_KEY_ENTRY permission" in new DefaultScope {
      testInterface.exportKey(any, any) returns "blah"
      interceptor.invoke(methodInvocation(exportKey, keystoreId, keyAlias)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForAnyEntityOfType(user, OperationType.DELETE, EntityType.SSG_KEY_ENTRY)
    }

    "pass multiple prechecks if all succeed" in new DefaultScope {
      keyLookupWillSucceed
      grant(READ, keyEntry)
      grant(READ, keystore)
      grantAll(UPDATE, SSG_KEYSTORE)
      val readAttributeFromKeyEntry = testInterfaceClass.getMethod("readAttributeFromKeyEntry", classOf[Long], classOf[String])

      testInterface.readAttributeFromKeyEntry(any, any) returns true
      interceptor.invoke(methodInvocation(readAttributeFromKeyEntry, keystoreId, keyAlias)) must_== true

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, READ, null)
      there was one(rbacServices).isPermittedForEntity(user, keystore, READ, null)
      there was one(rbacServices).isPermittedForAnyEntityOfType(user, OperationType.UPDATE, EntityType.SSG_KEYSTORE)
    }

    "pass CHECK_ARG_UPDATE_KEY_ENTRY pre check" in new DefaultScope {
      val toUpdate = makeEntry(keystoreId, keyAlias)
      val existing = keyEntry
      keyLookupWillSucceed
      grant(UPDATE, toUpdate)
      grant(UPDATE, keystore)
      grant(UPDATE, existing)

      testInterface.checkArgUpdateKeyEntry(toUpdate) returns true
      interceptor.invoke(methodInvocation(checkArgUpdateKeyEntry, toUpdate)) must_== true

      there was one(rbacServices).isPermittedForEntity(user, toUpdate, UPDATE, null)
      there was one(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null)
      there was one(rbacServices).isPermittedForEntity(user, existing, UPDATE, null)
    }

    "fail CHECK_ARG_UPDATE_KEY_ENTRY pre check if UPDATE permission not present for key entry to update" in new DefaultScope {
      interceptor.invoke(methodInvocation(checkArgUpdateKeyEntry, keyEntry)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, UPDATE, null)
      there was no(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null)
      there was no(keystore).getCertificateChain(keyAlias)
    }

    "fail CHECK_ARG_UPDATE_KEY_ENTRY pre check if UPDATE keystore permission not present" in new DefaultScope {
      keyLookupWillSucceed
      grant(UPDATE, keyEntry)

      interceptor.invoke(methodInvocation(checkArgUpdateKeyEntry, keyEntry)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, UPDATE, null)
      there was one(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null)
      there was no(keystore).getCertificateChain(keyAlias)
    }

    "fail CHECK_ARG_UPDATE_KEY_ENTRY pre check if UPDATE permission not present for updated key entry" in new DefaultScope {
      val toUpdate = makeEntry(keystoreId, keyAlias)
      val existing = keyEntry
      keyLookupWillSucceed
      grant(UPDATE, toUpdate)
      grant(UPDATE, keystore)

      interceptor.invoke(methodInvocation(checkArgUpdateKeyEntry, toUpdate)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(user, toUpdate, UPDATE, null)
      there was one(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null)
      there was one(rbacServices).isPermittedForEntity(user, existing, UPDATE, null)
    }

    "fail CHECK_ARG_UPDATE_KEY_ENTRY pre check if keystore lookup fails" in new DefaultScope {
      grant(UPDATE, keyEntry)

      interceptor.invoke(methodInvocation(checkArgUpdateKeyEntry, keyEntry)) must throwA[ObjectNotFoundException]

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, UPDATE, null)
      there was one(ssgKeyStoreManager).findByPrimaryKey(keystoreId)
      there was no(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null)
    }

    "fail CHECK_ARG_UPDATE_KEY_ENTRY pre check if key entry lookup fails" in new DefaultScope {
      grant(UPDATE, keyEntry)
      ssgKeyStoreManager.findByPrimaryKey(new lang.Long(keystoreId)) returns keystore
      grant(UPDATE, keystore)
      keystore.getCertificateChain(keyAlias) throws new ObjectNotFoundException("key entry not found")

      interceptor.invoke(methodInvocation(checkArgUpdateKeyEntry, keyEntry)) must throwA[ObjectNotFoundException]

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, UPDATE, null)
      there was one(rbacServices).isPermittedForEntity(user, keystore, UPDATE, null)
      there was one(keystore).getCertificateChain(keyAlias)
    }

    "fail CHECK_ARG_UPDATE_KEY_ENTRY if key entry is null" in new DefaultScope {
      interceptor.invoke(methodInvocation(checkArgUpdateKeyEntry, null)) must throwA[IllegalArgumentException]
      there was no(rbacServices).isPermittedForEntity(any, any, any, any)
    }

    "fail CHECK_ARG_UPDATE_KEY_ENTRY if operation is not UPDATE" in new DefaultScope {
      interceptor.invoke(methodInvocation(checkArgUpdateKeyEntryInvalidOp, keyEntry)) must throwA[IllegalArgumentException]
      there was no(rbacServices).isPermittedForEntity(any, any, any, any)
    }

    "fail CHECK_ARG_UPDATE_KEY_ENTRY if no args provided" in new DefaultScope {
      val checkArgUpdateKeyEntryNoArg = testInterfaceClass.getMethod("checkArgUpdateKeyEntryNoArg")
      interceptor.invoke(methodInvocation(checkArgUpdateKeyEntryNoArg)) must throwA[IllegalArgumentException]
      there was no(rbacServices).isPermittedForEntity(any, any, any, any)
    }

    "fail CHECK_ARG_UPDATE_KEY_ENTRY if arg provided is not SsgKeyEntry" in new DefaultScope {
      val checkArgUpdateKeyEntryInvalidArg = testInterfaceClass.getMethod("checkArgUpdateKeyEntryInvalidArg", classOf[String])
      interceptor.invoke(methodInvocation(checkArgUpdateKeyEntryInvalidArg, "not a key entry")) must throwA[IllegalArgumentException]
      there was no(rbacServices).isPermittedForEntity(any, any, any, any)
    }

    "fail multiple prechecks if even one fails" in new DefaultScope {
      keyLookupWillSucceed
      grant(READ, keyEntry)
      grant(READ, keystore)
      val readAttributeFromKeyEntry = testInterfaceClass.getMethod("readAttributeFromKeyEntry", classOf[Long], classOf[String])

      testInterface.readAttributeFromKeyEntry(any, any) returns true
      interceptor.invoke(methodInvocation(readAttributeFromKeyEntry, keystoreId, keyAlias)) must throwA[PermissionDeniedException]

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, READ, null)
      there was one(rbacServices).isPermittedForEntity(user, keystore, READ, null)
      there was one(rbacServices).isPermittedForAnyEntityOfType(user, OperationType.UPDATE, EntityType.SSG_KEYSTORE)
    }

    "pass return check if permission is granted" in new DefaultScope {
      keyLookupWillSucceed
      grant(READ, keystore)
      grant(READ, keyEntry)
      val findAllKeyEntries = testInterfaceClass.getMethod("findAllKeyEntries")

      val ret = util.Arrays.asList(keyEntry, keyEntry, keyEntry)
      testInterface.findAllKeyEntries() returns ret
      interceptor.invoke(methodInvocation(findAllKeyEntries)) must_== ret

      there were three(rbacServices).isPermittedForEntity(user, keystore, READ, null)
      there were three(rbacServices).isPermittedForEntity(user, keyEntry, READ, null)
    }

    "fail return check if permission is withheld for even a single element of a returned list" in new DefaultScope {
      keyLookupWillSucceed
      grant(READ, keystore)
      grant(READ, keyEntry)
      val findAllKeyEntries = testInterfaceClass.getMethod("findAllKeyEntries")

      val ret = util.Arrays.asList(keyEntry, keyEntry, makeEntry(4848, "blah"))
      testInterface.findAllKeyEntries() returns ret
      interceptor.invoke(methodInvocation(findAllKeyEntries)) must throwA[PermissionDeniedException]

      there were two(rbacServices).isPermittedForEntity(user, keystore, READ, null)
      there were two(rbacServices).isPermittedForEntity(user, keyEntry, READ, null)
    }

    "pass return filter if permission is granted" in new DefaultScope {
      keyLookupWillSucceed
      grant(READ, keystore)
      grant(READ, keyEntry)
      val findAllKeyEntriesFilter = testInterfaceClass.getMethod("findAllKeyEntriesFilter")

      val ret = util.Arrays.asList(keyEntry, keyEntry, keyEntry)
      testInterface.findAllKeyEntriesFilter() returns ret
      interceptor.invoke(methodInvocation(findAllKeyEntriesFilter)) must_== ret

      there were three(rbacServices).isPermittedForEntity(user, keystore, READ, null)
      there were three(rbacServices).isPermittedForEntity(user, keyEntry, READ, null)
    }

    "pass return filter with filtered results if permission is withheld an element of the returned list" in new DefaultScope {
      keyLookupWillSucceed
      grant(READ, keystore)
      grant(READ, keyEntry)
      val findAllKeyEntriesFilter = testInterfaceClass.getMethod("findAllKeyEntriesFilter")

      testInterface.findAllKeyEntriesFilter() returns util.Arrays.asList(keyEntry, keyEntry, makeEntry(keystoreId, "martian"))
      interceptor.invoke(methodInvocation(findAllKeyEntriesFilter)) must_== util.Arrays.asList(keyEntry, keyEntry)

      there were two(rbacServices).isPermittedForEntity(user, keyEntry, READ, null)
      there were three(rbacServices).isPermittedForEntity(user, keystore, READ, null)
    }

    "pass return check (single entity) if permission is granted" in new DefaultScope {
      keyLookupWillSucceed
      grant(READ, keystore)
      grant(READ, keyEntry)
      val findKeyEntry = testInterfaceClass.getMethod("findKeyEntry", classOf[Long], classOf[String])

      testInterface.findKeyEntry(keystoreId, keyAlias) returns keyEntry
      interceptor.invoke(methodInvocation(findKeyEntry, keystoreId, keyAlias)) must_== keyEntry

      there was one(rbacServices).isPermittedForEntity(user, keystore, READ, null)
      there was one(rbacServices).isPermittedForEntity(user, keyEntry, READ, null)
    }

    "pass return filter (single entity) with null result if permission is withheld for the returned entity itself" in new DefaultScope {
      keyLookupWillSucceed
      grant(READ, keystore)
      val findKeyEntryFilter = testInterfaceClass.getMethod("findKeyEntryFilter", classOf[Long], classOf[String])

      testInterface.findKeyEntryFilter(keystoreId, keyAlias) returns keyEntry
      interceptor.invoke(methodInvocation(findKeyEntryFilter, keystoreId, keyAlias)) must_== null

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, READ, null)
      there was one(rbacServices).isPermittedForEntity(user, keystore, READ, null)
    }

    "pass return filter (single entity) with null result if permission is withheld for the returned entity's keystore" in new DefaultScope {
      keyLookupWillSucceed
      grant(READ, keyEntry)
      val findKeyEntryFilter = testInterfaceClass.getMethod("findKeyEntryFilter", classOf[Long], classOf[String])

      testInterface.findKeyEntryFilter(keystoreId, keyAlias) returns keyEntry
      interceptor.invoke(methodInvocation(findKeyEntryFilter, keystoreId, keyAlias)) must_== null

      there was one(rbacServices).isPermittedForEntity(user, keyEntry, READ, null)
      there was one(rbacServices).isPermittedForEntity(user, keystore, READ, null)
    }

    "pass return check for method that retuns a list that contains at least one thing that isn't a key entry" in new DefaultScope {
      val findUnrelatedStuff = testInterfaceClass.getMethod("findUnrelatedStuff")

      val obj = new Object()
      testInterface.findUnrelatedStuff() returns util.Arrays.asList("foo", keyEntry, obj)
      interceptor.invoke(methodInvocation(findUnrelatedStuff)) must throwA[IllegalStateException](message = "Unable to filter return type for method")

      there was no(rbacServices).isPermittedForEntity(any, any, any, any)
    }
  }


  trait DefaultScope extends Scope {
    val testInterface = mock[KeyRbacTestInterface]
    val testInterfaceClass = classOf[KeyRbacTestInterface]

    // Test methods that may be used more than once
    val noPreChecksEmptyList = testInterfaceClass.getMethod("noPreChecksEmptyList")
    val updateKeyEntry = testInterfaceClass.getMethod("updateKeyEntry", classOf[Long], classOf[String])
    val createKeyEntry = testInterfaceClass.getMethod("createKeyEntry", classOf[Long], classOf[String])
    val isNonFatalPreCheckPassed = testInterfaceClass.getMethod("isNonFatalPreCheckPassed", classOf[Object])
    val exportKey = testInterfaceClass.getMethod("exportKey", classOf[Long], classOf[String])
    val checkArgUpdateKeyEntry = testInterfaceClass.getMethod("checkArgUpdateKeyEntry", classOf[SsgKeyEntry])
    val checkArgUpdateKeyEntryInvalidOp = testInterfaceClass.getMethod("checkArgUpdateKeyEntryInvalidOp", classOf[SsgKeyEntry])
    val checkArgOpCreateWithMetadata = testInterfaceClass.getMethod("checkArgOpCreateWithMetadata", classOf[Long], classOf[String], classOf[SsgKeyMetadata])
    val checkArgOpUpdateWithMetadata = testInterfaceClass.getMethod("checkArgOpUpdateWithMetadata", classOf[Long], classOf[String], classOf[SsgKeyMetadata])

    /** Override to access protected c'tor */
    class MyReflectiveMethodInvocation(proxy: AnyRef, target: AnyRef, method: Method, arguments: Array[AnyRef], targetClass: Class[_])
      extends ReflectiveMethodInvocation(proxy, target, method, arguments, targetClass, new util.ArrayList[AnyRef]())

    def methodInvocation(m: java.lang.reflect.Method, arguments: AnyRef*): MethodInvocation = new MyReflectiveMethodInvocation(null, testInterface, m, arguments.toArray, testInterfaceClass)

    def makeEntry(keystoreOid: Long, keyAlias: String): SsgKeyEntry = {
      val entry = mock[SsgKeyEntry]
      entry.getAlias returns keyAlias
      entry.getKeystoreId returns keystoreOid
      entry
    }

    def makeEntry(keystoreOid: Long, keyAlias: String, securityZone: SecurityZone): SsgKeyEntry = {
      val entry = mock[SsgKeyEntry]
      entry.getAlias returns keyAlias
      entry.getKeystoreId returns keystoreOid
      entry.getSecurityZone returns securityZone
      entry
    }

    def makeKeyMetadata(keystoreOid: Long, keyAlias: String, securityZone: SecurityZone) : SsgKeyMetadata = {
      val entry = mock[SsgKeyMetadata]
      entry.getAlias returns keyAlias
      entry.getKeystoreOid returns keystoreId
      entry.getSecurityZone returns securityZone;
      entry
    }

    val keystoreId = new java.lang.Long(82734)
    val keystore = mock[SsgKeyFinder]

    val keyAlias = "interceptor_test_key_alias"
    val keyEntry = makeEntry(keystoreId, keyAlias)
    val keyMetadata = makeKeyMetadata(keystoreId, keyAlias, new SecurityZone())

    val user = mock[User]
    user.getName returns keyAlias

    val ssgKeyStoreManager = mock[SsgKeyStoreManager]
    val rbacServices = mock[RbacServices]

    val interceptor = new PrivateKeyRbacInterceptor
    interceptor.setUser(user)
    interceptor.rbacServices = rbacServices
    interceptor.ssgKeyStoreManager = ssgKeyStoreManager

    /** configure the finder mocks to do a successful lookup of our mock keystore and key entry */
    def keyLookupWillSucceed = {
      ssgKeyStoreManager.findByPrimaryKey(new lang.Long(keystoreId)) returns keystore
      keystore.getCertificateChain(keyAlias) returns keyEntry
    }

    /** configure rbacServices mock to return true when queried about the specified permission on the specified entity */
    def grant(op: OperationType, entity: Entity) = rbacServices.isPermittedForEntity(user, entity, op, null) returns true

    /** configure rbacServies mock to return true when queried about the specified permission for all non-mocked SsgKeyEntry (mocked ones won't match the haveClass matcher) **/
    def grantAllNonMockedKeyEntries(op: OperationType) = rbacServices.isPermittedForEntity(beEqualTo(user), haveClass[SsgKeyEntry], beEqualTo(op), org.mockito.Matchers.eq(null)) returns true

    /** configure rbacServices mock to return true when queried about the specified permission on all entities of the specified type */
    def grantAll(op: OperationType, entityType: EntityType) = rbacServices.isPermittedForAnyEntityOfType(user, op, entityType) returns true
  }
}
