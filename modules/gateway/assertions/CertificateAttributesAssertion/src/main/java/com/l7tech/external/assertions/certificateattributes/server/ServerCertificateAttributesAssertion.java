package com.l7tech.external.assertions.certificateattributes.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.external.assertions.certificateattributes.CertificateAttributesAssertion;
import com.l7tech.external.assertions.certificateattributes.CertificateAttributesExtractor;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
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

    public AssertionStatus checkRequest(final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.FALSIFIED;

        AuthenticationResult result = context.getLastAuthenticationResult();
        if ( result != null ) {
            X509Certificate certificate = result.getAuthenticatedCert();

            // Federated IDP does not set the X.509 Certificate in the AuthenticationResult, so we'll have to find it ...
            if ( certificate == null && result.getUser() instanceof FederatedUser ) {
                FederatedUser fu = (FederatedUser) result.getUser();
                String subjectDn = fu.getSubjectDn();
                if ( subjectDn != null ) {
                    // find credential for this certificate
                    for ( LoginCredentials credential : context.getCredentials() ) {
                        if ( credential.getClientCert() != null && subjectDn.equals(credential.getClientCert().getSubjectDN().getName()) ) {
                            if ( certificate == null || CertUtils.certsAreEqual( certificate, credential.getClientCert() ) ) {
                                certificate = credential.getClientCert();
                            } else { // we can't tell which is the right credential, so fail
                                logger.info( "Found multiple certificates matching authorized user." );
                                certificate = null;
                                break;
                            }
                        }
                    }
                }
            }

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
