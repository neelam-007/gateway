package com.l7tech.identity;

/**
 * An identity provider configuration is not valid.
 * @see IdentityProviderConfig
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Sep 29, 2003<br/>
 * $Id$
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
