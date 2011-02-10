package com.l7tech.external.assertions.api3scale.server;

import com.l7tech.external.assertions.api3scale.Api3ScaleAuthorizeAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import net.threescale.api.ApiFactory;
import net.threescale.api.v2.Api2;
import net.threescale.api.v2.ApiException;
import net.threescale.api.v2.ApiUsageMetric;
import net.threescale.api.v2.AuthorizeResponse;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the Api3ScaleAssertion.
 *
 * @see com.l7tech.external.assertions.api3scale.Api3ScaleAuthorizeAssertion
 */
public class ServerApi3ScaleAuthorizeAssertion extends AbstractServerAssertion<Api3ScaleAuthorizeAssertion> {
    private static final Logger logger = Logger.getLogger(ServerApi3ScaleAuthorizeAssertion.class.getName());

    private final Api3ScaleAuthorizeAssertion assertion;
    private final Auditor auditor;
    private final String url = "http://layer7.3scale.net"; //"http://server.3scale.net";//

    public ServerApi3ScaleAuthorizeAssertion(Api3ScaleAuthorizeAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        String appKey = null;
        String appId = null;
        String referrer = null;
        String server = "http://su1.3scale.net";
        String providerKey = null;

        String queryStr = context.getRequest().getHttpRequestKnob().getQueryString();
        StringTokenizer tokenizer = new  StringTokenizer(queryStr, "&=");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals("provider_key"))
                providerKey = tokenizer.nextToken();
            else if (token.equals("app_key"))
                appKey = tokenizer.nextToken();
            else if (token.equals("app_id"))
                appId = tokenizer.nextToken();
            else if (token.equals("referrer"))
                referrer = tokenizer.nextToken();
        }


        Map<String, Object> vars = context.getVariableMap(assertion.getVariablesUsed(), auditor);
        if(assertion.getServer()!=null && !assertion.getServer().trim().isEmpty())
            server = ExpandVariables.process(assertion.getServer(), vars, auditor, true);
        if(assertion.getPrivateKey()!=null&& !assertion.getPrivateKey().trim().isEmpty())
            providerKey = ExpandVariables.process(assertion.getPrivateKey(), vars, auditor, true);
        
        if( providerKey == null || appId == null){
            logger.warning("Unable to extract API Keys");
            return AssertionStatus.FAILED;
        }
        
        try {            
            // This call returns the users current usage, you decide whether to allow the transaction or not
            Api2 api2 = ApiFactory.createV2Api(server, appId , providerKey );   // url, appid  privatekey
            AuthorizeResponse response = api2.authorize(appKey, referrer); // app key, referrer

            if(!response.getAuthorized())
                return AssertionStatus.FALSIFIED;

            String plan = response.getPlan();
            context.setVariable(assertion.getPrefixUsed()+ "." + Api3ScaleAuthorizeAssertion.SUFFIX_PLAN,plan);
            List<ApiUsageMetric>  usageReports = response.getUsageReports();
            String[] metrics = new String[usageReports.size()];
            for (int i = 0; i < usageReports.size(); i++) {
                metrics[i] = usageReports.get(i).toString();
            }
            context.setVariable(assertion.getPrefixUsed()+ "." + Api3ScaleAuthorizeAssertion.SUFFIX_USAGE,metrics);
            context.setVariable(assertion.getPrefixUsed()+ "." + Api3ScaleAuthorizeAssertion.SUFFIX_PROVIDER_KEY,providerKey);
            context.setVariable(Api3ScaleAuthorizeAssertion.PREFIX+ "." + Api3ScaleAuthorizeAssertion.SUFFIX_PROVIDER_KEY,providerKey);
            context.setVariable(assertion.getPrefixUsed()+ "." + Api3ScaleAuthorizeAssertion.SUFFIX_APP_ID,appId);
        }
        catch(ApiException e){
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                new String[]{"Failed to authorize: " +  e.getErrorCode() + " "+ e.getErrorMessage()}, e);
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
