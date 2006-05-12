package com.l7tech.server.policy.assertion.xmlsec;

import java.io.IOException;

import org.springframework.context.ApplicationContext;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssTimestamp;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;
import com.l7tech.policy.assertion.xmlsec.ResponseWssTimestamp;

/**
 * Server WssTimestamp assertion that is composed of request/response assertions.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class ServerWssTimestamp extends AbstractServerAssertion  implements ServerAssertion {

    //- PUBLIC

    public ServerWssTimestamp(WssTimestamp assertion, ApplicationContext applicationContext) {
        super(assertion);
        this.wssTimestamp = assertion;
        this.requestAssertion = buildRequestAssertion(applicationContext);
        this.responseAssertion = buildResponseAssertion(applicationContext);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        AssertionStatus as = requestAssertion.checkRequest(context);

        if (as == AssertionStatus.NONE) {
            as = responseAssertion.checkRequest(context);
        }

        return as;
    }

    //- PRIVATE

    private final WssTimestamp wssTimestamp;
    private final ServerRequestWssTimestamp requestAssertion;
    private final ServerResponseWssTimestamp responseAssertion;

    private ServerRequestWssTimestamp buildRequestAssertion(ApplicationContext applicationContext) {
        RequestWssTimestamp data = new RequestWssTimestamp();
        data.setMaxExpiryMilliseconds(wssTimestamp.getRequestMaxExpiryMilliseconds());
        data.setSignatureRequired(false);
        data.setTimeUnit(wssTimestamp.getRequestTimeUnit());
        data.setRecipientContext(wssTimestamp.getRecipientContext());
        return new ServerRequestWssTimestamp(data, applicationContext);
    }

    private ServerResponseWssTimestamp buildResponseAssertion(ApplicationContext applicationContext) {
        ResponseWssTimestamp data = new ResponseWssTimestamp();
        data.setExpiryMilliseconds(wssTimestamp.getResponseExpiryMilliseconds());
        data.setSignatureRequired(false);
        data.setTimeUnit(wssTimestamp.getResponseTimeUnit());
        data.setRecipientContext(wssTimestamp.getRecipientContext());
        return new ServerResponseWssTimestamp(data, applicationContext);
    }
}
