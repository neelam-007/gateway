package com.l7tech.external.assertions.api3scale.server;

import com.l7tech.external.assertions.api3scale.Api3ScaleReportAssertion;
import com.l7tech.external.assertions.api3scale.Api3ScaleTransactions;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ISO8601Date;
import net.threescale.api.ApiFactory;
import net.threescale.api.v2.Api2;
import net.threescale.api.v2.ApiException;
import net.threescale.api.v2.ApiTransaction;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the Api3ScaleAssertion.
 *
 * @see com.l7tech.external.assertions.api3scale.Api3ScaleReportAssertion
 */
public class ServerApi3ScaleReportAssertion extends AbstractServerAssertion<Api3ScaleReportAssertion> {
    private static final Logger logger = Logger.getLogger(ServerApi3ScaleReportAssertion.class.getName());

    private final Api3ScaleReportAssertion assertion;
    private final Auditor auditor;

    public ServerApi3ScaleReportAssertion(Api3ScaleReportAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        Map<String, Object> vars = context.getVariableMap(assertion.getVariablesUsed(), auditor);
        String  requestPrivateKey = ExpandVariables.process(assertion.getPrivateKey(), vars, auditor, true);
        String  requestAppId = ExpandVariables.process(assertion.getApplicationId(), vars, auditor, true);

        List<ApiTransaction> transactions = new ArrayList<ApiTransaction>();
        transactions.add(new ApiTransaction( requestAppId ,null ,assertion.getTransactionUsages()));

        try {
            // This call returns the users current usage, you decide whether to allow the transaction or not
            Api3ScaleHttpSender sender  = new Api3ScaleHttpSender();
            Api2 api2 = ApiFactory.createV2Api("http://su1.3scale.net", requestAppId, requestPrivateKey,sender); // url, appid  privatekey, httpsender
            api2.report(transactions.toArray(new ApiTransaction[transactions.size()]));
        }
        catch(ApiException e){
            auditor.logAndAudit(AssertionMessages.API_REPORT_FAILED, new String[]{ e.getErrorMessage()}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ApiAssertion is preparing itself to be unloaded");
    }
}
