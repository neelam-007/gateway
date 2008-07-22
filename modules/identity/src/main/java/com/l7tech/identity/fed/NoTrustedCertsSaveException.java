package com.l7tech.identity.fed;

import com.l7tech.objectmodel.SaveException;

/**
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class NoTrustedCertsSaveException extends SaveException {
    public NoTrustedCertsSaveException() {
        super();
    }

    public NoTrustedCertsSaveException(Throwable cause) {
        super(cause);
    }

    public NoTrustedCertsSaveException(String message) {
        super(message);
    }

    public NoTrustedCertsSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
