package com.l7tech.server.policy.assertion.xmlsec

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.mock.Mockito
import scala.collection.JavaConversions._
import com.l7tech.gateway.common.audit.AssertionMessages._
import com.l7tech.policy.assertion.AssertionStatus._
import org.specs2.specification.Scope
import com.l7tech.message.{Message}
import com.l7tech.server.message.PolicyEnforcementContextFactory._
import com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion
import com.l7tech.server.ApplicationContexts._
import com.l7tech.server.identity.cert.TrustedCertCache
import com.l7tech.gateway.common.audit.{TestAudit}
import com.l7tech.security.cert.TrustedCert
import com.l7tech.common.TestDocuments._
import java.security.cert.X509Certificate
import com.l7tech.objectmodel.FindException

/**
 * Unit test for the look up trusted certificate assertion
 */
class ServerLookupTrustedCertificateAssertionTest extends SpecificationWithJUnit with Mockito {

  "ServerLookupTrustedCertificateAssertion" should {

    "fail and audit if certificate not found" in new DefaultScope {
      sass.checkRequest(pec) must be equalTo FALSIFIED

      audit.isAuditPresent(CERT_LOOKUP_NAME) must beTrue
      audit.isAuditPresent(CERT_LOOKUP_NOTFOUND) must beTrue
    }

    "succeed with an audit and set a single valued variable when certificate found" in new DefaultScope {
      cache.findByName("alice") returns Seq(aliceTrustedCert)

      sass.checkRequest(pec) must be equalTo NONE

      audit.isAuditPresent(CERT_LOOKUP_NAME) must beTrue

      pec.getVariable("certificates") must be equalTo aliceTrustedCert.getCertificate
    }

    "succeed with an audit and set a multivalued variable when multiple certificates found" in new DefaultScope {
      ass.setAllowMultipleCertificates(true)
      cache.findByName("alice") returns Seq(aliceTrustedCert,aliceTrustedCert)

      sass.checkRequest(pec) must be equalTo NONE

      audit.isAuditPresent(CERT_LOOKUP_NAME) must beTrue

      pec.getVariable("certificates") must be equalTo Array(aliceTrustedCert.getCertificate,aliceTrustedCert.getCertificate)
    }

    "fail with an audit if multiple certificates found and not permitted" in new DefaultScope {
      cache.findByName("alice") returns Seq(aliceTrustedCert,aliceTrustedCert)

      sass.checkRequest(pec) must be equalTo FALSIFIED

      audit.isAuditPresent(CERT_LOOKUP_NAME) must beTrue
    }

    "fail with an audit on FindException" in new DefaultScope {
      cache.findByName("alice") throws new FindException

      sass.checkRequest(pec) must be equalTo FAILED

      audit.isAuditPresent(CERT_LOOKUP_NAME) must beTrue
      audit.isAuditPresent(CERT_LOOKUP_ERROR) must beTrue
    }
  }

  trait DefaultScope extends Scope {
    val ass = new LookupTrustedCertificateAssertion()
    ass.setTrustedCertificateName("alice")
    ass.setAllowMultipleCertificates(false)
    val sass = new ServerLookupTrustedCertificateAssertion(ass)

    val audit = new TestAudit()
    val auditFactory = audit.factory()
    val cache = mock[TrustedCertCache]
    val aliceTrustedCert = newTrustedCert( getWssInteropAliceCert );

    inject(sass, Map(
      "auditFactory" -> auditFactory,
      "trustedCertCache" -> cache
    ))

    val request = new Message()
    val response = new Message()
    val pec = createPolicyEnforcementContext(request, response)

    def newTrustedCert ( certificate : X509Certificate ) = {
      val trustedCert = new TrustedCert()
      trustedCert.setCertificate( certificate )
      trustedCert
    }
  }
}