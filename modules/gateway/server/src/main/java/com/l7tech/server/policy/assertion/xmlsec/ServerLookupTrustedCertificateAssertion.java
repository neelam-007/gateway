package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.server.identity.cert.TrustedCertCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Option.first;

import javax.inject.Inject;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * Server assertion for trusted certificate look up.
 */
public class ServerLookupTrustedCertificateAssertion extends AbstractServerAssertion<LookupTrustedCertificateAssertion> {

    @Inject
    private TrustedCertCache trustedCertCache;
    private final String[] variablesUsed;

    public ServerLookupTrustedCertificateAssertion( final LookupTrustedCertificateAssertion assertion ) {
        super( assertion );
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) {
        AssertionStatus result = AssertionStatus.FALSIFIED;

        final String trustedCertificateName = ExpandVariables.process(
                assertion.getTrustedCertificateName(),
                context.getVariableMap( variablesUsed, getAudit() ),
                getAudit() );

        logAndAudit( AssertionMessages.CERT_LOOKUP_NAME, trustedCertificateName );

        try {
            final Collection<TrustedCert> certificates = trustedCertCache.findByName( trustedCertificateName );
            switch ( certificates.size() ) {
                case 0:
                    logAndAudit( AssertionMessages.CERT_LOOKUP_NOTFOUND, trustedCertificateName );
                    break;
                case 1:
                    setVariable( context, certificates, false );
                    result = AssertionStatus.NONE;
                    break;
                default:
                    if ( !assertion.isAllowMultipleCertificates() ) {
                        logAndAudit( AssertionMessages.CERT_LOOKUP_MULTIPLE, trustedCertificateName );
                    } else {
                        setVariable( context, certificates, true );
                        result = AssertionStatus.NONE;
                    }
            }
        } catch ( FindException e ) {
            logAndAudit( AssertionMessages.CERT_LOOKUP_ERROR, new String[]{ExceptionUtils.getMessage( e )}, e );
            result = AssertionStatus.FAILED;
        }

        return result;
    }

    private void setVariable( final PolicyEnforcementContext context,
                              final Collection<TrustedCert> certificates,
                              final boolean multivalued ) {
        final Unary<X509Certificate,TrustedCert> certExtractor = new Unary<X509Certificate,TrustedCert>(){
            @Override
            public X509Certificate call( final TrustedCert trustedCert ) {
                return trustedCert.getCertificate();
            }
        };

        final String variableName = assertion.getVariableName();
        final Object variableValue;
        if ( multivalued ) {
            variableValue = map(certificates,certExtractor).toArray(new X509Certificate[certificates.size()]);
        } else {
            variableValue = first(certificates).map(certExtractor).toNull();
        }
        context.setVariable( variableName, variableValue );
    }
}
