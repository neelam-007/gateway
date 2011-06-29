package com.l7tech.external.assertions.api3scale.server;

import com.l7tech.external.assertions.api3scale.Api3ScaleAdmin;
import com.l7tech.external.assertions.api3scale.Api3ScaleAuthorizeAssertion;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import net.threescale.api.ApiFactory;
import net.threescale.api.v2.*;

import java.util.Map;
import java.util.logging.Logger;

/**
 * User: wlui
 */
public class Api3ScaleAdminImpl implements Api3ScaleAdmin{
    private static final Logger logger = Logger.getLogger(Api3ScaleAdminImpl.class.getName());
    private final Auditor auditor;

    public Api3ScaleAdminImpl() {
        this.auditor =  new LogOnlyAuditor(logger);
    }

    @Override
    public String testAuthorize(Api3ScaleAuthorizeAssertion ass, Map<String, Object> contextVars) throws Api3ScaleTestException {
        Auditor auditor = new LogOnlyAuditor(logger);
        String server = ExpandVariables.process(ass.getServer(), contextVars, auditor, true);
        String providerKey = ExpandVariables.process(ass.getPrivateKey(), contextVars, auditor, true);
        String appId = ExpandVariables.process(ass.getApplicationID(), contextVars, auditor, true);
        String appKey = null;
        if(ass.getApplicationKey()!=null&& !ass.getApplicationKey().trim().isEmpty())
            appKey = ExpandVariables.process(ass.getApplicationKey(), contextVars, auditor, true);
        Api3ScaleHttpSender sender  = new Api3ScaleHttpSender();
        Api2 api2 = ApiFactory.createV2Api(server, appId, providerKey, sender);
        try {
            api2.authorize(appKey, null); // app key, referrer
        } catch (ApiException e) {
            throw new Api3ScaleTestException( e.getMessage(), ExceptionUtils.getDebugException(e));
        }
        return sender.getHttpResponseString();
    }
}
