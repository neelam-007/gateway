package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.util.Either;
import com.l7tech.util.Functions;

import java.security.cert.X509Certificate;

/** @author alex */
public class ConfiguredTrustedCert extends ConfigurationBean<Either<X509Certificate,String>> {
    private final NewTrustedCertFactory factory;

    /**
     * @param cert the certificate that's trusted
     * @param factory the factory that was originally used to create this trusted cert bean, or null to skip the factory maintenance
     */
    ConfiguredTrustedCert(Either<X509Certificate,String> cert, NewTrustedCertFactory factory) {
        super("host.controller.remoteNodeManagement.trustedCert", "Trusted Certificate", cert, null, true);
        this.factory = factory;
    }

    @Override
    public String getShortValueDescription() {
        Either<X509Certificate,String> cert = getConfigValue();
        if (cert == null) return null;
        return cert.either( new Functions.Unary<String,X509Certificate>(){
            @Override
            public String call( final X509Certificate cert ) {
                return cert.getSubjectDN().getName();
            }
        }, new Functions.Unary<String,String>(){
            @Override
            public String call( final String s ) {
                return s;
            }
        } );
    }

    @Override
    public void onDelete() {
        if (factory != null) factory.release();
    }

}
