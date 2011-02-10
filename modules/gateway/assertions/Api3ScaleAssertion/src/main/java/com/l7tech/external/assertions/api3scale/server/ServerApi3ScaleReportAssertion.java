package com.l7tech.external.assertions.api3scale.server;

import com.l7tech.external.assertions.api3scale.Api3ScaleAuthorizeAssertion;
import com.l7tech.external.assertions.api3scale.Api3ScaleReportAssertion;
import com.l7tech.external.assertions.api3scale.Api3ScaleTransaction;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import net.threescale.api.ApiFactory;
import net.threescale.api.v2.Api2;
import net.threescale.api.v2.ApiException;
import net.threescale.api.v2.ApiTransaction;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.text.DateFormat;
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
        String queryStr = context.getRequest().getHttpRequestKnob().getQueryString();
        StringTokenizer tokenizer = new  StringTokenizer(queryStr, "&=");

        String  requestAppId = null;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals("app_id")){
                requestAppId = tokenizer.nextToken();
                break;
            }
        }

        if( requestAppId == null){
            logger.warning("Unable to extract API Keys");
            return AssertionStatus.FAILED;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> vars = context.getVariableMap(assertion.getVariablesUsed(), auditor);
        List<ApiTransaction > transactions = new ArrayList<ApiTransaction>();

        HashMap<String, String> metrics = new HashMap<String,  String>();
        for(Api3ScaleTransaction assTransaction: assertion.getTransactions()){
            metrics.clear();
            metrics.putAll(assTransaction.getMetrics());
            
            String timeStamp = null;
            String appId = null;
            if(assTransaction.getTimestamp()!= null && !assTransaction.getTimestamp().trim().isEmpty())
                timeStamp = ExpandVariables.process(assTransaction.getTimestamp(), vars, auditor, true);
            else
                timeStamp = dateFormat.format(new Date());
            transactions.add(new ApiTransaction( requestAppId ,timeStamp,metrics));
        }



        try {
            // This call returns the users current usage, you decide whether to allow the transaction or not
            Api2 api2 = ApiFactory.createV2Api("http://su1.3scale.net", requestAppId , (String) context.getVariable(Api3ScaleAuthorizeAssertion.PREFIX+ "." + Api3ScaleAuthorizeAssertion.SUFFIX_PROVIDER_KEY));   // url, appid  privatekey
            api2.report(transactions.toArray(new ApiTransaction[transactions.size()]));
        }
        catch(ApiException e){
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                new String[]{"Failed to report: " +  e.getErrorCode() + " "+ e.getErrorMessage()}, e);
            return AssertionStatus.FAILED;
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                new String[]{"Failed to get private key"}, e);
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
        logger.log(Level.INFO, "ServerApi3ScaleAssertion is preparing itself to be unloaded");
    }
}
