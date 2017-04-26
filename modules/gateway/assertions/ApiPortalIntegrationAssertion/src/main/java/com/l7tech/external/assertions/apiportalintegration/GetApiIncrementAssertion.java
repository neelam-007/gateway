package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.policy.assertion.*;
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
public class GetApiIncrementAssertion extends BaseIncrementAssertion {

    public GetApiIncrementAssertion() {
      super("portal.sync.api.increment", "Portal Get Api Incremental Update", "Portal Get Api Incremental Update Json message");
    }

}
