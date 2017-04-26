package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.VariableUseSupport;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This assertion will query the portal database via the supplied jdbc and retrieve the increment.  The output of this
 * assertion will be the json payload with the increment.
 *
 * @author wlui
 */
public class GetIncrementAssertion extends BaseIncrementAssertion {
  public GetIncrementAssertion() {
    super("portal.sync.increment", "Portal Get Incremental Update", "Portal Get Incremental Update Json message");
  }
}
