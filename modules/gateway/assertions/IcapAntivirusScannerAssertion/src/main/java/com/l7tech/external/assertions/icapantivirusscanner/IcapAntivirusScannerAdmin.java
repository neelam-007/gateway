package com.l7tech.external.assertions.icapantivirusscanner;

/**
 * <p>The admin interface to test the ICAP connection.</p>
 *
 * @author KDiep
 */
public interface IcapAntivirusScannerAdmin {

    /**
     * Test the connection to the specified ICAP server and service.
     *
     * @param host        the ICAP host.
     * @param port        the ICAP port.
     * @param serviceName the ICAP service name. Should not begin with a slash.
     * @throws IcapAntivirusScannerTestException
     *          if any error(s) occur during testing.
     */
    public void testConnection(String host, int port, String serviceName) throws IcapAntivirusScannerTestException;

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
