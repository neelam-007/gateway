package com.l7tech.external.assertions.amqpassertion;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import org.springframework.transaction.annotation.Transactional;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.TEST_CONFIGURATION;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 10/04/12
 * Time: 2:32 PM
 * To change this template use File | Settings | File Templates.
 */
@Secured
@Administrative
public interface AmqpAdmin {
    static public class AmqpTestException extends Exception {
        public AmqpTestException(String message) {
            super(message);
        }

        public AmqpTestException(String message, Throwable e) {
            super(message, e);
        }
    }

    @Transactional(readOnly = true)
    @Secured(stereotype = TEST_CONFIGURATION)
    boolean testSettings(AMQPDestination destination) throws AmqpTestException;

}
