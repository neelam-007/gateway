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
}
