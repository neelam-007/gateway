package com.l7tech.policy.exporter

import scala.collection.JavaConversions._
import org.specs2.mutable._
import org.specs2.mock._
import com.l7tech.identity.ldap.LdapIdentity
import javax.naming.directory.Attributes
import com.l7tech.identity.IdentityProviderConfig

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 10/11/12
*/
class IdProviderReferenceTest extends SpecificationWithJUnit with Mockito{
  "IdProviderReference" should {
    "return match when NTLM properties disabled" in {
      val local = Map(("enabled", "true"))
      val imported = Map(("enabled","false"))

      fixture.verifyNtlmProperties(local, imported) must beTrue
    }
    "return match when NTLM properties are the same" in {
      val local = Map(("enabled", "true"), ("server.dns.name", "server"), ("service.account", "ntlmUser"), ("domain.netbios.name", "DOMAIN"), ("host.netbios.name", "host"))
      val imported = Map(("enabled", "true"), ("server.dns.name", "server"), ("service.account", "ntlmUser"), ("domain.netbios.name", "DOMAIN"), ("host.netbios.name", "HOST"))
      fixture.verifyNtlmProperties(local, imported) must beTrue
    }
    "return match when required NTLM properties are the same" in {
      val local = Map(("enabled", "true"), ("server.dns.name", "server"), ("service.account", "ntlmUser"), ("domain.dns.name","domain.com"),("domain.netbios.name", "DOMAIN"), ("host.netbios.name", "host"))
      val imported = Map(("enabled", "true"), ("server.dns.name", "server"), ("service.account", "ntlmUser"),("domain.dns.name","mydomain"), ("domain.netbios.name", "DOMAIN"), ("host.netbios.name", "HOST"))
      fixture.verifyNtlmProperties(local, imported) must beTrue
    }
    "return no match when local NTLM properties disabled and imported enabled" in {
      val local = Map(("enabled", "false"), ("server.dns.name", "server"), ("service.account", "ntlmUser"), ("domain.netbios.name", "DOMAIN"), ("host.netbios.name", "host"))
      val imported = Map(("enabled", "true"), ("server.dns.name", "server"), ("service.account", "ntlmUser"), ("domain.netbios.name", "DOMAIN"), ("host.netbios.name", "HOST"))
      fixture.verifyNtlmProperties(local, imported) must beFalse
    }
    "return no match when local NTLM properties are missing or null and imported enabled" in {
      val imported = Map(("enabled", "true"), ("server.dns.name", "server"), ("service.account", "ntlmUser"), ("domain.netbios.name", "DOMAIN"), ("host.netbios.name", "HOST"))
      val local =  mock[java.util.Map[String,String]]
      fixture.verifyNtlmProperties(local, imported) must beFalse
      fixture.verifyNtlmProperties(null, imported) must beFalse
    }
    "return match when imported NTLM properties are missing or null" in {
      val imported = mock[java.util.Map[String,String]]
      val local = mock[java.util.Map[String,String]]

      fixture.verifyNtlmProperties(local, imported) must beTrue
      fixture.verifyNtlmProperties(local,null) must beTrue
    }
  }

  val mockFinder = mock[ExternalReferenceFinder]
  val testOid = 0L
  mockFinder.findIdentityProviderConfigByID(testOid) returns mock[IdentityProviderConfig]
  val fixture = new IdProviderReference(mockFinder, testOid)
}
