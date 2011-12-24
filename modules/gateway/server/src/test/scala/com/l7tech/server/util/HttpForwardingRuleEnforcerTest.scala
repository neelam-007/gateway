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
    "forward all application headers when forward all enabled" in {
      val httpRequestParams = new GenericHttpRequestParams
      val httpKnob = mockHttpKnob( Seq(
        ("X-Header-1", Array("Value")),
        ("X-Header-2", Array("Value1", "Value2")) ))

      handleRequestHeaders( null, message(httpKnob), httpRequestParams, context, "layer7", forwardAllRules, audit, vars, varNames)
      val headers : Iterable[HttpHeader] = httpRequestParams.getExtraHeaders

      headers must contain( header( "X-Header-1", "Value" ) )
      headers must contain( header( "X-Header-2", "Value1" ) )
      headers must contain( header( "X-Header-2", "Value2" ) )
    }

    "not forward non application headers when forward all enabled" in {
      val httpRequestParams = new GenericHttpRequestParams
      val httpKnob = mockHttpKnob( Seq(
        ("keep-alive", Array("close")),
        ("connection", Array("close")),
        ("server", Array("Apache")),
        ("content-type", Array("text/plain")),
        ("date", Array("1 Jan, 2000")),
        ("content-length", Array("666")),
        ("transfer-encoding", Array("chunked")) ))

      handleRequestHeaders( null, message(httpKnob), httpRequestParams, context, "layer7", forwardAllRules, audit, vars, varNames)
      val headers : Iterable[HttpHeader] = httpRequestParams.getExtraHeaders

      headers must beEmpty
    }

    "prefer explicitly added headers to request HTTP knob headers when forwarding all" in {
      val outboundHeaders = new HttpOutboundRequestFacet
      val httpRequestParams = new GenericHttpRequestParams
      outboundHeaders.setHeader( "X-Header-1", "Preferred Value" )
      val httpKnob = mockHttpKnob( Seq( ("X-Header-1", Array("Value")) ))

      handleRequestHeaders( outboundHeaders, message(httpKnob), httpRequestParams, context, "layer7", forwardAllRules, audit, vars, varNames)
      val headers : Iterable[HttpHeader] = httpRequestParams.getExtraHeaders

      headers must contain( header( "X-Header-1", "Preferred Value" ) )
      headers must not contain( header( "X-Header-1", "Value" ) )
    }

    "fail on invalid request HTTP knob header name when forwarding all" in {
      val httpRequestParams = new GenericHttpRequestParams
      val httpKnob = mockHttpKnob( Seq( ("", Array("Value")) ) )

      handleRequestHeaders( null, message(httpKnob), httpRequestParams, context, "layer7", forwardAllRules, audit, vars, varNames) must throwA[IOException]
    }

    "forward all source headers when there's no request HTTP knob" in {
      val outboundHeaders = new HttpOutboundRequestFacet
      val httpRequestParams = new GenericHttpRequestParams
      outboundHeaders.setHeader( "X-Header-1", "Value" )

      handleRequestHeaders( outboundHeaders, message(), httpRequestParams, context, "layer7", forwardAllRules, audit, vars, varNames)
      val headers : Iterable[HttpHeader] = httpRequestParams.getExtraHeaders

      headers must contain( header( "X-Header-1", "Value" ) )
    }

    "forward the SOAPAction from the message if there's no SOAPAction header" in {
      val outboundHeaders = new HttpOutboundRequestFacet
      val httpRequestParams = new GenericHttpRequestParams
      val emailKnob = mock[EmailKnob]
      emailKnob.getSoapAction returns "http://example.com/action"
      val requestMessage = message()
      requestMessage.initialize( parse(<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body/></soap:Envelope>.toString()) )
      requestMessage.attachEmailKnob(emailKnob)

      handleRequestHeaders( outboundHeaders, requestMessage, httpRequestParams, context, "layer7", forwardAllRules, audit, vars, varNames)
      val headers : Iterable[HttpHeader] = httpRequestParams.getExtraHeaders

      headers must contain( header( "SOAPAction", "http://example.com/action" ) )
    }

    "not forward an authorization header when credentials are configured" in {
      val httpRequestParams = new GenericHttpRequestParams
      httpRequestParams.setPasswordAuthentication(  new PasswordAuthentication("user", "password".toCharArray) )
      val httpKnob = mockHttpKnob( Seq( ("Authorization", Array("FakeAuth")) ))

      handleRequestHeaders( null, message(httpKnob), httpRequestParams, context, "layer7", forwardAllRules, audit, vars, varNames)
      val headers : Iterable[HttpHeader] = httpRequestParams.getExtraHeaders

      headers must not contain( header( "Authorization", "FakeAuth" ) )
    }

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

    "forward all response headers when forward all enabled" in {
      val inboundResponse = mockHttpInboundResponseKnob( Array( header("X-Header-1", "Value1"), header("Connection", "close") ) )
      val responseKnob = mock[HttpResponseKnob]
      handleResponseHeaders( inboundResponse, responseKnob, audit, forwardAllRules, false, context, emptyHttpRequestParams, vars, varNames );
      there was one(responseKnob).addHeader("X-Header-1", "Value1")
      there was no(responseKnob).addHeader("Connection", "close")
    }

    "forward all response headers (including special) when forward all enabled" in {
      val inboundResponse = mockHttpInboundResponseKnob( Array( header("X-Header-1", "Value1"), header("Connection", "close") ) )
      val responseKnob = mock[HttpResponseKnob]
      handleResponseHeaders( inboundResponse, responseKnob, audit, forwardAllRules, true, context, emptyHttpRequestParams, vars, varNames );
      there was one(responseKnob).addHeader("X-Header-1", "Value1")
      there was one(responseKnob).addHeader("Connection", "close")
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
