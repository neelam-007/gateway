package com.l7tech.console.panels.solutionkit;

import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains utility methods for the solution kit manager.
 */
public final class SolutionKitUtils {

    /**
     * Converts the given managed object to an entity.
     *
     * @param mo the managed object
     * @return the entity
     */
    public static JdbcConnection fromMangedObject (JDBCConnectionMO mo) {
        // todo (kpak) : It would be good idea to reuse code from the modular GatewayManagementAssertion's
        // com.l7tech.external.assertions.gatewaymanagement.server.JDBCConnectionResourceFactory#fromResource()
        //
        final JdbcConnection jdbcConnection = new JdbcConnection();
        jdbcConnection.setName(mo.getName());
        jdbcConnection.setEnabled(mo.isEnabled());
        jdbcConnection.setDriverClass(mo.getDriverClass());
        jdbcConnection.setJdbcUrl(mo.getJdbcUrl());

        Map<String, Object> connectionProps = mo.getConnectionProperties();
        if (connectionProps != null) {
            Object value = connectionProps.get("user");
            if (value != null) {
                jdbcConnection.setUserName((String) value);
            }

            value = connectionProps.get("password");
            if (value != null) {
                jdbcConnection.setPassword((String) value);
            }

            Map<String, Object> additionProps = new HashMap<>(mo.getConnectionProperties());
            additionProps.remove("user");
            additionProps.remove("password");
            jdbcConnection.setAdditionalProperties(additionProps);
        }

        Map<String, Object> props = mo.getProperties();
        if (props != null) {
            Object value = props.get("maximumPoolSize");
            if (value != null) {
                jdbcConnection.setMaxPoolSize((int) value);
            }

            value = props.get("minimumPoolSize");
            if (value != null) {
                jdbcConnection.setMinPoolSize((int) value);
            }
        }

        return jdbcConnection;
    }

    /**
     * Converts the given managed object to an entity.
     *
     * @param mo the managed object
     * @return the entity
     */
    public static SecurePassword fromMangedObject (StoredPasswordMO mo) {
        // todo (kpak) : It would be good idea to reuse code from the modular GatewayManagementAssertion's
        // com.l7tech.external.assertions.gatewaymanagement.server.SecurePasswordResourceFactory#fromResource()
        //
        final SecurePassword securePassword = new SecurePassword();
        securePassword.setName(mo.getName());

        Map<String, Object> props = mo.getProperties();
        if (props != null) {
            Object value = props.get("description");
            if (value != null) {
                securePassword.setDescription((String) value);
            }

            value =  props.get("type");
            if (value!= null) {
                if ("Password".equals(value)) {
                    securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
                } else if ("PEM Private Key".equals(value)) {
                    securePassword.setType(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
                }
            }

            value = props.get("usageFromVariable");
            if (value != null) {
                securePassword.setUsageFromVariable((Boolean) value);
            }
        }

        return securePassword;
    }

    private SolutionKitUtils() {
    }
}