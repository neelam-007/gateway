package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CustomizeErrorResponseAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.xml.SoapFaultLevel;

import java.io.IOException;

/**
 * Server implementation for CustomizeErrorResponseAssertion.
 */
public class ServerCustomizeErrorResponseAssertion extends AbstractServerAssertion<CustomizeErrorResponseAssertion> {

    //- PUBLIC

    public ServerCustomizeErrorResponseAssertion( final CustomizeErrorResponseAssertion assertion ) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        context.setFaultlevel( asSoapFaultLevel( assertion ) );
        return AssertionStatus.NONE;
    }

    //- PRIVATE

    private SoapFaultLevel asSoapFaultLevel( final CustomizeErrorResponseAssertion assertion ) {
        SoapFaultLevel faultLevel = new SoapFaultLevel();

        if ( assertion.getErrorLevel() == CustomizeErrorResponseAssertion.ErrorLevel.DROP_CONNECTION ) {
            faultLevel.setLevel( SoapFaultLevel.DROP_CONNECTION );
        } else {
            faultLevel.setLevel( SoapFaultLevel.TEMPLATE_FAULT );
            faultLevel.setFaultTemplateHttpStatus( assertion.getHttpStatus() );
            faultLevel.setFaultTemplateContentType( assertion.getContentType() );
            faultLevel.setFaultTemplate( assertion.getContent() );
            faultLevel.setIncludePolicyDownloadURL( assertion.isIncludePolicyDownloadURL() );
        }

        return faultLevel;
    }
}
