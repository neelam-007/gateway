package com.l7tech.server.transport.jms.prov;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.TibcoEmsConstants;
import com.l7tech.server.transport.jms.ConnectionFactoryCustomizer;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.tibco.tibjms.TibjmsConnectionFactory;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ConnectionFactoryCustomizer implementation for TIBCO EMS.
 *
 * <p>This customizer is required to configure SSL parameters for the TibjmsConnectionFactory.</p>
 *
 * @author Steve Jones
 * @author rmak
 * @since SecureSpan 3.7
 */
public class TibcoConnectionFactoryCustomizer implements ConnectionFactoryCustomizer {

    //- PUBLIC

    /**
     * Customize the given connection factory.
     *
     * @param jmsConnection
     * @param connectionFactory The factory to customize.
     * @param context The configuration context
     * @throws JmsConfigException if an error occurs
     */
    public void configureConnectionFactory(JmsConnection jmsConnection, ConnectionFactory connectionFactory, Context context) throws JmsConfigException {
        try {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Configuring connection factory.");

            Map environment = context.getEnvironment();

            // Translates the properties set by {@link TibcoEmsQueueExtraPropertiesPanel#setProperties}
            // into method calls to TibjmsConnectionFactory.
            if (connectionFactory instanceof TibjmsConnectionFactory) {
                final TibjmsConnectionFactory f = (TibjmsConnectionFactory) connectionFactory;
                Object value;

                value = environment.get(TibcoEmsConstants.TibjmsSSL.AUTH_ONLY);
                if (value != null) f.setSSLAuthOnly((Boolean)value);

                value = environment.get(TibcoEmsConstants.TibjmsSSL.DEBUG_TRACE);
                if (value != null) f.setSSLDebugTrace((Boolean)value);

                value = environment.get(TibcoEmsConstants.TibjmsSSL.ENABLE_VERIFY_HOST);
                if (value != null) f.setSSLEnableVerifyHost((Boolean)value);

                value = environment.get(TibcoEmsConstants.TibjmsSSL.ENABLE_VERIFY_HOST_NAME);
                if (value != null) f.setSSLEnableVerifyHostName((Boolean)value);

                value = environment.get(TibcoEmsConstants.TibjmsSSL.EXPECTED_HOST_NAME);
                if (value != null) f.setSSLExpectedHostName((String)value);

                value = environment.get(TibcoEmsConstants.TibjmsSSL.IDENTITY);
                if (value != null) {
                    f.setSSLIdentity((byte[]) value);
                    f.setSSLIdentityEncoding("PKCS12");
                }

                value = environment.get(TibcoEmsConstants.TibjmsSSL.PASSWORD);
                if (value != null) f.setSSLPassword((String)value);

                value = environment.get(TibcoEmsConstants.TibjmsSSL.TRACE);
                if (value != null) f.setSSLTrace((Boolean)value);

                value = environment.get(TibcoEmsConstants.TibjmsSSL.TRUSTED_CERTIFICATES);
                if (value != null) {
                    final List<X509Certificate> certs = (List<X509Certificate>)value;
                    for (X509Certificate cert : certs) {
                        f.setSSLTrustedCertificate(cert.getEncoded(), "DER");
                    }
                }
            }
        } catch(Exception exception) {
            throw new JmsConfigException("Error configuring connection factory.", exception);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(TibcoConnectionFactoryCustomizer.class.getName());
}
