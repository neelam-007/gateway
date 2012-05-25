package com.l7tech.server.policy.assertion

import scala.collection.JavaConversions._
import com.l7tech.gateway.common.audit.AssertionMessages._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import com.l7tech.message.Message
import com.l7tech.server.message.PolicyEnforcementContextFactory
import com.l7tech.util.NameValuePair
import com.l7tech.policy.assertion.AssertionStatus._
import com.l7tech.policy.assertion.MapValueAssertion
import com.l7tech.policy.variable.NoSuchVariableException
import com.l7tech.server.ApplicationContexts._
import com.l7tech.gateway.common.audit.TestAudit

/**
 * Unit test for ServerMapValueAssertion.
 */
class ServerMapValueAssertionTest extends SpecificationWithJUnit with Mockito {

  "ServerMapValueAssertion" should {

    "succeed if input value matched by a mapping expression" in new DefaultScope {
      sass.checkRequest(pec) must be equalTo NONE
      pec.getVariable("out") must be equalTo "/catthing"

      // PATTERN_NOT_MATCHED won't be audited because the very first pattern in the list gets matched
      audit.isAuditPresent(MAP_VALUE_PATTERN_MATCHED) must beTrue
      audit.isNoneOfAuditsPresent(List(MAP_VALUE_PATTERN_NOT_MATCHED, MAP_VALUE_NO_PATTERNS_MATCHED)) must beTrue
    }

    "fail if input value not matched by a mapping expression" in new DefaultScope {
      pec.setVariable("in", "nothingWillMatchThis")
      sass.checkRequest(pec) must be equalTo FAILED
      pec.getVariable("out") must throwA[NoSuchVariableException]
      audit.isAuditPresent(MAP_VALUE_PATTERN_MATCHED) must beFalse
      audit.isAllOfAuditsPresent(List(MAP_VALUE_PATTERN_NOT_MATCHED, MAP_VALUE_NO_PATTERNS_MATCHED)) must beTrue
    }
    
    "always fail cleanly if mappings is null" in new DefaultScope {
      ass.setMappings(null)
      sass = new ServerMapValueAssertion(ass, auditFactory)
      sass.checkRequest(pec) must be equalTo FAILED
      pec.getVariable("out") must throwA[NoSuchVariableException]
      audit.isAuditPresent(MAP_VALUE_PATTERN_MATCHED) must beFalse
      audit.isAuditPresent(MAP_VALUE_PATTERN_NOT_MATCHED) must beFalse
      audit.isAuditPresent(MAP_VALUE_NO_PATTERNS_MATCHED) must beTrue
    }
   
    "support context variable in the mapping output value" in new DefaultScope {
      pec.setVariable("in", "dog")
      sass.checkRequest(pec) must be equalTo NONE
      pec.getVariable("out") must be equalTo "/dawgthing"
      audit.isAllOfAuditsPresent(List(MAP_VALUE_PATTERN_MATCHED, MAP_VALUE_PATTERN_NOT_MATCHED)) must beTrue
      audit.isNoneOfAuditsPresent(List(MAP_VALUE_NO_PATTERNS_MATCHED)) must beTrue
    }

    "support regex capture group pseudo-variables in the mapping output value" in new DefaultScope {
      pec.setVariable("in", "fancie")
      sass.checkRequest(pec) must be equalTo NONE
      pec.getVariable("out") must be equalTo "/fanciething/ie"
      audit.isAllOfAuditsPresent(List(MAP_VALUE_PATTERN_MATCHED, MAP_VALUE_PATTERN_NOT_MATCHED)) must beTrue
      audit.isNoneOfAuditsPresent(List(MAP_VALUE_NO_PATTERNS_MATCHED)) must beTrue
    }

    "support context variable in the mapping pattern" in new DefaultScope() {
      pec.setVariable("in", "patzarfwithvar")
      pec.setVariable("patvar", "zarf");
      sass.checkRequest(pec) must be equalTo NONE
      pec.getVariable("out") must be equalTo "/patvar"
      audit.isAllOfAuditsPresent(List(MAP_VALUE_PATTERN_MATCHED, MAP_VALUE_PATTERN_NOT_MATCHED)) must beTrue
      audit.isNoneOfAuditsPresent(List(MAP_VALUE_NO_PATTERNS_MATCHED)) must beTrue
    }

    "support context variable in the mapping pattern (negative test)" in new DefaultScope() {
      pec.setVariable("in", "patzarffwithvar")
      pec.setVariable("patvar", "zarf");
      sass.checkRequest(pec) must be equalTo FAILED
      pec.getVariable("out") must throwA[NoSuchVariableException]
      audit.isAuditPresent(MAP_VALUE_PATTERN_MATCHED) must beFalse
      audit.isAllOfAuditsPresent(List(MAP_VALUE_PATTERN_NOT_MATCHED, MAP_VALUE_NO_PATTERNS_MATCHED)) must beTrue
    }

    "regex metacharacters in mapping pattern context variable must be treated as literals" in new DefaultScope() {
      pec.setVariable("in", "pata|bwithvar")
      pec.setVariable("patvar", "a|b");
      sass.checkRequest(pec) must be equalTo NONE
      pec.getVariable("out") must be equalTo "/patvar"
      audit.isAllOfAuditsPresent(List(MAP_VALUE_PATTERN_MATCHED, MAP_VALUE_PATTERN_NOT_MATCHED)) must beTrue
      audit.isNoneOfAuditsPresent(List(MAP_VALUE_NO_PATTERNS_MATCHED)) must beTrue
    }

    "regex metacharacters in mapping pattern context variable must be treated as literals (negative test)" in new DefaultScope() {
      pec.setVariable("in", "patawithvar")
      pec.setVariable("patvar", "a|b");
      sass.checkRequest(pec) must be equalTo FAILED
      pec.getVariable("out") must throwA[NoSuchVariableException]
      audit.isAuditPresent(MAP_VALUE_PATTERN_MATCHED) must beFalse
      audit.isAllOfAuditsPresent(List(MAP_VALUE_PATTERN_NOT_MATCHED, MAP_VALUE_NO_PATTERNS_MATCHED)) must beTrue
    }

    // @BugNumber(12178)
    "fail cleanly if the input expression is null" in new DefaultScope() {
      ass.setInputExpr(null);
      sass.checkRequest(pec) must be equalTo SERVER_ERROR
      audit.isAuditPresent(ASSERTION_MISCONFIGURED) must beTrue
      audit.isNoneOfAuditsPresent(List(MAP_VALUE_PATTERN_NOT_MATCHED, MAP_VALUE_NO_PATTERNS_MATCHED, MAP_VALUE_PATTERN_NOT_MATCHED)) must beTrue
    }
  }


  trait DefaultScope extends Scope {
    val ass = new MapValueAssertion()
    ass.setInputExpr("${in}")
    ass.setOutputVar("out")
    ass.setMappings(Array(
      new NameValuePair("cat", "/catthing"),
      new NameValuePair("dog", "/${dog}thing"),
      new NameValuePair("fanc(y|ie)", "/${0}thing/${1}"),
      new NameValuePair("pat${patvar}withvar", "/patvar")
    ))

    val audit = new TestAudit()
    val auditFactory = audit.factory()

    var sass = new ServerMapValueAssertion(ass)
    inject(sass, Map(
      "auditFactory" -> auditFactory
    ))

    val request: Message = new Message()
    val response: Message = new Message()
    val pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response)
    
    pec.setVariable("in", "cat")
    pec.setVariable("dog", "dawg")
  }
}
