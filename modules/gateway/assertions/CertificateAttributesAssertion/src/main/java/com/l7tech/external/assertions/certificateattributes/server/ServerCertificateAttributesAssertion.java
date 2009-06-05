package com.l7tech.external.assertions.certificateattributes.server;

import com.l7tech.external.assertions.certificateattributes.CertificateAttributesAssertion;
import com.l7tech.security.cert.CertificateAttribute;
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
import java.util.EnumSet;
import java.util.Map;
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
                String prefix = getAssertion().getVariablePrefix() + ".";
                for (CertificateAttribute attribute : EnumSet.allOf(CertificateAttribute.class)) {
                    for (Map.Entry<String, Collection<Object>> entry : attribute.extractValuesIncludingLegacyNames(certificate).entrySet()) {
                        Collection<Object> values = entry.getValue();
                        if (values == null || values.isEmpty()) {
                            context.setVariable(prefix + entry.getKey(), ""); // Set as empty, no null variables
                        } else if (attribute.isMultiValued()) {
                            context.setVariable(prefix + entry.getKey(), values.toArray());
                        } else {
                            context.setVariable(prefix + entry.getKey(), values.iterator().next());
                        }
                    }
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
