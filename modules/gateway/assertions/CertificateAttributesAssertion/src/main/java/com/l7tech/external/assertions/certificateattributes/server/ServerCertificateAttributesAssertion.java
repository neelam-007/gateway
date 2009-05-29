package com.l7tech.external.assertions.certificateattributes.server;

import com.l7tech.external.assertions.certificateattributes.CertificateAttributesAssertion;
import com.l7tech.external.assertions.certificateattributes.CertificateAttributesExtractor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Server side implementation of the CertificateAttributesAssertion.
 *
 * @see com.l7tech.external.assertions.certificateattributes.CertificateAttributesAssertion
 */
public class ServerCertificateAttributesAssertion extends AbstractServerAssertion<CertificateAttributesAssertion> {

    //- PUBLIC

    @SuppressWarnings({"UnusedDeclaration"})
    public ServerCertificateAttributesAssertion( final CertificateAttributesAssertion assertion,
                                                 final ApplicationContext context ) throws PolicyAssertionException {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.FALSIFIED;

        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        AuthenticationResult result = authContext.getLastAuthenticationResult();
        if ( result != null ) {
            X509Certificate certificate = result.getAuthenticatedCert();

            if ( certificate != null ) {
                CertificateAttributesExtractor cae = new CertificateAttributesExtractor( certificate );

                String prefix = getAssertion().getVariablePrefix() + ".";
                Collection<String> certificateAttributes = cae.getAttributeNames();
                for ( String attribute : CertificateAttributesExtractor.getSimpleCertificateAttributes() ) {
                    String name = prefix + attribute;
                    if ( certificateAttributes.contains( attribute ) ) {
                        context.setVariable( name, cae.getAttributeValue( attribute ) );
                    } else {
                        context.setVariable( name, "" ); // Set as empty, no null variables
                    }
                }

                for(String attribute : cae.getDynamicKeys()) {
                    String name = prefix + attribute;
                    context.setVariable(name, cae.getAttributeValue(attribute));
                }

                status = AssertionStatus.NONE;
            } else {
                logger.info( "No certificate in authentication result" );
            }
        } else {
            logger.info( "No authentication result" );
        }

        return status;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerCertificateAttributesAssertion.class.getName());
}
