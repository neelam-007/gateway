package com.l7tech.identity;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Sep 29, 2003
 * Time: 1:55:55 PM
 * $Id$
 *
 * An identity provider configuration is not valid
 * @see IdentityProviderConfig
 */
public class InvalidIdProviderCfgException extends Exception{
    public InvalidIdProviderCfgException() {
    }

    public InvalidIdProviderCfgException(String message) {
        super(message);
    }

    public InvalidIdProviderCfgException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidIdProviderCfgException(Throwable cause) {
        super(cause);
    }
}
