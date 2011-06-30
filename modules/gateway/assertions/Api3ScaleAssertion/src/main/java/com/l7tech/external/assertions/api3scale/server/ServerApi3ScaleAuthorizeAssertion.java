package com.l7tech.external.assertions.api3scale.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.api3scale.Api3ScaleAuthorizeAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import net.threescale.api.ApiFactory;
import net.threescale.api.v2.*;
import net.threescale.api.v2.ApiException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
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

    public ServerApi3ScaleAuthorizeAssertion(Api3ScaleAuthorizeAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        String appKey = null;

        Map<String, Object> vars = context.getVariableMap(assertion.getVariablesUsed(), auditor);

        String providerKey = ExpandVariables.process(assertion.getPrivateKey(), vars, auditor, true);
        String appId = ExpandVariables.process(assertion.getApplicationID(), vars, auditor, true);
        if(assertion.getApplicationKey()!=null&& !assertion.getApplicationKey().trim().isEmpty())
            appKey = ExpandVariables.process(assertion.getApplicationKey(), vars, auditor, true);
        String server = ExpandVariables.process(assertion.getServer(), vars, auditor, true);


        String strResponse;
        final AuthorizeResponse response;
        try {            
            Api3ScaleHttpSender sender  = new Api3ScaleHttpSender();
            Api2 api2 = ApiFactory.createV2Api(server, appId, providerKey,sender); // url, appid  privatekey, httpsender
            response = api2.authorize(appKey,null); // app key, referrer
            strResponse = sender.getHttpResponseString();
        }
        catch(ApiException e){
            auditor.logAndAudit(AssertionMessages.API_AUTHORIZE_FAILED,
                new String[]{e.getErrorMessage()}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }


        if(!response.getAuthorized()){
            auditor.logAndAudit(AssertionMessages.API_AUTHORIZE_FAILED,response.getReason());
            return AssertionStatus.FALSIFIED;
        }

        if(assertion.getUsage()!=null){
            Set<String> keys = assertion.getUsage().keySet();
            for(String key: keys){
                String value = assertion.getUsage().get(key);
                ApiUsageMetric metric = response.firstMetricByName(key);
                if(metric == null){
                    auditor.logAndAudit(AssertionMessages.API_AUTHORIZE_FAILED_WITH_INVALID_USAGE, key);
                    return AssertionStatus.FALSIFIED;
                }
                int currentValue = Integer.parseInt(metric.getCurrentValue());
                int maxValue = Integer.parseInt(metric.getMaxValue());
                int predictedUsage = Integer.parseInt(value);
                if(predictedUsage > (maxValue-currentValue)){
                    auditor.logAndAudit(AssertionMessages.API_AUTHORIZE_FAILED_WITH_USAGE, key );
                    return AssertionStatus.FALSIFIED;
                }
            }
        }

        try {
            Document doc = XmlUtil.stringToDocument(strResponse);
            Message message = context.getOrCreateTargetMessage( new MessageTargetableSupport(assertion.getOutputPrefix()), false );
            message.initialize(doc);
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{e.getMessage()},ExceptionUtils.getDebugException(e) );
        } catch (NoSuchVariableException e) {  // should not get here
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{e.getMessage()},ExceptionUtils.getDebugException(e) );
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
