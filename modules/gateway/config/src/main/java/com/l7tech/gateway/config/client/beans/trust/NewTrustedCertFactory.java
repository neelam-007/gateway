package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.gateway.config.client.beans.ConfigurableBeanFactory;

import java.net.URL;

/** @author alex */
public class NewTrustedCertFactory extends ConfigurableBeanFactory<NewTrustedCertFactory.NewTrustedCert> {
    protected NewTrustedCertFactory(int max) {
        super("host.controller.remoteNodeManagement.trustedCertUrlBeanFactory", "Trusted Certificate", 0, max);
    }

    @Override
    public NewTrustedCert make() {
        return new NewTrustedCert(this);
    }

    public static class NewTrustedCert extends EitherConfigurableBean<URL,String,TrustedCertUrl,TrustedCertThumbprint>{
        private NewTrustedCert( final NewTrustedCertFactory factory ) {
            super( "host.controller.remoteNodeManagement.tempTrustedCertUrl",
                   "ESM Certificate",
                    null,
                    new TrustedCertUrl(factory),
                    new TrustedCertThumbprint("host.controller.remoteNodeManagement.tempTrustedCertUrl", "ESM Certificate", null, factory) );
        }

        @Override
        protected boolean isA( final String input ) {
            return !TrustedCertThumbprint.isValidThumbprint( input );
        }
    }
}
