package com.l7tech.server.util;

import com.l7tech.server.util.PropertiesDecryptor;
import org.springframework.beans.factory.config.PropertiesFactoryBean;

import java.io.IOException;
import java.util.Properties;

/**
 * Extends PropertiesFactoryBean to support decrypting encrypted passwords.
 */
public class PasswordDecryptingPropertiesFactoryBean extends PropertiesFactoryBean {
    private final PropertiesDecryptor propertiesDecryptor;

    public PasswordDecryptingPropertiesFactoryBean(PropertiesDecryptor propertiesDecryptor) {
        this.propertiesDecryptor = propertiesDecryptor;
        if (propertiesDecryptor == null) throw new IllegalArgumentException("A PropertiesDecryptor is required.");
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
