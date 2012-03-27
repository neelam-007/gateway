package com.l7tech.server.policy.assertion

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import com.l7tech.message.Message
import com.l7tech.server.message.PolicyEnforcementContextFactory
import com.l7tech.util.NameValuePair
import com.l7tech.policy.assertion.AssertionStatus._
import com.l7tech.policy.assertion.MapValueAssertion
import com.l7tech.policy.variable.NoSuchVariableException

/**
 * Unit test for ServerMapValueAssertion.
 */
class ServerMapValueAssertionTest extends SpecificationWithJUnit with Mockito {

  "ServerMapValueAssertion" should {

    "succeed if input value matched by a mapping expression" in new DefaultScope {
      sass.checkRequest(pec) must be equalTo NONE
      pec.getVariable("out") must be equalTo "/catthing"
    }

    "fail if input value not matched by a mapping expression" in new DefaultScope {
      pec.setVariable("in", "nothingWillMatchThis")
      sass.checkRequest(pec) must be equalTo FAILED
      pec.getVariable("out") must throwA[NoSuchVariableException]
    }
    
    "always fail cleanly if mappings is null" in new DefaultScope {
      ass.setMappings(null)
      sass = new ServerMapValueAssertion(ass)
      sass.checkRequest(pec) must be equalTo FAILED
      pec.getVariable("out") must throwA[NoSuchVariableException]
    }
   
    "support context variable in the mapping output value" in new DefaultScope {
      pec.setVariable("in", "dog")
      sass.checkRequest(pec) must be equalTo NONE
      pec.getVariable("out") must be equalTo "/dawgthing"
    }

    "support regex capture group pseudo-variables in the mapping output value" in new DefaultScope {
      pec.setVariable("in", "fancie")
      sass.checkRequest(pec) must be equalTo NONE
      pec.getVariable("out") must be equalTo "/fanciething/ie"
    }
  }


  trait DefaultScope extends Scope {
    val ass = new MapValueAssertion()
    ass.setInputExpr("${in}")
    ass.setOutputVar("out")
    ass.setMappings(Array(
      new NameValuePair("cat", "/catthing"),
      new NameValuePair("dog", "/${dog}thing"),
      new NameValuePair("fanc(y|ie)", "/${0}thing/${1}")
    ))

    var sass = new ServerMapValueAssertion(ass)

    val request: Message = new Message()
    val response: Message = new Message()
    val pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response)
    
    pec.setVariable("in", "cat")
    pec.setVariable("dog", "dawg")
  }
}
