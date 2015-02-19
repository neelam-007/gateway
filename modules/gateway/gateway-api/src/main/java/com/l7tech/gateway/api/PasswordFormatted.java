package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 */
@XmlType(name = "PasswordFormattedType")
public class PasswordFormatted {

    //- PUBLIC

    /**
     * The password format.  Plaintext "plain" or Hashed with SHA512crypt "sha512crypt".  Plaintext by default.
     * @return
     */
    @XmlAttribute(name="format")
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * This is the bundle key that the password is encrypted with
     *
     * @return The bundle key the password is encrypted with.
     */
    @XmlAttribute(name = "bundleKey")
    public String getBundleKey() {
        return bundleKey;
    }

    /**
     * Sets the bundle key to encrypt the password with
     *
     * @param bundleKey The bundle key to encrypt the password with.
     */
    public void setBundleKey(String bundleKey) {
        this.bundleKey = bundleKey;
    }

    @XmlValue
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    //- PACKAGE

    PasswordFormatted() {
    }

    //- PRIVATE

    private String format = "plain";
    private String password;
    private String bundleKey;
}
