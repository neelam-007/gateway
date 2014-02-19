package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.api.CertificateData;
import com.l7tech.gateway.api.TrustedCertificateMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Option;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * 
 */
@ResourceFactory.ResourceType(type=TrustedCertificateMO.class)
public class CertificateResourceFactory extends SecurityZoneableEntityManagerResourceFactory<TrustedCertificateMO, TrustedCert, EntityHeader> {

    //- PUBLIC

    public CertificateResourceFactory( final RbacServices services,
                                       final SecurityFilter securityFilter,
                                       final PlatformTransactionManager transactionManager,
                                       final TrustedCertManager trustedCertManager,
                                       final SecurityZoneManager securityZoneManager ) {
        super( false, false, services, securityFilter, transactionManager, trustedCertManager, securityZoneManager );
    }

    //- PROTECTED

    @Override
    public TrustedCertificateMO asResource( final TrustedCert trustedCert ) {
        TrustedCertificateMO certificate = ManagedObjectFactory.createTrustedCertificate();

        certificate.setName( trustedCert.getName() );
        if (  trustedCert.getCertificate() != null ) {
            try {
                certificate.setCertificateData( ManagedObjectFactory.createCertificateData( trustedCert.getCertificate() ) );
            } catch ( ManagedObjectFactory.FactoryException e ) {
                throw new ResourceAccessException( e );
            }
        } else {
            certificate.setCertificateData( ManagedObjectFactory.createCertificateData( trustedCert.getCertBase64() ) );
        }
        certificate.setRevocationCheckingPolicyId( trustedCert.getRevocationCheckPolicyOid()==null ?
                null :
                trustedCert.getRevocationCheckPolicyOid().toString() );
        certificate.setProperties( getProperties( trustedCert, TrustedCert.class ) );
        certificate.getProperties().put(
                "revocationCheckingEnabled",
                trustedCert.getRevocationCheckPolicyType()!=TrustedCert.PolicyUsageType.NONE);

        // handle SecurityZone
        doSecurityZoneAsResource( certificate, trustedCert );

        return certificate;
    }

    @Override
    public TrustedCert fromResource( final Object resource, boolean strict ) throws InvalidResourceException {
        if ( !(resource instanceof TrustedCertificateMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected certificate");

        final TrustedCertificateMO certificateResource = (TrustedCertificateMO) resource;

        final X509Certificate x509Certificate = getCertificate( certificateResource );

        final TrustedCert certificateEntity = new TrustedCert();
        certificateEntity.setName( asName(certificateResource.getName()) );
        certificateEntity.setCertificate( x509Certificate );
        final boolean revocationCheckingEnabled = getProperty( certificateResource.getProperties(), "revocationCheckingEnabled", Option.some( Boolean.TRUE ), Boolean.class ).some();
        if ( revocationCheckingEnabled ) {
            if ( certificateResource.getRevocationCheckingPolicyId() != null ) {
                certificateEntity.setRevocationCheckPolicyType( TrustedCert.PolicyUsageType.SPECIFIED );
                certificateEntity.setRevocationCheckPolicyOid(toInternalId( EntityType.REVOCATION_CHECK_POLICY, certificateResource.getRevocationCheckingPolicyId(), "Revocation Checking Policy Identifier" ) );
            } else {
                certificateEntity.setRevocationCheckPolicyType( TrustedCert.PolicyUsageType.USE_DEFAULT );
            }
        } else {
            certificateEntity.setRevocationCheckPolicyType( TrustedCert.PolicyUsageType.NONE );
        }
        setProperties( certificateEntity, certificateResource.getProperties(), TrustedCert.class );

        // handle SecurityZone
        doSecurityZoneFromResource( certificateResource, certificateEntity, strict );

        return certificateEntity;
    }

    @Override
    protected void updateEntity( final TrustedCert oldEntity, final TrustedCert newEntity ) throws InvalidResourceException {
        oldEntity.setName( newEntity.getName() );
        oldEntity.setCertificate( newEntity.getCertificate()  );
        oldEntity.setTrustAnchor( newEntity.isTrustAnchor() );
        oldEntity.setVerifyHostname( newEntity.isVerifyHostname() );
        for ( final TrustedCert.TrustedFor trustedFor : TrustedCert.TrustedFor.values() ) {
            oldEntity.setTrustedFor( trustedFor, newEntity.isTrustedFor(trustedFor) );
        }
        oldEntity.setRevocationCheckPolicyType( newEntity.getRevocationCheckPolicyType() );
        oldEntity.setRevocationCheckPolicyOid( newEntity.getRevocationCheckPolicyOid() );
        oldEntity.setSecurityZone( newEntity.getSecurityZone() );
    }

    //- PRIVATE

    private X509Certificate getCertificate( final TrustedCertificateMO certificateResource ) throws InvalidResourceException {
        final X509Certificate x509Certificate;
        try {
            final Certificate certificate = CertUtils.getFactory().generateCertificate(
                    new ByteArrayInputStream( getEncoded(certificateResource.getCertificateData()) ) );

            if ( !(certificate instanceof X509Certificate) )
                throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "unexpected encoded certificate type");

            x509Certificate = (X509Certificate) certificate;
        } catch ( CertificateException e ) {
            throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "encoded certificate error: " + ExceptionUtils.getMessage(e));
        }
        return x509Certificate;
    }

    private byte[] getEncoded( final CertificateData certificateData ) throws InvalidResourceException {
        if ( certificateData == null || certificateData.getEncoded().length == 0 ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "encoded certificate data");
        }
        return certificateData.getEncoded();
    }
}
