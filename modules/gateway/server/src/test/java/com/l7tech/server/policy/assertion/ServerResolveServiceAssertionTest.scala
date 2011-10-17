package com.l7tech.server.policy.assertion

import scala.collection.JavaConversions
import org.specs2.mutable._
import org.specs2.mock._
import com.l7tech.policy.assertion.ResolveServiceAssertion
import com.l7tech.gateway.common.service.PublishedService
import com.l7tech.policy.assertion.AssertionStatus._
import com.l7tech.server.message.PolicyEnforcementContextFactory
import com.l7tech.server.service.ServiceCache
import java.util.ArrayList
import com.l7tech.server.service.resolution.ServiceResolutionException
import com.l7tech.message.{HasServiceOidImpl, HasServiceOid, Message}
import java.util.logging.Logger
import org.specs2.specification.Scope
import com.l7tech.gateway.common.audit.{Audit, AuditFactory}
import com.l7tech.gateway.common.audit.AssertionMessages._
import org.mockito.Matchers

/**
 * Unit test for ServerResolveServiceAssertion.
 */
class ServerResolveServiceAssertionTest extends SpecificationWithJUnit with Mockito {
  "ServerResolveServiceAssertion" should {

    "fail if resolved service already present in context" in new DefaultScope {
      pec.setService(service)

      sass.checkRequest(pec) must be equalTo SERVER_ERROR

      there was no(sass.serviceCache).resolve(anyString, anyString, anyString)
      there was one(audit).logAndAudit(RESOLVE_SERVICE_ALREADY_RESOLVED)
    }

    "fail if HasServiceOid knob already present on default request" in new DefaultScope {
      pec.getRequest.attachKnob(classOf[HasServiceOid], new HasServiceOidImpl(123))

      sass.checkRequest(pec) must be equalTo SERVER_ERROR

      there was no(sass.serviceCache).resolve(anyString, anyString, anyString)
      there was one(audit).logAndAudit(RESOLVE_SERVICE_ALREADY_HARDWIRED)
    }

    "succeed if matching service is present in service cache" in new DefaultScope {
      sass.serviceCache.resolve("/foo", null, null) returns serviceList(service)

      sass.checkRequest(pec) must be equalTo NONE

      there was one(sass.serviceCache).resolve(anyString, anyString, anyString)
      val hasServiceOid = pec.getRequest.getKnob(classOf[HasServiceOid])
      hasServiceOid must not be null
      hasServiceOid.getServiceOid must be equalTo service.getOid
      there was one(audit).logAndAudit(Matchers.eq(RESOLVE_SERVICE_SUCCEEDED), any)
    }

    "fail if no maching service in service cache" in new DefaultScope {
      sass.serviceCache.resolve("/foo", null, null) returns serviceList()

      sass.checkRequest(pec) must be equalTo FAILED

      there was one(sass.serviceCache).resolve(anyString, anyString, anyString)
      pec.getRequest.getKnob(classOf[HasServiceOid]) must be equalTo null
      there was one(audit).logAndAudit(RESOLVE_SERVICE_NOT_FOUND)
    }

    "fail if more than one matching service in service cache" in new DefaultScope {
      sass.serviceCache.resolve("/foo", null, null) returns serviceList(newPublishedService, newPublishedService)

      sass.checkRequest(pec) must be equalTo FAILED

      there was one(sass.serviceCache).resolve(anyString, anyString, anyString)
      pec.getRequest.getKnob(classOf[HasServiceOid]) must be equalTo null
      there was one(audit).logAndAudit(RESOLVE_SERVICE_FOUND_MULTI)
    }

    "fail if service cache throws ServiceResolutionException" in new DefaultScope {
      sass.serviceCache.resolve("/foo", null, null) throws new ServiceResolutionException("HORRIBLE DISASTROUS EXCEPTION OH MY")

      sass.checkRequest(pec) must be equalTo FAILED

      there was one(sass.serviceCache).resolve(anyString, anyString, anyString)
      pec.getRequest.getKnob(classOf[HasServiceOid]) must be equalTo null
      there was one(audit).logAndAudit(Matchers.eq(RESOLVE_SERVICE_FAILED), Matchers.any(classOf[Array[String]]), Matchers.any(classOf[Throwable]))
      there was no(audit).logAndAudit(any, Matchers.anyVararg[String]())
    }
  }

  trait DefaultScope extends Scope {
    val ass = new ResolveServiceAssertion()
    ass.setUri("/foo")

    val audit = mock[Audit]
    val auditFactory = new AuditFactory() { def newInstance(source: Any, logger: Logger): Audit = audit }
    val sass = new ServerResolveServiceAssertion(ass, auditFactory)
    sass.serviceCache = mock[ServiceCache]

    val request: Message = new Message()
    val response: Message = new Message()
    val pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response)

    val service = newPublishedService
    service.setOid(2424)

    def newPublishedService = new PublishedService()

    def serviceList(services: PublishedService*): ArrayList[PublishedService] = new ArrayList(JavaConversions.asJavaCollection(services))
  }
}