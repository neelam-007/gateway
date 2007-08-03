package com.l7tech.server.util;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.io.IOException;
import java.util.Properties;

/**
 * A PropertyPlaceholderConfigurer that knows how to decrypt encrypted password properties.
 */
public class PasswordDecryptingPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
    private final PropertiesDecryptor propertiesDecryptor;

    public PasswordDecryptingPropertyPlaceholderConfigurer(PropertiesDecryptor propertiesDecryptor) {
        this.propertiesDecryptor = propertiesDecryptor;
    }

    protected void loadProperties(Properties props) throws IOException {
        super.loadProperties(props);
        propertiesDecryptor.decryptEncryptedPasswords(props);
    }

    protected Properties mergeProperties() throws IOException {
        Properties props = super.mergeProperties();
        propertiesDecryptor.decryptEncryptedPasswords(props);
        return props;
    }

    public void setProperties(Properties properties) {
        super.setProperties(properties);
        propertiesDecryptor.decryptEncryptedPasswords(properties);
    }

    public void setPropertiesArray(Properties[] propertiesArray) {
        super.setPropertiesArray(propertiesArray);
        for (Properties properties : propertiesArray) {
            propertiesDecryptor.decryptEncryptedPasswords(properties);
        }
    }
}
