package com.l7tech.server.identity.cert

import com.l7tech.common.TestDocuments
import com.l7tech.security.cert.TrustedCert
import com.l7tech.security.cert.TrustedCert.TrustedFor
import com.l7tech.common.io.CertUtils

import org.specs2.mutable.SpecificationWithJUnit
import scala.collection.JavaConversions._

import java.security.cert.X509Certificate
import com.l7tech.objectmodel.Goid

class TrustedCertCacheTest extends SpecificationWithJUnit {
  "Should have immutable results" in {
    val testCache = createTestCache
    Some(testCache.findByPrimaryKey(new Goid(0, 1))).foreach(_.setIssuerDn("CN=Test")) must throwA[IllegalStateException]
    testCache.findByName("Alice").foreach(_.setIssuerDn("CN=Test")) must throwA[IllegalStateException]
    testCache.findBySubjectDn("cn=alice,ou=oasis interop test cert,o=oasis").foreach(_.setIssuerDn("CN=Test")) must throwA[IllegalStateException]
    testCache.findByTrustFlag(TrustedFor.SIGNING_CLIENT_CERTS).foreach(_.setIssuerDn("CN=Test")) must throwA[IllegalStateException]
  }

  def createTestCache: TrustedCertCache = {
    new TrustedCertCacheImpl( new TestTrustedCertManager(
      trustedCert( new Goid(0, 1), TestDocuments.getWssInteropAliceCert ),
      trustedCert( new Goid(0, 2), TestDocuments.getWssInteropBobCert ),
      trustedCert( new Goid(0, 3), TestDocuments.getWssInteropIpCert, Seq(TrustedFor.SIGNING_CLIENT_CERTS) )
    ) )
  }

  def trustedCert( oid: Goid, certificate: X509Certificate, trustedFor: Seq[TrustedFor] = Nil ): TrustedCert = {
    val trustedCert = new TrustedCert()
    trustedCert.setGoid( oid )
    trustedCert.setName( CertUtils.getCn(certificate) )
    trustedCert.setCertificate( certificate )
    trustedFor.foreach(trustedCert.setTrustedFor(_, true))
    trustedCert
  }
}