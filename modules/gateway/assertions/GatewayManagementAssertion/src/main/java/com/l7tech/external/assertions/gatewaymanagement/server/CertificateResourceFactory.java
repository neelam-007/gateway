package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.TrustedCertificateMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 
 */
@ResourceFactory.ResourceType(type=TrustedCertificateMO.class)
public class CertificateResourceFactory extends EntityManagerResourceFactory<TrustedCertificateMO, TrustedCert, EntityHeader> {

    //- PUBLIC

    public CertificateResourceFactory( final RbacServices services,
                                       final SecurityFilter securityFilter,
                                       final PlatformTransactionManager transactionManager,
                                       final TrustedCertManager trustedCertManager ) {
        super( true, false, services, securityFilter, transactionManager, trustedCertManager );
    }

    //- PROTECTED

    @Override
    protected TrustedCertificateMO asResource( final TrustedCert trustedCert ) {
        TrustedCertificateMO certificate = ManagedObjectFactory.createCertificate();

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
        certificate.setProperties( getProperties( trustedCert, TrustedCert.class ) );

        return certificate;
    }

}
