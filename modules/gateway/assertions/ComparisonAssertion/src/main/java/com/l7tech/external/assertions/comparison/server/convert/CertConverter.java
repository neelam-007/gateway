package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.policy.variable.DataType;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Attempts to convert the provided {@link Object} into a {@link X509Certificate}, with the following rules:
 * <ul>
 * <li>Strings are interpreted as PEM-encoded certs
 * <li>byte arrays are interpreted as DER-encoded certs
 * <li>any other value will result in a {@link ConversionException}.
 * </ul>
 * @author alex
*/
public class CertConverter extends ValueConverterSupport<X509Certificate> {

    public CertConverter() {
        super( DataType.CERTIFICATE );
    }

    @Override
    public X509Certificate convert( final Object val ) throws ConversionException {
        if (val instanceof String) {
            String b64 = (String) val;
            try {
                return CertUtils.decodeFromPEM(b64);
            } catch (Exception e) {
                throw new ConversionException("Couldn't decode certificate", e);
            }
        } else if (val instanceof byte[]) {
            try {
                return CertUtils.decodeCert((byte[])val);
            } catch (CertificateException e) {
                throw new ConversionException("Couldn't decode certificate", e);
            }
        } else {
            throw new ConversionException("Unsupported value type: " + val.getClass().getName());
        }
    }
}
