package com.l7tech.gateway.api;

import javax.xml.bind.annotation.*;

/**
 * This holds a password
 *
 */
@XmlRootElement(name = "Password")
@XmlType(name = "PasswordType", propOrder = {"value"})
public class Password {


    private String value;

    Password() {
    }

    @XmlElement(name = "value")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
