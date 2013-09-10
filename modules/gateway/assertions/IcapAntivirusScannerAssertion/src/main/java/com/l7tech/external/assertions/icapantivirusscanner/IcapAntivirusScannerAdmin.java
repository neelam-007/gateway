package com.l7tech.external.assertions.icapantivirusscanner;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;

/**
 * <p>The admin interface to test the ICAP connection.</p>
 *
 * @author KDiep
 */
@Secured
public interface IcapAntivirusScannerAdmin {

    /**
     * Test the connection to the specified ICAP server and service.
     *
     * @param icapServerUrl        the ICAP icapServerUrl.
     * @throws IcapAntivirusScannerTestException
     *          if any error(s) occur during testing.
     */
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    public void testConnection(String icapServerUrl) throws IcapAntivirusScannerTestException;

    static public class IcapAntivirusScannerTestException extends Exception {

        public IcapAntivirusScannerTestException() {
            super();
        }

        public IcapAntivirusScannerTestException(String message) {
            super(message);
        }

        public IcapAntivirusScannerTestException(String message, Exception e) {
            super(message, e);
        }

    }
}
