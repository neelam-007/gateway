package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.server.identity.cert.TrustedCertCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Unary;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;

import static com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion.LookupType;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Option.first;
import static com.l7tech.util.Option.optional;
import static java.util.Arrays.asList;

/**
 * Server assertion for trusted certificate look up.
 */
public class ServerLookupTrustedCertificateAssertion extends AbstractServerAssertion<LookupTrustedCertificateAssertion> {

    @Inject
    private TrustedCertCache trustedCertCache;

    @Inject @Named("securityTokenResolver")
    private SecurityTokenResolver securityTokenResolver;

    private final String[] variablesUsed;

    public ServerLookupTrustedCertificateAssertion( final LookupTrustedCertificateAssertion assertion ) {
        super( assertion );
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) {
        AssertionStatus result = AssertionStatus.FALSIFIED;

        LookupType lookupType = optional(assertion.getLookupType()).orSome(LookupType.TRUSTED_CERT_NAME);
        String logVal = null;
        final Map<String,Object> variableMap = context.getVariableMap(variablesUsed, getAudit());

        try {
            final Collection<X509Certificate> certificates;

            switch ( assertion.getLookupType() ) {
                case CERT_ISSUER_SERIAL:
                    X500Principal issuer = parseX500Principal(ExpandVariables.process(assertion.getCertIssuerDn(), variableMap, getAudit()));
                    BigInteger serial = parseCertSerial(ExpandVariables.process(assertion.getCertSerialNumber(), variableMap, getAudit()));
                    logVal = issuer + "/" + serial;
                    logAndAudit( AssertionMessages.CERT_ANY_LOOKUP_NAME, lookupType.toString(), logVal );
                    certificates = optional( securityTokenResolver.lookupByIssuerAndSerial(issuer, serial) ).toList();
                    break;

                case CERT_SKI:
                    logVal = ExpandVariables.process( assertion.getCertSubjectKeyIdentifier(), variableMap, getAudit());
                    logAndAudit( AssertionMessages.CERT_ANY_LOOKUP_NAME, lookupType.toString(), logVal );
                    certificates = optional( securityTokenResolver.lookupBySki(logVal) ).toList();
                    break;

                case CERT_SUBJECT_DN:
                    logVal = CertUtils.getDN(parseX500Principal(ExpandVariables.process(assertion.getCertSubjectDn(), variableMap, getAudit())));
                    logAndAudit( AssertionMessages.CERT_ANY_LOOKUP_NAME, lookupType.toString(), logVal );
                    certificates = optional( securityTokenResolver.lookupByKeyName(logVal) ).toList();
                    break;

                case CERT_THUMBPRINT_SHA1:
                    logVal = ExpandVariables.process( assertion.getCertThumbprintSha1(), variableMap, getAudit());
                    logAndAudit( AssertionMessages.CERT_ANY_LOOKUP_NAME, lookupType.toString(), logVal );
                    certificates = optional( securityTokenResolver.lookup(logVal) ).toList();
                    break;

                case TRUSTED_CERT_NAME:
                default:
                    logVal = ExpandVariables.process( assertion.getTrustedCertificateName(), variableMap, getAudit());
                    logAndAudit( AssertionMessages.CERT_ANY_LOOKUP_NAME, lookupType.toString(), logVal );
                    certificates = map( trustedCertCache.findByName(logVal), certExtractor );
                    break;
            }

            switch ( certificates.size() ) {
                case 0:
                    logAndAudit( AssertionMessages.CERT_ANY_LOOKUP_NOTFOUND, lookupType.toString(), logVal );
                    break;
                case 1:
                    setVariable( context, certificates, false );
                    result = AssertionStatus.NONE;
                    break;
                default:
                    if ( !assertion.isAllowMultipleCertificates() ) {
                        logAndAudit( AssertionMessages.CERT_ANY_LOOKUP_MULTIPLE, lookupType.toString(), logVal );
                    } else {
                        setVariable( context, certificates, true );
                        result = AssertionStatus.NONE;
                    }
            }
        } catch ( FindException e ) {
            logAndAudit( AssertionMessages.CERT_ANY_LOOKUP_ERROR, new String[]{lookupType.toString(), logVal, ExceptionUtils.getMessage( e )}, ExceptionUtils.getDebugException(e) );
            result = AssertionStatus.FAILED;
        }

        return result;
    }

    private X500Principal parseX500Principal(String name) throws FindException {
        try {
            return new X500Principal(name);
        } catch (IllegalArgumentException e) {
            throw new FindException("Invalid DN: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private BigInteger parseCertSerial(String num) throws FindException {
        try {
            return new BigInteger(num);
        } catch (NumberFormatException e) {
            throw new FindException("Invalid certificate serial number: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static final Unary<X509Certificate,TrustedCert> certExtractor = new Unary<X509Certificate,TrustedCert>(){
        @Override
        public X509Certificate call( final TrustedCert trustedCert ) {
            return trustedCert.getCertificate();
        }
    };

    private void setVariable( final PolicyEnforcementContext context,
                              final Collection<X509Certificate> certificates,
                              final boolean multivalued ) {

        final String variableName = assertion.getVariableName();
        final Object variableValue;
        if ( multivalued ) {
            variableValue = certificates.toArray(new X509Certificate[certificates.size()]);
        } else {
            variableValue = first(certificates).toNull();
        }
        context.setVariable( variableName, variableValue );
    }
}
