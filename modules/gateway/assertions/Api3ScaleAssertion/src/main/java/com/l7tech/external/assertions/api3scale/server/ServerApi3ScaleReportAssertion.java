package com.l7tech.external.assertions.api3scale.server;

import com.l7tech.external.assertions.api3scale.Api3ScaleReportAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import net.threescale.api.ApiFactory;
import net.threescale.api.v2.Api2;
import net.threescale.api.v2.ApiException;
import net.threescale.api.v2.ApiTransaction;

import java.io.IOException;
import java.util.*;

/**
 * Server side implementation of the Api3ScaleAssertion.
 *
 * @see com.l7tech.external.assertions.api3scale.Api3ScaleReportAssertion
 */
public class ServerApi3ScaleReportAssertion extends AbstractServerAssertion<Api3ScaleReportAssertion> {

    public ServerApi3ScaleReportAssertion(Api3ScaleReportAssertion assertion) throws PolicyAssertionException {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        Map<String, Object> vars = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
        String  requestPrivateKey = ExpandVariables.process(assertion.getPrivateKey(), vars, getAudit(), true);
        String  requestAppId = ExpandVariables.process(assertion.getApplicationId(), vars, getAudit(), true);
        String  server = ExpandVariables.process(assertion.getServer(), vars, getAudit(), true);

        List<ApiTransaction> transactions = new ArrayList<ApiTransaction>();
        transactions.add(new ApiTransaction( requestAppId ,null ,assertion.getTransactionUsages()));

        try {
            // This call returns the users current usage, you decide whether to allow the transaction or not
            Api3ScaleHttpSender sender  = new Api3ScaleHttpSender();
            Api2 api2 = ApiFactory.createV2Api(server, requestAppId, requestPrivateKey,sender); // url, appid  privatekey, httpsender
            api2.report(transactions.toArray(new ApiTransaction[transactions.size()]));
        }
        catch(ApiException e){
            logAndAudit( AssertionMessages.API_REPORT_FAILED, new String[]{ e.getErrorMessage() }, ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }
}
