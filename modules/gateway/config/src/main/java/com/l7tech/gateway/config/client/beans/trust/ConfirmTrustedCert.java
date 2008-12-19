/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.gateway.config.client.beans.BooleanConfigurableBean;
import com.l7tech.gateway.config.client.beans.ConfigResult;
import com.l7tech.gateway.config.client.beans.ConfigurationContext;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.util.HexUtils;
import com.l7tech.common.io.CertUtils;

import java.security.cert.X509Certificate;
import java.security.GeneralSecurityException;

/** @author alex */
public class ConfirmTrustedCert extends BooleanConfigurableBean {
    private final X509Certificate cert;

    ConfirmTrustedCert( final X509Certificate cert ) {
        super("host.controller.remoteNodeManagement.confirmTrustedCert", "Confirm Trusted Certificate", false);
        this.cert = cert;
    }

    @Override
    public String getDisplayValue() {
        return super.getShortValueDescription();
    }

    @Override
    public String getShortValueDescription() {
       StringBuilder description = new StringBuilder();

        description.append("\n");
        description.append("  Issuer       : ");
        description.append(cert.getIssuerDN().getName());
        description.append("\n");
        description.append("  Serial Number: ");
        description.append(hexFormat(cert.getSerialNumber().toByteArray()));
        description.append("\n");
        description.append("  Subject      : ");
        description.append(cert.getSubjectDN().getName());
        description.append("\n");
        description.append("  Thumbprint   : ");
        description.append(getCertificateThumbprint(cert));

        return description.toString();
    }

    @Override
    public ConfigResult onConfiguration( final Boolean value,
                                         final ConfigurationContext context) {
        if ( value ) {
            if ( context.getBeans() != null ) {
                for ( ConfigurationBean bean : context.getBeans() ) {
                    if ( bean instanceof NewTrustedCertFactory ) {
                        context.removeBean( bean );
                        break;
                    }
                }
            }
            return ConfigResult.pop(new ConfiguredTrustedCert(cert));
        } else {
            return ConfigResult.pop();
        }
    }

    private String hexFormat( final byte[] bytes ) {
        StringBuilder builder = new StringBuilder();

        for ( int i=0; i<bytes.length; i++ ) {
            String byteHex = HexUtils.hexDump(new byte[]{bytes[i]});
            builder.append(byteHex.toUpperCase());
            if ( i<bytes.length-1 ) {
                builder.append(':');
            }
        }

        return builder.toString();
    }

    private String getCertificateThumbprint( final X509Certificate cert ) {
        String thumbprint = "<Not Available>";

        try {
            thumbprint = CertUtils.getCertificateFingerprint( cert, "SHA1" ).substring(5);
        } catch ( GeneralSecurityException gse ) {
            // display <Not Available>
        }

        return thumbprint;
    }
}
