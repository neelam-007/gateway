package com.l7tech.external.assertions.mqnative;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin interface for SSM code to perform MQ Native functions in server-side
 *
 * @author ghuang
 */

@Secured
@Administrative
public interface MqNativeAdmin {

    static public class MqNativeTestException extends Exception {
        public MqNativeTestException(String message) {
            super(message);
        }
    }

    /**
     * Get the default value of the MQ message max bytes defined in "io.mqMessageMaxBytes"
     * @return the maximum number of bytes permitted for a MQ Native Queue  message, or 0 for unlimited (Integer)
     */
    @Transactional(readOnly=true)
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    long getDefaultMqMessageMaxBytes();

    /**
     * Test the specified MQ native settings which may or may not exist in the database.
     *
     * @param mqNativeActiveConnector the specified MQ native settings
     * @throws MqNativeTestException if we can't get a handle to the destinations
     */
    @Transactional(readOnly=true)
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    void testSettings(SsgActiveConnector mqNativeActiveConnector) throws MqNativeTestException;
}