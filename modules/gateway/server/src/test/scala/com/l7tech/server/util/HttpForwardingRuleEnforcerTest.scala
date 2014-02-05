package com.l7tech.server.util

import scala.collection.JavaConversions._
import org.specs2.mutable._
import org.specs2.mock._
import com.l7tech.server.message.{PolicyEnforcementContext}
import com.l7tech.server.util.HttpForwardingRuleEnforcer._
import com.l7tech.gateway.common.audit.Audit
import com.l7tech.policy.assertion.HttpPassthroughRuleSet
import com.l7tech.message._
import java.io.IOException
import java.net.PasswordAuthentication
import com.l7tech.common.http.{HttpConstants, HttpHeader, GenericHttpHeader, GenericHttpRequestParams}
import com.l7tech.common.io.XmlUtil.parse

/**
 * Unit tests for header forwarding.
 *
 * Currently lacking coverage for:
 *
 * - Forwarding of specified headers
 * - XVC header forwarding
 */
class HttpForwardingRuleEnforcerTest extends SpecificationWithJUnit with Mockito {
  "HttpForwardingRuleEnforcer" should {

    "not forward request parameters if the source message is not via HTTP" in {
      val params = handleRequestParameters( context, message(), forwardAllRules, audit, vars, varNames )
      params must beNull
    }

    "not forward request parameters if the source message is not a form" in {
      val httpKnob = mock[HttpServletRequestKnob]
      httpKnob.getHeaderValues("content-type") returns Array("text/plain")
      httpKnob.getRequestBodyParameterMap returns Map( ( "param1", Array("value1") ) )
      val params = handleRequestParameters( context, message(httpKnob), forwardAllRules, audit, vars, varNames )
      params must beNull
    }

    "forward all request parameters when forward all enabled" in {
      val httpKnob = mock[HttpServletRequestKnob]
      httpKnob.getHeaderValues("content-type") returns Array("application/x-www-form-urlencoded")
      httpKnob.getRequestBodyParameterMap returns Map( ( "param1", Array("value1") ) )
      val params = handleRequestParameters( context, message(httpKnob), forwardAllRules, audit, vars, varNames )
      params.size() mustEqual 1
      params(0).name mustEqual "param1"
      params(0).value mustEqual "value1"
    }
  }

  val audit = mock[Audit]
  val context = mock[PolicyEnforcementContext]
  val vars : Map[String,_] = Map()
  val varNames : Array[String] = Array()
  val forwardAllRules = mock[HttpPassthroughRuleSet]; forwardAllRules.isForwardAll returns true
  val emptyHttpRequestParams = new GenericHttpRequestParams
  def header ( name : String, value : String ) : HttpHeader = new GenericHttpHeader( name, value )
  def message ( request : HttpRequestKnob = null, response : HttpResponseKnob = null ) : Message = {
    val message = new Message
    Option(request).foreach( message.attachHttpRequestKnob(_) )
    Option(response).foreach( message.attachHttpResponseKnob(_) )
    message
  }
  def mockHttpKnob( headers : Seq[Tuple2[String,Array[String]]] ) : HttpRequestKnob = {
    val httpKnob = mock[HttpRequestKnob]
    httpKnob.getHeaderNames returns (headers.map(_._1)).toArray
    headers.foreach( t => httpKnob.getHeaderValues( t._1 ) returns t._2 )
    httpKnob
  }
  def mockHttpInboundResponseKnob( headers : Array[HttpHeader] ) : HttpInboundResponseKnob = {
    val inboundResponse = mock[HttpInboundResponseKnob]
    inboundResponse.getHeadersArray returns headers
    inboundResponse.getHeaderValues(HttpConstants.HEADER_SET_COOKIE) returns Array()
    inboundResponse
  }
}
