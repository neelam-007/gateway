package com.l7tech.server.policy.assertion

import scala.collection.JavaConversions
import org.specs2.mutable._
import org.specs2.mock._
import com.l7tech.policy.assertion.ResolveServiceAssertion
import com.l7tech.gateway.common.service.PublishedService
import com.l7tech.policy.assertion.AssertionStatus._
import com.l7tech.server.message.{PolicyEnforcementContext, PolicyEnforcementContextFactory}
import com.l7tech.server.service.ServiceCache
import java.util.ArrayList
import com.l7tech.message.{HasServiceOid, Message}
import com.l7tech.server.service.resolution.ServiceResolutionException

/**
 * Unit test for ServerResolveServiceAssertion.
 */
class ServerResolveServiceAssertionTest extends SpecificationWithJUnit with Mockito {

  "Should fail if service already resolved" in {
    val sass = newServerAssertion
    val pec = newContext
    pec.setService(newPublishedService)

    sass.checkRequest(pec) must be equalTo SERVER_ERROR

    there was no(sass.serviceCache).resolve(anyString, anyString, anyString)
  }

  "Should succeed if matching service in service cache" in {
    val sass = newServerAssertion
    val pec = newContext
    val service = newPublishedService
    sass.serviceCache.resolve("/foo", null, null) returns serviceList(service)

    sass.checkRequest(pec) must be equalTo NONE

    there was one(sass.serviceCache).resolve(anyString, anyString, anyString)
    val hasServiceOid = pec.getRequest.getKnob(classOf[HasServiceOid])
    hasServiceOid must not be null
    hasServiceOid.getServiceOid must be equalTo service.getOid
  }

  "Should fail if no maching service in service cache" in {
    val sass = newServerAssertion
    val pec = newContext
    sass.serviceCache.resolve("/foo", null, null) returns serviceList()

    sass.checkRequest(pec) must be equalTo FAILED

    there was one(sass.serviceCache).resolve(anyString, anyString, anyString)
    pec.getRequest.getKnob(classOf[HasServiceOid]) must be equalTo null
  }

  "Should fail if more than one matching service in service cache" in {
    val sass = newServerAssertion
    val pec = newContext
    sass.serviceCache.resolve("/foo", null, null) returns serviceList(newPublishedService, newPublishedService)

    sass.checkRequest(pec) must be equalTo FAILED

    there was one(sass.serviceCache).resolve(anyString, anyString, anyString)
    pec.getRequest.getKnob(classOf[HasServiceOid]) must be equalTo null
  }

  "Should fail if service cache throws ServiceResolutionException" in {
    val sass = newServerAssertion
    val pec = newContext
    sass.serviceCache.resolve("/foo", null, null) throws new ServiceResolutionException("HORRIBLE DISASTROUS EXCEPTION OH MY")

    sass.checkRequest(pec) must be equalTo FAILED

    there was one(sass.serviceCache).resolve(anyString, anyString, anyString)
    pec.getRequest.getKnob(classOf[HasServiceOid]) must be equalTo null
  }

  //
  // Utility methods
  //

  def newAssertion: ResolveServiceAssertion = {
    val ass = new ResolveServiceAssertion()
    ass.setUri("/foo")
    ass
  }

  def newContext: PolicyEnforcementContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message())

  def newServerAssertion: ServerResolveServiceAssertion = {
    val sass = new ServerResolveServiceAssertion(newAssertion)
    sass.serviceCache = mock[ServiceCache]
    sass
  }

  def newPublishedService: PublishedService = new PublishedService()

  def serviceList(services: PublishedService*): ArrayList[PublishedService] = new ArrayList(JavaConversions.asJavaCollection(services))
}