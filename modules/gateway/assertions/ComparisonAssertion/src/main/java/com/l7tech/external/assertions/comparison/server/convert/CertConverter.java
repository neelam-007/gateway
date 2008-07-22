/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.common.io.CertUtils;

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
public class CertConverter implements ValueConverter<X509Certificate> {
    public X509Certificate convert(Object val) throws ConversionException {
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
