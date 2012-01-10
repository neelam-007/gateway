package com.l7tech.server.policy.assertion.credential.wss

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import java.util.logging.Logger
import com.l7tech.message.Message
import com.l7tech.server.message.PolicyEnforcementContextFactory
import com.l7tech.policy.assertion.credential.wss.WssDigest
import com.l7tech.common.io.XmlUtil
import com.l7tech.policy.assertion.AssertionStatus
import com.l7tech.xml.soap.{SoapVersion, SoapUtil}
import com.l7tech.util.ConfigFactory
import com.l7tech.security.xml.SimpleSecurityTokenResolver
import org.mockito.Matchers
import com.l7tech.gateway.common.audit.{Messages, AssertionMessages, AuditFactory, Audit}

/**
 * Unit test for ServerWssDigest.
 */
class ServerWssDigestTest extends SpecificationWithJUnit with Mockito {
  "ServerWssDigest" should {

    "fail if request is not SOAP" in new DefaultScope {
      request.initialize(XmlUtil.createEmptyDocument("blah", "ns", "urn:blah"))

      sass.checkRequest(pec) must be equalTo AssertionStatus.NOT_APPLICABLE

      there was one(audit).logAndAudit(AssertionMessages.REQUEST_NOT_SOAP)
    }

    "fail if request does not contain a security header" in new DefaultScope {
      request.initialize(SoapUtil.createSoapEnvelopeAndGetBody(SoapVersion.SOAP_1_1).getOwnerDocument)

      sass.checkRequest(pec) must be equalTo AssertionStatus.AUTH_REQUIRED

      there was one(audit).logAndAudit(AssertionMessages.REQUESTWSS_NO_SECURITY)
    }

    "succeed if valid username and password are present" in new DefaultScope {
      ass.setRequiredPassword("ecilA")
      ass.setRequiredUsername("Alice")

      sass.checkRequest(pec) must be equalTo AssertionStatus.NONE

      there was one(audit).logAndAudit(Matchers.eq(AssertionMessages.USERDETAIL_INFO), anyString)
    }

    "fail if request username does not match" in new DefaultScope {
      ass.setRequiredPassword("ecilA")
      ass.setRequiredUsername("qwerasdf")

      sass.checkRequest(pec) must be equalTo AssertionStatus.FALSIFIED

      there was one(audit).logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, "Ignoring UsernameToken that does not contain a matching username")
    }

    "fail if request password does not match" in new DefaultScope {
      ass.setRequiredUsername("Alice")
      ass.setRequiredPassword("hsadkjhf")

      sass.checkRequest(pec) must be equalTo AssertionStatus.FALSIFIED

      there was one(audit).logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, "UsernameToken digest value does not match the expected value")
    }

    "succeed if valid username and password are present using context variables" in new DefaultScope {
      ass.setRequiredUsername("${username1}")
      ass.setRequiredPassword("${password1}")

      sass.checkRequest(pec) must be equalTo AssertionStatus.NONE

      there was one(audit).logAndAudit(Matchers.eq(AssertionMessages.USERDETAIL_INFO), anyString)
    }

    "fail if request username does not match using context variables" in new DefaultScope {
      ass.setRequiredUsername("${username2}")
      ass.setRequiredPassword("${password1}")

      sass.checkRequest(pec) must be equalTo AssertionStatus.FALSIFIED

      there was one(audit).logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, "Ignoring UsernameToken that does not contain a matching username")
    }

    "fail if request password does not match using context variables" in new DefaultScope {
      ass.setRequiredUsername("${username1}")
      ass.setRequiredPassword("${password2}")

      sass.checkRequest(pec) must be equalTo AssertionStatus.FALSIFIED

      there was one(audit).logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, "UsernameToken digest value does not match the expected value")
    }
  }

  trait DefaultScope extends Scope {
    val interopToken =
      "<wsse:UsernameToken xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"unt_jH5NZ9r24lIs1049\"><wsse:Username>Alice</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest\">jlRIlWrSTh0O8I7AgmGX35vcr6Q=</wsse:Password><wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">NLu+blE91TuBXeCRdQts4X5vBGpP/XNq+1PcZSjcQSA=</wsse:Nonce><wsu:Created>2008-11-11T01:52:36Z</wsu:Created></wsse:UsernameToken>";

    val requestXml =
      "<?xml version='1.0' encoding='UTF-8'?><S:Envelope xmlns:S=\"http://www.w3.org/2003/05/soap-envelope\"><S:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" S:mustUnderstand=\"true\"><wsse:UsernameToken xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"unt_jH5NZ9r24lIs1049\"><wsse:Username>Alice</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest\">jlRIlWrSTh0O8I7AgmGX35vcr6Q=</wsse:Password><wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">NLu+blE91TuBXeCRdQts4X5vBGpP/XNq+1PcZSjcQSA=</wsse:Nonce><wsu:Created>2008-11-11T01:52:36Z</wsu:Created></wsse:UsernameToken></wsse:Security></S:Header><S:Body><EchoRequest xmlns=\"http://example.com/ws/2008/09/securitypolicy\">Test A2113 From Oracle Weblogic Server</EchoRequest></S:Body></S:Envelope>";

    val ass = new WssDigest()
    ass.setRequiredUsername("${username1}${username2}")  // Ensure all test variables get marked as used.  Actual test will override the bean requirements later.
    ass.setRequiredPassword("${password1}${password2}")
    ass.setRequireNonce(true)
    ass.setRequireTimestamp(true)

    val audit = mock[Audit]
    val auditFactory = new AuditFactory() { def newInstance(source: Object, logger: Logger): Audit = audit }
    val sass = new ServerWssDigest(ass, auditFactory)
    sass.config = ConfigFactory.getCachedConfig
    sass.securityTokenResolver = new SimpleSecurityTokenResolver

    val request: Message = new Message(XmlUtil.stringAsDocument(requestXml));
    val response: Message = new Message()
    val pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response)
    pec.setVariable("username1", "Alice")
    pec.setVariable("username2", "blahblah")
    pec.setVariable("password1", "ecilA")
    pec.setVariable("password2", "wkljhdasdds")
  }
}