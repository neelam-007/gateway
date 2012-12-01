package com.l7tech.server.policy.assertion

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import com.l7tech.policy.assertion.{TargetMessageType, AssertionStatus, MessageBufferingAssertion}
import com.l7tech.message.Message
import com.l7tech.server.message.PolicyEnforcementContextFactory
import com.l7tech.common.mime.{ContentTypeHeader, ByteArrayStashManager}
import com.l7tech.util.{IOUtils, Charsets}
import java.io.ByteArrayInputStream
import com.l7tech.common.io.NullOutputStream
import com.l7tech.gateway.common.audit.{AssertionMessages, AuditFactory, Audit}
import java.util.logging.Logger

/**
 * Unit test for [[com.l7tech.server.policy.assertion.ServerMessageBufferingAssertion]].
 */
class ServerMessageBufferingAssertionTest extends SpecificationWithJUnit with Mockito {

  "ServerMessageBufferingAssertion" should {
    "fail if message is not initialized " in new DefaultScope {
      ass.setTarget(TargetMessageType.RESPONSE)

      sass.checkRequest(context) must_== AssertionStatus.SERVER_ERROR

      there was one(audit).logAndAudit(beEqualTo(AssertionMessages.MESSAGE_NOT_INITIALIZED), any)
    }

    "buffer target message immediately when so configured" in new DefaultScope {
      alwaysBuffer(ass)
      reqMime.getFirstPart.isBodyStashed must beFalse

      sass.checkRequest(context) must_== AssertionStatus.NONE

      reqMime.isBufferingDisallowed must beFalse
      reqMime.getFirstPart.isBodyStashed must beTrue
      reqMime.getFirstPart.isBodyRead must beTrue
      reqMime.getFirstPart.isBodyAvailable must beTrue
    }

    "fail to buffer target message if it is already gone" in new DefaultScope {
      alwaysBuffer(ass)
      IOUtils.copyStream(reqMime.getEntireMessageBodyAsInputStream(true), new NullOutputStream)

      sass.checkRequest(context) must_== AssertionStatus.SERVER_ERROR

      reqMime.isBufferingDisallowed must beFalse
      reqMime.getFirstPart.isBodyRead must beTrue
      reqMime.getFirstPart.isBodyStashed must beFalse
      reqMime.getFirstPart.isBodyAvailable must beFalse
      there was one(audit).logAndAudit(beEqualTo(AssertionMessages.NO_SUCH_PART), anyString, anyString)
    }

    "disallow buffering when so configured" in new DefaultScope {
      neverBuffer(ass)

      sass.checkRequest(context) must_== AssertionStatus.NONE

      reqMime.isBufferingDisallowed must beTrue
      reqMime.getFirstPart.isBodyStashed must beFalse
      reqMime.getFirstPart.isBodyRead must beFalse
      reqMime.getFirstPart.isBodyAvailable must beTrue
    }

    "fail to disallow buffering if message already buffered" in new DefaultScope {
      neverBuffer(ass)
      reqMime.getContentLength

      sass.checkRequest(context) must_== AssertionStatus.FAILED

      reqMime.isBufferingDisallowed must beTrue
      reqMime.getFirstPart.isBodyStashed must beTrue
      reqMime.getFirstPart.isBodyRead must beTrue
      there was one(audit).logAndAudit(beEqualTo(AssertionMessages.MESSAGE_ALREADY_BUFFERED), any)
    }
  }

  trait DefaultScope extends Scope {
    def mess(body: String) = new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(body.getBytes(Charsets.UTF8)))
    def pec(req: Message, resp: Message) = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, resp)

    val audit = mock[Audit]
    val auditFactory = new AuditFactory() { def newInstance(source: Object, logger: Logger): Audit = audit }

    val ass = new MessageBufferingAssertion()
    val sass = new ServerMessageBufferingAssertion(ass, auditFactory)

    val req = mess("blah")
    val resp = new Message()
    val context = pec(req, resp)
    val reqMime = req.getMimeKnob

    def setBuffer(ass: MessageBufferingAssertion, buff: Boolean): MessageBufferingAssertion = {
      ass.setAlwaysBuffer(buff)
      ass.setNeverBuffer(!buff)
      ass
    }
    def alwaysBuffer(ass: MessageBufferingAssertion) = setBuffer(ass, buff = true)
    def neverBuffer(ass: MessageBufferingAssertion) = setBuffer(ass, buff = false)
  }
}
